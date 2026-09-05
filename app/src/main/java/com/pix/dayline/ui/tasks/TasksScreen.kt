package com.pix.dayline.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.pix.dayline.model.AgendaKind
import com.pix.dayline.model.DaylineItem
import com.pix.dayline.model.Recurrence
import com.pix.dayline.model.isCompletedOn
import com.pix.dayline.model.occursOn
import com.pix.dayline.ui.components.FloatingControls
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun TasksScreen(
    items: List<DaylineItem>,
    onMenu: () -> Unit,
    onToday: () -> Unit,
    onAdd: (LocalDate) -> Unit,
    onEdit: (DaylineItem) -> Unit,
    onToggleTask: (DaylineItem, LocalDate) -> Unit
) {
    val today = remember { LocalDate.now() }
    val tasks = items.filter { it.kind == AgendaKind.TASK }.sortedBy { it.title.lowercase() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 32.dp, end = 82.dp, top = 38.dp, bottom = 130.dp)
        ) {
            Text(
                text = "Tasks",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(30.dp))

            if (tasks.isEmpty()) {
                Text(
                    text = "No tasks yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    tasks.forEach { task ->
                        val occursToday = task.occursOn(today)
                        val completed = occursToday && task.isCompletedOn(today)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { onEdit(task) }
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = task.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    textDecoration = if (completed) TextDecoration.LineThrough else TextDecoration.None
                                )
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = taskSummary(task),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (occursToday) {
                                Text(
                                    text = if (completed) "✓" else "○",
                                    modifier = Modifier.clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = { onToggleTask(task, today) }
                                    ),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    }
                }
            }
        }

        FloatingControls(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 24.dp),
            onMenu = onMenu,
            onToday = onToday,
            onAdd = { onAdd(today) }
        )
    }
}

private fun taskSummary(task: DaylineItem): String {
    val repeat = when (task.recurrence) {
        Recurrence.ONCE -> "Once"
        Recurrence.DAILY -> "Daily"
        Recurrence.WEEKDAYS -> "Weekdays"
        Recurrence.WEEKLY -> "Weekly"
        Recurrence.MONTHLY -> "Monthly"
    }
    return "$repeat · starts ${task.startDate.format(DateTimeFormatter.ofPattern("MMM d"))}"
}
