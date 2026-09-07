package com.pix.dayline.glyph

import com.pix.dayline.model.DaylineGlyphSignal

/**
 * Native 13×13 frames for the Phone (4a) Pro Glyph Matrix.
 *
 * Idle mode keeps Dayline's large expressive eyes. Focus mode uses a separate
 * compact layout: small expressive eyes live at the top of the matrix while a
 * stable MM:SS countdown owns the bottom five rows. The timer never mixes with
 * the eye pixels and the normal CENTER -> expression -> CENTER rhythm remains.
 */
object GlyphMatrixPatterns {
    const val SIZE = 13
    const val MAX_RAW_BRIGHTNESS = 2047

    fun frame(
        signal: DaylineGlyphSignal,
        brightness: Int = MAX_RAW_BRIGHTNESS,
        focusRemainingSeconds: Long? = null
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

        // -----------------------------------------------------------------
        // Normal / idle face. These large patterns stay unchanged by Focus.
        // -----------------------------------------------------------------
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

        fun renderNormalSignal() {
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
        }

        // -----------------------------------------------------------------
        // Focus face. Rows 0..4 belong to the eyes, rows 8..12 to MM:SS.
        // Rows 5..7 intentionally stay empty to keep both zones separated.
        // -----------------------------------------------------------------
        fun smallOpenEye(x: Int) {
            pattern(
                x, 1,
                ".##.",
                "####",
                ".##."
            )
        }

        fun smallClosedEye(x: Int) {
            hLine(x, x + 3, 2)
        }

        fun smallHappyEye(x: Int) {
            pattern(
                x, 1,
                ".##.",
                "#..#"
            )
        }

        fun smallSleepyEye(x: Int) {
            pattern(
                x, 1,
                "####",
                ".##."
            )
        }

        fun smallSquintEye(x: Int) {
            pattern(
                x, 1,
                "#..#",
                ".##.",
                "#..#"
            )
        }

        fun smallHeartEye(x: Int) {
            pattern(
                x, 1,
                "#..#",
                "####",
                ".##."
            )
        }

        fun focusCenterEyes() {
            smallOpenEye(1)
            smallOpenEye(8)
        }

        fun focusLeftEyes() {
            // Same directional rule as idle mode: shift the complete eye pair left.
            smallOpenEye(0)
            smallOpenEye(7)
        }

        fun focusRightEyes() {
            // Same directional rule as idle mode: shift the complete eye pair right.
            smallOpenEye(2)
            smallOpenEye(9)
        }

        fun renderFocusEyes() {
            when (signal) {
                DaylineGlyphSignal.LOOK_LEFT -> focusLeftEyes()
                DaylineGlyphSignal.LOOK_RIGHT -> focusRightEyes()

                DaylineGlyphSignal.BLINK -> {
                    smallClosedEye(1)
                    smallClosedEye(8)
                }

                DaylineGlyphSignal.WINK -> {
                    smallOpenEye(1)
                    smallClosedEye(8)
                }

                DaylineGlyphSignal.HAPPY -> {
                    smallHappyEye(1)
                    smallHappyEye(8)
                }

                DaylineGlyphSignal.SLEEPY -> {
                    smallSleepyEye(1)
                    smallSleepyEye(8)
                }

                DaylineGlyphSignal.SQUINT -> {
                    smallSquintEye(1)
                    smallSquintEye(8)
                }

                DaylineGlyphSignal.HEARTS -> {
                    smallHeartEye(1)
                    smallHeartEye(8)
                }

                else -> focusCenterEyes()
            }
        }

        fun digitRows(digit: Int): Array<String> = when (digit) {
            0 -> arrayOf("###", "#.#", "#.#", "#.#", "###")
            1 -> arrayOf(".#.", "##.", ".#.", ".#.", "###")
            2 -> arrayOf("###", "..#", "###", "#..", "###")
            3 -> arrayOf("###", "..#", "###", "..#", "###")
            4 -> arrayOf("#.#", "#.#", "###", "..#", "..#")
            5 -> arrayOf("###", "#..", "###", "..#", "###")
            6 -> arrayOf("###", "#..", "###", "#.#", "###")
            7 -> arrayOf("###", "..#", "..#", "..#", "..#")
            8 -> arrayOf("###", "#.#", "###", "#.#", "###")
            9 -> arrayOf("###", "#.#", "###", "..#", "###")
            else -> arrayOf("...", "...", "...", "...", "...")
        }

        fun drawDigit(digit: Int, x: Int) {
            digitRows(digit).forEachIndexed { row, glyph ->
                glyph.forEachIndexed { column, pixel ->
                    if (pixel == '#') set(x + column, 8 + row)
                }
            }
        }

        fun drawTimer(remainingSeconds: Long) {
            // MM:SS fills all 13 columns exactly: 3 + 3 + 1 + 3 + 3.
            // Focus cycles above 99:59 are clamped visually until they enter range.
            val clamped = remainingSeconds.coerceIn(0L, 99L * 60L + 59L)
            val minutes = (clamped / 60L).toInt()
            val seconds = (clamped % 60L).toInt()

            drawDigit(minutes / 10, 0)
            drawDigit(minutes % 10, 3)
            set(6, 9)
            set(6, 11)
            drawDigit(seconds / 10, 7)
            drawDigit(seconds % 10, 10)
        }

        if (focusRemainingSeconds != null) {
            renderFocusEyes()
            drawTimer(focusRemainingSeconds)
        } else {
            renderNormalSignal()
        }

        return pixels
    }
}
