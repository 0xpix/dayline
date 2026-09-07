package com.pix.dayline.glyph

import android.content.Context

/** Small visual-only preference store for the Nothing Glyph Matrix. */
class GlyphVisualPreferencesStore(context: Context) {
    private val prefs = context.getSharedPreferences("dayline_glyph_visual", Context.MODE_PRIVATE)

    fun loadBrightness(): Int = prefs
        .getInt(KEY_BRIGHTNESS, DEFAULT_BRIGHTNESS)
        .coerceIn(MIN_BRIGHTNESS, MAX_BRIGHTNESS)

    fun saveBrightness(value: Int) {
        prefs.edit()
            .putInt(KEY_BRIGHTNESS, value.coerceIn(MIN_BRIGHTNESS, MAX_BRIGHTNESS))
            .apply()
    }

    companion object {
        private const val KEY_BRIGHTNESS = "brightness"
        const val MIN_BRIGHTNESS = 96
        const val MAX_BRIGHTNESS = 255
        const val DEFAULT_BRIGHTNESS = 255
    }
}
