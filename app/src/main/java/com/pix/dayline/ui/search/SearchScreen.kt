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
import com.pix.dayline.model.DaylineSpace
import com.pix.dayline.model.FocusCycle
import com.pix.dayline.model.isCompletedOn
import com.pix.dayline.model.occursOn
import com.pix.dayline.ui.components.FloatingControls
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private data class SearchHit(val item: DaylineItem, val occurrenceDate: LocalDate)

@Composable
fun SearchScreen(
    items: List<DaylineItem>,
    spaces: List<DaylineSpace>,
    onMenu: () -> Unit,
    onToday: () -> Unit,
    onOpen: (DaylineItem, LocalDate) -> Unit
) {
    var query by remember { mutableStateOf(TextFieldValue("")) }
    val results = remember(items, spaces, query.text) { search(items, spaces, query.text) }

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
                                "PhD · tomorrow · unfinished · focus",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .45f)
                            )
                        }
                        inner()
                    }
                }
            )

            Spacer(Modifier.height(16.dp))
            Text(
                "today   tomorrow   unfinished   focus   September",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))

            if (query.text.isBlank()) {
                Text(
                    "Combine names, Spaces, calendar names and commands. Tap a result to open that exact occurrence.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (results.isEmpty()) {
                Text(
                    "Nothing found.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                results.take(80).forEach { hit ->
                    val item = hit.item
                    val space = spaces.firstOrNull { it.id == item.spaceId }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onOpen(item, hit.occurrenceDate) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(item.title, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                buildString {
                                    append(hit.occurrenceDate.format(DateTimeFormatter.ofPattern("EEE, MMM d")))
                                    item.startTime?.let {
                                        append(" · ${it.format(DateTimeFormatter.ofPattern("HH:mm"))}")
                                    }
                                    space?.let { append(" · ${it.name}") }
                                    item.calendarName?.let { append(" · $it") }
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            when {
                                item.focusCycle != FocusCycle.OFF -> "FOCUS"
                                item.kind == AgendaKind.TASK -> "TASK"
                                else -> "EVENT"
                            },
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

private fun search(
    items: List<DaylineItem>,
    spaces: List<DaylineSpace>,
    raw: String
): List<SearchHit> {
    val locale = Locale.getDefault()
    val tokens = raw.trim()
        .lowercase(locale)
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
    if (tokens.isEmpty()) return emptyList()

    val today = LocalDate.now()
    val requestedDate = when {
        "tomorrow" in tokens -> today.plusDays(1)
        "today" in tokens -> today
        else -> null
    }
    val requestedMonth = tokens.firstNotNullOfOrNull(::monthToken)
    val spacesById = spaces.associateBy { it.id }

    return items.mapNotNull { item ->
        if (requestedDate != null && !item.occursOn(requestedDate)) return@mapNotNull null
        val monthOccurrence = requestedMonth?.let { findMonthOccurrence(item, it, today) }
        if (requestedMonth != null && monthOccurrence == null) return@mapNotNull null
        val occurrenceDate = requestedDate ?: monthOccurrence ?: item.startDate

        val searchable = buildString {
            append(item.title.lowercase(locale))
            append(' ')
            append(item.calendarName?.lowercase(locale).orEmpty())
            append(' ')
            append(spacesById[item.spaceId]?.name?.lowercase(locale).orEmpty())
            append(' ')
            append(item.startDate)
        }

        val matchesAll = tokens.all { token ->
            when {
                token == "today" -> item.occursOn(today)
                token == "tomorrow" -> item.occursOn(today.plusDays(1))
                token == "unfinished" || token == "todo" ->
                    item.kind == AgendaKind.TASK && !item.isCompletedOn(requestedDate ?: today)
                token == "focus" || token == "pomodoro" -> item.focusCycle != FocusCycle.OFF
                token == "task" || token == "tasks" -> item.kind == AgendaKind.TASK
                token == "event" || token == "events" -> item.kind == AgendaKind.EVENT
                monthToken(token) != null -> monthOccurrence != null
                else -> searchable.contains(token)
            }
        }

        if (!matchesAll) return@mapNotNull null
        SearchHit(item, occurrenceDate)
    }.sortedWith(
        compareBy<SearchHit> { it.occurrenceDate }
            .thenBy { it.item.startTime == null }
            .thenBy { it.item.startTime }
            .thenBy { it.item.title }
    )
}

private fun findMonthOccurrence(item: DaylineItem, month: Month, today: LocalDate): LocalDate? {
    val yearCandidates = listOf(today.year, today.year + 1, item.startDate.year).distinct()
    for (year in yearCandidates) {
        val ym = YearMonth.of(year, month)
        var date = maxOf(item.startDate, ym.atDay(1))
        val end = ym.atEndOfMonth()
        while (!date.isAfter(end)) {
            if (item.occursOn(date)) return date
            date = date.plusDays(1)
        }
    }
    return null
}

private fun monthToken(token: String): Month? {
    if (token.length < 3) return null
    val locale = Locale.getDefault()
    return Month.entries.firstOrNull { month ->
        val full = month.getDisplayName(TextStyle.FULL, locale).lowercase(locale)
        val short = month.getDisplayName(TextStyle.SHORT, locale).lowercase(locale)
        token == full || token == short || token == month.name.lowercase(locale) ||
            (token.length >= 3 && full.startsWith(token))
    }
}
