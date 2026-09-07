package com.pix.dayline.glyph

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.pix.dayline.data.DaylineStore
import com.pix.dayline.model.*

class DaylineGlyphController(context: Context) {
    private val appContext = context.applicationContext
    private val bridge = NothingGlyphBridge(appContext, appMatrix = true)
    private val handler = Handler(Looper.getMainLooper())
    private val store = DaylineStore(appContext)

    fun status(): GlyphHardwareStatus = bridge.status()

    fun preview(signal: DaylineGlyphSignal, durationMillis: Long = 3_000L) {
        val prefs = store.loadGlyphPreferences()
        val brightness = if (prefs.dimAtNight && prefs.quietHoursEnabled &&
            GlyphStateResolver.inQuietHours(
                java.time.LocalTime.now(),
                prefs.quietStart,
                prefs.quietEnd
            )
        ) 96 else 255
        bridge.show(GlyphMatrixPatterns.frame(signal, brightness))
        handler.removeCallbacksAndMessages(TOKEN)
        handler.postAtTime({ bridge.close() }, TOKEN, android.os.SystemClock.uptimeMillis() + durationMillis)
    }

    fun blinkPreview() {
        val prefs = store.loadGlyphPreferences()
        val quiet = prefs.dimAtNight && prefs.quietHoursEnabled &&
            GlyphStateResolver.inQuietHours(java.time.LocalTime.now(), prefs.quietStart, prefs.quietEnd)
        val brightness = if (quiet) 96 else 255
        handler.removeCallbacksAndMessages(null)
        bridge.show(GlyphMatrixPatterns.frame(DaylineGlyphSignal.CENTER, brightness))
        handler.postDelayed({ bridge.show(GlyphMatrixPatterns.frame(DaylineGlyphSignal.BLINK, brightness)) }, 120L)
        handler.postDelayed({ bridge.show(GlyphMatrixPatterns.frame(DaylineGlyphSignal.CENTER, brightness)) }, 280L)
        handler.postDelayed({ bridge.close() }, 2_200L)
    }

    fun close() {
        handler.removeCallbacksAndMessages(null)
        bridge.close()
    }

    private companion object { val TOKEN = Any() }
}
