package com.pix.dayline.ui.tasks

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pix.dayline.model.*
import com.pix.dayline.planning.FreeSlot
import com.pix.dayline.planning.PlanningEngine
import com.pix.dayline.ui.theme.composeColor
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    item: DaylineItem,
    items: List<DaylineItem>,
    spaces: List<DaylineSpace>,
    onBack: () -> Unit,
    onSave: (DaylineItem) -> Unit,
    onDelete: (DaylineItem) -> Unit
) {
    var working by remember(item.id) { mutableStateOf(item) }
    var detailText by remember(item.id) { mutableStateOf(TextFieldValue("")) }
    var fitOpen by remember(item.id) { mutableStateOf(false) }
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val space = spaces.firstOrNull { it.id == working.spaceId }

    fun persist(next: DaylineItem) {
        working = next
        onSave(next)
    }

    fun pickDueTime() {
        val initial = working.startTime ?: LocalTime.of(17, 0)
        TimePickerDialog(context, { _, hour, minute ->
            val start = LocalTime.of(hour, minute)
            persist(working.copy(startTime = start, endTime = start.plusMinutes(working.estimatedDurationMinutes.toLong()), allDay = false))
        }, initial.hour, initial.minute, true).show()
    }

    fun pickWindowDate(earliest: Boolean) {
        val initial = if (earliest) working.earliestDate ?: working.startDate else working.deadlineDate ?: working.startDate.plusDays(7)
        DatePickerDialog(context, { _, year, month, day ->
            val chosen = LocalDate.of(year, month + 1, day)
            val next = if (earliest) {
                working.copy(
                    earliestDate = chosen,
                    deadlineDate = working.deadlineDate?.takeIf { !it.isBefore(chosen) }
                )
            } else {
                working.copy(
                    deadlineDate = chosen,
                    earliestDate = working.earliestDate?.takeIf { !it.isAfter(chosen) }
                )
            }
            persist(next)
        }, initial.year, initial.monthValue - 1, initial.dayOfMonth).show()
    }

    Box(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(
            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
            bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        )
    ) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(start = 32.dp, end = 32.dp, top = 92.dp, bottom = 110.dp)
        ) {
            Text("‹ Tasks", modifier = Modifier.heightIn(min = 48.dp).clickable { onBack() }.wrapContentHeight(Alignment.CenterVertically), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(36.dp))
            Text(working.title, style = MaterialTheme.typography.displayMedium.copy(fontSize = 38.sp, lineHeight = 40.sp))
            Spacer(Modifier.height(24.dp))

            MetaRow("▦", space?.name ?: "No space")
            MetaRow("◷", buildString {
                append(working.startDate.format(DateTimeFormatter.ofPattern("EEE, MMM d")))
                working.startTime?.let { append(", ${it.format(TIME)}") }
            }, onClick = ::pickDueTime)
            MetaRow("⌛", "Estimate · ${durationLabel(working.estimatedDurationMinutes)}") {
                val next = nextEstimate(working.estimatedDurationMinutes)
                persist(working.copy(estimatedDurationMinutes = next, endTime = working.startTime?.plusMinutes(next.toLong())))
            }
            MetaRow("→", "Earliest · ${working.earliestDate?.format(DATE) ?: "Any day"}") { pickWindowDate(true) }
            MetaRow("⌁", "Deadline · ${working.deadlineDate?.format(DATE) ?: "None"}") { pickWindowDate(false) }
            if (working.earliestDate != null || working.deadlineDate != null) {
                Text("CLEAR SCHEDULING WINDOW", modifier = Modifier.heightIn(min = 44.dp).clickable { persist(working.copy(earliestDate = null, deadlineDate = null)) }.wrapContentHeight(Alignment.CenterVertically), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            MetaRow("↻", working.recurrence.name.lowercase().replace('_', ' '))
            MetaRow("!", "Priority · ${working.priority.name.lowercase()}") {
                persist(working.copy(priority = when (working.priority) {
                    TaskPriority.LOW -> TaskPriority.NORMAL
                    TaskPriority.NORMAL -> TaskPriority.HIGH
                    TaskPriority.HIGH -> TaskPriority.LOW
                }))
            }
            MetaRow("♢", working.reminderMinutes?.let { "$it min before" } ?: "No reminder")

            Spacer(Modifier.height(24.dp))
            Text("FIT INTO MY DAY  ›", modifier = Modifier.heightIn(min = 48.dp).clickable { fitOpen = true }.wrapContentHeight(Alignment.CenterVertically), style = MaterialTheme.typography.labelLarge)
            Text(
                if (working.earliestDate != null || working.deadlineDate != null) "Finds a free ${durationLabel(working.estimatedDurationMinutes)} block inside your scheduling window."
                else "Finds free ${durationLabel(working.estimatedDurationMinutes)} blocks locally from your calendar.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(28.dp))
            Text("SUBTASKS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))

            working.details.forEach { detail ->
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(28.dp).border(1.5.dp, working.color.composeColor().copy(alpha = .65f), CircleShape)
                        .background(if (detail.done) working.color.composeColor().copy(alpha = .25f) else androidx.compose.ui.graphics.Color.Transparent, CircleShape)
                        .clickable {
                            persist(working.copy(details = working.details.map { if (it.id == detail.id) it.copy(done = !it.done) else it }))
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        })
                    Spacer(Modifier.width(14.dp))
                    Text(detail.text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground.copy(alpha = if (detail.done) .4f else 1f))
                }
            }

            Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("+", style = MaterialTheme.typography.titleMedium, color = working.color.composeColor())
                Spacer(Modifier.width(12.dp))
                BasicTextField(
                    value = detailText,
                    onValueChange = { detailText = it },
                    modifier = Modifier.weight(1f),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onBackground),
                    singleLine = true,
                    decorationBox = { inner -> Box { if (detailText.text.isBlank()) Text("Subtask", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .45f)); inner() } }
                )
                if (detailText.text.isNotBlank()) {
                    Text("Add", modifier = Modifier.clickable {
                        persist(working.copy(details = working.details + TaskDetail(UUID.randomUUID().toString(), detailText.text.trim())))
                        detailText = TextFieldValue("")
                    }.padding(12.dp), style = MaterialTheme.typography.labelMedium)
                }
            }

            Spacer(Modifier.height(32.dp))
            Text("Convert to event", modifier = Modifier.heightIn(min = 48.dp).clickable {
                val start = working.startTime ?: LocalTime.of(9, 0)
                onSave(working.copy(kind = AgendaKind.EVENT, startTime = start, endTime = start.plusMinutes(working.estimatedDurationMinutes.toLong()), allDay = false))
                onBack()
            }.wrapContentHeight(Alignment.CenterVertically), style = MaterialTheme.typography.bodyLarge)
            Text(if (working.startTime == null) "Set a due time" else "Change due time", modifier = Modifier.heightIn(min = 48.dp).clickable { pickDueTime() }.wrapContentHeight(Alignment.CenterVertically), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(22.dp))
            Text("Delete", modifier = Modifier.heightIn(min = 48.dp).clickable { onDelete(working) }.wrapContentHeight(Alignment.CenterVertically), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Text("✓", modifier = Modifier.align(Alignment.BottomEnd).padding(28.dp).size(52.dp).background(MaterialTheme.colorScheme.onBackground, CircleShape).clickable {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress); onSave(working); onBack()
        }.padding(13.dp), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.background)
    }

    if (fitOpen) {
        FitTaskSheet(task = working, items = items, onChoose = { slot ->
            persist(working.copy(startDate = slot.date, startTime = slot.start, endTime = slot.end, allDay = false))
            fitOpen = false
        }, onDismiss = { fitOpen = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FitTaskSheet(task: DaylineItem, items: List<DaylineItem>, onChoose: (FreeSlot) -> Unit, onDismiss: () -> Unit) {
    val suggestions = remember(items, task.id, task.estimatedDurationMinutes, task.earliestDate, task.deadlineDate) {
        PlanningEngine.suggestionsForTask(items, task, maxResults = 6)
    }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 28.dp).padding(bottom = 36.dp)) {
            Text("Fit into my day", style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(7.dp))
            Text(buildString {
                append("${task.estimatedDurationMinutes} min")
                task.earliestDate?.let { append(" · from ${it.format(DATE)}") }
                task.deadlineDate?.let { append(" · by ${it.format(DATE)}") }
                if (task.earliestDate == null && task.deadlineDate == null) append(" · next 7 days")
            }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
            if (suggestions.isEmpty()) Text("No free block fits this task inside the selected window.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            else suggestions.forEach { slot ->
                Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable { onChoose(slot) }.padding(vertical = 11.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(dayLabel(slot.date), style = MaterialTheme.typography.bodyLarge)
                    Text("${slot.start.format(TIME)} — ${slot.end.format(TIME)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun MetaRow(icon: String, value: String, onClick: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier), verticalAlignment = Alignment.CenterVertically) {
        Text(icon, modifier = Modifier.width(30.dp), style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun nextEstimate(current: Int): Int {
    val options = listOf(15, 30, 45, 60, 90, 120)
    val index = options.indexOf(current)
    return if (index < 0 || index == options.lastIndex) options.first() else options[index + 1]
}

private fun durationLabel(minutes: Int): String = when {
    minutes % 60 == 0 -> "${minutes / 60} h"
    minutes > 60 -> "${minutes / 60} h ${minutes % 60} min"
    else -> "$minutes min"
}

private fun dayLabel(date: LocalDate): String = when (date) {
    LocalDate.now() -> "Today"
    LocalDate.now().plusDays(1) -> "Tomorrow"
    else -> date.format(DateTimeFormatter.ofPattern("EEE, MMM d"))
}

private val TIME = DateTimeFormatter.ofPattern("HH:mm")
private val DATE = DateTimeFormatter.ofPattern("MMM d")
