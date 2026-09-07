package com.pix.dayline.glyph

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.pix.dayline.model.GlyphHardwareStatus
import java.lang.reflect.Proxy

/**
 * Reflection keeps the open-source tree buildable when Nothing's closed-source
 * Glyph Matrix AAR is absent. GitHub Actions downloads the official AAR before
 * building distributable APKs, so these classes are present in those APKs.
 *
 * The Nothing service can be restarted by the OS while an AOD toy is alive.
 * Dayline therefore treats every disconnect/send failure as recoverable and
 * reconnects while retaining only the newest pending frame.
 */
class NothingGlyphBridge(
    private val context: Context,
    private val appMatrix: Boolean
) {
    private var manager: Any? = null
    private var callback: Any? = null
    private var connected = false
    private var connecting = false
    private var pendingFrame: IntArray? = null
    private var connectionGeneration = 0L

    private val reconnectHandler = Handler(Looper.getMainLooper())
    private val reconnectRunnable = Runnable {
        if (!connected) {
            invalidateConnection()
            connect()
        }
    }

    fun status(): GlyphHardwareStatus {
        val managerPresent = classExists(MANAGER_CLASS)
        if (!managerPresent) {
            return GlyphHardwareStatus(
                available = false,
                detail = "Glyph Matrix SDK is not bundled in this build."
            )
        }

        val matrix = detectMatrixSize()
        val nothing = Build.MANUFACTURER.contains("nothing", ignoreCase = true) ||
            Build.BRAND.contains("nothing", ignoreCase = true)
        return if (matrix == 13 || (nothing && Build.MODEL.contains("4a", ignoreCase = true))) {
            GlyphHardwareStatus(
                available = true,
                matrixSize = matrix ?: 13,
                deviceLabel = "Nothing Phone (4a) Pro",
                detail = "13×13 Glyph Matrix"
            )
        } else {
            GlyphHardwareStatus(
                available = false,
                matrixSize = matrix,
                deviceLabel = if (nothing) Build.MODEL else "Unsupported device",
                detail = if (nothing) "Dayline v0.14 targets the 13×13 Phone (4a) Pro matrix." else null
            )
        }
    }

    fun connect(onReady: (() -> Unit)? = null) {
        if (connected) {
            onReady?.invoke()
            return
        }
        if (connecting) return

        // A non-null manager while disconnected is stale. This was the old
        // freeze path: show() queued frames forever while connect() returned.
        if (manager != null) invalidateConnection()

        connecting = true
        reconnectHandler.removeCallbacks(reconnectRunnable)
        val generation = ++connectionGeneration

        runCatching {
            val managerClass = Class.forName(MANAGER_CLASS)
            val callbackClass = Class.forName("$MANAGER_CLASS\$Callback")
            val instance = managerClass
                .getMethod("getInstance", Context::class.java)
                .invoke(null, context.applicationContext)
            manager = instance

            val proxy = Proxy.newProxyInstance(
                callbackClass.classLoader,
                arrayOf(callbackClass)
            ) { _, method, _ ->
                if (generation != connectionGeneration) return@newProxyInstance null

                when (method.name) {
                    "onServiceConnected" -> {
                        val registered = runCatching { register(instance, managerClass) }
                            .onFailure { Log.w(TAG, "Glyph registration failed", it) }
                            .isSuccess

                        connecting = false
                        if (!registered) {
                            connected = false
                            scheduleReconnect()
                            return@newProxyInstance null
                        }

                        connected = true
                        reconnectHandler.removeCallbacks(reconnectRunnable)

                        val pending = pendingFrame
                        if (pending != null) {
                            if (sendFrame(pending)) {
                                pendingFrame = null
                            } else {
                                pendingFrame = pending.copyOf()
                                recoverConnection()
                                return@newProxyInstance null
                            }
                        }
                        onReady?.invoke()
                    }

                    "onServiceDisconnected" -> {
                        Log.w(TAG, "Glyph Matrix service disconnected; reconnecting")
                        recoverConnection()
                    }
                }
                null
            }
            callback = proxy
            managerClass.getMethod("init", callbackClass).invoke(instance, proxy)
        }.onFailure {
            Log.w(TAG, "Glyph Matrix bridge unavailable", it)
            connecting = false
            connected = false
            manager = null
            callback = null
            scheduleReconnect()
        }
    }

    /**
     * @return true only when the frame reached the SDK call immediately.
     * A false result means the newest frame was retained and reconnection is in
     * progress; callers should retry rather than treating it as delivered.
     */
    fun show(frame: IntArray): Boolean {
        if (frame.size != GlyphMatrixPatterns.SIZE * GlyphMatrixPatterns.SIZE) return false

        if (!connected) {
            pendingFrame = frame.copyOf()
            connect()
            return false
        }

        if (sendFrame(frame)) return true

        pendingFrame = frame.copyOf()
        recoverConnection()
        return false
    }

    fun close() {
        reconnectHandler.removeCallbacks(reconnectRunnable)
        connectionGeneration++

        val instance = manager
        if (instance != null) {
            runCatching {
                val cls = instance.javaClass
                if (appMatrix) {
                    cls.methods.firstOrNull { it.name == "closeAppMatrix" && it.parameterCount == 0 }
                        ?.invoke(instance)
                } else {
                    cls.methods.firstOrNull { it.name == "turnOff" && it.parameterCount == 0 }
                        ?.invoke(instance)
                }
                cls.methods.firstOrNull { it.name == "unInit" && it.parameterCount == 0 }
                    ?.invoke(instance)
            }
        }

        connected = false
        connecting = false
        pendingFrame = null
        manager = null
        callback = null
    }

    private fun sendFrame(frame: IntArray): Boolean {
        val instance = manager ?: return false
        return runCatching {
            val name = if (appMatrix) "setAppMatrixFrame" else "setMatrixFrame"
            val method = instance.javaClass.methods.firstOrNull {
                it.name == name && it.parameterTypes.size == 1 && it.parameterTypes[0] == IntArray::class.java
            } ?: error("$name(int[]) not available")
            method.invoke(instance, frame)
            true
        }.onFailure {
            Log.w(TAG, "Unable to send Glyph Matrix frame; reconnecting", it)
        }.getOrDefault(false)
    }

    private fun recoverConnection() {
        invalidateConnection()
        scheduleReconnect()
    }

    private fun invalidateConnection() {
        connectionGeneration++
        connected = false
        connecting = false
        manager = null
        callback = null
    }

    private fun scheduleReconnect() {
        reconnectHandler.removeCallbacks(reconnectRunnable)
        reconnectHandler.postDelayed(reconnectRunnable, RECONNECT_DELAY_MS)
    }

    private fun register(instance: Any, managerClass: Class<*>) {
        val glyphClass = Class.forName(GLYPH_CLASS)
        // Nothing documents DEVICE_25111p for Phone (4a) Pro, but older SDK
        // binaries have shipped without that field. Fall back to the documented
        // target identifier so Dayline remains compatible while the SDK catches up.
        val target = runCatching {
            glyphClass.getField("DEVICE_25111p").get(null) as String
        }.getOrElse { "25111p" }
        managerClass.getMethod("register", String::class.java).invoke(instance, target)
    }

    private fun detectMatrixSize(): Int? = runCatching {
        val common = Class.forName(COMMON_CLASS)
        val method = common.methods.firstOrNull {
            it.name == "getDeviceMatrixLength" && it.parameterCount == 0
        } ?: return@runCatching null
        (method.invoke(null) as? Number)?.toInt()
    }.getOrNull()

    companion object {
        private const val TAG = "DaylineGlyph"
        private const val RECONNECT_DELAY_MS = 750L
        private const val MANAGER_CLASS = "com.nothing.ketchum.GlyphMatrixManager"
        private const val GLYPH_CLASS = "com.nothing.ketchum.Glyph"
        private const val COMMON_CLASS = "com.nothing.ketchum.Common"

        fun classExists(name: String): Boolean = runCatching { Class.forName(name) }.isSuccess

        fun openToyManager(context: Context): Boolean = runCatching {
            val intent = Intent().apply {
                component = ComponentName(
                    "com.nothing.thirdparty",
                    "com.nothing.thirdparty.matrix.toys.manager.ToysManagerActivity"
                )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }
}
