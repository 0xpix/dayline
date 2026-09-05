package com.pix.dayline.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FloatingControls(
    modifier: Modifier = Modifier,
    showToday: Boolean = true,
    showAdd: Boolean = true,
    onMenu: () -> Unit,
    onToday: () -> Unit = {},
    onAdd: () -> Unit = {}
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CircleAction(label = "≡", filled = false, onClick = onMenu)
        if (showToday) {
            CircleAction(filled = false, onClick = onToday) { color ->
                TodayGlyphIcon(color)
            }
        }
        if (showAdd) {
            CircleAction(label = "+", filled = true, size = 54, textSize = 31, onClick = onAdd)
        }
    }
}

@Composable
private fun CircleAction(
    label: String? = null,
    filled: Boolean,
    onClick: () -> Unit,
    size: Int = 48,
    textSize: Int = 23,
    content: (@Composable (Color) -> Unit)? = null
) {
    val background = if (filled) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.surface
    val foreground = if (filled) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurface

    Surface(
        modifier = Modifier
            .size(size.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        shape = CircleShape,
        color = background,
        contentColor = foreground,
        shadowElevation = if (filled) 0.dp else 1.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (content != null) {
                content(foreground)
            } else if (label != null) {
                Text(
                    text = label,
                    color = foreground,
                    fontSize = textSize.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun TodayGlyphIcon(color: Color) {
    Canvas(modifier = Modifier.size(21.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val stroke = 1.7.dp.toPx()
        val radius = size.minDimension * 0.31f
        val tickInner = size.minDimension * 0.38f
        val tickOuter = size.minDimension * 0.47f

        drawCircle(
            color = color,
            radius = radius,
            center = center,
            style = Stroke(width = stroke)
        )
        drawCircle(color = color, radius = 1.45.dp.toPx(), center = center)

        drawLine(color, Offset(center.x, center.y - tickOuter), Offset(center.x, center.y - tickInner), stroke)
        drawLine(color, Offset(center.x, center.y + tickInner), Offset(center.x, center.y + tickOuter), stroke)
        drawLine(color, Offset(center.x - tickOuter, center.y), Offset(center.x - tickInner, center.y), stroke)
        drawLine(color, Offset(center.x + tickInner, center.y), Offset(center.x + tickOuter, center.y), stroke)
    }
}
