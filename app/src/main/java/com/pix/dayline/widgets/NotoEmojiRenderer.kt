package com.pix.dayline.widgets

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.pix.dayline.R
import kotlin.math.roundToInt

/**
 * Renders the Google Fonts "Noto Emoji" downloadable font into a transparent
 * bitmap for Glance. The font file itself is not bundled with Dayline.
 *
 * If Google Play Services has not finished providing the font yet, Android's
 * system emoji fallback is rendered temporarily; Glance still tints the result
 * to Dayline's monochrome widget foreground.
 */
object NotoEmojiRenderer {
    @Volatile
    private var cachedTypeface: Typeface? = null

    private fun typeface(context: Context): Typeface {
        cachedTypeface?.let { return it }

        val loaded = runCatching {
            ResourcesCompat.getFont(context, R.font.noto_emoji)
        }.getOrNull()

        if (loaded != null) {
            cachedTypeface = loaded
            return loaded
        }

        return Typeface.DEFAULT
    }

    fun hasGlyph(
        context: Context,
        glyph: String
    ): Boolean {
        val paint = Paint(
            Paint.ANTI_ALIAS_FLAG or
                Paint.SUBPIXEL_TEXT_FLAG
        ).apply {
            textSize = 64f
            typeface = typeface(context)
        }

        return runCatching {
            paint.hasGlyph(glyph)
        }.getOrDefault(false)
    }

    fun render(
        context: Context,
        glyph: String,
        sizeDp: Int
    ): Bitmap {
        val density = context.resources.displayMetrics.density
        val sizePx = (sizeDp * density).roundToInt().coerceAtLeast(1)

        val bitmap = Bitmap.createBitmap(
            sizePx,
            sizePx,
            Bitmap.Config.ARGB_8888
        )

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            textSize = sizePx * 0.76f
            typeface = typeface(context)
        }

        val metrics = paint.fontMetrics
        val baseline = sizePx / 2f - (metrics.ascent + metrics.descent) / 2f

        Canvas(bitmap).drawText(
            glyph,
            sizePx / 2f,
            baseline,
            paint
        )

        return bitmap
    }
}
