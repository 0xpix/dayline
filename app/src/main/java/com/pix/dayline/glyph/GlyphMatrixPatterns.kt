package com.pix.dayline.glyph

import com.pix.dayline.model.DaylineGlyphSignal

/**
 * Native 13×13 frames for the Phone (4a) Pro Glyph Matrix.
 * Values are 0..255 brightness. Eyes are deliberately solid: there are no
 * black pupil holes inside the lit eye shapes.
 */
object GlyphMatrixPatterns {
    const val SIZE = 13

    fun frame(signal: DaylineGlyphSignal, brightness: Int = 255): IntArray {
        val value = brightness.coerceIn(0, 255)
        val pixels = IntArray(SIZE * SIZE)

        fun set(x: Int, y: Int, b: Int = value) {
            if (x in 0 until SIZE && y in 0 until SIZE) pixels[y * SIZE + x] = b.coerceIn(0, 255)
        }

        fun hLine(x0: Int, x1: Int, y: Int, b: Int = value) {
            for (x in x0..x1) set(x, y, b)
        }

        fun vLine(x: Int, y0: Int, y1: Int, b: Int = value) {
            for (y in y0..y1) set(x, y, b)
        }

        fun block(x0: Int, y0: Int, w: Int, h: Int, b: Int = value) {
            for (y in y0 until y0 + h) for (x in x0 until x0 + w) set(x, y, b)
        }

        fun centerEyes() {
            block(2, 4, 3, 4)
            block(8, 4, 3, 4)
        }

        fun leftEyes() {
            block(1, 4, 3, 4)
            block(7, 4, 3, 4)
        }

        fun rightEyes() {
            block(3, 4, 3, 4)
            block(9, 4, 3, 4)
        }

        fun blink() {
            hLine(2, 4, 6)
            hLine(8, 10, 6)
        }

        fun happy() {
            set(2, 6); set(3, 5); set(4, 5); set(5, 6)
            set(7, 6); set(8, 5); set(9, 5); set(10, 6)
        }

        fun sleepy() {
            hLine(2, 4, 6); set(5, 7)
            set(7, 7); hLine(8, 10, 6)
        }

        fun focusEyes() {
            hLine(2, 4, 6)
            hLine(8, 10, 6)
            set(3, 5); set(9, 5)
        }

        fun tinyCheck() {
            set(9, 9); set(10, 10); set(11, 9); set(12, 8)
        }

        fun rightArrow() {
            set(11, 5); set(12, 6); set(11, 7)
        }

        when (signal) {
            DaylineGlyphSignal.IDLE,
            DaylineGlyphSignal.CENTER -> centerEyes()

            DaylineGlyphSignal.LOOK_LEFT -> leftEyes()
            DaylineGlyphSignal.LOOK_RIGHT -> rightEyes()
            DaylineGlyphSignal.BLINK -> blink()

            DaylineGlyphSignal.WINK -> {
                block(2, 4, 3, 4)
                hLine(8, 10, 6)
            }

            DaylineGlyphSignal.HAPPY -> happy()

            DaylineGlyphSignal.EXCITED -> {
                happy()
                set(2, 4); set(5, 5); set(7, 5); set(10, 4)
            }

            DaylineGlyphSignal.SLEEPY -> sleepy()

            DaylineGlyphSignal.SURPRISED -> {
                block(2, 3, 3, 5)
                block(8, 3, 3, 5)
            }

            DaylineGlyphSignal.PLAYFUL -> {
                block(2, 4, 3, 4)
                set(8, 7); set(9, 6); set(10, 5); set(11, 6)
            }

            DaylineGlyphSignal.CURIOUS -> {
                block(1, 3, 4, 4)
                block(8, 5, 3, 3)
            }

            DaylineGlyphSignal.SIDE_EYE -> {
                hLine(1, 4, 6)
                hLine(7, 10, 6)
                set(1, 5); set(7, 5)
            }

            DaylineGlyphSignal.ROLLING -> {
                block(2, 3, 3, 3)
                block(8, 6, 3, 3)
            }

            DaylineGlyphSignal.SQUINT -> blink()

            DaylineGlyphSignal.HEARTS -> {
                // Two compact pixel hearts.
                set(1, 4); set(3, 4); hLine(1, 4, 5); hLine(1, 4, 6)
                set(2, 7); set(3, 7)
                set(8, 4); set(10, 4); hLine(8, 11, 5); hLine(8, 11, 6)
                set(9, 7); set(10, 7)
            }

            DaylineGlyphSignal.NEXT_EVENT -> {
                centerEyes(); rightArrow()
            }

            DaylineGlyphSignal.REMINDER_SOON -> {
                centerEyes()
                // Tiny alert/bell cue in the top-right corner.
                set(11, 1); set(10, 2); set(11, 2); set(12, 2); set(11, 3)
            }

            DaylineGlyphSignal.FOCUS -> {
                focusEyes()
                // Partial progress ring around the eyes.
                hLine(3, 9, 1); hLine(3, 9, 11)
                vLine(1, 3, 9); set(2, 2); set(10, 2); set(11, 3); set(11, 4)
            }

            DaylineGlyphSignal.REST -> {
                sleepy()
                hLine(4, 8, 10)
            }

            DaylineGlyphSignal.TASK_DONE -> {
                happy(); tinyCheck()
            }

            DaylineGlyphSignal.CONFLICT -> {
                focusEyes()
                // Two overlapping mini squares.
                hLine(9, 11, 1); vLine(9, 1, 3); vLine(11, 1, 3)
                hLine(10, 12, 2); vLine(10, 2, 4); vLine(12, 2, 4); hLine(10, 12, 4)
            }

            DaylineGlyphSignal.NO_PLANS -> {
                happy()
                // Tiny empty-day marker.
                hLine(5, 7, 2); set(4, 3); set(8, 3); hLine(5, 7, 4)
            }

            DaylineGlyphSignal.FREE_NOW,
            DaylineGlyphSignal.DAY_OPEN -> {
                sleepy()
                // Empty/open schedule slot.
                hLine(4, 8, 2); set(3, 3); set(9, 3); hLine(4, 8, 4)
            }

            DaylineGlyphSignal.EVENT_STARTED -> {
                centerEyes(); hLine(1, 3, 10); set(4, 9); set(5, 8)
            }

            DaylineGlyphSignal.EVENT_ENDED -> {
                happy(); hLine(9, 11, 10)
            }

            DaylineGlyphSignal.MISSED -> {
                sleepy(); set(11, 2); set(11, 3); set(11, 5)
            }

            DaylineGlyphSignal.MOVED -> {
                centerEyes(); set(1, 10); hLine(2, 4, 10); set(5, 9)
            }

            DaylineGlyphSignal.SYNC_OK -> {
                centerEyes(); tinyCheck()
            }

            DaylineGlyphSignal.SYNC_ERROR -> {
                focusEyes(); set(11, 2); set(11, 3); set(11, 5)
            }

            DaylineGlyphSignal.GO -> {
                rightEyes(); rightArrow()
            }
        }

        return pixels
    }
}
