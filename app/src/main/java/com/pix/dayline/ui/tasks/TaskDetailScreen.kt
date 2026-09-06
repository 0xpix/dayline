package com.pix.dayline.ui.tasks

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
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
import com.pix.dayline.ui.theme.composeColor
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@Composable
fun TaskDetailScreen(
    item: DaylineItem,
    spaces: List<DaylineSpace>,
    onBack: () -> Unit,
    onSave: (DaylineItem) -> Unit,
    onDelete: (DaylineItem) -> Unit
) {
    var working by remember(item.id) { mutableStateOf(item) }
    var detailText by remember(item.id) { mutableStateOf(TextFieldValue("")) }
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val space = spaces.firstOrNull { it.id == working.spaceId }

    fun pickDueTime() {
        val initial = working.startTime ?: LocalTime.of(17, 0)
        TimePickerDialog(
            context,
            { _, hour, minute ->
                working = working.copy(startTime = LocalTime.of(hour, minute))
                onSave(working)
            },
            initial.hour,
            initial.minute,
            true
        ).show()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            )
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 32.dp, end = 32.dp, top = 54.dp, bottom = 110.dp)
        ) {
            Text(
                "‹ Tasks",
                modifier = Modifier.clickable { onBack() },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(48.dp))
            Text(
                working.title,
                style = MaterialTheme.typography.displayMedium.copy(fontSize = 38.sp, lineHeight = 40.sp)
            )
            Spacer(Modifier.height(24.dp))

            MetaRow("▦", space?.name ?: "No space")
            MetaRow(
                "◷",
                buildString {
                    append(working.startDate.format(DateTimeFormatter.ofPattern("EEE, MMM d")))
                    working.startTime?.let { append(", ${it.format(DateTimeFormatter.ofPattern("HH:mm"))}") }
                },
                onClick = ::pickDueTime
            )
            MetaRow("↻", working.recurrence.name.lowercase().replace('_', ' '))
            MetaRow(
                "!",
                "Priority · ${working.priority.name.lowercase()}",
                onClick = {
                    working = working.copy(
                        priority = when (working.priority) {
                            TaskPriority.LOW -> TaskPriority.NORMAL
                            TaskPriority.NORMAL -> TaskPriority.HIGH
                            TaskPriority.HIGH -> TaskPriority.LOW
                        }
                    )
                    onSave(working)
                }
            )
            MetaRow("♢", working.reminderMinutes?.let { "$it min before" } ?: "No reminder")

            Spacer(Modifier.height(26.dp))
            Text("SUBTASKS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))

            working.details.forEach { detail ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(20.dp)
                            .border(1.5.dp, working.color.composeColor().copy(alpha = .65f), CircleShape)
                            .background(
                                if (detail.done) working.color.composeColor().copy(alpha = .25f)
                                else androidx.compose.ui.graphics.Color.Transparent,
                                CircleShape
                            )
                            .clickable {
                                working = working.copy(
                                    details = working.details.map {
                                        if (it.id == detail.id) it.copy(done = !it.done) else it
                                    }
                                )
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSave(working)
                            }
                    )
                    Spacer(Modifier.width(14.dp))
                    Text(
                        detail.text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = if (detail.done) .4f else 1f)
                    )
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("+", style = MaterialTheme.typography.titleMedium, color = working.color.composeColor())
                Spacer(Modifier.width(12.dp))
                BasicTextField(
                    value = detailText,
                    onValueChange = { detailText = it },
                    modifier = Modifier.weight(1f),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onBackground),
                    singleLine = true,
                    decorationBox = { inner ->
                        Box {
                            if (detailText.text.isBlank()) {
                                Text("Subtask", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .45f))
                            }
                            inner()
                        }
                    }
                )
                if (detailText.text.isNotBlank()) {
                    Text(
                        "Add",
                        modifier = Modifier.clickable {
                            working = working.copy(
                                details = working.details + TaskDetail(
                                    UUID.randomUUID().toString(),
                                    detailText.text.trim()
                                )
                            )
                            detailText = TextFieldValue("")
                            onSave(working)
                        },
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
            Text(
                "Convert to event",
                modifier = Modifier.clickable {
                    val start = working.startTime ?: LocalTime.of(9, 0)
                    onSave(
                        working.copy(
                            kind = AgendaKind.EVENT,
                            startTime = start,
                            endTime = working.endTime?.takeIf { it.isAfter(start) } ?: start.plusHours(1)
                        )
                    )
                    onBack()
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(10.dp))
            Text(
                if (working.startTime == null) "Set a due time" else "Change due time",
                modifier = Modifier.clickable { pickDueTime() },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(38.dp))
            Text(
                "Delete",
                modifier = Modifier.clickable { onDelete(working) },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            "✓",
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(28.dp)
                .size(52.dp)
                .background(MaterialTheme.colorScheme.onBackground, CircleShape)
                .clickable {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSave(working)
                    onBack()
                }
                .padding(13.dp),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.background
        )
    }
}

@Composable
private fun MetaRow(icon: String, value: String, onClick: (() -> Unit)? = null) {
    Row(
        Modifier
            .padding(vertical = 5.dp)
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick)
                else Modifier
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, modifier = Modifier.width(30.dp), style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
