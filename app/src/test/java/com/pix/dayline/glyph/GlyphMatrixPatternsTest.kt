package com.pix.dayline.glyph

import com.pix.dayline.model.DaylineGlyphSignal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GlyphMatrixPatternsTest {
    private fun lit(frame: IntArray): Set<Pair<Int, Int>> = buildSet {
        for (y in 0 until GlyphMatrixPatterns.SIZE) {
            for (x in 0 until GlyphMatrixPatterns.SIZE) {
                if (frame[y * GlyphMatrixPatterns.SIZE + x] > 0) add(x to y)
            }
        }
    }

    private fun pixel(frame: IntArray, x: Int, y: Int): Int =
        frame[y * GlyphMatrixPatterns.SIZE + x]

    @Test
    fun centerEyesStaySolidAndSymmetric() {
        val frame = GlyphMatrixPatterns.frame(DaylineGlyphSignal.CENTER)
        val lit = lit(frame)

        assertEquals(42, lit.size)
        assertTrue(pixel(frame, 3, 6) > 0)
        assertTrue(pixel(frame, 9, 6) > 0)

        val mirrored = lit.map { (x, y) -> (12 - x) to y }.toSet()
        assertEquals(lit, mirrored)
    }

    @Test
    fun lookDirectionsShiftTheWholeFaceExactlyOneColumn() {
        val center = lit(GlyphMatrixPatterns.frame(DaylineGlyphSignal.CENTER))
        val left = lit(GlyphMatrixPatterns.frame(DaylineGlyphSignal.LOOK_LEFT))
        val right = lit(GlyphMatrixPatterns.frame(DaylineGlyphSignal.LOOK_RIGHT))

        assertEquals(center.map { (x, y) -> (x - 1) to y }.toSet(), left)
        assertEquals(center.map { (x, y) -> (x + 1) to y }.toSet(), right)
    }

    @Test
    fun removedLegacyExpressionsFallBackToCenter() {
        val center = GlyphMatrixPatterns.frame(DaylineGlyphSignal.CENTER)

        listOf(
            DaylineGlyphSignal.CURIOUS,
            DaylineGlyphSignal.PLAYFUL,
            DaylineGlyphSignal.SURPRISED,
            DaylineGlyphSignal.SIDE_EYE,
            DaylineGlyphSignal.EXCITED,
            DaylineGlyphSignal.ROLLING
        ).forEach { signal ->
            assertTrue(center.contentEquals(GlyphMatrixPatterns.frame(signal)))
        }
    }

    @Test
    fun focusTimerKeepsTheOuterColumnsClear() {
        val frame = GlyphMatrixPatterns.frame(
            signal = DaylineGlyphSignal.CENTER,
            focusRemainingSeconds = 5 * 60L + 7L
        )

        for (y in 0 until GlyphMatrixPatterns.SIZE) {
            assertEquals(0, pixel(frame, 0, y))
            assertEquals(0, pixel(frame, 1, y))
            assertEquals(0, pixel(frame, 11, y))
            assertEquals(0, pixel(frame, 12, y))
        }
    }

    @Test
    fun brightnessIsClampedToTheRawMatrixRange() {
        val full = GlyphMatrixPatterns.frame(
            DaylineGlyphSignal.CENTER,
            brightness = GlyphMatrixPatterns.MAX_RAW_BRIGHTNESS + 5_000
        )
        val off = GlyphMatrixPatterns.frame(
            DaylineGlyphSignal.CENTER,
            brightness = -100
        )

        assertEquals(
            GlyphMatrixPatterns.MAX_RAW_BRIGHTNESS,
            full.maxOrNull()
        )
        assertTrue(off.all { it == 0 })
    }
}
