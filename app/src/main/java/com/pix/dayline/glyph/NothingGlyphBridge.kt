package com.pix.dayline.glyph

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.pix.dayline.model.GlyphHardwareStatus
import java.lang.reflect.Proxy

/**
 * Reflection keeps the open-source tree buildable when Nothing's closed-source
 * Glyph Matrix AAR is absent. GitHub Actions downloads the official AAR before
 * building distributable APKs, so these classes are present in those APKs.
 */
class NothingGlyphBridge(
    private val context: Context,
    private val appMatrix: Boolean
) {
    private var manager: Any? = null
    private var callback: Any? = null
    private var connected = false
    private var pendingFrame: IntArray? = null

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
        if (manager != null) return
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
                when (method.name) {
                    "onServiceConnected" -> {
                        runCatching { register(instance, managerClass) }
                            .onFailure { Log.w(TAG, "Glyph registration failed", it) }
                        connected = true
                        pendingFrame?.let { sendFrame(it) }
                        pendingFrame = null
                        onReady?.invoke()
                    }
                    "onServiceDisconnected" -> connected = false
                }
                null
            }
            callback = proxy
            managerClass.getMethod("init", callbackClass).invoke(instance, proxy)
        }.onFailure {
            Log.w(TAG, "Glyph Matrix bridge unavailable", it)
            manager = null
            callback = null
            connected = false
        }
    }

    fun show(frame: IntArray) {
        if (frame.size != GlyphMatrixPatterns.SIZE * GlyphMatrixPatterns.SIZE) return
        if (manager == null) connect()
        if (!connected) {
            pendingFrame = frame.copyOf()
            return
        }
        sendFrame(frame)
    }

    fun close() {
        val instance = manager ?: return
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
        connected = false
        pendingFrame = null
        manager = null
        callback = null
    }

    private fun sendFrame(frame: IntArray) {
        val instance = manager ?: return
        runCatching {
            val name = if (appMatrix) "setAppMatrixFrame" else "setMatrixFrame"
            val method = instance.javaClass.methods.firstOrNull {
                it.name == name && it.parameterTypes.size == 1 && it.parameterTypes[0] == IntArray::class.java
            } ?: error("$name(int[]) not available")
            method.invoke(instance, frame)
        }.onFailure { Log.w(TAG, "Unable to send Glyph Matrix frame", it) }
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
