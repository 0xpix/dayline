package com.pix.dayline.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pix.dayline.model.*
import com.pix.dayline.planning.FreeSlot
import com.pix.dayline.planning.PlanningEngine
import com.pix.dayline.ui.components.FloatingControls
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.Month
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

private data class SearchHit(val item: DaylineItem, val occurrenceDate: LocalDate)

@Composable
fun SearchScreen(
    items: List<DaylineItem>,
    spaces: List<DaylineSpace>,
    onMenu: () -> Unit,
    onToday: () -> Unit,
    onOpen: (DaylineItem, LocalDate) -> Unit,
    onAddAt: (LocalDate, LocalTime) -> Unit = { _, _ -> }
) {
    var query by remember { mutableStateOf(TextFieldValue("")) }
    val parsed = remember(query.text) { SearchQuery.parse(query.text) }
    val results = remember(items, spaces, query.text) { search(items, spaces, query.text) }
    val freeSlots = remember(items, parsed) { if (parsed.free) freeSearch(items, parsed) else emptyList() }

    Box(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(
            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
            bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        )
    ) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(start = 30.dp, end = 30.dp, top = 56.dp, bottom = 138.dp)
        ) {
            Text("Search", style = MaterialTheme.typography.displayMedium)
            Spacer(Modifier.height(26.dp))

            BasicTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                textStyle = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onBackground, fontSize = 22.sp),
                singleLine = true,
                decorationBox = { inner -> Box(contentAlignment = Alignment.CenterStart) {
                    if (query.text.isBlank()) Text("tomorrow · free Friday afternoon", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .45f))
                    inner()
                } }
            )

            Spacer(Modifier.height(14.dp))
            Text("today   tomorrow   this week   next week   unfinished   focus   free Friday afternoon", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(26.dp))

            when {
                query.text.isBlank() -> Text("Search names, Spaces, calendars, dates, weeks, task state, or free time. Everything is resolved locally.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                parsed.free -> {
                    Text("FREE TIME", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    if (freeSlots.isEmpty()) Text("No matching free time found.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    else freeSlots.forEach { slot ->
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 54.dp).clickable { onAddAt(slot.date, slot.start) }.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(dayLabel(slot.date), style = MaterialTheme.typography.bodyLarge)
                                Text("${slot.durationMinutes} min free", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("${slot.start.format(TIME)} — ${slot.end.format(TIME)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                results.isEmpty() -> Text("Nothing found.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> {
                    results.take(80).groupBy { it.occurrenceDate }.forEach { (date, hits) ->
                        Text(dayLabel(date).uppercase(Locale.getDefault()), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(5.dp))
                        hits.forEach { hit ->
                            val item = hit.item
                            val space = spaces.firstOrNull { it.id == item.spaceId }
                            Row(
                                Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onOpen(item, hit.occurrenceDate) }.padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(item.title, style = MaterialTheme.typography.bodyLarge)
                                    Text(buildString {
                                        when {
                                            item.allDay -> append("All day")
                                            item.startTime != null -> append(item.startTime.format(TIME))
                                        }
                                        space?.let { if (isNotEmpty()) append(" · "); append(it.name) }
                                        item.calendarName?.let { if (isNotEmpty()) append(" · "); append(it) }
                                    }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(when {
                                    item.focusCycle != FocusCycle.OFF -> "FOCUS"
                                    item.kind == AgendaKind.TASK -> "TASK"
                                    else -> "EVENT"
                                }, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }

        FloatingControls(modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 28.dp), showAdd = false, onMenu = onMenu, onToday = onToday)
    }
}

private data class SearchQuery(
    val rawTokens: List<String>,
    val free: Boolean,
    val exactDate: LocalDate?,
    val rangeStart: LocalDate?,
    val rangeEnd: LocalDate?,
    val dayPart: Pair<LocalTime, LocalTime>?
) {
    companion object {
        fun parse(raw: String): SearchQuery {
            val locale = Locale.getDefault()
            val tokens = raw.trim().lowercase(locale).split(Regex("\\s+")).filter(String::isNotBlank)
            val today = LocalDate.now()
            val weekday = tokens.firstNotNullOfOrNull(::weekdayToken)
            val exact = when {
                "tomorrow" in tokens -> today.plusDays(1)
                "today" in tokens -> today
                weekday != null -> today.with(TemporalAdjusters.nextOrSame(weekday))
                else -> null
            }
            val thisWeek = "week" in tokens && "this" in tokens
            val nextWeek = "week" in tokens && "next" in tokens
            val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val rangeStart = when { thisWeek -> monday; nextWeek -> monday.plusWeeks(1); else -> null }
            val rangeEnd = when { thisWeek -> monday.plusDays(6); nextWeek -> monday.plusWeeks(1).plusDays(6); else -> null }
            val dayPart = when {
                "morning" in tokens -> LocalTime.of(7, 0) to LocalTime.of(12, 0)
                "afternoon" in tokens -> LocalTime.of(12, 0) to LocalTime.of(18, 0)
                "evening" in tokens -> LocalTime.of(18, 0) to LocalTime.of(22, 0)
                else -> null
            }
            return SearchQuery(tokens, "free" in tokens, exact, rangeStart, rangeEnd, dayPart)
        }
    }
}

private fun freeSearch(items: List<DaylineItem>, query: SearchQuery): List<FreeSlot> {
    val today = LocalDate.now()
    val dates = when {
        query.exactDate != null -> listOf(query.exactDate)
        query.rangeStart != null && query.rangeEnd != null -> generateSequence(query.rangeStart) { if (it < query.rangeEnd) it.plusDays(1) else null }.toList()
        else -> (0L..6L).map(today::plusDays)
    }
    val (dayStart, dayEnd) = query.dayPart ?: (LocalTime.of(7, 0) to LocalTime.of(22, 0))
    return dates.flatMap { date -> PlanningEngine.freeSlots(items, date, dayStart, dayEnd, minMinutes = 30) }.filter { slot ->
        dateAllowed(slot.date, query)
    }.take(12)
}

private fun search(items: List<DaylineItem>, spaces: List<DaylineSpace>, raw: String): List<SearchHit> {
    val locale = Locale.getDefault()
    val query = SearchQuery.parse(raw)
    val tokens = query.rawTokens.filterNot { it in setOf("this", "next", "week", "morning", "afternoon", "evening", "free") || weekdayToken(it) != null }
    if (query.rawTokens.isEmpty()) return emptyList()
    val today = LocalDate.now()
    val requestedMonth = query.rawTokens.firstNotNullOfOrNull(::monthToken)
    val spacesById = spaces.associateBy { it.id }

    return items.mapNotNull { item ->
        val occurrenceDate = when {
            query.exactDate != null -> query.exactDate.takeIf(item::occursOn)
            query.rangeStart != null && query.rangeEnd != null -> findOccurrence(item, query.rangeStart, query.rangeEnd)
            requestedMonth != null -> findMonthOccurrence(item, requestedMonth, today)
            else -> item.startDate
        } ?: return@mapNotNull null

        val searchable = buildString {
            append(item.title.lowercase(locale)); append(' ')
            append(item.calendarName?.lowercase(locale).orEmpty()); append(' ')
            append(spacesById[item.spaceId]?.name?.lowercase(locale).orEmpty()); append(' ')
            append(item.startDate)
        }

        val matchesAll = tokens.all { token -> when {
            token == "today" -> item.occursOn(today)
            token == "tomorrow" -> item.occursOn(today.plusDays(1))
            token == "unfinished" || token == "todo" -> item.kind == AgendaKind.TASK && !item.isCompletedOn(occurrenceDate)
            token == "focus" || token == "pomodoro" -> item.focusCycle != FocusCycle.OFF
            token == "task" || token == "tasks" -> item.kind == AgendaKind.TASK
            token == "event" || token == "events" -> item.kind == AgendaKind.EVENT
            token == "allday" || token == "all-day" -> item.allDay
            monthToken(token) != null -> requestedMonth != null
            else -> searchable.contains(token)
        } }
        if (!matchesAll) null else SearchHit(item, occurrenceDate)
    }.sortedWith(compareBy<SearchHit> { it.occurrenceDate }.thenBy { it.item.startTime == null }.thenBy { it.item.startTime }.thenBy { it.item.title })
}

private fun dateAllowed(date: LocalDate, query: SearchQuery): Boolean = when {
    query.exactDate != null -> date == query.exactDate
    query.rangeStart != null && query.rangeEnd != null -> !date.isBefore(query.rangeStart) && !date.isAfter(query.rangeEnd)
    else -> true
}

private fun findOccurrence(item: DaylineItem, start: LocalDate, end: LocalDate): LocalDate? {
    var date = maxOf(item.startDate, start)
    while (!date.isAfter(end)) { if (item.occursOn(date)) return date; date = date.plusDays(1) }
    return null
}

private fun findMonthOccurrence(item: DaylineItem, month: Month, today: LocalDate): LocalDate? {
    for (year in listOf(today.year, today.year + 1, item.startDate.year).distinct()) {
        val ym = YearMonth.of(year, month)
        val hit = findOccurrence(item, maxOf(item.startDate, ym.atDay(1)), ym.atEndOfMonth())
        if (hit != null) return hit
    }
    return null
}

private fun weekdayToken(token: String): DayOfWeek? {
    val locale = Locale.getDefault()
    return DayOfWeek.entries.firstOrNull { day ->
        val full = day.getDisplayName(TextStyle.FULL, locale).lowercase(locale)
        val short = day.getDisplayName(TextStyle.SHORT, locale).lowercase(locale)
        token == full || token == short || (token.length >= 3 && full.startsWith(token))
    }
}

private fun monthToken(token: String): Month? {
    if (token.length < 3) return null
    val locale = Locale.getDefault()
    return Month.entries.firstOrNull { month ->
        val full = month.getDisplayName(TextStyle.FULL, locale).lowercase(locale)
        val short = month.getDisplayName(TextStyle.SHORT, locale).lowercase(locale)
        token == full || token == short || token == month.name.lowercase(locale) || full.startsWith(token)
    }
}

private fun dayLabel(date: LocalDate): String = when (date) {
    LocalDate.now() -> "Today"
    LocalDate.now().plusDays(1) -> "Tomorrow"
    else -> date.format(DateTimeFormatter.ofPattern("EEE, MMM d"))
}

private val TIME = DateTimeFormatter.ofPattern("HH:mm")
