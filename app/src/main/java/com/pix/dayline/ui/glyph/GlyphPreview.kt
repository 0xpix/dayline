package com.pix.dayline.ui.glyph

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pix.dayline.glyph.GlyphMatrixPatterns
import com.pix.dayline.model.DaylineGlyphSignal
import kotlinx.coroutines.delay

@Composable
fun GlyphMatrixPreview(
    signal: DaylineGlyphSignal,
    modifier: Modifier = Modifier,
    animate: Boolean = true
) {
    var visibleSignal by remember { mutableStateOf(DaylineGlyphSignal.CENTER) }

    LaunchedEffect(signal, animate) {
        if (!animate || signal == DaylineGlyphSignal.CENTER) {
            visibleSignal = signal
            return@LaunchedEffect
        }

        // Match the real Glyph transition contract in Settings too. Every preview
        // starts and ends on CENTER so rapidly tapping expressions never creates
        // a misleading HAPPY -> SQUINT -> LEFT sequence.
        visibleSignal = DaylineGlyphSignal.CENTER
        delay(250)
        visibleSignal = signal
        delay(if (signal == DaylineGlyphSignal.BLINK) 350 else 900)
        visibleSignal = DaylineGlyphSignal.CENTER
    }

    Canvas(modifier.aspectRatio(1f)) {
        // One preview surface only. The old component drew a circular container
        // plus 169 dark cells, which visually produced a square inside the circle.
        drawCircle(Color.Black, radius = size.minDimension / 2f)
        drawMatrix(GlyphMatrixPatterns.frame(visibleSignal))
    }
}

private fun DrawScope.drawMatrix(frame: IntArray) {
    val n = GlyphMatrixPatterns.SIZE
    val padding = size.minDimension * 0.095f
    val available = size.minDimension - padding * 2f
    val gap = available * 0.012f
    val cell = (available - gap * (n - 1)) / n
    val total = cell * n + gap * (n - 1)
    val x0 = (size.width - total) / 2f
    val y0 = (size.height - total) / 2f

    for (y in 0 until n) {
        for (x in 0 until n) {
            val brightness = frame[y * n + x]
            if (brightness <= 0) continue

            val alpha = (brightness / GlyphMatrixPatterns.MAX_RAW_BRIGHTNESS.toFloat())
                .coerceIn(0.12f, 1f)
            val left = x0 + x * (cell + gap)
            val top = y0 + y * (cell + gap)
            drawRoundRect(
                color = Color.White.copy(alpha = alpha),
                topLeft = Offset(left, top),
                size = Size(cell, cell),
                cornerRadius = CornerRadius(cell * 0.22f, cell * 0.22f)
            )
        }
    }
}
