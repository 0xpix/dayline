package com.pix.dayline.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.foundation.shape.CircleShape
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
import com.pix.dayline.model.Recurrence
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
    onSave: (DaylineItem) -> Unit,
    onDelete: ((DaylineItem) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var title by remember(editing?.id) { mutableStateOf(TextFieldValue(editing?.title.orEmpty())) }
    var date by remember(editing?.id, initialDate) { mutableStateOf(editing?.startDate ?: initialDate) }
    var time by remember(editing?.id) { mutableStateOf(editing?.time) }
    var kind by remember(editing?.id, initialKind) { mutableStateOf(editing?.kind ?: initialKind) }
    var recurrence by remember(editing?.id) { mutableStateOf(editing?.recurrence ?: Recurrence.ONCE) }

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

            Spacer(Modifier.height(28.dp))

            Text("Type", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChoicePill("Event", kind == AgendaKind.EVENT) { kind = AgendaKind.EVENT }
                ChoicePill("Task", kind == AgendaKind.TASK) { kind = AgendaKind.TASK }
            }

            Spacer(Modifier.height(22.dp))

            Text("When", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
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

                PlainPill(time?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "All day") {
                    val initial = time ?: LocalTime.of(9, 0)
                    TimePickerDialog(
                        context,
                        { _, hour, minute -> time = LocalTime.of(hour, minute) },
                        initial.hour,
                        initial.minute,
                        true
                    ).show()
                }

                if (time != null) {
                    Text(
                        text = "×",
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { time = null }
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(22.dp))

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

            Spacer(Modifier.height(10.dp))
            Text(
                text = recurrenceDescription(recurrence, date),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(28.dp))

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
                                    time = time,
                                    recurrence = recurrence,
                                    completedDates = editing?.completedDates ?: emptySet()
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
private fun RepeatPill(
    label: String,
    value: Recurrence,
    selected: Recurrence,
    onSelect: (Recurrence) -> Unit
) {
    ChoicePill(label, value == selected) { onSelect(value) }
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
