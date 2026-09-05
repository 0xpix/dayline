package com.pix.dayline.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun AgendaList(
    items: List<DaylineItem>,
    date: LocalDate,
    modifier: Modifier = Modifier,
    emptyText: String = "Nothing planned.",
    onEdit: (DaylineItem) -> Unit,
    onToggleTask: (DaylineItem, LocalDate) -> Unit
) {
    if (items.isEmpty()) {
        Text(
            text = emptyText,
            modifier = modifier,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items.forEach { item ->
            val completed = item.kind == AgendaKind.TASK && item.isCompletedOn(date)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { onEdit(item) }
                )
            ) {
                Text(
                    text = when (item.kind) {
                        AgendaKind.TASK -> if (completed) "DONE" else "TODO"
                        AgendaKind.EVENT -> item.time?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "ALL DAY"
                    },
                    modifier = Modifier.width(70.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = item.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    textDecoration = if (completed) TextDecoration.LineThrough else TextDecoration.None
                )

                if (item.recurrence != Recurrence.ONCE) {
                    Text(
                        text = "↻",
                        modifier = Modifier.width(28.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Spacer(Modifier.width(28.dp))
                }

                if (item.kind == AgendaKind.TASK) {
                    Text(
                        text = if (completed) "✓" else "○",
                        modifier = Modifier
                            .width(28.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onToggleTask(item, date) }
                            ),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
    }
}
