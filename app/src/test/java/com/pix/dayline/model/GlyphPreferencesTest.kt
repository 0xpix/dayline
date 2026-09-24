package com.pix.dayline.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class GlyphPreferencesTest {
    @Test
    fun currentDefaultsAreEyesFirst() {
        val defaults = GlyphPreferences()

        assertEquals(GlyphMode.EYES_ONLY, defaults.mode)
        assertEquals(GlyphGlanceFrequency.NORMAL, defaults.glanceFrequency)
        assertFalse(defaults.showAppStates)
        assertFalse(defaults.restAnimation)
    }

    @Test
    fun legacyEnabledModeNormalizesToEyesOnly() {
        val legacy = GlyphPreferences(
            mode = GlyphMode.EYES_AND_STATES,
            showAppStates = true,
            restAnimation = true
        )

        val normalized = legacy.normalizedForCurrentGlyph()

        assertEquals(GlyphMode.EYES_ONLY, normalized.mode)
        assertFalse(normalized.showAppStates)
        assertFalse(normalized.restAnimation)
    }

    @Test
    fun disabledGlyphRemainsDisabledDuringNormalization() {
        val disabled = GlyphPreferences(
            mode = GlyphMode.OFF,
            showAppStates = true,
            restAnimation = true
        )

        val normalized = disabled.normalizedForCurrentGlyph()

        assertEquals(GlyphMode.OFF, normalized.mode)
        assertFalse(normalized.showAppStates)
        assertFalse(normalized.restAnimation)
    }

    @Test
    fun normalizationPreservesCurrentUserChoices() {
        val current = GlyphPreferences(
            mode = GlyphMode.EYES_ONLY,
            blinkEnabled = false,
            randomGlancesEnabled = false,
            glanceFrequency = GlyphGlanceFrequency.FREQUENT,
            reduceMotion = true
        )

        assertEquals(current, current.normalizedForCurrentGlyph())
    }
}
