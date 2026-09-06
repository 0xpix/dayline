package com.pix.dayline.widgets

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.pix.dayline.data.WidgetFontChoice
import kotlin.math.ceil
import kotlin.math.max

data class DotMatrixImage(
    val bitmap: Bitmap,
    val widthDp: Int,
    val heightDp: Int
)

object DotMatrixRenderer {
    private val glyphs: Map<Char, Array<String>> = mapOf(
        'A' to arrayOf("01110","10001","10001","11111","10001","10001","10001"),
        'B' to arrayOf("11110","10001","10001","11110","10001","10001","11110"),
        'C' to arrayOf("01111","10000","10000","10000","10000","10000","01111"),
        'D' to arrayOf("11110","10001","10001","10001","10001","10001","11110"),
        'E' to arrayOf("11111","10000","10000","11110","10000","10000","11111"),
        'F' to arrayOf("11111","10000","10000","11110","10000","10000","10000"),
        'G' to arrayOf("01111","10000","10000","10111","10001","10001","01111"),
        'H' to arrayOf("10001","10001","10001","11111","10001","10001","10001"),
        'I' to arrayOf("11111","00100","00100","00100","00100","00100","11111"),
        'J' to arrayOf("00111","00010","00010","00010","10010","10010","01100"),
        'K' to arrayOf("10001","10010","10100","11000","10100","10010","10001"),
        'L' to arrayOf("10000","10000","10000","10000","10000","10000","11111"),
        'M' to arrayOf("10001","11011","10101","10101","10001","10001","10001"),
        'N' to arrayOf("10001","11001","10101","10011","10001","10001","10001"),
        'O' to arrayOf("01110","10001","10001","10001","10001","10001","01110"),
        'P' to arrayOf("11110","10001","10001","11110","10000","10000","10000"),
        'Q' to arrayOf("01110","10001","10001","10001","10101","10010","01101"),
        'R' to arrayOf("11110","10001","10001","11110","10100","10010","10001"),
        'S' to arrayOf("01111","10000","10000","01110","00001","00001","11110"),
        'T' to arrayOf("11111","00100","00100","00100","00100","00100","00100"),
        'U' to arrayOf("10001","10001","10001","10001","10001","10001","01110"),
        'V' to arrayOf("10001","10001","10001","10001","10001","01010","00100"),
        'W' to arrayOf("10001","10001","10001","10101","10101","10101","01010"),
        'X' to arrayOf("10001","10001","01010","00100","01010","10001","10001"),
        'Y' to arrayOf("10001","10001","01010","00100","00100","00100","00100"),
        'Z' to arrayOf("11111","00001","00010","00100","01000","10000","11111"),

        '0' to arrayOf("01110","10001","10011","10101","11001","10001","01110"),
        '1' to arrayOf("00100","01100","00100","00100","00100","00100","01110"),
        '2' to arrayOf("01110","10001","00001","00010","00100","01000","11111"),
        '3' to arrayOf("11110","00001","00001","01110","00001","00001","11110"),
        '4' to arrayOf("00010","00110","01010","10010","11111","00010","00010"),
        '5' to arrayOf("11111","10000","10000","11110","00001","00001","11110"),
        '6' to arrayOf("01110","10000","10000","11110","10001","10001","01110"),
        '7' to arrayOf("11111","00001","00010","00100","01000","01000","01000"),
        '8' to arrayOf("01110","10001","10001","01110","10001","10001","01110"),
        '9' to arrayOf("01110","10001","10001","01111","00001","00001","01110"),

        ':' to arrayOf("00000","00100","00100","00000","00100","00100","00000"),
        '.' to arrayOf("00000","00000","00000","00000","00000","00110","00110"),
        '-' to arrayOf("00000","00000","00000","11111","00000","00000","00000"),
        '/' to arrayOf("00001","00010","00010","00100","01000","01000","10000"),
        '!' to arrayOf("00100","00100","00100","00100","00100","00000","00100"),
        '?' to arrayOf("01110","10001","00001","00010","00100","00000","00100"),
        '&' to arrayOf("01100","10010","10100","01000","10101","10010","01101"),
        '\'' to arrayOf("00100","00100","00000","00000","00000","00000","00000"),
        '(' to arrayOf("00010","00100","01000","01000","01000","00100","00010"),
        ')' to arrayOf("01000","00100","00010","00010","00010","00100","01000"),
        ',' to arrayOf("00000","00000","00000","00000","00110","00100","01000"),
        '+' to arrayOf("00000","00100","00100","11111","00100","00100","00000"),
        '>' to arrayOf("10000","01000","00100","00010","00100","01000","10000"),
        '<' to arrayOf("00001","00010","00100","01000","00100","00010","00001"),
        ' ' to arrayOf("00000","00000","00000","00000","00000","00000","00000")
    )


    /**
     * True when every Unicode code point can be represented by the dot grid.
     * Event titles containing emoji/accented scripts fall back to native
     * Glance Text instead of being replaced with '?'.
     */
    fun canRender(rawText: String): Boolean {
        val normalized = rawText.uppercase()
            .replace('↗', '>')

        var index = 0
        while (index < normalized.length) {
            val codePoint = normalized.codePointAt(index)

            if (
                !Character.isBmpCodePoint(codePoint) ||
                glyphs[codePoint.toChar()] == null
            ) {
                return false
            }

            index += Character.charCount(codePoint)
        }

        return true
    }

    fun render(
        context: Context,
        rawText: String,
        fontChoice: WidgetFontChoice,
        scale: Float = 1f,
        maxChars: Int = 24
    ): DotMatrixImage {
        val density = context.resources.displayMetrics.density
        val text = rawText.uppercase()
            .replace('↗', '>')
            .take(maxChars)

        if (fontChoice == WidgetFontChoice.MONO) {
            return renderMono(context, text, scale)
        }

        val bold = fontChoice == WidgetFontChoice.DOT_BOLD

        val dotDp = (if (bold) 1.72f else 1.08f) * scale
        val stepDp = (if (bold) 2.18f else 2.12f) * scale
        val charGapDp = (if (bold) 1.45f else 1.80f) * scale
        val glyphWidthDp = stepDp * 4 + dotDp
        val glyphHeightDp = stepDp * 6 + dotDp

        val widthDp = max(
            1,
            ceil(
                text.length * glyphWidthDp +
                    max(0, text.length - 1) * charGapDp
            ).toInt()
        )
        val heightDp = max(1, ceil(glyphHeightDp).toInt())

        val bitmapWidth = max(1, ceil(widthDp * density).toInt())
        val bitmapHeight = max(1, ceil(heightDp * density).toInt())
        val bitmap = Bitmap.createBitmap(
            bitmapWidth,
            bitmapHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)

        // Always draw opaque white. Glance applies a ColorProvider tint later,
        // so the same bitmap automatically remains readable in light/dark mode.
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            this.style = Paint.Style.FILL
        }

        val dotPx = dotDp * density
        val stepPx = stepDp * density
        val gapPx = charGapDp * density

        var cursorX = 0f
        text.forEach { char ->
            val glyph = glyphs[char] ?: glyphs['?']!!
            glyph.forEachIndexed { row, line ->
                line.forEachIndexed { col, bit ->
                    if (bit == '1') {
                        val left = cursorX + col * stepPx
                        val top = row * stepPx

                        if (bold) {
                            canvas.drawRoundRect(
                                RectF(left, top, left + dotPx, top + dotPx),
                                dotPx * 0.28f,
                                dotPx * 0.28f,
                                paint
                            )
                        } else {
                            canvas.drawOval(
                                RectF(left, top, left + dotPx, top + dotPx),
                                paint
                            )
                        }
                    }
                }
            }
            cursorX += glyphWidthDp * density + gapPx
        }

        return DotMatrixImage(bitmap, widthDp, heightDp)
    }
    private fun renderMono(
        context: Context,
        text: String,
        scale: Float
    ): DotMatrixImage {
        val density = context.resources.displayMetrics.density
        val scaledDensity = context.resources.displayMetrics.scaledDensity

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 10.5f * scale * scaledDensity
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }

        val widthPx = max(1, ceil(paint.measureText(text).toDouble()).toInt())
        val fm = paint.fontMetrics
        val heightPx = max(1, ceil((fm.bottom - fm.top).toDouble()).toInt())

        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawText(text, 0f, -fm.top, paint)

        return DotMatrixImage(
            bitmap = bitmap,
            widthDp = max(1, ceil(widthPx / density).toInt()),
            heightDp = max(1, ceil(heightPx / density).toInt())
        )
    }

}
