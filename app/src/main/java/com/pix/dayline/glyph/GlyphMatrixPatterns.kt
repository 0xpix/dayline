package com.pix.dayline.glyph

import com.pix.dayline.model.DaylineGlyphSignal
import kotlin.math.ceil

/**
 * Native 13×13 frames for the Phone (4a) Pro Glyph Matrix.
 *
 * The face is the primary visual language. Focus mode never replaces the eyes:
 * a circular perimeter remains faintly visible and completed focus pixels light
 * up one by one around that perimeter.
 */
object GlyphMatrixPatterns {
    const val SIZE = 13
    const val MAX_RAW_BRIGHTNESS = 2047

    fun frame(
        signal: DaylineGlyphSignal,
        brightness: Int = MAX_RAW_BRIGHTNESS,
        focusProgress: Float? = null,
        focusBreak: Boolean = false
    ): IntArray {
        val value = brightness.coerceIn(0, MAX_RAW_BRIGHTNESS)
        val pixels = IntArray(SIZE * SIZE)

        fun set(x: Int, y: Int, b: Int = value) {
            if (x in 0 until SIZE && y in 0 until SIZE) {
                pixels[y * SIZE + x] = b.coerceIn(0, MAX_RAW_BRIGHTNESS)
            }
        }

        fun hLine(x0: Int, x1: Int, y: Int, b: Int = value) {
            for (x in x0..x1) set(x, y, b)
        }

        fun block(x0: Int, y0: Int, w: Int, h: Int, b: Int = value) {
            for (y in y0 until y0 + h) {
                for (x in x0 until x0 + w) set(x, y, b)
            }
        }

        fun pattern(x0: Int, y0: Int, vararg rows: String) {
            rows.forEachIndexed { rowIndex, row ->
                row.forEachIndexed { columnIndex, pixel ->
                    if (pixel != '.' && pixel != ' ') {
                        set(x0 + columnIndex, y0 + rowIndex)
                    }
                }
            }
        }

        fun roundedEye(x: Int, y: Int = 4) {
            pattern(
                x, y,
                ".###.",
                "#####",
                "#####",
                "#####",
                ".###."
            )
        }

        fun centerEyes() {
            roundedEye(1)
            roundedEye(7)
        }

        fun leftEyes() {
            roundedEye(0)
            roundedEye(6)
        }

        fun rightEyes() {
            roundedEye(2)
            roundedEye(8)
        }

        fun closedEye(x: Int, y: Int = 6) {
            hLine(x, x + 4, y)
            hLine(x + 1, x + 3, y + 1)
        }

        fun blink() {
            closedEye(1)
            closedEye(7)
        }

        fun happyEye(x: Int) {
            pattern(
                x, 5,
                ".###.",
                "#####",
                "##.##"
            )
        }

        fun happy() {
            happyEye(1)
            happyEye(7)
        }

        fun sleepyEye(x: Int) {
            pattern(
                x, 5,
                "#####",
                ".###.",
                "....."
            )
        }

        fun sleepy() {
            sleepyEye(1)
            sleepyEye(7)
        }

        fun squintEyes() {
            pattern(1, 5, ".###.", "#####", ".###.")
            pattern(7, 5, ".###.", "#####", ".###.")
        }

        fun heartEye(x: Int) {
            pattern(
                x, 4,
                ".#.#.",
                "#####",
                "#####",
                ".###.",
                "..#.."
            )
        }

        fun hearts() {
            heartEye(1)
            heartEye(7)
        }

        fun largeArrow() {
            pattern(
                2, 3,
                "......#..",
                "......##.",
                "......###",
                "#########",
                "......###",
                "......##.",
                "......#.."
            )
        }

        fun focusTarget() {
            pattern(
                2, 2,
                "...###...",
                "..#...#..",
                ".#.....#.",
                "#..###..#",
                "#..###..#",
                "#..###..#",
                ".#.....#.",
                "..#...#..",
                "...###..."
            )
        }

        fun restZ() {
            pattern(
                3, 3,
                "#######",
                "#######",
                "....##.",
                "...##..",
                "..##...",
                ".##....",
                "#######",
                "#######"
            )
        }

        fun largeCheck() {
            pattern(
                2, 3,
                "........#",
                ".......##",
                "......##.",
                "#....##..",
                "##..##...",
                ".####....",
                "..##....."
            )
        }

        fun largeX() {
            pattern(
                2, 2,
                "##.....##",
                ".##...##.",
                "..##.##..",
                "...###...",
                "...###...",
                "...###...",
                "..##.##..",
                ".##...##.",
                "##.....##"
            )
        }

        fun openCircle() {
            pattern(
                2, 2,
                "..#####..",
                ".##...##.",
                "##.....##",
                "#.......#",
                "#.......#",
                "#.......#",
                "##.....##",
                ".##...##.",
                "..#####.."
            )
        }

        fun sunrise() {
            pattern(
                2, 2,
                "....#....",
                ".#..#..#.",
                ".........",
                "...###...",
                "..#####..",
                ".##...##.",
                "#########",
                ".........",
                "#########"
            )
        }

        fun emptyCalendar() {
            pattern(
                2, 2,
                "..#...#..",
                "..#...#..",
                "#########",
                "#.......#",
                "#.......#",
                "#.......#",
                "#.......#",
                "#.......#",
                "#########"
            )
        }

        fun playTriangle() {
            pattern(
                3, 3,
                "#......",
                "###....",
                "#####..",
                "#######",
                "#####..",
                "###....",
                "#......"
            )
        }

        fun largeExclamation() {
            block(5, 2, 3, 6)
            block(5, 10, 3, 2)
        }

        fun bell() {
            pattern(
                3, 2,
                "...#...",
                "..###..",
                ".#####.",
                ".#####.",
                ".#####.",
                "#######",
                ".......",
                "..###.."
            )
        }

        fun moveArrow() {
            pattern(
                2, 3,
                "..#...#..",
                ".##...##.",
                "#########",
                ".##...##.",
                "..#...#..",
                ".........",
                "..#####.."
            )
        }

        when (signal) {
            DaylineGlyphSignal.IDLE,
            DaylineGlyphSignal.CENTER -> centerEyes()

            DaylineGlyphSignal.LOOK_LEFT -> leftEyes()
            DaylineGlyphSignal.LOOK_RIGHT -> rightEyes()
            DaylineGlyphSignal.BLINK -> blink()

            DaylineGlyphSignal.WINK -> {
                roundedEye(1)
                closedEye(7)
            }

            DaylineGlyphSignal.HAPPY -> happy()
            DaylineGlyphSignal.SLEEPY -> sleepy()
            DaylineGlyphSignal.SQUINT -> squintEyes()
            DaylineGlyphSignal.HEARTS -> hearts()

            // Retained only so older queued/preference values remain harmless.
            // These expressions are no longer used by the live face or settings.
            DaylineGlyphSignal.CURIOUS,
            DaylineGlyphSignal.PLAYFUL,
            DaylineGlyphSignal.SURPRISED,
            DaylineGlyphSignal.SIDE_EYE,
            DaylineGlyphSignal.EXCITED,
            DaylineGlyphSignal.ROLLING -> centerEyes()

            // Legacy app-state frames remain for source/backward compatibility,
            // but the live Glyph service no longer displays them automatically.
            DaylineGlyphSignal.NEXT_EVENT -> largeArrow()
            DaylineGlyphSignal.REMINDER_SOON -> bell()
            DaylineGlyphSignal.FOCUS -> focusTarget()
            DaylineGlyphSignal.REST -> restZ()
            DaylineGlyphSignal.TASK_DONE -> largeCheck()
            DaylineGlyphSignal.CONFLICT -> largeX()
            DaylineGlyphSignal.NO_PLANS -> emptyCalendar()
            DaylineGlyphSignal.FREE_NOW -> openCircle()
            DaylineGlyphSignal.DAY_OPEN -> sunrise()
            DaylineGlyphSignal.EVENT_STARTED -> playTriangle()
            DaylineGlyphSignal.EVENT_ENDED -> largeCheck()
            DaylineGlyphSignal.MISSED -> largeExclamation()
            DaylineGlyphSignal.MOVED -> moveArrow()
            DaylineGlyphSignal.SYNC_OK -> largeCheck()
            DaylineGlyphSignal.SYNC_ERROR -> largeX()
            DaylineGlyphSignal.GO -> playTriangle()
        }

        focusProgress?.let { rawProgress ->
            // A true discrete circle centred on the face. The whole circle stays
            // faintly visible, then completed pixels brighten one-by-one. This
            // prevents early focus progress from looking like a random top line.
            val ring = listOf(
                5 to 0, 6 to 0, 7 to 0,
                8 to 1, 9 to 1,
                10 to 2, 11 to 3,
                11 to 4, 12 to 5, 12 to 6, 12 to 7, 11 to 8, 11 to 9,
                10 to 10, 9 to 11, 8 to 11,
                7 to 12, 6 to 12, 5 to 12,
                4 to 11, 3 to 11, 2 to 10,
                1 to 9, 1 to 8, 0 to 7, 0 to 6, 0 to 5, 1 to 4, 1 to 3,
                2 to 2, 3 to 1, 4 to 1
            )
            val ordered = if (focusBreak) ring.asReversed() else ring
            val progress = rawProgress.coerceIn(0f, 1f)
            val count = ceil(progress * ordered.size).toInt().coerceIn(0, ordered.size)

            val outlineBrightness = (value * 0.12f).toInt().coerceAtLeast(24)
            ring.forEach { (x, y) -> set(x, y, outlineBrightness) }

            val completedBrightness = if (focusBreak) {
                (value * 0.65f).toInt().coerceAtLeast(64)
            } else {
                value
            }
            for (index in 0 until count) {
                val (x, y) = ordered[index]
                set(x, y, completedBrightness)
            }

            // Keep the current progress pixel at full brightness so movement is
            // obvious even during the softer break phase.
            if (count > 0) {
                val (x, y) = ordered[count - 1]
                set(x, y, value)
            }
        }

        return pixels
    }
}
