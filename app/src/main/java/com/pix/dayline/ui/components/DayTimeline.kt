package com.pix.dayline.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.pix.dayline.model.AgendaKind
import com.pix.dayline.model.DaylineItem
import com.pix.dayline.model.FocusCycle
import com.pix.dayline.model.Recurrence
import com.pix.dayline.model.TaskPriority
import com.pix.dayline.model.isCompletedOn
import com.pix.dayline.model.overlaps
import com.pix.dayline.ui.theme.composeColor
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayTimeline(
    items: List<DaylineItem>,
    date: LocalDate,
    emptyText: String = "Your day is clear.",
    onEdit: (DaylineItem) -> Unit,
    onToggleTask: (DaylineItem, LocalDate) -> Unit,
    onReschedule: (DaylineItem) -> Unit,
    onResize: (DaylineItem) -> Unit = onReschedule,
    onCreateAt: (LocalDate, LocalTime) -> Unit = { _, _ -> },
    onScheduleTask: (DaylineItem) -> Unit = onReschedule,
    onCurrentTimeTap: () -> Unit = {}
) {
    var now by remember(date) { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(date) {
        while (true) {
            now = LocalTime.now()
            delay(30_000L)
        }
    }

    val scheduled = items
        .filter { it.startTime != null }
        .sortedWith(compareBy<DaylineItem> { it.startTime }.thenBy { it.title })
    val anytime = items.filter { it.startTime == null }
    val conflictsById = remember(scheduled) {
        buildMap<String, MutableList<DaylineItem>> {
            scheduled.forEachIndexed { index, first ->
                scheduled.drop(index + 1).forEach { second ->
                    if (first.overlaps(second)) {
                        getOrPut(first.id) { mutableListOf() }.add(second)
                        getOrPut(second.id) { mutableListOf() }.add(first)
                    }
                }
            }
        }.mapValues { it.value.toList() }
    }
    var conflictSelection by remember { mutableStateOf<Pair<DaylineItem, List<DaylineItem>>?>(null) }

    if (scheduled.isEmpty() && anytime.isEmpty()) {
        Column {
            EmptyDayRail(date = date, now = now, onCreateAt = onCreateAt)
            Spacer(Modifier.height(16.dp))
            Text(
                emptyText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    Column {
        var previousEnd = LocalTime.of(6, 0)
        var nowPlaced = false

        scheduled.forEach { item ->
            val start = item.startTime ?: return@forEach
            val gapStart = previousEnd
            if (start.isAfter(gapStart.plusMinutes(14))) {
                TimelineGap(
                    date = date,
                    from = gapStart,
                    to = start,
                    onCreateAt = onCreateAt
                )
            } else {
                Spacer(Modifier.height(8.dp))
            }

            val eventEnd = item.endTime
                ?.takeIf { it.isAfter(start) }
                ?: start.plusHours(1)

            if (
                date == LocalDate.now() &&
                !nowPlaced &&
                (
                    (!now.isBefore(gapStart) && now.isBefore(start)) ||
                    (!now.isBefore(start) && now.isBefore(eventEnd))
                )
            ) {
                CurrentTimeMarker(now, onCurrentTimeTap)
                Spacer(Modifier.height(8.dp))
                nowPlaced = true
            }

            TimelineItem(
                item = item,
                date = date,
                now = now,
                conflictItems = conflictsById[item.id].orEmpty(),
                onConflict = { conflictSelection = item to conflictsById[item.id].orEmpty() },
                onEdit = onEdit,
                onToggleTask = onToggleTask,
                onReschedule = onReschedule,
                onResize = onResize
            )

            if (eventEnd.isAfter(previousEnd)) {
                previousEnd = eventEnd
            }
        }

        if (
            date == LocalDate.now() &&
            scheduled.isNotEmpty() &&
            !nowPlaced &&
            !now.isBefore(previousEnd)
        ) {
            Spacer(Modifier.height(10.dp))
            CurrentTimeMarker(now, onCurrentTimeTap)
        }

        if (previousEnd.isBefore(LocalTime.of(22, 0))) {
            TimelineGap(
                date = date,
                from = previousEnd,
                to = LocalTime.of(22, 0),
                onCreateAt = onCreateAt
            )
        }

        if (anytime.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            Text(
                "ANYTIME",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                anytime.forEach { item ->
                    AnytimeItem(
                        item = item,
                        date = date,
                        onEdit = onEdit,
                        onToggleTask = onToggleTask,
                        onScheduleTask = onScheduleTask
                    )
                }
            }
        }
    }

    conflictSelection?.let { (selected, peers) ->
        ConflictSheet(
            selected = selected,
            peers = peers,
            onDismiss = { conflictSelection = null }
        )
    }
}

@Composable
private fun TimelineItem(
    item: DaylineItem,
    date: LocalDate,
    now: LocalTime,
    conflictItems: List<DaylineItem>,
    onConflict: () -> Unit,
    onEdit: (DaylineItem) -> Unit,
    onToggleTask: (DaylineItem, LocalDate) -> Unit,
    onReschedule: (DaylineItem) -> Unit,
    onResize: (DaylineItem) -> Unit
) {
    val start = item.startTime ?: return
    val end = item.endTime?.takeIf { it.isAfter(start) } ?: start.plusHours(1)
    val completed = item.kind == AgendaKind.TASK && item.isCompletedOn(date)
    val accent = item.color.composeColor()
    val height = durationToHeight(start, end)
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val pixelsPer15Minutes = with(density) { 18.dp.toPx() }
    val conflict = conflictItems.isNotEmpty()

    var hasRendered by remember(item.id) { mutableStateOf(false) }
    var changedPulse by remember(item.id) { mutableStateOf(false) }
    LaunchedEffect(item.startTime, item.endTime) {
        if (hasRendered) {
            changedPulse = true
            delay(420L)
            changedPulse = false
        } else {
            hasRendered = true
        }
    }
    val railAlpha by animateFloatAsState(
        targetValue = if (changedPulse) 0.45f else 1f,
        label = "timeline-change"
    )

    var dragging by remember(item.id, item.startTime) { mutableStateOf(false) }
    var dragOffsetPx by remember(item.id, item.startTime) { mutableFloatStateOf(0f) }
    var lastDragStep by remember(item.id) { mutableIntStateOf(0) }
    var resizing by remember(item.id, item.endTime) { mutableStateOf(false) }
    var resizeOffsetPx by remember(item.id, item.endTime) { mutableFloatStateOf(0f) }
    var lastResizeStep by remember(item.id) { mutableIntStateOf(0) }

    val dragStep = (dragOffsetPx / pixelsPer15Minutes).roundToInt()
    val previewItem = if (dragging) shiftItem(item, dragStep * 15) else item
    val previewStart = previewItem.startTime ?: start
    val previewEnd = previewItem.endTime?.takeIf { it.isAfter(previewStart) } ?: previewStart.plusHours(1)

    val resizeStep = (resizeOffsetPx / pixelsPer15Minutes).roundToInt()
    val resizedEnd = if (resizing) {
        shiftEnd(item, resizeStep * 15).endTime ?: end
    } else end

    Column {
        if (item.bufferBeforeMinutes > 0) {
            Text(
                "BUFFER · ${item.bufferBeforeMinutes}M BEFORE",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
            )
            Spacer(Modifier.height(5.dp))
        }

        Row(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = 0,
                        y = if (dragging) dragOffsetPx.roundToInt() else 0
                    )
                }
                .zIndex(if (dragging || resizing) 2f else 0f)
                .pointerInput(item.id, item.startTime, item.endTime, item.calendarReadOnly) {
                    if (item.calendarReadOnly) return@pointerInput

                    // Keep gesture-local values inside the pointerInput coroutine.
                    // A derived Compose value such as dragStep can be stale when
                    // onDragEnd runs because this pointerInput block stays alive
                    // across recompositions.
                    var gestureOffsetPx = 0f
                    var gestureStep = 0

                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            gestureOffsetPx = 0f
                            gestureStep = 0
                            dragging = true
                            dragOffsetPx = 0f
                            lastDragStep = 0
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onDragCancel = {
                            gestureOffsetPx = 0f
                            gestureStep = 0
                            dragging = false
                            dragOffsetPx = 0f
                        },
                        onDragEnd = {
                            val commitStep = gestureStep
                            dragging = false
                            dragOffsetPx = 0f

                            if (commitStep != 0) {
                                onReschedule(
                                    shiftItem(
                                        item,
                                        commitStep * 15
                                    )
                                )
                            }
                        }
                    ) { change, dragAmount ->
                        change.consume()

                        gestureOffsetPx += dragAmount.y
                        gestureStep =
                            (gestureOffsetPx / pixelsPer15Minutes).roundToInt()

                        dragOffsetPx = gestureOffsetPx

                        if (gestureStep != lastDragStep) {
                            lastDragStep = gestureStep
                            haptics.performHapticFeedback(
                                HapticFeedbackType.SegmentFrequentTick
                            )
                        }
                    }
                }
                .clickable(
                    enabled = !dragging,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { onEdit(item) }
                ),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.width(58.dp)) {
                Text(
                    previewStart.format(DateTimeFormatter.ofPattern("HH:mm")),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (dragging || resizing) accent else MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height((height - 30.dp).coerceAtLeast(4.dp)))
                Text(
                    resizedEnd.format(DateTimeFormatter.ofPattern("HH:mm")),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (resizing) accent else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .padding(top = 2.dp, end = 14.dp)
                    .width(if (dragging || resizing) 6.dp else 4.dp)
                    .height(height)
                    .background(accent.copy(alpha = railAlpha), RoundedCornerShape(99.dp))
            )

            Column(modifier = Modifier.padding(top = 1.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        item.title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        textDecoration = if (completed) TextDecoration.LineThrough else TextDecoration.None
                    )
                    if (conflict) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "!",
                            modifier = Modifier
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = onConflict
                                )
                                .padding(horizontal = 5.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    if (item.kind == AgendaKind.TASK) {
                        Spacer(Modifier.width(10.dp))
                        Text(
                            if (completed) "✓" else "○",
                            modifier = Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onToggleTask(item, date) },
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                Spacer(Modifier.height(3.dp))
                Text(
                    timelineMeta(item, start, end, date, now, conflict),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (conflict) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!item.calendarReadOnly) {
                    Spacer(Modifier.height(9.dp))
                    Box(
                        modifier = Modifier
                            .width(64.dp)
                            .height(28.dp)
                            .pointerInput(item.id, item.endTime) {
                                // The handle responds immediately, but commit from gesture-local
                                // state so the final 15-minute step cannot be lost to recomposition.
                                var gestureOffsetPx = 0f
                                var gestureStep = 0

                                detectDragGestures(
                                    onDragStart = {
                                        gestureOffsetPx = 0f
                                        gestureStep = 0
                                        resizing = true
                                        resizeOffsetPx = 0f
                                        lastResizeStep = 0
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                    onDragCancel = {
                                        gestureOffsetPx = 0f
                                        gestureStep = 0
                                        resizing = false
                                        resizeOffsetPx = 0f
                                    },
                                    onDragEnd = {
                                        val commitStep = gestureStep
                                        resizing = false
                                        resizeOffsetPx = 0f
                                        if (commitStep != 0) {
                                            onResize(shiftEnd(item, commitStep * 15))
                                        }
                                    }
                                ) { change, amount ->
                                    change.consume()
                                    gestureOffsetPx += amount.y
                                    gestureStep =
                                        (gestureOffsetPx / pixelsPer15Minutes).roundToInt()
                                    resizeOffsetPx = gestureOffsetPx
                                    if (gestureStep != lastResizeStep) {
                                        lastResizeStep = gestureStep
                                        haptics.performHapticFeedback(
                                            HapticFeedbackType.SegmentFrequentTick
                                        )
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            Modifier
                                .width(38.dp)
                                .height(3.dp)
                                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f), CircleShape)
                        )
                    }
                }
            }
        }

        if (item.bufferAfterMinutes > 0) {
            Spacer(Modifier.height(5.dp))
            Text(
                "BUFFER · ${item.bufferAfterMinutes}M AFTER",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
            )
        }
    }
}

@Composable
private fun TimelineGap(
    date: LocalDate,
    from: LocalTime,
    to: LocalTime,
    onCreateAt: (LocalDate, LocalTime) -> Unit
) {
    val total = Duration.between(from, to).toMinutes().coerceAtLeast(15L)
    val height = (total / 15L * 5L).coerceIn(16L, 52L).toInt().dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .pointerInput(date, from, to) {
                detectTapGestures { offset ->
                    val fraction = if (size.height == 0) 0f else (offset.y / size.height).coerceIn(0f, 1f)
                    val raw = (total * fraction).roundToInt()
                    val snapped = (raw / 15) * 15
                    val target = from.plusMinutes(snapped.toLong())
                    onCreateAt(date, target)
                }
            }
    )
}

@Composable
private fun EmptyDayRail(
    date: LocalDate,
    now: LocalTime,
    onCreateAt: (LocalDate, LocalTime) -> Unit
) {
    val from = LocalTime.of(6, 0)
    val to = LocalTime.of(22, 0)
    val total = Duration.between(from, to).toMinutes()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .pointerInput(date) {
                detectTapGestures { offset ->
                    val fraction = if (size.height == 0) 0f else (offset.y / size.height).coerceIn(0f, 1f)
                    val raw = (total * fraction).roundToInt()
                    val snapped = (raw / 15) * 15
                    onCreateAt(date, from.plusMinutes(snapped.toLong()))
                }
            }
    ) {
        Text(
            "Tap the empty day to add a time",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.Center)
        )
        if (date == LocalDate.now() && !now.isBefore(from) && now.isBefore(to)) {
            Text(
                now.format(DateTimeFormatter.ofPattern("HH:mm")),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.CenterStart)
            )
        }
    }
}

@Composable
private fun CurrentTimeMarker(now: LocalTime, onClick: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                onClick()
            }
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            now.format(DateTimeFormatter.ofPattern("HH:mm")),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(58.dp)
        )
        Box(
            Modifier
                .width(6.dp)
                .height(6.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
        )
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.55f))
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConflictSheet(
    selected: DaylineItem,
    peers: List<DaylineItem>,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 26.dp).padding(bottom = 32.dp)) {
            Text("Overlap", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(6.dp))
            Text(
                "These blocks occupy the same time. Dayline keeps both and marks the collision instead of moving anything silently.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(22.dp))
            (listOf(selected) + peers).distinctBy { it.id }.forEach { item ->
                Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .width(5.dp)
                            .height(28.dp)
                            .background(item.color.composeColor(), RoundedCornerShape(99.dp))
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.title, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            conflictTime(item),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun conflictTime(item: DaylineItem): String {
    val start = item.startTime ?: return "Anytime"
    val end = item.endTime?.takeIf { it.isAfter(start) } ?: start.plusHours(1)
    return "${start.format(DateTimeFormatter.ofPattern("HH:mm"))}–${end.format(DateTimeFormatter.ofPattern("HH:mm"))}"
}

@Composable
private fun AnytimeItem(
    item: DaylineItem,
    date: LocalDate,
    onEdit: (DaylineItem) -> Unit,
    onToggleTask: (DaylineItem, LocalDate) -> Unit,
    onScheduleTask: (DaylineItem) -> Unit
) {
    val completed = item.kind == AgendaKind.TASK && item.isCompletedOn(date)
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val pixelsPer15 = with(density) { 18.dp.toPx() }
    var drag by remember(item.id) { mutableFloatStateOf(0f) }
    var dragging by remember(item.id) { mutableStateOf(false) }
    var lastStep by remember(item.id) { mutableIntStateOf(0) }
    val step = (drag / pixelsPer15).roundToInt()
    val previewTime = LocalTime.of(9, 0).plusMinutes((step * 15).toLong()).let {
        if (it.isBefore(LocalTime.of(0, 15))) LocalTime.of(0, 15) else it
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(item.id, item.calendarReadOnly) {
                if (
                    item.kind != AgendaKind.TASK ||
                    item.calendarReadOnly
                ) {
                    return@pointerInput
                }

                var gestureOffsetPx = 0f
                var gestureStep = 0

                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        gestureOffsetPx = 0f
                        gestureStep = 0
                        dragging = true
                        drag = 0f
                        lastStep = 0
                        haptics.performHapticFeedback(
                            HapticFeedbackType.LongPress
                        )
                    },
                    onDragCancel = {
                        gestureOffsetPx = 0f
                        gestureStep = 0
                        dragging = false
                        drag = 0f
                    },
                    onDragEnd = {
                        val targetTime = LocalTime.of(9, 0)
                            .plusMinutes((gestureStep * 15).toLong())
                            .let {
                                if (it.isBefore(LocalTime.of(0, 15))) {
                                    LocalTime.of(0, 15)
                                } else {
                                    it
                                }
                            }

                        val scheduled = item.copy(
                            startTime = targetTime,
                            endTime = targetTime.plusHours(1)
                        )

                        dragging = false
                        drag = 0f
                        onScheduleTask(scheduled)
                    }
                ) { change, amount ->
                    change.consume()

                    gestureOffsetPx += amount.y
                    gestureStep =
                        (gestureOffsetPx / pixelsPer15).roundToInt()
                    drag = gestureOffsetPx

                    if (gestureStep != lastStep) {
                        lastStep = gestureStep
                        haptics.performHapticFeedback(
                            HapticFeedbackType.SegmentFrequentTick
                        )
                    }
                }
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onEdit(item) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .padding(end = 14.dp)
                .width(6.dp)
                .height(6.dp)
                .background(item.color.composeColor(), CircleShape)
        )
        Column {
            Text(
                item.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textDecoration = if (completed) TextDecoration.LineThrough else TextDecoration.None
            )
            if (dragging) {
                Text(
                    "Release to schedule · ${previewTime.format(DateTimeFormatter.ofPattern("HH:mm"))}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = item.color.composeColor()
                )
            } else if (item.kind == AgendaKind.TASK) {
                Text(
                    "${item.priority.name.lowercase()} · hold + drag to schedule",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (item.kind == AgendaKind.TASK) {
            Spacer(Modifier.width(12.dp))
            Text(
                if (completed) "✓" else "○",
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onToggleTask(item, date) },
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

private fun shiftItem(item: DaylineItem, deltaMinutes: Int): DaylineItem {
    val start = item.startTime ?: return item
    val end = item.endTime?.takeIf { it.isAfter(start) }
    val startMinutes = start.hour * 60 + start.minute
    val duration = end?.let { Duration.between(start, it).toMinutes().toInt() } ?: 60
    val latestStart = (24 * 60 - 1 - duration).coerceAtLeast(0)
    val shiftedStart = (startMinutes + deltaMinutes).coerceIn(0, latestStart)
    val newStart = LocalTime.of(shiftedStart / 60, shiftedStart % 60)
    val endMinutes = (shiftedStart + duration).coerceAtMost(24 * 60 - 1)
    val newEnd = LocalTime.of(endMinutes / 60, endMinutes % 60)
    return item.copy(startTime = newStart, endTime = newEnd)
}

private fun shiftEnd(item: DaylineItem, deltaMinutes: Int): DaylineItem {
    val start = item.startTime ?: return item
    val originalEnd = item.endTime?.takeIf { it.isAfter(start) } ?: start.plusHours(1)
    val startMinutes = start.hour * 60 + start.minute
    val endMinutes = originalEnd.hour * 60 + originalEnd.minute
    val shifted = (endMinutes + deltaMinutes).coerceIn(startMinutes + 15, 24 * 60 - 1)
    return item.copy(endTime = LocalTime.of(shifted / 60, shifted % 60))
}

private fun durationToHeight(start: LocalTime, end: LocalTime): Dp {
    val minutes = Duration.between(start, end).toMinutes().coerceAtLeast(30)
    val value = 42 + (minutes.coerceAtMost(360) / 60.0 * 12.0).roundToInt()
    return value.dp.coerceIn(46.dp, 114.dp)
}

private fun timelineMeta(
    item: DaylineItem,
    start: LocalTime,
    end: LocalTime,
    date: LocalDate,
    now: LocalTime,
    conflict: Boolean
): String = buildList {
    val minutes = Duration.between(start, end).toMinutes()
    add(if (minutes % 60L == 0L) "${minutes / 60}h" else "${minutes / 60}h ${minutes % 60}m")

    if (date == LocalDate.now() && !now.isBefore(start) && now.isBefore(end)) {
        val left = Duration.between(now, end).toMinutes().coerceAtLeast(0)
        val total = Duration.between(start, end).toMinutes().coerceAtLeast(1)
        val elapsed = Duration.between(start, now).toMinutes().coerceIn(0, total)
        add("${left}m left")
        add("${elapsed * 100 / total}%")
    }
    if (item.focusCycle != FocusCycle.OFF) add("${item.focusMinutes}/${item.breakMinutes} focus")
    if (item.focusSessionsCompleted > 0) add("${item.focusSessionsCompleted} sessions")
    item.calendarName?.let(::add)
    if (conflict) add("overlap")
}.joinToString(" · ")
