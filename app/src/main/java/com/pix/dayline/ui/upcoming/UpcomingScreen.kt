package com.pix.dayline.ui.upcoming

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.pix.dayline.model.*
import com.pix.dayline.ui.components.FloatingControls
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private sealed interface UpcomingFilter {
    data object All : UpcomingFilter
    data object Events : UpcomingFilter
    data object Tasks : UpcomingFilter
    data object Focus : UpcomingFilter
    data object Holidays : UpcomingFilter
    data object Meetings : UpcomingFilter
    data class Calendar(val name: String) : UpcomingFilter
    data class Space(val id: String, val name: String) : UpcomingFilter
}

private enum class UpcomingWindow(val label: String) {
    TODAY("Today"), TOMORROW("Tomorrow"), WEEK("7 days"), ALL("All")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpcomingScreen(
    items: List<DaylineItem>,
    spaces: List<DaylineSpace>,
    onMenu: () -> Unit,
    onToday: () -> Unit,
    onAdd: (LocalDate) -> Unit,
    onEdit: (DaylineItem, LocalDate) -> Unit,
    onToggleTask: (DaylineItem, LocalDate) -> Unit,
    onSwipeToday: () -> Unit = onToday
) {
    val today = remember { LocalDate.now() }
    var filter by remember { mutableStateOf<UpcomingFilter>(UpcomingFilter.All) }
    var window by remember { mutableStateOf(UpcomingWindow.WEEK) }
    var filterOpen by remember { mutableStateOf(false) }
    var dragTotal by remember { mutableFloatStateOf(0f) }

    val calendarNames = remember(items) {
        items.mapNotNull { it.calendarName }.filter(String::isNotBlank).distinct().sorted()
    }
    val activeSpaces = remember(items, spaces) {
        spaces.filter { space -> items.any { it.spaceId == space.id } }
    }
    val filteredItems = remember(items, filter) { items.filter { matchesUpcomingFilter(it, filter) } }
    val candidateDates = remember(today, window) {
        when (window) {
            UpcomingWindow.TODAY -> listOf(today)
            UpcomingWindow.TOMORROW -> listOf(today.plusDays(1))
            UpcomingWindow.WEEK -> (0L..6L).map(today::plusDays)
            UpcomingWindow.ALL -> (0L..120L).map(today::plusDays)
        }
    }
    val dates = remember(filteredItems, candidateDates) {
        candidateDates.filter { date -> filteredItems.any { it.occursOn(date) } }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(onSwipeToday) {
                detectHorizontalDragGestures(
                    onDragStart = { dragTotal = 0f },
                    onHorizontalDrag = { _, amount -> dragTotal += amount },
                    onDragCancel = { dragTotal = 0f },
                    onDragEnd = {
                        if (dragTotal > 120f) onSwipeToday()
                        dragTotal = 0f
                    }
                )
            }
            .padding(
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            )
    ) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(start = 30.dp, end = 30.dp, top = 52.dp, bottom = 138.dp)
        ) {
            Text("Upcoming", style = MaterialTheme.typography.displayMedium)
            Spacer(Modifier.height(10.dp))
            Text(
                "Swipe right for Today",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))

            Surface(
                modifier = Modifier.fillMaxWidth().clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { filterOpen = true },
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${window.label} · ${filterLabel(filter)}", style = MaterialTheme.typography.bodyLarge)
                    Text("FILTER  ›", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(34.dp))
            if (dates.isEmpty()) {
                Text(
                    emptyMessage(filter, window),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            dates.forEach { date ->
                val dayItems = filteredItems.filter { it.occursOn(date) }
                    .sortedWith(compareBy<DaylineItem> { it.startTime == null }.thenBy { it.startTime }.thenBy { it.title })
                UpcomingDay(
                    date = date,
                    today = today,
                    items = dayItems,
                    spaces = spaces,
                    onEdit = onEdit,
                    onToggleTask = onToggleTask
                )
            }
        }

        FloatingControls(
            Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 28.dp),
            onMenu = onMenu,
            onToday = onToday,
            onAdd = { onAdd(today) }
        )
    }

    if (filterOpen) {
        ModalBottomSheet(
            onDismissRequest = { filterOpen = false },
            containerColor = MaterialTheme.colorScheme.background
        ) {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(horizontal = 28.dp).padding(bottom = 36.dp)
            ) {
                Text("Upcoming filter", style = MaterialTheme.typography.displaySmall)
                Spacer(Modifier.height(24.dp))
                FilterSection("WHEN") {
                    UpcomingWindow.entries.forEach { option ->
                        FilterRow(option.label, option == window) { window = option }
                    }
                }
                Spacer(Modifier.height(24.dp))
                FilterSection("SHOW") {
                    listOf(
                        UpcomingFilter.All,
                        UpcomingFilter.Events,
                        UpcomingFilter.Tasks,
                        UpcomingFilter.Focus,
                        UpcomingFilter.Meetings,
                        UpcomingFilter.Holidays
                    ).forEach { option ->
                        FilterRow(filterLabel(option), option == filter) { filter = option }
                    }
                    calendarNames.forEach { name ->
                        val option = UpcomingFilter.Calendar(name)
                        FilterRow(name, option == filter) { filter = option }
                    }
                    activeSpaces.forEach { space ->
                        val option = UpcomingFilter.Space(space.id, space.name)
                        FilterRow(space.name, option == filter) { filter = option }
                    }
                }
                Spacer(Modifier.height(18.dp))
                Text(
                    "DONE",
                    modifier = Modifier.clickable { filterOpen = false }.padding(vertical = 12.dp),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun FilterSection(label: String, content: @Composable ColumnScope.() -> Unit) {
    Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(8.dp))
    Column(content = content)
}

@Composable
private fun FilterRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 11.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(if (selected) "●" else "○", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun matchesUpcomingFilter(item: DaylineItem, filter: UpcomingFilter): Boolean = when (filter) {
    UpcomingFilter.All -> true
    UpcomingFilter.Events -> item.kind == AgendaKind.EVENT
    UpcomingFilter.Tasks -> item.kind == AgendaKind.TASK
    UpcomingFilter.Focus -> item.kind == AgendaKind.EVENT && item.focusCycle != FocusCycle.OFF
    UpcomingFilter.Holidays -> item.isHolidayLike()
    UpcomingFilter.Meetings -> item.isMeetingLike()
    is UpcomingFilter.Calendar -> item.calendarName == filter.name
    is UpcomingFilter.Space -> item.spaceId == filter.id
}

private fun filterLabel(filter: UpcomingFilter): String = when (filter) {
    UpcomingFilter.All -> "All"
    UpcomingFilter.Events -> "Events"
    UpcomingFilter.Tasks -> "Tasks"
    UpcomingFilter.Focus -> "Focus"
    UpcomingFilter.Meetings -> "Meetings"
    UpcomingFilter.Holidays -> "Holidays"
    is UpcomingFilter.Calendar -> filter.name
    is UpcomingFilter.Space -> filter.name
}

private fun DaylineItem.isHolidayLike(): Boolean {
    val text = "${calendarName.orEmpty()} $title".lowercase()
    return listOf("holiday", "public holiday", "feiertag", "bank holiday").any { it in text }
}

private fun DaylineItem.isMeetingLike(): Boolean {
    if (kind != AgendaKind.EVENT) return false
    val text = title.lowercase()
    return listOf("meeting", "meet", "call", "sync", "standup", "1:1", "appointment", "interview", "zoom", "teams").any { it in text }
}

private fun emptyMessage(filter: UpcomingFilter, window: UpcomingWindow): String {
    val whenText = when (window) {
        UpcomingWindow.TODAY -> "today"
        UpcomingWindow.TOMORROW -> "tomorrow"
        UpcomingWindow.WEEK -> "in the next 7 days"
        UpcomingWindow.ALL -> "coming up"
    }
    return "No ${filterLabel(filter).lowercase()} $whenText."
}

@Composable
private fun UpcomingDay(
    date: LocalDate,
    today: LocalDate,
    items: List<DaylineItem>,
    spaces: List<DaylineSpace>,
    onEdit: (DaylineItem, LocalDate) -> Unit,
    onToggleTask: (DaylineItem, LocalDate) -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(bottom = 34.dp)) {
        Text(
            when (date) {
                today -> "TODAY"
                today.plusDays(1) -> "TOMORROW"
                else -> "${date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).uppercase()} ${date.dayOfMonth}"
            },
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            date.month.getDisplayName(TextStyle.FULL, Locale.getDefault()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))

        items.forEach { item ->
            val done = item.kind == AgendaKind.TASK && item.isCompletedOn(date)
            Row(
                Modifier.fillMaxWidth().clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onEdit(item, date) }.padding(vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    item.startTime?.format(TIME) ?: if (item.kind == AgendaKind.TASK) "TODO" else "ALL",
                    modifier = Modifier.width(62.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        item.title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = if (done) .45f else 1f)
                    )
                    val meta = buildList {
                        spaces.firstOrNull { it.id == item.spaceId }?.name?.let(::add)
                            ?: item.calendarName?.let(::add)
                        if (item.kind == AgendaKind.TASK) add("${item.estimatedDurationMinutes} min")
                        if (item.focusCycle != FocusCycle.OFF) add("${item.focusMinutes}/${item.breakMinutes} focus")
                    }.joinToString(" · ")
                    if (meta.isNotBlank()) {
                        Text(meta, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (item.kind == AgendaKind.TASK && !item.calendarReadOnly) {
                    Text(
                        if (done) "✓" else "○",
                        modifier = Modifier.clickable { onToggleTask(item, date) }.padding(8.dp),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

private val TIME = DateTimeFormatter.ofPattern("HH:mm")
