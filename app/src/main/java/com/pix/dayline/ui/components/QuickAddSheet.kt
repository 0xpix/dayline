package com.pix.dayline.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pix.dayline.model.AgendaKind
import com.pix.dayline.model.DaylineItem
import com.pix.dayline.model.DaylineSpace
import com.pix.dayline.model.FocusCycle
import com.pix.dayline.model.ItemColor
import com.pix.dayline.model.Recurrence
import com.pix.dayline.ui.theme.composeColor
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddSheet(
    initialDate: LocalDate,
    initialKind: AgendaKind = AgendaKind.EVENT,
    editing: DaylineItem? = null,
    spaces: List<DaylineSpace> = emptyList(),
    onSave: (DaylineItem) -> Unit,
    onDelete: ((DaylineItem) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var title by remember(editing?.id) { mutableStateOf(TextFieldValue(editing?.title.orEmpty())) }
    var date by remember(editing?.id, initialDate) { mutableStateOf(editing?.startDate ?: initialDate) }
    var startTime by remember(editing?.id) { mutableStateOf(editing?.startTime) }
    var endTime by remember(editing?.id) { mutableStateOf(editing?.endTime) }
    var kind by remember(editing?.id, initialKind) { mutableStateOf(editing?.kind ?: initialKind) }
    var recurrence by remember(editing?.id) { mutableStateOf(editing?.recurrence ?: Recurrence.ONCE) }
    var reminderMinutes by remember(editing?.id) { mutableStateOf(editing?.reminderMinutes) }
    var focusCycle by remember(editing?.id) {
        mutableStateOf(editing?.focusCycle ?: FocusCycle.OFF)
    }
    var itemColor by remember(editing?.id) { mutableStateOf(editing?.color ?: ItemColor.MONO) }
    var spaceId by remember(editing?.id) { mutableStateOf(editing?.spaceId) }

    fun pickStartTime() {
        val initial = startTime ?: LocalTime.of(9, 0)
        TimePickerDialog(
            context,
            { _, hour, minute ->
                val next = LocalTime.of(hour, minute)
                startTime = next
                if (endTime == null || !endTime!!.isAfter(next)) {
                    endTime = next.plusHours(1)
                }
            },
            initial.hour,
            initial.minute,
            true
        ).show()
    }

    fun pickEndTime() {
        val start = startTime ?: LocalTime.of(9, 0)
        val initial = endTime ?: start.plusHours(1)
        TimePickerDialog(
            context,
            { _, hour, minute ->
                val next = LocalTime.of(hour, minute)
                endTime = if (next.isAfter(start)) next else start.plusHours(1)
            },
            initial.hour,
            initial.minute,
            true
        ).show()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) {
        Column(modifier = Modifier.padding(horizontal = 28.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (editing == null) "New" else "Edit",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (editing != null && onDelete != null) {
                    Text(
                        text = "Delete",
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onDelete(editing) }
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            BasicTextField(
                value = title,
                onValueChange = { title = it },
                textStyle = MaterialTheme.typography.titleMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 23.sp,
                    lineHeight = 28.sp
                ),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    Box {
                        if (title.text.isBlank()) {
                            Text(
                                text = "What are you doing?",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 23.sp,
                                    lineHeight = 28.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                            )
                        }
                        inner()
                    }
                }
            )

            Spacer(Modifier.height(26.dp))

            Text("Type", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChoicePill("Event", kind == AgendaKind.EVENT) { kind = AgendaKind.EVENT }
                ChoicePill("Task", kind == AgendaKind.TASK) { kind = AgendaKind.TASK }
            }

            Spacer(Modifier.height(20.dp))

            if (spaces.isNotEmpty()) {
                Text("Space", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChoicePill("None", spaceId == null) { spaceId = null }
                    spaces.take(3).forEach { space -> ChoicePill(space.name, spaceId == space.id) { spaceId = space.id } }
                }
                if (spaces.size > 3) { Spacer(Modifier.height(8.dp)); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { spaces.drop(3).take(3).forEach { space -> ChoicePill(space.name, spaceId == space.id) { spaceId = space.id } } } }
                Spacer(Modifier.height(20.dp))
            }

            Text("When", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                PlainPill(date.format(DateTimeFormatter.ofPattern("EEE, MMM d"))) {
                    DatePickerDialog(
                        context,
                        { _, year, month, day -> date = LocalDate.of(year, month + 1, day) },
                        date.year,
                        date.monthValue - 1,
                        date.dayOfMonth
                    ).show()
                }
                PlainPill(startTime?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "All day", ::pickStartTime)
                if (startTime != null) {
                    PlainPill(endTime?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "+ end", ::pickEndTime)
                    Text(
                        text = "×",
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                startTime = null
                                endTime = null
                                reminderMinutes = null
                            }
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (startTime != null && endTime != null) {
                Spacer(Modifier.height(7.dp))
                Text(
                    text = "${startTime!!.format(DateTimeFormatter.ofPattern("HH:mm"))} → ${endTime!!.format(DateTimeFormatter.ofPattern("HH:mm"))}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(20.dp))

            Text("Repeat", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RepeatPill("Once", Recurrence.ONCE, recurrence) { recurrence = it }
                    RepeatPill("Daily", Recurrence.DAILY, recurrence) { recurrence = it }
                    RepeatPill("Weekly", Recurrence.WEEKLY, recurrence) { recurrence = it }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RepeatPill("Weekdays", Recurrence.WEEKDAYS, recurrence) { recurrence = it }
                    RepeatPill("Monthly", Recurrence.MONTHLY, recurrence) { recurrence = it }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = recurrenceDescription(recurrence, date),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )


            if (
                kind == AgendaKind.EVENT &&
                startTime != null &&
                endTime != null
            ) {
                Spacer(Modifier.height(20.dp))

                Text(
                    "Focus cycle",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChoicePill(
                        "Off",
                        focusCycle == FocusCycle.OFF
                    ) { focusCycle = FocusCycle.OFF }

                    ChoicePill(
                        "25 / 5",
                        focusCycle == FocusCycle.POMODORO_25_5
                    ) { focusCycle = FocusCycle.POMODORO_25_5 }
                }

                if (focusCycle == FocusCycle.POMODORO_25_5) {
                    Spacer(Modifier.height(7.dp))
                    Text(
                        "25 min focus · 5 min rest · repeats until this event ends.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Text("Reminder", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReminderPill("Off", null, reminderMinutes, startTime != null) { reminderMinutes = it }
                ReminderPill("5m", 5, reminderMinutes, startTime != null) { reminderMinutes = it }
                ReminderPill("10m", 10, reminderMinutes, startTime != null) { reminderMinutes = it }
                ReminderPill("15m", 15, reminderMinutes, startTime != null) { reminderMinutes = it }
            }
            if (startTime == null) {
                Spacer(Modifier.height(7.dp))
                Text(
                    text = "Add a start time to enable reminders.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(20.dp))

            Text("Color", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                ItemColor.entries.forEach { color ->
                    ColorSwatch(color = color, selected = itemColor == color) { itemColor = color }
                }
            }

            Spacer(Modifier.height(26.dp))

            val canSave = title.text.isNotBlank()
            Surface(
                modifier = Modifier
                    .align(Alignment.End)
                    .clickable(
                        enabled = canSave,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            onSave(
                                DaylineItem(
                                    id = editing?.id ?: UUID.randomUUID().toString(),
                                    title = title.text.trim(),
                                    kind = kind,
                                    startDate = date,
                                    startTime = startTime,
                                    endTime = endTime?.takeIf { startTime != null && it.isAfter(startTime) },
                                    recurrence = recurrence,
                                    reminderMinutes = reminderMinutes.takeIf { startTime != null },
                                    focusCycle = focusCycle.takeIf {
                                        kind == AgendaKind.EVENT &&
                                            startTime != null &&
                                            endTime != null
                                    } ?: FocusCycle.OFF,
                                    color = itemColor,
                                    spaceId = spaceId,
                                    details = editing?.details ?: emptyList(),
                                    completedDates = editing?.completedDates ?: emptySet(),
                                    calendarEventId = editing?.calendarEventId,
                                    calendarName = editing?.calendarName,
                                    calendarReadOnly = editing?.calendarReadOnly ?: false
                                )
                            )
                        }
                    ),
                shape = CircleShape,
                color = if (canSave) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.surface,
                contentColor = if (canSave) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Text(
                    text = if (editing == null) "Add" else "Save",
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 11.dp),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(Modifier.height(22.dp))
        }
    }
}

@Composable
private fun ChoicePill(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        ),
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurface
    ) {
        Text(label, modifier = Modifier.padding(horizontal = 15.dp, vertical = 8.dp), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun PlainPill(label: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        ),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface
    ) {
        Text(label, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun RepeatPill(label: String, value: Recurrence, selected: Recurrence, onSelect: (Recurrence) -> Unit) {
    ChoicePill(label, value == selected) { onSelect(value) }
}

@Composable
private fun ReminderPill(
    label: String,
    value: Int?,
    selected: Int?,
    enabled: Boolean,
    onSelect: (Int?) -> Unit
) {
    val active = value == selected
    Surface(
        modifier = Modifier.clickable(
            enabled = enabled || value == null,
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = { onSelect(value) }
        ),
        shape = CircleShape,
        color = if (active) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.surface,
        contentColor = if (active) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurface
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = if (!enabled && value != null) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f) else androidx.compose.ui.graphics.Color.Unspecified
        )
    }
}

@Composable
private fun ColorSwatch(color: ItemColor, selected: Boolean, onClick: () -> Unit) {
    val composeColor = color.composeColor()
    Box(
        modifier = Modifier
            .size(27.dp)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                shape = CircleShape
            )
            .padding(4.dp)
            .background(composeColor, CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    )
}

private fun recurrenceDescription(recurrence: Recurrence, date: LocalDate): String = when (recurrence) {
    Recurrence.ONCE -> "Only once."
    Recurrence.DAILY -> "Every day from ${date.format(DateTimeFormatter.ofPattern("MMM d"))}."
    Recurrence.WEEKDAYS -> "Every Monday to Friday."
    Recurrence.WEEKLY -> "Every ${date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())}."
    Recurrence.MONTHLY -> "Every month on the ${ordinal(date.dayOfMonth)}."
}

private fun ordinal(day: Int): String {
    val suffix = if (day in 11..13) "th" else when (day % 10) {
        1 -> "st"
        2 -> "nd"
        3 -> "rd"
        else -> "th"
    }
    return "$day$suffix"
}
