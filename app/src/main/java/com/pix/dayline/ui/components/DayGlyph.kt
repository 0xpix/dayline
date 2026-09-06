package com.pix.dayline.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.pix.dayline.model.DaylineItem
import java.time.LocalDate
import java.time.LocalTime

/**
 * A minimal 24-hour horizon. Every visual mark has a temporal meaning:
 * faint rail = full day, solid strokes = planned blocks, ring/dot = now.
 */
@Composable
fun DayGlyph(
    items: List<DaylineItem>,
    date: LocalDate,
    modifier: Modifier = Modifier
) {
    val foreground = MaterialTheme.colorScheme.onBackground
    val muted = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.16f)
    val accent = MaterialTheme.colorScheme.primary
    val background = MaterialTheme.colorScheme.background

    Column(modifier = modifier.width(176.dp)) {
        Canvas(modifier = Modifier.fillMaxWidth().height(15.dp)) {
            val left = 2.dp.toPx()
            val right = size.width - 2.dp.toPx()
            val y = size.height / 2f
            val width = right - left

            fun x(minutes: Int): Float = left + width * (
                minutes.coerceIn(0, 1440).toFloat() / 1440f
            )

            drawLine(
                color = muted,
                start = Offset(left, y),
                end = Offset(right, y),
                strokeWidth = 1.2.dp.toPx(),
                cap = StrokeCap.Round
            )

            listOf(360, 720, 1080).forEach { minute ->
                val px = x(minute)
                drawLine(
                    color = muted,
                    start = Offset(px, y - 2.5.dp.toPx()),
                    end = Offset(px, y + 2.5.dp.toPx()),
                    strokeWidth = 1.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            items.forEach { item ->
                val start = item.startTime ?: return@forEach
                val end = item.endTime?.takeIf { it.isAfter(start) } ?: start.plusHours(1)
                val startMinutes = (start.hour * 60 + start.minute - item.bufferBeforeMinutes)
                    .coerceAtLeast(0)
                val endMinutes = (end.hour * 60 + end.minute + item.bufferAfterMinutes)
                    .coerceAtMost(1440)

                drawLine(
                    color = foreground,
                    start = Offset(x(startMinutes), y),
                    end = Offset(x(endMinutes.coerceAtLeast(startMinutes + 10)), y),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            if (date == LocalDate.now()) {
                val now = LocalTime.now()
                val minute = now.hour * 60 + now.minute
                val center = Offset(x(minute), y)
                drawCircle(color = accent, radius = 3.4.dp.toPx(), center = center)
                drawCircle(
                    color = background,
                    radius = 1.4.dp.toPx(),
                    center = center
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            Text("00", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.weight(1f))
            Text("06", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.weight(1f))
            Text("12", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.weight(1f))
            Text("18", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.weight(1f))
            Text("24", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
