package com.pix.dayline.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.pix.dayline.model.DaylineItem
import java.time.LocalDate
import java.time.LocalTime

/**
 * Minimal 24-hour "day signal".
 *
 * 12 positions = 2 hours each.
 * - faint dot: open time
 * - solid dot: scheduled time
 * - ring: current time bucket
 *
 * It intentionally avoids a large decorative circle so the Today page
 * keeps more whitespace and feels closer to Nothing/Dawn.
 */
@Composable
fun DayGlyph(
    items: List<DaylineItem>,
    date: LocalDate,
    modifier: Modifier = Modifier
) {
    val busy = BooleanArray(12)

    items.forEach { item ->
        val start = item.startTime ?: return@forEach
        val end = item.endTime

        val startMinute = start.hour * 60 + start.minute
        val endMinute = if (end != null && end.isAfter(start)) {
            end.hour * 60 + end.minute
        } else {
            startMinute + 60
        }

        for (slot in 0 until 12) {
            val slotStart = slot * 120
            val slotEnd = slotStart + 120

            if (startMinute < slotEnd && endMinute > slotStart) {
                busy[slot] = true
            }
        }
    }

    val foreground = MaterialTheme.colorScheme.onBackground
    val accent = MaterialTheme.colorScheme.primary
    val muted = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.18f)

    val now = LocalTime.now()
    val currentSlot = if (date == LocalDate.now()) {
        ((now.hour * 60 + now.minute) / 120).coerceIn(0, 11)
    } else {
        -1
    }

    Canvas(
        modifier = modifier.then(
            Modifier
                .size(width = 116.dp, height = 18.dp)
        )
    ) {
        val left = 7.dp.toPx()
        val right = size.width - 7.dp.toPx()
        val y = size.height / 2f
        val step = (right - left) / 11f

        // One almost invisible rail keeps the dots visually connected.
        drawLine(
            color = muted,
            start = Offset(left, y),
            end = Offset(right, y),
            strokeWidth = 1.dp.toPx()
        )

        for (slot in 0 until 12) {
            val center = Offset(left + slot * step, y)

            if (slot == currentSlot) {
                drawCircle(
                    color = accent,
                    radius = 5.2.dp.toPx(),
                    center = center,
                    style = Stroke(width = 1.7.dp.toPx())
                )
                drawCircle(
                    color = accent,
                    radius = 2.1.dp.toPx(),
                    center = center
                )
            } else {
                drawCircle(
                    color = if (busy[slot]) foreground else muted,
                    radius = if (busy[slot]) 2.4.dp.toPx() else 1.7.dp.toPx(),
                    center = center
                )
            }
        }
    }
}
