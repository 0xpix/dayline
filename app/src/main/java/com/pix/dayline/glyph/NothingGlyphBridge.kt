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
 * The Matrix SDK owns a bound proxy service. Keep that lifecycle deliberately
 * conservative: initialize once while the Toy is bound, retain only the newest
 * pending frame while disconnected, and give the system service time to recover
 * naturally. Only if it stays disconnected does Dayline perform a clean
 * unInit -> init recovery with backoff.
 */
class NothingGlyphBridge(
    private val context: Context,
    private val appMatrix: Boolean
) {
    private var manager: Any? = null
    private var callback: Any? = null
    private var connected = false
    private var connecting = false
    private var closed = false
    private var pendingFrame: IntArray? = null
    private var lastDeliveredFrame: IntArray? = null
    private var connectionGeneration = 0L
    private var recoveryScheduled = false
    private var recoveryDelayMs = INITIAL_RECOVERY_DELAY_MS
    private var onReadyCallback: (() -> Unit)? = null

    private val mainHandler = Handler(Looper.getMainLooper())
    private val recoveryRunnable = Runnable {
        recoveryScheduled = false
        if (closed || connected) return@Runnable

        Log.w(TAG, "Glyph Matrix still disconnected; rebuilding SDK binding")
        teardownBinding()
        recoveryDelayMs = (recoveryDelayMs * 2L).coerceAtMost(MAX_RECOVERY_DELAY_MS)
        initializeBinding()
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
                detail = if (nothing) "Dayline targets the 13×13 Phone (4a) Pro matrix." else null
            )
        }
    }

    fun connect(onReady: (() -> Unit)? = null) {
        if (onReady != null) onReadyCallback = onReady
        closed = false

        if (connected) {
            onReady?.invoke()
            return
        }
        if (connecting) return

        // If init() already established the SDK binding, do not churn it just
        // because the proxy service is temporarily disconnected. Android/Nothing
        // may reconnect the existing binding on its own.
        if (manager != null) {
            scheduleRecovery()
            return
        }

        initializeBinding()
    }

    /**
     * @return true only when the frame is already current or reached the SDK
     * immediately. A false result means the newest frame is retained for
     * delivery after the existing binding reconnects or the conservative
     * recovery watchdog fires.
     */
    fun show(frame: IntArray): Boolean {
        if (frame.size != GlyphMatrixPatterns.SIZE * GlyphMatrixPatterns.SIZE) return false

        if (!connected) {
            pendingFrame = frame.copyOf()
            if (manager == null && !connecting) {
                connect()
            } else {
                scheduleRecovery()
            }
            return false
        }

        if (sendFrame(frame)) return true

        // A frame-send exception is a real transport failure, but repeatedly
        // tearing down/rebinding every render tick made the Matrix less stable.
        // Queue the newest frame and allow one delayed recovery attempt.
        pendingFrame = frame.copyOf()
        lastDeliveredFrame = null
        connected = false
        connecting = false
        recoveryDelayMs = INITIAL_RECOVERY_DELAY_MS
        scheduleRecovery()
        return false
    }

    fun close() {
        closed = true
        mainHandler.removeCallbacks(recoveryRunnable)
        recoveryScheduled = false
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
            }.onFailure { Log.w(TAG, "Unable to close Glyph Matrix display", it) }
            safeUnInit(instance)
        }

        connected = false
        connecting = false
        pendingFrame = null
        lastDeliveredFrame = null
        manager = null
        callback = null
        onReadyCallback = null
        recoveryDelayMs = INITIAL_RECOVERY_DELAY_MS
    }

    private fun initializeBinding() {
        if (closed || connected || connecting) return

        connecting = true
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
                // SDK callbacks may arrive on a binder thread. All bridge state
                // is owned by the main looper to avoid callback/render races.
                mainHandler.post {
                    if (closed || generation != connectionGeneration || manager !== instance) {
                        return@post
                    }
                    when (method.name) {
                        "onServiceConnected" -> handleServiceConnected(instance, managerClass)
                        "onServiceDisconnected" -> handleServiceDisconnected()
                    }
                }
                null
            }
            callback = proxy
            managerClass.getMethod("init", callbackClass).invoke(instance, proxy)

            // If the SDK never delivers onServiceConnected, recover later. Do
            // not repeatedly re-init on every requested animation frame.
            scheduleRecovery()
        }.onFailure {
            Log.w(TAG, "Glyph Matrix bridge unavailable", it)
            val instance = manager
            if (instance != null) safeUnInit(instance)
            connecting = false
            connected = false
            lastDeliveredFrame = null
            manager = null
            callback = null
            scheduleRecovery()
        }
    }

    private fun handleServiceConnected(instance: Any, managerClass: Class<*>) {
        val registered = runCatching { register(instance, managerClass) }
            .onFailure { Log.w(TAG, "Glyph registration failed", it) }
            .isSuccess

        connecting = false
        if (!registered) {
            connected = false
            lastDeliveredFrame = null
            recoveryDelayMs = INITIAL_RECOVERY_DELAY_MS
            scheduleRecovery()
            return
        }

        connected = true
        recoveryDelayMs = INITIAL_RECOVERY_DELAY_MS
        mainHandler.removeCallbacks(recoveryRunnable)
        recoveryScheduled = false

        val pending = pendingFrame
        if (pending != null) {
            if (sendFrame(pending)) {
                pendingFrame = null
            } else {
                connected = false
                lastDeliveredFrame = null
                scheduleRecovery()
                return
            }
        }

        onReadyCallback?.invoke()
    }

    private fun handleServiceDisconnected() {
        Log.w(TAG, "Glyph Matrix service disconnected; waiting for proxy recovery")
        connected = false
        connecting = false
        lastDeliveredFrame = null
        recoveryDelayMs = INITIAL_RECOVERY_DELAY_MS

        // Keep manager/callback alive here. The SDK binding can reconnect
        // without Dayline calling init() again. The watchdog only rebuilds the
        // binding if that natural recovery does not happen in time.
        scheduleRecovery()
    }

    private fun sendFrame(frame: IntArray): Boolean {
        val previous = lastDeliveredFrame
        if (previous != null && previous.contentEquals(frame)) return true

        val instance = manager ?: return false
        return runCatching {
            val name = if (appMatrix) "setAppMatrixFrame" else "setMatrixFrame"
            val method = instance.javaClass.methods.firstOrNull {
                it.name == name && it.parameterTypes.size == 1 && it.parameterTypes[0] == IntArray::class.java
            } ?: error("$name(int[]) not available")
            method.invoke(instance, frame)
            lastDeliveredFrame = frame.copyOf()
            true
        }.onFailure {
            Log.w(TAG, "Unable to send Glyph Matrix frame", it)
        }.getOrDefault(false)
    }

    private fun teardownBinding() {
        mainHandler.removeCallbacks(recoveryRunnable)
        recoveryScheduled = false
        connectionGeneration++

        manager?.let(::safeUnInit)
        connected = false
        connecting = false
        lastDeliveredFrame = null
        manager = null
        callback = null
    }

    private fun safeUnInit(instance: Any) {
        runCatching {
            instance.javaClass.methods
                .firstOrNull { it.name == "unInit" && it.parameterCount == 0 }
                ?.invoke(instance)
        }.onFailure { Log.w(TAG, "Unable to unInit Glyph Matrix bridge", it) }
    }

    private fun scheduleRecovery() {
        if (closed || connected || recoveryScheduled) return
        recoveryScheduled = true
        mainHandler.postDelayed(recoveryRunnable, recoveryDelayMs)
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
        private const val INITIAL_RECOVERY_DELAY_MS = 5_000L
        private const val MAX_RECOVERY_DELAY_MS = 30_000L
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
