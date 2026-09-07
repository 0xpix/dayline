package com.pix.dayline.glyph

import android.content.Context

data class GlyphDiagnosticsSnapshot(
    val lastFrameAt: Long?,
    val disconnectCount: Int,
    val recoveryCount: Int,
    val sendFailureCount: Int,
    val lastError: String?
)

/** Tiny local-only counters for beta diagnostics; no frame contents are stored. */
class GlyphDiagnosticsStore(context: Context) {
    private val prefs = context.getSharedPreferences("dayline_glyph_diagnostics", Context.MODE_PRIVATE)

    fun recordFrame(epochMillis: Long = System.currentTimeMillis()) {
        prefs.edit().putLong(KEY_LAST_FRAME, epochMillis).apply()
    }

    fun recordDisconnect() = increment(KEY_DISCONNECTS)
    fun recordRecovery() = increment(KEY_RECOVERIES)

    fun recordSendFailure(error: Throwable? = null) {
        val editor = prefs.edit().putInt(KEY_SEND_FAILURES, prefs.getInt(KEY_SEND_FAILURES, 0) + 1)
        error?.message?.takeIf { it.isNotBlank() }?.let { editor.putString(KEY_LAST_ERROR, it.take(160)) }
        editor.apply()
    }

    fun snapshot(): GlyphDiagnosticsSnapshot = GlyphDiagnosticsSnapshot(
        lastFrameAt = prefs.getLong(KEY_LAST_FRAME, -1L).takeIf { it >= 0L },
        disconnectCount = prefs.getInt(KEY_DISCONNECTS, 0),
        recoveryCount = prefs.getInt(KEY_RECOVERIES, 0),
        sendFailureCount = prefs.getInt(KEY_SEND_FAILURES, 0),
        lastError = prefs.getString(KEY_LAST_ERROR, null)?.takeIf { it.isNotBlank() }
    )

    private fun increment(key: String) {
        prefs.edit().putInt(key, prefs.getInt(key, 0) + 1).apply()
    }

    private companion object {
        const val KEY_LAST_FRAME = "last_frame_at"
        const val KEY_DISCONNECTS = "disconnect_count"
        const val KEY_RECOVERIES = "recovery_count"
        const val KEY_SEND_FAILURES = "send_failure_count"
        const val KEY_LAST_ERROR = "last_error"
    }
}
