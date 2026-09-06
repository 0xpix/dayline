package com.pix.dayline.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pix.dayline.model.AgendaKind
import com.pix.dayline.model.DaylineItem
import com.pix.dayline.model.isCompletedOn
import com.pix.dayline.ui.components.FloatingControls
import java.time.LocalDate
import java.time.Month
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun SearchScreen(
    items: List<DaylineItem>,
    onMenu: () -> Unit,
    onToday: () -> Unit,
    onOpen: (DaylineItem) -> Unit
) {
    var query by remember { mutableStateOf(TextFieldValue("")) }
    val results = remember(items, query.text) { search(items, query.text) }

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
                .padding(start = 30.dp, end = 30.dp, top = 56.dp, bottom = 138.dp)
        ) {
            Text("Search", style = MaterialTheme.typography.displayMedium)
            Spacer(Modifier.height(26.dp))

            BasicTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.titleMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 22.sp
                ),
                singleLine = true,
                decorationBox = { inner ->
                    Box {
                        if (query.text.isBlank()) {
                            Text(
                                "gym · tomorrow · unfinished · September",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .45f)
                            )
                        }
                        inner()
                    }
                }
            )

            Spacer(Modifier.height(28.dp))
            if (query.text.isBlank()) {
                Text(
                    "Search titles, dates, calendar names, spaces, or commands like tomorrow and unfinished.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (results.isEmpty()) {
                Text("Nothing found.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                results.take(80).forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onOpen(item) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(item.title, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                buildString {
                                    append(item.startDate.format(DateTimeFormatter.ofPattern("EEE, MMM d")))
                                    item.startTime?.let { append(" · ${it.format(DateTimeFormatter.ofPattern("HH:mm"))}") }
                                    item.calendarName?.let { append(" · $it") }
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            if (item.kind == AgendaKind.TASK) "TASK" else "EVENT",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        FloatingControls(
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 28.dp),
            showAdd = false,
            onMenu = onMenu,
            onToday = onToday
        )
    }
}

private fun search(items: List<DaylineItem>, raw: String): List<DaylineItem> {
    val q = raw.trim().lowercase(Locale.getDefault())
    if (q.isBlank()) return emptyList()
    val today = LocalDate.now()

    val month = Month.entries.firstOrNull { month ->
        month.name.lowercase(Locale.getDefault()).startsWith(q.take(3)) ||
            month.getDisplayName(java.time.format.TextStyle.FULL, Locale.getDefault())
                .lowercase(Locale.getDefault()) == q
    }

    return items.filter { item ->
        when (q) {
            "tomorrow" -> item.startDate == today.plusDays(1)
            "today" -> item.startDate == today
            "unfinished", "todo" ->
                item.kind == AgendaKind.TASK && !item.isCompletedOn(today)
            else -> {
                item.title.lowercase(Locale.getDefault()).contains(q) ||
                    item.calendarName?.lowercase(Locale.getDefault())?.contains(q) == true ||
                    item.startDate.toString().contains(q) ||
                    month?.let { item.startDate.month == it } == true
            }
        }
    }.sortedWith(compareBy<DaylineItem> { it.startDate }.thenBy { it.startTime })
}
