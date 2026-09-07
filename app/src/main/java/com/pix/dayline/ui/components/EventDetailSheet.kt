package com.pix.dayline.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
    val currentEnd = item.endTime ?: item.startTime?.plusMinutes(duration.toLong())
    val candidates = allItems.filterNot { it.id == item.id }
    val laterToday = currentEnd?.let {
        PlanningEngine.nextSlotAfter(candidates, occurrenceDate, it, duration)
    }
    val tomorrow = PlanningEngine.fittingSlots(
        candidates,
        occurrenceDate.plusDays(1),
        duration,
        maxResults = 1
    ).firstOrNull()
    val nextFree = PlanningEngine.suggestions(
        candidates,
        occurrenceDate,
        duration,
        maxResults = 1
    ).firstOrNull()

    fun moved(slot: FreeSlot): DaylineItem = item.copy(
        startDate = slot.date,
        startTime = slot.start,
        endTime = slot.end
    )

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 28.dp).padding(bottom = 36.dp)) {
            Text(item.title, style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(10.dp))
            Text(
                occurrenceDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault())),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(eventTimeLine(item), style = MaterialTheme.typography.titleMedium)

            val meta = buildList {
                spaceName?.let(::add)
                item.calendarName?.let(::add)
                if (item.recurrence != Recurrence.ONCE) add(item.recurrence.name.lowercase().replace('_', ' '))
            }.joinToString(" · ")
            if (meta.isNotBlank()) {
                Spacer(Modifier.height(7.dp))
                Text(meta, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(Modifier.height(28.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                DetailAction("EDIT", enabled = !item.calendarReadOnly, onClick = onEdit)
                DetailAction("CLOSE", onClick = onDismiss)
            }

            if (!item.calendarReadOnly && item.startTime != null) {
                Spacer(Modifier.height(30.dp))
                Text("QUICK MOVE", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(10.dp))
                laterToday?.let { slot ->
                    MoveRow("Later today", slot) { onMove(moved(slot), occurrenceDate) }
                }
                tomorrow?.let { slot ->
                    MoveRow("Tomorrow", slot) { onMove(moved(slot), occurrenceDate) }
                }
                nextFree?.takeIf { it != laterToday && it != tomorrow }?.let { slot ->
                    MoveRow("Next free slot", slot) { onMove(moved(slot), occurrenceDate) }
                }
                MoveEditRow(onClick = onEdit)
            }
        }
    }
}

@Composable
private fun DetailAction(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Text(
        label,
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick).padding(vertical = 8.dp),
        style = MaterialTheme.typography.labelLarge,
        color = if (enabled) MaterialTheme.colorScheme.onBackground
        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
    )
}

@Composable
private fun MoveRow(label: String, slot: FreeSlot, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(
            "${dayLabel(slot.date)} · ${slot.start.format(TIME)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MoveEditRow(onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Pick date / time", style = MaterialTheme.typography.bodyLarge)
        Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun eventTimeLine(item: DaylineItem): String {
    val start = item.startTime ?: return "Anytime"
    val end = item.endTime?.takeIf { it.isAfter(start) }
    return if (end != null) {
        "${start.format(TIME)} — ${end.format(TIME)} · ${durationLabel(item.planningDurationMinutes)}"
    } else start.format(TIME)
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
