package com.pix.dayline.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pix.dayline.model.AgendaKind
import com.pix.dayline.model.DaylineItem
import com.pix.dayline.model.isCompletedOn
import com.pix.dayline.ui.theme.composeColor
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun DayTimeline(
    items: List<DaylineItem>,
    date: LocalDate,
    emptyText: String = "Your day is clear.",
    onEdit: (DaylineItem) -> Unit,
    onToggleTask: (DaylineItem, LocalDate) -> Unit
) {
    if (items.isEmpty()) {
        Text(
            text = emptyText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    val scheduled = items.filter { it.startTime != null }
        .sortedWith(compareBy<DaylineItem> { it.startTime }.thenBy { it.title })
    val anytime = items.filter { it.startTime == null }

    Column {
        var previousEnd: LocalTime? = null
        scheduled.forEachIndexed { index, item ->
            val start = item.startTime ?: return@forEachIndexed
            val gap = previousEnd?.let { Duration.between(it, start).toMinutes() } ?: 0L
            if (index > 0) Spacer(Modifier.height(gapToSpace(gap)))

            TimelineItem(
                item = item,
                date = date,
                onEdit = onEdit,
                onToggleTask = onToggleTask
            )
            previousEnd = item.endTime?.takeIf { it.isAfter(start) } ?: start
        }

        if (anytime.isNotEmpty()) {
            if (scheduled.isNotEmpty()) Spacer(Modifier.height(26.dp))
            Text(
                text = "ANYTIME",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                anytime.forEach { item ->
                    AnytimeItem(item, date, onEdit, onToggleTask)
                }
            }
        }
    }
}

@Composable
private fun TimelineItem(
    item: DaylineItem,
    date: LocalDate,
    onEdit: (DaylineItem) -> Unit,
    onToggleTask: (DaylineItem, LocalDate) -> Unit
) {
    val start = item.startTime ?: return
    val end = item.endTime?.takeIf { it.isAfter(start) }
    val completed = item.kind == AgendaKind.TASK && item.isCompletedOn(date)
    val accent = item.color.composeColor()
    val height = durationToHeight(start, end)

    Row(
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = { onEdit(item) }
        ),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.width(58.dp)) {
            Text(
                text = start.format(DateTimeFormatter.ofPattern("HH:mm")),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (end != null) {
                Spacer(Modifier.height((height - 30.dp).coerceAtLeast(4.dp)))
                Text(
                    text = end.format(DateTimeFormatter.ofPattern("HH:mm")),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Box(
            modifier = Modifier
                .padding(top = 2.dp, end = 14.dp)
                .width(4.dp)
                .height(height)
                .background(accent, RoundedCornerShape(99.dp))
        )

        Column(modifier = Modifier.padding(top = 1.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    textDecoration = if (completed) TextDecoration.LineThrough else TextDecoration.None
                )

                if (item.kind == AgendaKind.TASK) {
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = if (completed) "✓" else "○",
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onToggleTask(item, date) }
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            val meta = buildList {
                if (end != null) add(durationLabel(start, end))
                item.reminderMinutes?.let { add("${it}m reminder") }
            }.joinToString(" · ")

            if (meta.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = meta,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AnytimeItem(
    item: DaylineItem,
    date: LocalDate,
    onEdit: (DaylineItem) -> Unit,
    onToggleTask: (DaylineItem, LocalDate) -> Unit
) {
    val completed = item.kind == AgendaKind.TASK && item.isCompletedOn(date)
    Row(
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = { onEdit(item) }
        ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .padding(end = 14.dp)
                .width(6.dp)
                .height(6.dp)
                .background(item.color.composeColor(), CircleShape)
        )
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textDecoration = if (completed) TextDecoration.LineThrough else TextDecoration.None
        )
        if (item.kind == AgendaKind.TASK) {
            Spacer(Modifier.width(12.dp))
            Text(
                text = if (completed) "✓" else "○",
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { onToggleTask(item, date) }
                ),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

private fun gapToSpace(minutes: Long): Dp {
    if (minutes <= 0) return 10.dp
    return (10 + (minutes.coerceAtMost(180) / 15.0).roundToInt()).dp.coerceAtMost(28.dp)
}

private fun durationToHeight(start: LocalTime, end: LocalTime?): Dp {
    val minutes = end?.let { Duration.between(start, it).toMinutes() }?.coerceAtLeast(30) ?: 45
    val value = 42 + (minutes.coerceAtMost(360) / 60.0 * 12.0).roundToInt()
    return value.dp.coerceIn(46.dp, 114.dp)
}

private fun durationLabel(start: LocalTime, end: LocalTime): String {
    val total = Duration.between(start, end).toMinutes()
    val hours = total / 60
    val minutes = total % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
        hours > 0 -> "${hours}h"
        else -> "${minutes}m"
    }
}
