package com.pix.dayline.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pix.dayline.model.DaylineItem
import com.pix.dayline.model.Recurrence
import com.pix.dayline.planning.FreeSlot
import com.pix.dayline.planning.PlanningEngine
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailSheet(
    item: DaylineItem,
    occurrenceDate: LocalDate,
    allItems: List<DaylineItem>,
    spaceName: String?,
    onEdit: () -> Unit,
    onMove: (DaylineItem, LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val duration = item.planningDurationMinutes
    val currentStart = item.startTime
    val currentEnd = item.endTime ?: currentStart?.plusMinutes(duration.toLong())
    val candidates = allItems.filterNot { it.id == item.id }
    val before = currentStart?.let { PlanningEngine.previousSlotBefore(candidates, occurrenceDate, it, duration) }
    val after = currentEnd?.let { PlanningEngine.nextSlotAfter(candidates, occurrenceDate, it, duration) }
    val tomorrow = occurrenceDate.plusDays(1)
    val tomorrowMorning = PlanningEngine.fittingSlots(candidates, tomorrow, duration, dayStart = LocalTime.of(7, 0), dayEnd = LocalTime.of(12, 0), maxResults = 1).firstOrNull()
    val tomorrowAfternoon = PlanningEngine.fittingSlots(candidates, tomorrow, duration, dayStart = LocalTime.of(12, 0), dayEnd = LocalTime.of(18, 0), maxResults = 1).firstOrNull()
    val nextFree = PlanningEngine.suggestions(candidates, occurrenceDate, duration, maxResults = 1).firstOrNull()

    fun moved(slot: FreeSlot): DaylineItem = item.copy(
        startDate = slot.date,
        startTime = slot.start,
        endTime = slot.end,
        allDay = false
    )

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 28.dp).padding(bottom = 36.dp)) {
            Text(item.title, style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(10.dp))
            Text(occurrenceDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault())), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(eventTimeLine(item), style = MaterialTheme.typography.titleMedium)

            val meta = buildList {
                spaceName?.let(::add)
                item.calendarName?.let(::add)
                if (item.recurrence != Recurrence.ONCE) add(item.recurrence.name.lowercase().replace('_', ' '))
                item.timeZoneId?.takeIf { it != ZoneId.systemDefault().id }?.let { add(it) }
            }.joinToString(" · ")
            if (meta.isNotBlank()) {
                Spacer(Modifier.height(7.dp))
                Text(meta, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(Modifier.height(26.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                DetailAction("EDIT", enabled = !item.calendarReadOnly, onClick = onEdit)
                DetailAction("CLOSE", onClick = onDismiss)
            }

            if (!item.calendarReadOnly && item.startTime != null && !item.allDay) {
                Spacer(Modifier.height(28.dp))
                Text("QUICK MOVE", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                before?.let { MoveRow("Before this block", it) { onMove(moved(it), occurrenceDate) } }
                after?.let { MoveRow("After this block", it) { onMove(moved(it), occurrenceDate) } }
                tomorrowMorning?.let { MoveRow("Tomorrow morning", it) { onMove(moved(it), occurrenceDate) } }
                tomorrowAfternoon?.let { MoveRow("Tomorrow afternoon", it) { onMove(moved(it), occurrenceDate) } }
                nextFree?.takeIf { it != before && it != after && it != tomorrowMorning && it != tomorrowAfternoon }?.let { slot ->
                    MoveRow("Next free slot", slot) { onMove(moved(slot), occurrenceDate) }
                }
                MoveEditRow(onClick = onEdit)
            }
        }
    }
}

@Composable
private fun DetailAction(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Text(label, modifier = Modifier.heightIn(min = 48.dp).clickable(enabled = enabled, onClick = onClick).wrapContentHeight(), style = MaterialTheme.typography.labelLarge, color = if (enabled) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .45f))
}

@Composable
private fun MoveRow(label: String, slot: FreeSlot, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable(onClick = onClick).padding(vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text("${dayLabel(slot.date)} · ${slot.start.format(TIME)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MoveEditRow(onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable(onClick = onClick).padding(vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Pick date / time", style = MaterialTheme.typography.bodyLarge)
        Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun eventTimeLine(item: DaylineItem): String {
    if (item.allDay || item.startTime == null) return "All day"
    val start = item.startTime
    val end = item.endTime?.takeIf { it.isAfter(start) }
    return if (end != null) "${start.format(TIME)} — ${end.format(TIME)} · ${durationLabel(item.planningDurationMinutes)}" else start.format(TIME)
}

private fun durationLabel(minutes: Int): String = when {
    minutes % 60 == 0 -> "${minutes / 60}h"
    minutes > 60 -> "${minutes / 60}h ${minutes % 60}m"
    else -> "${minutes}m"
}

private fun dayLabel(date: LocalDate): String {
    val today = LocalDate.now()
    return when (date) {
        today -> "Today"
        today.plusDays(1) -> "Tomorrow"
        else -> date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    }
}

private val TIME = DateTimeFormatter.ofPattern("HH:mm")
