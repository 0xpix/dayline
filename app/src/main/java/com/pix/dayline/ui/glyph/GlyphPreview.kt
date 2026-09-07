package com.pix.dayline.ui.glyph

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.pix.dayline.glyph.GlyphMatrixPatterns
import com.pix.dayline.model.DaylineGlyphSignal
import kotlinx.coroutines.delay

@Composable
fun GlyphMatrixPreview(
    signal: DaylineGlyphSignal,
    modifier: Modifier = Modifier,
    animate: Boolean = true
) {
    var visibleSignal by remember(signal) { mutableStateOf(signal) }

    LaunchedEffect(signal, animate) {
        visibleSignal = signal
        if (animate && signal == DaylineGlyphSignal.BLINK) {
            visibleSignal = DaylineGlyphSignal.CENTER
            delay(120)
            visibleSignal = DaylineGlyphSignal.BLINK
            delay(150)
            visibleSignal = DaylineGlyphSignal.CENTER
        }
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .background(Color.Black, CircleShape)
            .padding(12.dp)
    ) {
        Canvas(Modifier.fillMaxWidth().aspectRatio(1f)) {
            drawMatrix(GlyphMatrixPatterns.frame(visibleSignal))
        }
    }
}

private fun DrawScope.drawMatrix(frame: IntArray) {
    val n = GlyphMatrixPatterns.SIZE
    val gap = size.minDimension * 0.012f
    val cell = (size.minDimension - gap * (n - 1)) / n
    val total = cell * n + gap * (n - 1)
    val x0 = (size.width - total) / 2f
    val y0 = (size.height - total) / 2f

    for (y in 0 until n) {
        for (x in 0 until n) {
            val brightness = frame[y * n + x]
            val lit = brightness > 0
            val color = if (lit) {
                Color.White.copy(alpha = (brightness / 255f).coerceIn(0.2f, 1f))
            } else {
                Color(0xFF1A1A1A)
            }
            val left = x0 + x * (cell + gap)
            val top = y0 + y * (cell + gap)
            drawRoundRect(
                color = color,
                topLeft = Offset(left, top),
                size = Size(cell, cell),
                cornerRadius = CornerRadius(cell * 0.18f, cell * 0.18f)
            )
        }
    }
}
