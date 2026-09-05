package com.pix.dayline.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.pix.dayline.model.AgendaKind
import com.pix.dayline.model.DaylineItem
import com.pix.dayline.model.FocusCycle
import com.pix.dayline.model.Recurrence
import com.pix.dayline.model.isCompletedOn
import com.pix.dayline.ui.theme.composeColor
import kotlinx.coroutines.delay
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
    onToggleTask: (DaylineItem, LocalDate) -> Unit,
    onReschedule: (DaylineItem) -> Unit
) {
    if (items.isEmpty()) {
        Column {
            if (date == LocalDate.now()) {
                CurrentTimeMarker(LocalTime.now())
                Spacer(Modifier.height(18.dp))
            }

            Text(
                text = emptyText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val scheduled = items.filter { it.startTime != null }
        .sortedWith(compareBy<DaylineItem> { it.startTime }.thenBy { it.title })
    val anytime = items.filter { it.startTime == null }

    var now by remember(date) {
        mutableStateOf(LocalTime.now())
    }

    LaunchedEffect(date) {
        while (true) {
            now = LocalTime.now()
            delay(30_000L)
        }
    }

    val showNow = date == LocalDate.now()

    Column {
        var previousEnd: LocalTime? = null
        var nowPlaced = false

        scheduled.forEachIndexed { index, item ->
            val start = item.startTime ?: return@forEachIndexed
            val gap = previousEnd?.let { Duration.between(it, start).toMinutes() } ?: 0L

            val eventEnd =
                item.endTime?.takeIf { it.isAfter(start) } ?: start

            val activeNow =
                !now.isBefore(start) &&
                now.isBefore(eventEnd)

            if (
                showNow &&
                !nowPlaced &&
                (now.isBefore(start) || activeNow)
            ) {
                if (index > 0) Spacer(Modifier.height(gapToSpace(gap) / 2))
                CurrentTimeMarker(now)
                Spacer(Modifier.height(gapToSpace(gap) / 2))
                nowPlaced = true
            } else if (index > 0) {
                Spacer(Modifier.height(gapToSpace(gap)))
            }

            TimelineItem(
                item = item,
                date = date,
                onEdit = onEdit,
                onToggleTask = onToggleTask,
                onReschedule = onReschedule
            )

            previousEnd = eventEnd
        }

        if (showNow && !nowPlaced && scheduled.isNotEmpty()) {
            Spacer(Modifier.height(18.dp))
            CurrentTimeMarker(now)
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
    onToggleTask: (DaylineItem, LocalDate) -> Unit,
    onReschedule: (DaylineItem) -> Unit
) {
    val start = item.startTime ?: return
    val end = item.endTime?.takeIf { it.isAfter(start) }
    val completed = item.kind == AgendaKind.TASK && item.isCompletedOn(date)
    val accent = item.color.composeColor()
    val height = durationToHeight(start, end)

    val density = LocalDensity.current
    val pixelsPer15Minutes = with(density) { 18.dp.toPx() }

    var dragging by remember(item.id) { mutableStateOf(false) }
    var dragOffsetPx by remember(item.id) { mutableFloatStateOf(0f) }

    val previewItem = remember(item, dragging, dragOffsetPx, pixelsPer15Minutes) {
        if (!dragging) {
            item
        } else {
            val steps = (dragOffsetPx / pixelsPer15Minutes).roundToInt()
            shiftItem(item, steps * 15)
        }
    }

    val previewStart = previewItem.startTime ?: start
    val previewEnd = previewItem.endTime?.takeIf { it.isAfter(previewStart) }

    Row(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = 0,
                    y = if (dragging) dragOffsetPx.roundToInt() else 0
                )
            }
            .zIndex(if (dragging) 2f else 0f)
            .pointerInput(item.id, item.startTime, item.endTime, item.calendarReadOnly) {
                if (item.calendarReadOnly) return@pointerInput

                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        dragging = true
                        dragOffsetPx = 0f
                    },
                    onDragCancel = {
                        dragging = false
                        dragOffsetPx = 0f
                    },
                    onDragEnd = {
                        val steps = (dragOffsetPx / pixelsPer15Minutes).roundToInt()

                        if (steps != 0) {
                            onReschedule(shiftItem(item, steps * 15))
                        }

                        dragging = false
                        dragOffsetPx = 0f
                    }
                ) { change, dragAmount ->
                    change.consume()
                    dragOffsetPx += dragAmount.y
                }
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onEdit(item) }
            ),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.width(58.dp)) {
            Text(
                text = previewStart.format(DateTimeFormatter.ofPattern("HH:mm")),
                style = MaterialTheme.typography.labelMedium,
                color = if (dragging) accent else MaterialTheme.colorScheme.onBackground
            )

            if (previewEnd != null) {
                Spacer(Modifier.height((height - 30.dp).coerceAtLeast(4.dp)))

                Text(
                    text = previewEnd.format(DateTimeFormatter.ofPattern("HH:mm")),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (dragging) accent else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Box(
            modifier = Modifier
                .padding(top = 2.dp, end = 14.dp)
                .width(if (dragging) 6.dp else 4.dp)
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

            if (dragging) {
                Spacer(Modifier.height(3.dp))

                Text(
                    text = buildString {
                        append("Release · ")
                        append(previewStart.format(DateTimeFormatter.ofPattern("HH:mm")))
                        previewEnd?.let {
                            append("–")
                            append(it.format(DateTimeFormatter.ofPattern("HH:mm")))
                        }
                        if (item.recurrence != Recurrence.ONCE) {
                            append(" · series")
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = accent
                )
            } else {
                val meta = buildList {
                    if (end != null) add(durationLabel(start, end))
                    item.reminderMinutes?.let { add("${it}m reminder") }
                    if (item.focusCycle == FocusCycle.POMODORO_25_5) {
                        add("25/5 focus")
                    }
                    item.calendarName?.let { add(it) }
                    if (!item.calendarReadOnly) add("hold + drag to move")
                }.joinToString(" · ")

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
private fun CurrentTimeMarker(
    now: LocalTime
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = now.format(
                DateTimeFormatter.ofPattern("HH:mm")
            ),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(58.dp)
        )

        Box(
            modifier = Modifier
                .width(6.dp)
                .height(6.dp)
                .background(
                    MaterialTheme.colorScheme.primary,
                    CircleShape
                )
        )

        Spacer(Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(
                    MaterialTheme.colorScheme.primary.copy(
                        alpha = 0.55f
                    )
                )
        )
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

private fun shiftItem(item: DaylineItem, deltaMinutes: Int): DaylineItem {
    val start = item.startTime ?: return item
    val end = item.endTime?.takeIf { it.isAfter(start) }

    val startMinutes = start.hour * 60 + start.minute
    val duration = end?.let {
        Duration.between(start, it).toMinutes().toInt()
    } ?: 0

    val latestStart = (24 * 60 - 1 - duration).coerceAtLeast(0)
    val shiftedStart = (startMinutes + deltaMinutes).coerceIn(0, latestStart)

    val newStart = LocalTime.of(shiftedStart / 60, shiftedStart % 60)
    val newEnd = end?.let {
        val endMinutes = shiftedStart + duration
        LocalTime.of(endMinutes / 60, endMinutes % 60)
    }

    return item.copy(
        startTime = newStart,
        endTime = newEnd
    )
}

private fun gapToSpace(minutes: Long): Dp {
    if (minutes <= 0) return 10.dp

    return (10 + (minutes.coerceAtMost(180) / 15.0).roundToInt())
        .dp
        .coerceAtMost(28.dp)
}

private fun durationToHeight(start: LocalTime, end: LocalTime?): Dp {
    val minutes = end
        ?.let { Duration.between(start, it).toMinutes() }
        ?.coerceAtLeast(30)
        ?: 45

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
