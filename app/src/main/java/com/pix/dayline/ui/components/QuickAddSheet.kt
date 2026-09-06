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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
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
import com.pix.dayline.model.*
import com.pix.dayline.ui.theme.composeColor
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuickAddSheet(
    initialDate: LocalDate,
    initialKind: AgendaKind = AgendaKind.EVENT,
    initialTime: LocalTime? = null,
    editing: DaylineItem? = null,
    spaces: List<DaylineSpace> = emptyList(),
    templates: List<EventTemplate> = emptyList(),
    onSave: (DaylineItem, RecurrenceEditScope) -> Unit,
    onSaveTemplate: (EventTemplate) -> Unit = {},
    onDelete: ((DaylineItem) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var title by remember(editing?.id) { mutableStateOf(TextFieldValue(editing?.title.orEmpty())) }
    var date by remember(editing?.id, initialDate) { mutableStateOf(editing?.startDate ?: initialDate) }
    var startTime by remember(editing?.id, initialTime) {
        mutableStateOf(editing?.startTime ?: initialTime)
    }
    var endTime by remember(editing?.id, initialTime) {
        mutableStateOf(
            editing?.endTime ?: initialTime?.plusHours(1)
        )
    }
    var kind by remember(editing?.id, initialKind) { mutableStateOf(editing?.kind ?: initialKind) }
    var recurrence by remember(editing?.id) { mutableStateOf(editing?.recurrence ?: Recurrence.ONCE) }
    var editScope by remember(editing?.id) { mutableStateOf(RecurrenceEditScope.ENTIRE_SERIES) }
    var reminderMinutes by remember(editing?.id) { mutableStateOf(editing?.reminderMinutes) }
    var focusCycle by remember(editing?.id) { mutableStateOf(editing?.focusCycle ?: FocusCycle.OFF) }
    var customFocus by remember(editing?.id) { mutableStateOf((editing?.customFocusMinutes ?: 25).toString()) }
    var customBreak by remember(editing?.id) { mutableStateOf((editing?.customBreakMinutes ?: 5).toString()) }
    var bufferBefore by remember(editing?.id) { mutableStateOf(editing?.bufferBeforeMinutes ?: 0) }
    var bufferAfter by remember(editing?.id) { mutableStateOf(editing?.bufferAfterMinutes ?: 0) }
    var priority by remember(editing?.id) { mutableStateOf(editing?.priority ?: TaskPriority.NORMAL) }
    var itemColor by remember(editing?.id) { mutableStateOf(editing?.color ?: ItemColor.MONO) }
    var spaceId by remember(editing?.id) { mutableStateOf(editing?.spaceId) }

    fun applyTemplate(template: EventTemplate) {
        title = TextFieldValue(template.title)
        kind = template.kind
        startTime = template.startTime ?: startTime ?: LocalTime.of(9, 0)
        endTime = startTime?.plusMinutes(template.durationMinutes.toLong())
        spaceId = template.spaceId
        itemColor = template.color
        focusCycle = template.focusCycle
        customFocus = template.customFocusMinutes.toString()
        customBreak = template.customBreakMinutes.toString()
        bufferBefore = template.bufferBeforeMinutes
        bufferAfter = template.bufferAfterMinutes
        priority = template.priority
    }

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
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    if (editing == null) "New" else "Edit",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (editing != null && onDelete != null) {
                    Text(
                        "Delete",
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onDelete(editing) },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (
                editing == null &&
                templates.isNotEmpty()
            ) {
                SectionHeader(
                    label = "Templates",
                    top = 24
                )

                FlowRow(
                    horizontalArrangement =
                        Arrangement.spacedBy(9.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(9.dp)
                ) {
                    templates.forEach { template ->
                        ChoicePill(
                            template.title,
                            false
                        ) {
                            applyTemplate(template)
                        }
                    }
                }
            }

            Spacer(
                Modifier.height(24.dp)
            )

            Text(
                "TITLE",
                style =
                    MaterialTheme.typography.labelMedium,
                color =
                    MaterialTheme.colorScheme
                        .onSurfaceVariant
            )

            Spacer(
                Modifier.height(10.dp)
            )

            Surface(
                shape = RoundedCornerShape(20.dp),
                color =
                    MaterialTheme.colorScheme.surface
            ) {
                BasicTextField(
                    value = title,
                    onValueChange = {
                        title = it
                    },
                    textStyle =
                        MaterialTheme.typography
                            .titleMedium.copy(
                                color =
                                    MaterialTheme.colorScheme
                                        .onBackground,
                                fontSize = 23.sp,
                                lineHeight = 30.sp
                            ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 18.dp,
                            vertical = 16.dp
                        ),
                    decorationBox = { inner ->
                        Box {
                            if (title.text.isBlank()) {
                                Text(
                                    "What are you doing?",
                                    style =
                                        MaterialTheme.typography
                                            .titleMedium.copy(
                                                fontSize = 23.sp,
                                                lineHeight = 30.sp
                                            ),
                                    color =
                                        MaterialTheme.colorScheme
                                            .onSurfaceVariant
                                            .copy(alpha = 0.52f)
                                )
                            }

                            inner()
                        }
                    }
                )
            }

            Section("Type") {
                ChoicePill("Event", kind == AgendaKind.EVENT) { kind = AgendaKind.EVENT }
                ChoicePill("Task", kind == AgendaKind.TASK) { kind = AgendaKind.TASK }
            }

            if (kind == AgendaKind.TASK) {
                Section("Priority") {
                    ChoicePill("Low", priority == TaskPriority.LOW) { priority = TaskPriority.LOW }
                    ChoicePill("Normal", priority == TaskPriority.NORMAL) { priority = TaskPriority.NORMAL }
                    ChoicePill("High", priority == TaskPriority.HIGH) { priority = TaskPriority.HIGH }
                }
            }

            if (spaces.isNotEmpty()) {
                SectionHeader(
                    label = "Space",
                    top = 28
                )

                FlowRow(
                    horizontalArrangement =
                        Arrangement.spacedBy(9.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(9.dp)
                ) {
                    spaces.forEach { space ->
                        ChoicePill(
                            space.name,
                            spaceId == space.id
                        ) {
                            spaceId = space.id
                        }
                    }

                    ChoicePill(
                        "No space",
                        spaceId == null
                    ) {
                        spaceId = null
                    }
                }

            }

            SectionHeader(
                label = "When",
                top = 28
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                PlainPill(date.format(DateTimeFormatter.ofPattern("EEE, MMM d"))) {
                    DatePickerDialog(
                        context,
                        { _, year, month, day -> date = LocalDate.of(year, month + 1, day) },
                        date.year,
                        date.monthValue - 1,
                        date.dayOfMonth
                    ).show()
                }
                PlainPill(startTime?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "Anytime") {
                    pickStartTime()
                }
                if (startTime != null) {
                    PlainPill(endTime?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "+1h") {
                        pickEndTime()
                    }
                }
            }

            if (startTime != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Tap Anytime to clear the time.",
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        startTime = null
                        endTime = null
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Section("Repeat") {
                RepeatPill(
                    "Once",
                    Recurrence.ONCE,
                    recurrence
                ) { recurrence = it }

                RepeatPill(
                    "Daily",
                    Recurrence.DAILY,
                    recurrence
                ) { recurrence = it }

                RepeatPill(
                    "Weekdays",
                    Recurrence.WEEKDAYS,
                    recurrence
                ) { recurrence = it }

                RepeatPill(
                    "Sunday only",
                    Recurrence.SUNDAYS,
                    recurrence
                ) { recurrence = it }

                RepeatPill(
                    "Every day except Sunday",
                    Recurrence.EXCEPT_SUNDAY,
                    recurrence
                ) { recurrence = it }

                RepeatPill(
                    "Weekly",
                    Recurrence.WEEKLY,
                    recurrence
                ) { recurrence = it }

                RepeatPill(
                    "Monthly",
                    Recurrence.MONTHLY,
                    recurrence
                ) { recurrence = it }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                recurrenceDescription(recurrence, date),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (editing != null && editing.recurrence != Recurrence.ONCE) {
                Section("Apply changes") {
                    ChoicePill("This", editScope == RecurrenceEditScope.THIS_OCCURRENCE) {
                        editScope = RecurrenceEditScope.THIS_OCCURRENCE
                    }
                    ChoicePill("Following", editScope == RecurrenceEditScope.THIS_AND_FOLLOWING) {
                        editScope = RecurrenceEditScope.THIS_AND_FOLLOWING
                    }
                    ChoicePill("Series", editScope == RecurrenceEditScope.ENTIRE_SERIES) {
                        editScope = RecurrenceEditScope.ENTIRE_SERIES
                    }
                }
            }

            if (kind == AgendaKind.EVENT && startTime != null && endTime != null) {
                Section("Focus cycle") {
                    ChoicePill("Off", focusCycle == FocusCycle.OFF) { focusCycle = FocusCycle.OFF }
                    ChoicePill("25 / 5", focusCycle == FocusCycle.POMODORO_25_5) { focusCycle = FocusCycle.POMODORO_25_5 }
                    ChoicePill("50 / 10", focusCycle == FocusCycle.FOCUS_50_10) { focusCycle = FocusCycle.FOCUS_50_10 }
                    ChoicePill("Custom", focusCycle == FocusCycle.CUSTOM) { focusCycle = FocusCycle.CUSTOM }
                }
                if (focusCycle == FocusCycle.CUSTOM) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        NumericField("Focus", customFocus, Modifier.width(92.dp)) { customFocus = it }
                        NumericField("Rest", customBreak, Modifier.width(92.dp)) { customBreak = it }
                    }
                    Spacer(Modifier.height(7.dp))
                    Text("Minutes · repeats until the event ends.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (kind == AgendaKind.EVENT && startTime != null) {
                Section("Buffer") {
                    ChoicePill("Before ${bufferBefore}m", bufferBefore > 0) {
                        bufferBefore = nextBuffer(bufferBefore)
                    }
                    ChoicePill("After ${bufferAfter}m", bufferAfter > 0) {
                        bufferAfter = nextBuffer(bufferAfter)
                    }
                }
            }

            Section("Reminder") {
                ReminderPill("Off", null, reminderMinutes, startTime != null) { reminderMinutes = it }
                ReminderPill("5m", 5, reminderMinutes, startTime != null) { reminderMinutes = it }
                ReminderPill("10m", 10, reminderMinutes, startTime != null) { reminderMinutes = it }
                ReminderPill("15m", 15, reminderMinutes, startTime != null) { reminderMinutes = it }
            }

            SectionHeader(
                label = "Color",
                top = 28
            )
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                ItemColor.entries.forEach { color ->
                    ColorSwatch(color, itemColor == color) { itemColor = color }
                }
            }

            Spacer(Modifier.height(26.dp))
            val canSave = title.text.isNotBlank()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (editing == null && canSave) {
                    Text(
                        "Save template",
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            val duration = if (startTime != null && endTime != null) {
                                java.time.Duration.between(startTime, endTime).toMinutes().toInt().coerceAtLeast(15)
                            } else 60
                            onSaveTemplate(
                                EventTemplate(
                                    id = UUID.randomUUID().toString(),
                                    title = title.text.trim(),
                                    kind = kind,
                                    durationMinutes = duration,
                                    startTime = startTime,
                                    spaceId = spaceId,
                                    color = itemColor,
                                    focusCycle = focusCycle,
                                    customFocusMinutes = customFocus.toIntOrNull()?.coerceIn(5, 180) ?: 25,
                                    customBreakMinutes = customBreak.toIntOrNull()?.coerceIn(1, 60) ?: 5,
                                    bufferBeforeMinutes = bufferBefore,
                                    bufferAfterMinutes = bufferAfter,
                                    priority = priority
                                )
                            )
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Spacer(Modifier.width(1.dp))
                }

                Surface(
                    modifier = Modifier.clickable(
                        enabled = canSave,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        val saved = DaylineItem(
                            id = editing?.id ?: UUID.randomUUID().toString(),
                            title = title.text.trim(),
                            kind = kind,
                            startDate = date,
                            startTime = startTime,
                            endTime = endTime?.takeIf { startTime != null && it.isAfter(startTime) },
                            recurrence = recurrence,
                            recurrenceEndDate = editing?.recurrenceEndDate,
                            excludedDates = editing?.excludedDates ?: emptySet(),
                            seriesParentId = editing?.seriesParentId,
                            reminderMinutes = reminderMinutes.takeIf { startTime != null },
                            focusCycle = focusCycle.takeIf {
                                kind == AgendaKind.EVENT && startTime != null && endTime != null
                            } ?: FocusCycle.OFF,
                            customFocusMinutes = customFocus.toIntOrNull()?.coerceIn(5, 180) ?: 25,
                            customBreakMinutes = customBreak.toIntOrNull()?.coerceIn(1, 60) ?: 5,
                            focusSessionsCompleted = editing?.focusSessionsCompleted ?: 0,
                            focusedMinutesCompleted = editing?.focusedMinutesCompleted ?: 0,
                            bufferBeforeMinutes = bufferBefore,
                            bufferAfterMinutes = bufferAfter,
                            priority = priority,
                            color = itemColor,
                            spaceId = spaceId,
                            details = editing?.details ?: emptyList(),
                            completedDates = editing?.completedDates ?: emptySet(),
                            calendarEventId = editing?.calendarEventId,
                            calendarId = editing?.calendarId,
                            calendarName = editing?.calendarName,
                            calendarReadOnly = editing?.calendarReadOnly ?: false
                        )
                        onSave(saved, editScope)
                    },
                    shape = CircleShape,
                    color = if (canSave) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.surface,
                    contentColor = if (canSave) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    Text(
                        if (editing == null) "Add" else "Save",
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 11.dp),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun SectionHeader(
    label: String,
    top: Int = 30
) {
    Spacer(
        Modifier.height(top.dp)
    )

    Text(
        text = label.uppercase(),
        style =
            MaterialTheme.typography.labelMedium,
        color =
            MaterialTheme.colorScheme
                .onSurfaceVariant
    )

    Spacer(
        Modifier.height(12.dp)
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Section(
    label: String,
    content: @Composable () -> Unit
) {
    SectionHeader(label)

    FlowRow(
        horizontalArrangement =
            Arrangement.spacedBy(10.dp),
        verticalArrangement =
            Arrangement.spacedBy(10.dp)
    ) {
        content()
    }
}

@Composable
private fun NumericField(label: String, value: String, modifier: Modifier = Modifier, onValue: (String) -> Unit) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface) {
            BasicTextField(
                value = value,
                onValueChange = { next -> onValue(next.filter(Char::isDigit).take(3)) },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onBackground),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
            )
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
        shape = RoundedCornerShape(18.dp),
        color =
            if (selected) {
                MaterialTheme.colorScheme.onBackground
            } else {
                MaterialTheme.colorScheme.surface
            },
        contentColor =
            if (selected) {
                MaterialTheme.colorScheme.background
            } else {
                MaterialTheme.colorScheme.onSurface
            }
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 10.dp
            ),
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1
        )
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
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 10.dp
            ),
            style =
                MaterialTheme.typography.bodyLarge,
            maxLines = 1
        )
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
            label,
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = if (!enabled && value != null) {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
            } else androidx.compose.ui.graphics.Color.Unspecified
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
    Recurrence.SUNDAYS -> "Every Sunday."
    Recurrence.EXCEPT_SUNDAY -> "Every day except Sunday."
    Recurrence.WEEKLY -> "Every ${date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())}."
    Recurrence.MONTHLY -> "Every month on the ${ordinal(date.dayOfMonth)}."
}

private fun nextBuffer(current: Int): Int = when (current) {
    0 -> 10
    10 -> 15
    15 -> 20
    20 -> 30
    30 -> 45
    45 -> 60
    else -> 0
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
