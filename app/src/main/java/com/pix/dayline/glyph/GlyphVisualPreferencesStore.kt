package com.pix.dayline.glyph

import android.content.Context
import kotlin.math.roundToInt

/**
 * Small visual-only preference store for the Nothing Glyph Matrix.
 *
 * TEST GLYPH is the legacy validation name for the in-app expression preview;
 * the user-facing settings now call it Preview expressions.
 *
 * Dayline sends raw IntArray frames to GlyphMatrixManager. Nothing's official
 * example project uses raw values up to 2046, so this store uses the native
 * 0..2047 raw range instead of the old 0..255 object-brightness range.
 */
class GlyphVisualPreferencesStore(context: Context) {
    private val prefs = context.getSharedPreferences("dayline_glyph_visual", Context.MODE_PRIVATE)

    fun loadBrightness(): Int {
        val stored = prefs.getInt(KEY_BRIGHTNESS, DEFAULT_BRIGHTNESS)

        // Transparently migrate the old Dayline slider. An old saved 255
        // (100%) therefore becomes the new raw maximum 2047.
        if (stored in 0..LEGACY_MAX_BRIGHTNESS) {
            val migrated = ((stored / LEGACY_MAX_BRIGHTNESS.toFloat()) * MAX_BRIGHTNESS)
                .roundToInt()
                .coerceIn(MIN_BRIGHTNESS, MAX_BRIGHTNESS)
            prefs.edit().putInt(KEY_BRIGHTNESS, migrated).apply()
            return migrated
        }

        return stored.coerceIn(MIN_BRIGHTNESS, MAX_BRIGHTNESS)
    }

    fun saveBrightness(value: Int) {
        prefs.edit()
            .putInt(KEY_BRIGHTNESS, value.coerceIn(MIN_BRIGHTNESS, MAX_BRIGHTNESS))
            .apply()
    }

    companion object {
        private const val KEY_BRIGHTNESS = "brightness"
        private const val LEGACY_MAX_BRIGHTNESS = 255

        const val MIN_BRIGHTNESS = 256
        const val MAX_BRIGHTNESS = 2047
        const val DEFAULT_BRIGHTNESS = 2047
        const val BRIGHTNESS_STEP = 256
    }
}
