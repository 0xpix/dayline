package com.pix.dayline.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.pix.dayline.model.DaylineItem
import java.time.LocalDate
import java.time.LocalTime

/**
 * A quiet 24-hour ribbon.
 *
 * The rail is the day.
 * Scheduled events become small solid segments at their real position.
 * The ring is "now".
 *
 * No decorative orbit/dots — every mark has temporal meaning.
 */
@Composable
fun DayGlyph(
    items: List<DaylineItem>,
    date: LocalDate,
    modifier: Modifier = Modifier
) {
    val foreground = MaterialTheme.colorScheme.onBackground
    val muted = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.18f)
    val accent = MaterialTheme.colorScheme.primary
    val background = MaterialTheme.colorScheme.background

    Canvas(
        modifier = modifier.size(
            width = 138.dp,
            height = 18.dp
        )
    ) {
        val left = 6.dp.toPx()
        val right = size.width - 6.dp.toPx()
        val y = size.height / 2f
        val railWidth = right - left

        fun x(minutes: Int): Float =
            left + railWidth * (
                minutes.coerceIn(0, 24 * 60).toFloat() /
                    (24 * 60).toFloat()
            )

        drawLine(
            color = muted,
            start = Offset(left, y),
            end = Offset(right, y),
            strokeWidth = 1.2.dp.toPx(),
            cap = StrokeCap.Round
        )

        items.forEach { item ->
            val start = item.startTime ?: return@forEach
            val end = item.endTime
                ?.takeIf { it.isAfter(start) }
                ?: start.plusHours(1)

            val startMinutes = start.hour * 60 + start.minute
            val endMinutes = end.hour * 60 + end.minute

            drawLine(
                color = foreground,
                start = Offset(x(startMinutes), y),
                end = Offset(
                    x(endMinutes.coerceAtLeast(startMinutes + 15)),
                    y
                ),
                strokeWidth = 2.8.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        if (date == LocalDate.now()) {
            val now = LocalTime.now()
            val nowMinutes = now.hour * 60 + now.minute
            val center = Offset(x(nowMinutes), y)

            drawCircle(
                color = background,
                radius = 4.2.dp.toPx(),
                center = center
            )
            drawCircle(
                color = accent,
                radius = 3.4.dp.toPx(),
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )
            drawCircle(
                color = accent,
                radius = 1.4.dp.toPx(),
                center = center
            )
        }
    }
}
