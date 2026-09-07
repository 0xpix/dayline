package com.pix.dayline.glyph

import com.pix.dayline.model.DaylineGlyphSignal

/**
 * Native 13×13 frames for the Phone (4a) Pro Glyph Matrix.
 *
 * Visual rule:
 * - idle personality uses large, solid eyes;
 * - short Dayline app states use one bold centered symbol;
 * - never squeeze a tiny status icon beside the eyes.
 *
 * This keeps the face dominant during normal use while making the brief app
 * states readable from a glance on a low-resolution 13×13 matrix.
 */
object GlyphMatrixPatterns {
    const val SIZE = 13

    fun frame(signal: DaylineGlyphSignal, brightness: Int = 255): IntArray {
        val value = brightness.coerceIn(0, 255)
        val pixels = IntArray(SIZE * SIZE)

        fun set(x: Int, y: Int, b: Int = value) {
            if (x in 0 until SIZE && y in 0 until SIZE) {
                pixels[y * SIZE + x] = b.coerceIn(0, 255)
            }
        }

        fun hLine(x0: Int, x1: Int, y: Int, b: Int = value) {
            for (x in x0..x1) set(x, y, b)
        }

        fun vLine(x: Int, y0: Int, y1: Int, b: Int = value) {
            for (y in y0..y1) set(x, y, b)
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

        // The base eye is intentionally large and solid, matching the Dot-style
        // silhouette the Dayline face is built around:
        // .###.
        // #####
        // #####
        // #####
        // .###.
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
                ".....",
                "#####",
                ".###."
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

            DaylineGlyphSignal.EXCITED -> {
                pattern(1, 4, ".###.", "#####", "#####", ".###.")
                pattern(7, 4, ".###.", "#####", "#####", ".###.")
                set(0, 2); set(2, 1); set(10, 1); set(12, 2)
            }

            DaylineGlyphSignal.SLEEPY -> sleepy()

            DaylineGlyphSignal.SURPRISED -> {
                block(1, 3, 5, 6)
                block(7, 3, 5, 6)
            }

            DaylineGlyphSignal.PLAYFUL -> {
                roundedEye(1)
                happyEye(7)
            }

            DaylineGlyphSignal.CURIOUS -> {
                roundedEye(0, 3)
                roundedEye(7, 5)
            }

            DaylineGlyphSignal.SIDE_EYE -> {
                pattern(0, 5, "#####", ".####", "..###")
                pattern(6, 5, "#####", ".####", "..###")
            }

            DaylineGlyphSignal.ROLLING -> {
                roundedEye(1, 2)
                roundedEye(7, 6)
            }

            DaylineGlyphSignal.SQUINT -> squintEyes()

            DaylineGlyphSignal.HEARTS -> {
                pattern(
                    0, 4,
                    ".##.##.",
                    "#######",
                    "#######",
                    ".#####.",
                    "..###..",
                    "...#..."
                )
                pattern(
                    7, 4,
                    ".##.##",
                    "######",
                    "######",
                    ".####.",
                    "..##..",
                    "...#.."
                )
            }

            // Short Dayline states use one large symbol instead of squeezing a
            // tiny badge next to the face.
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

        return pixels
    }
}
