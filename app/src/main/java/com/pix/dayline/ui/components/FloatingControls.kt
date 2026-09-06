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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A quiet Dawn-like action rail.
 *
 * The menu/today controls are intentionally small and airy. The primary add
 * action is the only visually heavy control.
 */
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
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CircleAction(
            filled = false,
            size = 42,
            onClick = onMenu
        ) { color ->
            MenuLinesIcon(color)
        }

        if (showToday) {
            CircleAction(
                filled = false,
                size = 42,
                onClick = onToday
            ) { color ->
                DaylineLogoIcon(color)
            }
        }

        if (showAdd) {
            CircleAction(
                label = "+",
                filled = true,
                size = 50,
                textSize = 28,
                onClick = onAdd
            )
        }
    }
}

@Composable
private fun CircleAction(
    label: String? = null,
    filled: Boolean,
    onClick: () -> Unit,
    size: Int = 42,
    textSize: Int = 22,
    content: (@Composable (Color) -> Unit)? = null
) {
    val background =
        if (filled) {
            MaterialTheme.colorScheme.onBackground
        } else {
            MaterialTheme.colorScheme.surface
        }

    val foreground =
        if (filled) {
            MaterialTheme.colorScheme.background
        } else {
            MaterialTheme.colorScheme.onSurface
        }

    Surface(
        modifier = Modifier
            .size(size.dp)
            .clickable(
                interactionSource = remember {
                    MutableInteractionSource()
                },
                indication = null,
                onClick = onClick
            ),
        shape = CircleShape,
        color = background,
        contentColor = foreground,
        tonalElevation = 0.dp,
        shadowElevation = if (filled) 1.dp else 3.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (content != null) {
                content(foreground)
            } else if (label != null) {
                Text(
                    text = label,
                    color = foreground,
                    fontSize = textSize.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun MenuLinesIcon(color: Color) {
    Canvas(modifier = Modifier.size(18.dp)) {
        val stroke = 1.55.dp.toPx()
        val left = size.width * 0.22f
        val right = size.width * 0.78f
        val shortRight = size.width * 0.66f

        drawLine(
            color,
            Offset(left, size.height * 0.34f),
            Offset(right, size.height * 0.34f),
            stroke,
            StrokeCap.Round
        )
        drawLine(
            color,
            Offset(left, size.height * 0.50f),
            Offset(shortRight, size.height * 0.50f),
            stroke,
            StrokeCap.Round
        )
        drawLine(
            color,
            Offset(left, size.height * 0.66f),
            Offset(right, size.height * 0.66f),
            stroke,
            StrokeCap.Round
        )
    }
}

@Composable
private fun DaylineLogoIcon(
    color: Color
) {
    Canvas(
        modifier = Modifier.size(23.dp)
    ) {
        fun x(value: Float): Float =
            size.width * value / 108f

        fun y(value: Float): Float =
            size.height * value / 108f

        fun block(
            left: Float,
            top: Float,
            right: Float,
            bottom: Float
        ) {
            drawRect(
                color = color,
                topLeft = Offset(
                    x(left),
                    y(top)
                ),
                size = Size(
                    x(right - left),
                    y(bottom - top)
                )
            )
        }

        block(34f, 28f, 50f, 34f)
        block(58f, 28f, 74f, 34f)
        block(74f, 34f, 80f, 50f)
        block(74f, 58f, 80f, 74f)
        block(58f, 74f, 74f, 80f)
        block(34f, 74f, 50f, 80f)
        block(28f, 58f, 34f, 74f)
        block(28f, 34f, 34f, 50f)
        block(38f, 51f, 64f, 57f)
        block(68f, 49f, 77f, 58f)
    }
}
