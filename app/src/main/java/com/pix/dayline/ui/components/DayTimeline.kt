package com.pix.dayline.ui.components

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.alpha
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
import com.pix.dayline.model.isCompletedOn
import com.pix.dayline.model.overlaps
import com.pix.dayline.planning.FreeSlot
import com.pix.dayline.planning.PlanningEngine
import com.pix.dayline.ui.theme.composeColor
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs
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
    onCreateTaskAt: (LocalDate, LocalTime) -> Unit = { _, _ -> },
    onStartFocusAt: (LocalDate, LocalTime, LocalTime) -> Unit = { _, _, _ -> },
    onScheduleTask: (DaylineItem) -> Unit = onReschedule,
    onCurrentTimeTap: () -> Unit = {},
    onAutoScroll: (Float) -> Unit = {}
) {
    var now by remember(date) { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(date) {
        while (true) {
            now = LocalTime.now()
            delay(30_000L)
        }
    }

    val allDay = items.filter { it.kind == AgendaKind.EVENT && (it.allDay || it.startTime == null) }
    val scheduled = items.filter { it.startTime != null && !it.allDay }
        .sortedWith(compareBy<DaylineItem> { it.startTime }.thenBy { it.title })
    val anytimeTasks = items.filter { it.kind == AgendaKind.TASK && it.startTime == null }
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
    var freeSelection by remember { mutableStateOf<FreeSlot?>(null) }

    Column {
        if (allDay.isNotEmpty()) {
            AllDayStrip(allDay, onEdit)
            Spacer(Modifier.height(22.dp))
        }

        if (scheduled.isEmpty() && anytimeTasks.isEmpty()) {
            EmptyDayRail(date = date, now = now, onCreateAt = onCreateAt)
            Spacer(Modifier.height(14.dp))
            Text(emptyText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            var previousEnd = LocalTime.of(6, 0)
            var nowPlaced = false

            scheduled.forEach { item ->
                val start = item.startTime ?: return@forEach
                val gapStart = previousEnd
                if (start.isAfter(gapStart.plusMinutes(14))) {
                    TimelineGap(date, gapStart, start) { freeSelection = it }
                } else Spacer(Modifier.height(10.dp))

                val eventEnd = effectiveEnd(item)
                if (date == LocalDate.now() && !nowPlaced &&
                    ((!now.isBefore(gapStart) && now.isBefore(start)) || (!now.isBefore(start) && now.isBefore(eventEnd)))) {
                    CurrentTimeMarker(now, onCurrentTimeTap)
                    Spacer(Modifier.height(10.dp))
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
                    onResize = onResize,
                    onAutoScroll = onAutoScroll
                )
                if (eventEnd.isAfter(previousEnd)) previousEnd = eventEnd
            }

            if (date == LocalDate.now() && scheduled.isNotEmpty() && !nowPlaced && !now.isBefore(previousEnd)) {
                Spacer(Modifier.height(12.dp))
                CurrentTimeMarker(now, onCurrentTimeTap)
            }

            if (previousEnd.isBefore(LocalTime.of(22, 0))) {
                TimelineGap(date, previousEnd, LocalTime.of(22, 0)) { freeSelection = it }
            }

            if (anytimeTasks.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                Text("ANYTIME", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    anytimeTasks.forEach { item ->
                        AnytimeItem(item, date, onEdit, onToggleTask, onScheduleTask)
                    }
                }
            }
        }
    }

    freeSelection?.let { slot ->
        FreeGapSheet(
            slot = slot,
            onEvent = { onCreateAt(date, slot.start); freeSelection = null },
            onTask = { onCreateTaskAt(date, slot.start); freeSelection = null },
            onFocus = { onStartFocusAt(date, slot.start, slot.end); freeSelection = null },
            onDismiss = { freeSelection = null }
        )
    }

    conflictSelection?.let { (selected, peers) ->
        ConflictSheet(
            selected = selected,
            peers = peers,
            date = date,
            allItems = scheduled,
            onMove = { onReschedule(it); conflictSelection = null },
            onDismiss = { conflictSelection = null }
        )
    }
}

@Composable
private fun AllDayStrip(items: List<DaylineItem>, onEdit: (DaylineItem) -> Unit) {
    Column {
        Text("ALL DAY", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        items.forEach { item ->
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { onEdit(item) }.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.width(5.dp).height(28.dp).background(item.color.composeColor(), RoundedCornerShape(99.dp)))
                Spacer(Modifier.width(12.dp))
                Text(item.title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Text("ALL DAY", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
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
    onResize: (DaylineItem) -> Unit,
    onAutoScroll: (Float) -> Unit
) {
    val start = item.startTime ?: return
    val end = effectiveEnd(item)
    val completed = item.kind == AgendaKind.TASK && item.isCompletedOn(date)
    val past = date == LocalDate.now() && !end.isAfter(now)
    val accent = item.color.composeColor()
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val pixelsPerFive = with(density) { 8.dp.toPx() }
    val conflict = conflictItems.isNotEmpty()

    var dragging by remember(item.id, item.startTime) { mutableStateOf(false) }
    var dragOffsetPx by remember(item.id, item.startTime) { mutableFloatStateOf(0f) }
    var lastDragStep by remember(item.id) { mutableIntStateOf(0) }
    var resizing by remember(item.id, item.endTime) { mutableStateOf(false) }
    var resizeOffsetPx by remember(item.id, item.endTime) { mutableFloatStateOf(0f) }
    var lastResizeStep by remember(item.id) { mutableIntStateOf(0) }

    val dragStep = (dragOffsetPx / pixelsPerFive).roundToInt()
    val previewItem = if (dragging) shiftItem(item, dragStep * 5) else item
    val previewStart = previewItem.startTime ?: start
    val previewEnd = effectiveEnd(previewItem)
    val resizeStep = (resizeOffsetPx / pixelsPerFive).roundToInt()
    val resizedEnd = if (resizing) shiftEnd(item, resizeStep * 5).endTime ?: end else end
    val displayEnd = if (resizing) resizedEnd else previewEnd
    val displayHeight = durationToHeight(previewStart, displayEnd)

    Column(Modifier.alpha(if (past && !dragging && !resizing) 0.48f else 1f)) {
        if (item.bufferBeforeMinutes > 0) {
            Text("BUFFER · ${item.bufferBeforeMinutes}M BEFORE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .65f))
            Spacer(Modifier.height(5.dp))
        }

        if (dragging || resizing) {
            Surface(shape = RoundedCornerShape(99.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                Text(
                    if (resizing) "END ${displayEnd.format(TIME)} · ${durationLabel(Duration.between(previewStart, displayEnd).toMinutes().toInt())}"
                    else "MOVE ${previewStart.format(TIME)} → ${previewEnd.format(TIME)}",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = accent
                )
            }
            Spacer(Modifier.height(7.dp))
        }

        Row(
            modifier = Modifier.offset { IntOffset(0, if (dragging) dragOffsetPx.roundToInt() else 0) }
                .zIndex(if (dragging || resizing) 2f else 0f),
            verticalAlignment = Alignment.Top
        ) {
            Column(Modifier.width(58.dp)) {
                Text(previewStart.format(TIME), style = MaterialTheme.typography.labelMedium, color = if (dragging || resizing) accent else MaterialTheme.colorScheme.onBackground)
                Spacer(Modifier.height((displayHeight - 30.dp).coerceAtLeast(4.dp)))
                Text(displayEnd.format(TIME), style = MaterialTheme.typography.labelMedium, color = if (resizing) accent else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box(Modifier.padding(top = 2.dp, end = 14.dp).width(if (dragging || resizing) 6.dp else 4.dp).height(displayHeight).background(accent, RoundedCornerShape(99.dp)))

            Column(Modifier.weight(1f).padding(top = 1.dp)) {
                Column(
                    Modifier.fillMaxWidth()
                        .pointerInput(item.id, item.startTime, item.endTime, item.calendarReadOnly) {
                            if (item.calendarReadOnly) return@pointerInput
                            var gestureOffset = 0f
                            var gestureStep = 0
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    dragging = true; dragOffsetPx = 0f; lastDragStep = 0; gestureOffset = 0f; gestureStep = 0
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onDragCancel = { dragging = false; dragOffsetPx = 0f },
                                onDragEnd = {
                                    val commit = gestureStep
                                    dragging = false; dragOffsetPx = 0f
                                    if (commit != 0) onReschedule(shiftItem(item, commit * 5))
                                }
                            ) { change, amount ->
                                change.consume()
                                gestureOffset += amount.y
                                gestureStep = (gestureOffset / pixelsPerFive).roundToInt()
                                dragOffsetPx = gestureOffset
                                if (gestureStep != lastDragStep) {
                                    lastDragStep = gestureStep
                                    haptics.performHapticFeedback(if (gestureStep % 3 == 0) HapticFeedbackType.SegmentTick else HapticFeedbackType.SegmentFrequentTick)
                                }
                                if (abs(gestureOffset) > 140f) onAutoScroll(amount.y * .7f)
                            }
                        }
                        .clickable(enabled = !dragging && !resizing, interactionSource = remember { MutableInteractionSource() }, indication = null) { onEdit(item) }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(item.title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground, textDecoration = if (completed) TextDecoration.LineThrough else TextDecoration.None, modifier = Modifier.weight(1f, fill = false))
                        if (conflict) {
                            Spacer(Modifier.width(8.dp))
                            Text("!", modifier = Modifier.clickable(onClick = onConflict).padding(8.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.error)
                        }
                        if (item.kind == AgendaKind.TASK) {
                            Spacer(Modifier.width(8.dp))
                            Text(if (completed) "✓" else "○", modifier = Modifier.clickable { onToggleTask(item, date) }.padding(8.dp), style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(timelineMeta(item, previewStart, displayEnd, date, now, conflict), style = MaterialTheme.typography.bodyMedium, color = if (conflict) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                }

                if (!item.calendarReadOnly) {
                    Spacer(Modifier.height(6.dp))
                    Box(
                        Modifier.width(120.dp).height(48.dp)
                            .pointerInput(item.id, item.endTime) {
                                var gestureOffset = 0f
                                var gestureStep = 0
                                detectDragGestures(
                                    onDragStart = {
                                        resizing = true; resizeOffsetPx = 0f; lastResizeStep = 0; gestureOffset = 0f; gestureStep = 0
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                    onDragCancel = { resizing = false; resizeOffsetPx = 0f },
                                    onDragEnd = {
                                        val commit = gestureStep
                                        resizing = false; resizeOffsetPx = 0f
                                        if (commit != 0) onResize(shiftEnd(item, commit * 5))
                                    }
                                ) { change, amount ->
                                    change.consume(); gestureOffset += amount.y
                                    gestureStep = (gestureOffset / pixelsPerFive).roundToInt(); resizeOffsetPx = gestureOffset
                                    if (gestureStep != lastResizeStep) {
                                        lastResizeStep = gestureStep
                                        haptics.performHapticFeedback(if (gestureStep % 3 == 0) HapticFeedbackType.SegmentTick else HapticFeedbackType.SegmentFrequentTick)
                                    }
                                    if (abs(gestureOffset) > 140f) onAutoScroll(amount.y * .7f)
                                }
                            },
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Box(Modifier.width(48.dp).height(4.dp).background(if (resizing) accent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .34f), CircleShape))
                    }
                }
            }
        }

        if (item.bufferAfterMinutes > 0) {
            Spacer(Modifier.height(5.dp))
            Text("BUFFER · ${item.bufferAfterMinutes}M AFTER", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .65f))
        }
    }
}

@Composable
private fun TimelineGap(date: LocalDate, from: LocalTime, to: LocalTime, onSelect: (FreeSlot) -> Unit) {
    val total = Duration.between(from, to).toMinutes().coerceAtLeast(15L)
    val visible = total >= 30
    val height = if (visible) 48.dp else 24.dp
    Box(Modifier.fillMaxWidth().height(height).clickable { onSelect(FreeSlot(date, from, to)) }, contentAlignment = Alignment.CenterStart) {
        if (visible) {
            Text(
                "FREE · ${from.format(TIME)}–${to.format(TIME)} · ${durationLabel(total.toInt())}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .72f)
            )
        }
    }
}

@Composable
private fun EmptyDayRail(date: LocalDate, now: LocalTime, onCreateAt: (LocalDate, LocalTime) -> Unit) {
    val from = LocalTime.of(6, 0)
    val to = LocalTime.of(22, 0)
    val total = Duration.between(from, to).toMinutes()
    Box(Modifier.fillMaxWidth().height(190.dp).pointerInput(date) {
        detectTapGestures { offset ->
            val fraction = if (size.height == 0) 0f else (offset.y / size.height).coerceIn(0f, 1f)
            val raw = (total * fraction).roundToInt()
            val snapped = (raw / 5) * 5
            onCreateAt(date, from.plusMinutes(snapped.toLong()))
        }
    }) {
        Text("Tap the empty day to add a time", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.Center))
        if (date == LocalDate.now() && !now.isBefore(from) && now.isBefore(to)) {
            Text(now.format(TIME), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.align(Alignment.CenterStart))
        }
    }
}

@Composable
private fun CurrentTimeMarker(now: LocalTime, onClick: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable {
            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick); onClick()
        }.padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(now.format(TIME), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.width(58.dp))
        Box(Modifier.width(7.dp).height(7.dp).background(MaterialTheme.colorScheme.onBackground, CircleShape))
        Spacer(Modifier.width(9.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.onBackground.copy(alpha = .28f)))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FreeGapSheet(slot: FreeSlot, onEvent: () -> Unit, onTask: () -> Unit, onFocus: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 28.dp).padding(bottom = 34.dp)) {
            Text("Free time", style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(6.dp))
            Text("${slot.start.format(TIME)} — ${slot.end.format(TIME)} · ${durationLabel(slot.durationMinutes)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(22.dp))
            SheetAction("Add event", onEvent)
            SheetAction("Schedule task", onTask)
            SheetAction("Start Focus", onFocus)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConflictSheet(
    selected: DaylineItem,
    peers: List<DaylineItem>,
    date: LocalDate,
    allItems: List<DaylineItem>,
    onMove: (DaylineItem) -> Unit,
    onDismiss: () -> Unit
) {
    val peersDistinct = peers.distinctBy { it.id }
    val totalOverlap = peersDistinct.maxOfOrNull { PlanningEngine.overlapMinutes(selected, it) } ?: 0
    val duration = selected.planningDurationMinutes
    val candidates = allItems.filterNot { it.id == selected.id }
    val selectedStart = selected.startTime
    val selectedEnd = selectedStart?.let { effectiveEnd(selected) }
    val afterPeer = peersDistinct.mapNotNull { it.startTime?.let { _ -> effectiveEnd(it) } }.maxOrNull()
    val moveAfter = afterPeer?.let { PlanningEngine.nextSlotAfter(candidates, date, it, duration) }
    val nextFree = selectedEnd?.let { PlanningEngine.nextSlotAfter(candidates, date, it, duration) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 28.dp).padding(bottom = 34.dp)) {
            Text("Time conflict", style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(7.dp))
            Text("${selected.title} overlaps by ${totalOverlap} min.", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(18.dp))
            peersDistinct.forEach { item ->
                Text("${item.title} · ${item.startTime?.format(TIME)}–${effectiveEnd(item).format(TIME)}", modifier = Modifier.padding(vertical = 5.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(20.dp))
            moveAfter?.let { slot -> SheetAction("Move after conflict · ${slot.start.format(TIME)}") { onMove(selected.copy(startTime = slot.start, endTime = slot.end)) } }
            nextFree?.takeIf { it != moveAfter }?.let { slot -> SheetAction("Next free slot · ${slot.start.format(TIME)}") { onMove(selected.copy(startTime = slot.start, endTime = slot.end)) } }
            SheetAction("Keep overlap", onDismiss)
        }
    }
}

@Composable
private fun SheetAction(label: String, onClick: () -> Unit) {
    Text(label, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(onClick = onClick).padding(vertical = 13.dp), style = MaterialTheme.typography.bodyLarge)
}

@Composable
private fun AnytimeItem(
    item: DaylineItem,
    date: LocalDate,
    onEdit: (DaylineItem) -> Unit,
    onToggleTask: (DaylineItem, LocalDate) -> Unit,
    onScheduleTask: (DaylineItem) -> Unit
) {
    val completed = item.isCompletedOn(date)
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val pixelsPerFive = with(density) { 8.dp.toPx() }
    var drag by remember(item.id) { mutableFloatStateOf(0f) }
    var dragging by remember(item.id) { mutableStateOf(false) }
    var lastStep by remember(item.id) { mutableIntStateOf(0) }
    val step = (drag / pixelsPerFive).roundToInt()
    val previewTime = clampTime(LocalTime.of(9, 0), step * 5, item.estimatedDurationMinutes)

    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp)
            .pointerInput(item.id, item.calendarReadOnly) {
                if (item.calendarReadOnly) return@pointerInput
                var gestureOffset = 0f
                var gestureStep = 0
                detectDragGesturesAfterLongPress(
                    onDragStart = { dragging = true; drag = 0f; lastStep = 0; haptics.performHapticFeedback(HapticFeedbackType.LongPress) },
                    onDragCancel = { dragging = false; drag = 0f },
                    onDragEnd = {
                        val target = clampTime(LocalTime.of(9, 0), gestureStep * 5, item.estimatedDurationMinutes)
                        dragging = false; drag = 0f
                        onScheduleTask(item.copy(startDate = date, startTime = target, endTime = target.plusMinutes(item.estimatedDurationMinutes.toLong())))
                    }
                ) { change, amount ->
                    change.consume(); gestureOffset += amount.y; gestureStep = (gestureOffset / pixelsPerFive).roundToInt(); drag = gestureOffset
                    if (gestureStep != lastStep) { lastStep = gestureStep; haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick) }
                }
            }
            .clickable { onEdit(item) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.padding(end = 14.dp).width(6.dp).height(6.dp).background(item.color.composeColor(), CircleShape))
        Column(Modifier.weight(1f)) {
            Text(item.title, style = MaterialTheme.typography.bodyLarge, textDecoration = if (completed) TextDecoration.LineThrough else TextDecoration.None)
            Text(if (dragging) "Release · ${previewTime.format(TIME)}" else "${durationLabel(item.estimatedDurationMinutes)} · hold + drag to schedule", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(if (completed) "✓" else "○", modifier = Modifier.clickable { onToggleTask(item, date) }.padding(10.dp), style = MaterialTheme.typography.titleMedium)
    }
}

private fun effectiveEnd(item: DaylineItem): LocalTime {
    val start = item.startTime ?: return LocalTime.of(0, 0)
    return item.endTime?.takeIf { it.isAfter(start) }
        ?: start.plusMinutes(if (item.kind == AgendaKind.TASK) item.estimatedDurationMinutes.toLong() else 60L)
}

private fun shiftItem(item: DaylineItem, deltaMinutes: Int): DaylineItem {
    val start = item.startTime ?: return item
    val end = effectiveEnd(item)
    val duration = Duration.between(start, end).toMinutes().toInt().coerceAtLeast(5)
    val startMinutes = start.hour * 60 + start.minute
    val latestStart = (24 * 60 - 1 - duration).coerceAtLeast(0)
    val shiftedStart = (startMinutes + deltaMinutes).coerceIn(0, latestStart)
    val newStart = LocalTime.of(shiftedStart / 60, shiftedStart % 60)
    val endMinutes = (shiftedStart + duration).coerceAtMost(24 * 60 - 1)
    return item.copy(startTime = newStart, endTime = LocalTime.of(endMinutes / 60, endMinutes % 60), allDay = false)
}

private fun shiftEnd(item: DaylineItem, deltaMinutes: Int): DaylineItem {
    val start = item.startTime ?: return item
    val originalEnd = effectiveEnd(item)
    val startMinutes = start.hour * 60 + start.minute
    val endMinutes = originalEnd.hour * 60 + originalEnd.minute
    val shifted = (endMinutes + deltaMinutes).coerceIn(startMinutes + 5, 24 * 60 - 1)
    return item.copy(endTime = LocalTime.of(shifted / 60, shifted % 60), allDay = false)
}

private fun clampTime(base: LocalTime, deltaMinutes: Int, durationMinutes: Int): LocalTime {
    val baseMinutes = base.hour * 60 + base.minute
    val latest = (24 * 60 - 1 - durationMinutes).coerceAtLeast(0)
    val value = (baseMinutes + deltaMinutes).coerceIn(0, latest)
    return LocalTime.of(value / 60, value % 60)
}

private fun durationToHeight(start: LocalTime, end: LocalTime): Dp {
    val minutes = Duration.between(start, end).toMinutes().coerceAtLeast(5)
    return (46 + (minutes.coerceAtMost(360) / 60.0 * 12.0).roundToInt()).dp.coerceIn(48.dp, 116.dp)
}

private fun timelineMeta(item: DaylineItem, start: LocalTime, end: LocalTime, date: LocalDate, now: LocalTime, conflict: Boolean): String = buildList {
    add(durationLabel(Duration.between(start, end).toMinutes().toInt()))
    if (date == LocalDate.now() && !now.isBefore(start) && now.isBefore(end)) {
        add("${Duration.between(now, end).toMinutes().coerceAtLeast(0)}m left")
    }
    if (item.focusCycle != FocusCycle.OFF) add("${item.focusMinutes}/${item.breakMinutes} focus")
    item.calendarName?.let(::add)
    item.timeZoneId?.takeIf { it != java.time.ZoneId.systemDefault().id }?.let { add(it.substringAfterLast('/')) }
    if (conflict) add("overlap")
}.joinToString(" · ")

private fun durationLabel(minutes: Int): String = when {
    minutes >= 60 && minutes % 60 == 0 -> "${minutes / 60}h"
    minutes >= 60 -> "${minutes / 60}h ${minutes % 60}m"
    else -> "${minutes}m"
}

private val TIME = DateTimeFormatter.ofPattern("HH:mm")
