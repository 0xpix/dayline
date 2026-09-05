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

@Composable
fun DayGlyph(
    items: List<DaylineItem>,
    date: LocalDate,
    modifier: Modifier = Modifier
) {
    val active = BooleanArray(24)
    items.forEach { item ->
        val start = item.startTime ?: return@forEach
        val end = item.endTime
        if (end != null && end.isAfter(start)) {
            val firstHour = start.hour
            val lastHour = if (end.minute == 0) (end.hour - 1).coerceAtLeast(firstHour) else end.hour
            for (hour in firstHour..lastHour.coerceAtMost(23)) active[hour] = true
        } else {
            active[start.hour] = true
        }
    }

    val onBackground = MaterialTheme.colorScheme.onBackground
    val muted = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.14f)
    val currentHour = if (date == LocalDate.now()) LocalTime.now().hour else -1

    Canvas(modifier = modifier.size(78.dp)) {
        val stroke = 5.5.dp.toPx()
        val inset = stroke / 2f + 2.dp.toPx()
        val arcSize = androidx.compose.ui.geometry.Size(size.width - inset * 2f, size.height - inset * 2f)
        val topLeft = Offset(inset, inset)

        for (hour in 0 until 24) {
            val startAngle = -90f + (hour * 15f) + 2.5f
            val segmentColor = if (active[hour]) onBackground else muted
            drawArc(
                color = segmentColor,
                startAngle = startAngle,
                sweepAngle = 9.5f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }

        if (currentHour >= 0) {
            val angle = Math.toRadians((-90.0 + currentHour * 15.0 + 7.5))
            val radius = size.minDimension * 0.28f
            val center = Offset(size.width / 2f, size.height / 2f)
            val dot = Offset(
                x = center.x + kotlin.math.cos(angle).toFloat() * radius,
                y = center.y + kotlin.math.sin(angle).toFloat() * radius
            )
            drawCircle(color = onBackground, radius = 2.5.dp.toPx(), center = dot)
        }
    }
}
