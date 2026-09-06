package com.pix.dayline.ui.upcoming

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pix.dayline.model.*
import com.pix.dayline.ui.components.FloatingControls
import com.pix.dayline.ui.theme.composeColor
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private sealed interface UpcomingFilter {
    data object All : UpcomingFilter
    data object Events : UpcomingFilter
    data object Tasks : UpcomingFilter
    data object Holidays : UpcomingFilter
    data object Meetings : UpcomingFilter
    data class Calendar(
        val name: String
    ) : UpcomingFilter
    data class Space(
        val id: String,
        val name: String
    ) : UpcomingFilter
}

@Composable
fun UpcomingScreen(
    items: List<DaylineItem>,
    spaces: List<DaylineSpace>,
    onMenu: () -> Unit,
    onToday: () -> Unit,
    onAdd: (LocalDate) -> Unit,
    onEdit: (DaylineItem, LocalDate) -> Unit,
    onToggleTask: (DaylineItem, LocalDate) -> Unit
) {
    val today = remember { LocalDate.now() }
    var filter by remember {
        mutableStateOf<UpcomingFilter>(
            UpcomingFilter.All
        )
    }

    val calendarNames = remember(items) {
        items.mapNotNull { it.calendarName }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    val activeSpaces = remember(items, spaces) {
        spaces.filter { space ->
            items.any { it.spaceId == space.id }
        }
    }

    val filteredItems = remember(
        items,
        filter
    ) {
        items.filter { item ->
            matchesUpcomingFilter(item, filter)
        }
    }

    val dates = remember(
        filteredItems,
        today
    ) {
        (0L..120L)
            .map { today.plusDays(it) }
            .filter { date ->
                filteredItems.any { it.occursOn(date) }
            }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                MaterialTheme.colorScheme.background
            )
            .padding(
                top =
                    WindowInsets.statusBars
                        .asPaddingValues()
                        .calculateTopPadding(),
                bottom =
                    WindowInsets.navigationBars
                        .asPaddingValues()
                        .calculateBottomPadding()
            )
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(
                    start = 30.dp,
                    end = 30.dp,
                    top = 56.dp,
                    bottom = 138.dp
                )
        ) {
            Text(
                "Upcoming",
                style =
                    MaterialTheme.typography.displayMedium
            )

            Spacer(Modifier.height(18.dp))

            Text(
                "FILTER",
                style =
                    MaterialTheme.typography.labelMedium,
                color =
                    MaterialTheme.colorScheme
                        .onSurfaceVariant
            )

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(
                        rememberScrollState()
                    ),
                horizontalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {
                UpcomingFilterChip(
                    "All",
                    filter == UpcomingFilter.All
                ) {
                    filter = UpcomingFilter.All
                }

                UpcomingFilterChip(
                    "Meetings",
                    filter == UpcomingFilter.Meetings
                ) {
                    filter = UpcomingFilter.Meetings
                }

                UpcomingFilterChip(
                    "Holidays",
                    filter == UpcomingFilter.Holidays
                ) {
                    filter = UpcomingFilter.Holidays
                }

                UpcomingFilterChip(
                    "Events",
                    filter == UpcomingFilter.Events
                ) {
                    filter = UpcomingFilter.Events
                }

                UpcomingFilterChip(
                    "Tasks",
                    filter == UpcomingFilter.Tasks
                ) {
                    filter = UpcomingFilter.Tasks
                }

                calendarNames.forEach { name ->
                    val candidate =
                        UpcomingFilter.Calendar(name)

                    UpcomingFilterChip(
                        shortFilterLabel(name),
                        filter == candidate
                    ) {
                        filter = candidate
                    }
                }

                activeSpaces.forEach { space ->
                    val candidate =
                        UpcomingFilter.Space(
                            space.id,
                            space.name
                        )

                    UpcomingFilterChip(
                        space.name,
                        filter == candidate
                    ) {
                        filter = candidate
                    }
                }
            }

            Spacer(Modifier.height(30.dp))

            if (dates.isEmpty()) {
                Text(
                    emptyMessage(filter),
                    style =
                        MaterialTheme.typography.bodyMedium,
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                )
            }

            dates.forEach { date ->
                val dayItems =
                    filteredItems
                        .filter { it.occursOn(date) }
                        .sortedWith(
                            compareBy<DaylineItem> {
                                it.startTime == null
                            }.thenBy {
                                it.startTime
                            }
                        )

                UpcomingDay(
                    date,
                    today,
                    dayItems,
                    spaces,
                    onEdit,
                    onToggleTask
                )
            }
        }

        FloatingControls(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = 20.dp,
                    bottom = 28.dp
                ),
            onMenu = onMenu,
            onToday = onToday,
            onAdd = { onAdd(today) }
        )
    }
}

@Composable
private fun UpcomingFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(
            interactionSource = remember {
                MutableInteractionSource()
            },
            indication = null,
            onClick = onClick
        ),
        shape = RoundedCornerShape(16.dp),
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
                horizontal = 13.dp,
                vertical = 8.dp
            ),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1
        )
    }
}

private fun matchesUpcomingFilter(
    item: DaylineItem,
    filter: UpcomingFilter
): Boolean = when (filter) {
    UpcomingFilter.All -> true
    UpcomingFilter.Events ->
        item.kind == AgendaKind.EVENT
    UpcomingFilter.Tasks ->
        item.kind == AgendaKind.TASK
    UpcomingFilter.Holidays ->
        item.isHolidayLike()
    UpcomingFilter.Meetings ->
        item.isMeetingLike()
    is UpcomingFilter.Calendar ->
        item.calendarName == filter.name
    is UpcomingFilter.Space ->
        item.spaceId == filter.id
}

private fun DaylineItem.isHolidayLike(): Boolean {
    val haystack =
        "${calendarName.orEmpty()} $title"
            .lowercase()

    return listOf(
        "holiday",
        "holidays",
        "public holiday",
        "feiertag",
        "feiertage",
        "bank holiday"
    ).any { it in haystack }
}

private fun DaylineItem.isMeetingLike(): Boolean {
    if (kind != AgendaKind.EVENT) return false

    val haystack = title.lowercase()

    return listOf(
        "meeting",
        "meet",
        "call",
        "sync",
        "standup",
        "stand-up",
        "1:1",
        "1-on-1",
        "appointment",
        "interview",
        "zoom",
        "teams"
    ).any { it in haystack }
}

private fun shortFilterLabel(
    raw: String
): String =
    if (raw.length <= 18) {
        raw
    } else {
        raw.take(17) + "…"
    }

private fun emptyMessage(
    filter: UpcomingFilter
): String = when (filter) {
    UpcomingFilter.All ->
        "Nothing upcoming."
    UpcomingFilter.Meetings ->
        "No meetings coming up."
    UpcomingFilter.Holidays ->
        "No holidays coming up."
    UpcomingFilter.Events ->
        "No events coming up."
    UpcomingFilter.Tasks ->
        "No tasks coming up."
    is UpcomingFilter.Calendar ->
        "Nothing upcoming in ${filter.name}."
    is UpcomingFilter.Space ->
        "Nothing upcoming in ${filter.name}."
}

@Composable
private fun UpcomingDay(
    date: LocalDate,
    today: LocalDate,
    items: List<DaylineItem>,
    spaces: List<DaylineSpace>,
    onEdit: (DaylineItem, LocalDate) -> Unit,
    onToggleTask: (
        DaylineItem,
        LocalDate
    ) -> Unit
) {
    val accent =
        items.firstOrNull()
            ?.color
            ?.composeColor()
            ?: MaterialTheme.colorScheme.onBackground

    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 34.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            date.dayOfMonth.toString(),
            style =
                MaterialTheme.typography.displayMedium
                    .copy(
                        fontSize = 46.sp,
                        lineHeight = 48.sp
                    ),
            color =
                if (date == today) {
                    accent
                } else {
                    MaterialTheme.colorScheme
                        .onBackground.copy(
                            alpha =
                                if (
                                    date.isBefore(
                                        today.plusDays(2)
                                    )
                                ) 1f else .30f
                        )
                },
            modifier = Modifier.width(72.dp)
        )

        Column(Modifier.weight(1f)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.End
            ) {
                Column(
                    horizontalAlignment =
                        Alignment.End
                ) {
                    Text(
                        if (date == today) {
                            "Today"
                        } else {
                            date.dayOfWeek
                                .getDisplayName(
                                    TextStyle.FULL,
                                    Locale.getDefault()
                                )
                        },
                        style =
                            MaterialTheme.typography
                                .labelMedium
                    )

                    Text(
                        date.month.getDisplayName(
                            TextStyle.FULL,
                            Locale.getDefault()
                        ),
                        style =
                            MaterialTheme.typography
                                .bodyMedium,
                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            items.forEach { item ->
                val done =
                    item.kind == AgendaKind.TASK &&
                        item.isCompletedOn(date)

                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .clickable(
                            interactionSource =
                                remember {
                                    MutableInteractionSource()
                                },
                            indication = null,
                            onClick = {
                                if (
                                    item.kind ==
                                    AgendaKind.TASK
                                ) {
                                    onToggleTask(
                                        item,
                                        date
                                    )
                                } else {
                                    onEdit(item, date)
                                }
                            }
                        ),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    Text(
                        if (done) {
                            "DONE"
                        } else {
                            item.startTime?.format(
                                DateTimeFormatter
                                    .ofPattern("HH:mm")
                            ) ?: if (
                                item.kind ==
                                AgendaKind.TASK
                            ) {
                                "TODO"
                            } else {
                                "ALL"
                            }
                        },
                        modifier =
                            Modifier.width(64.dp),
                        style =
                            MaterialTheme.typography
                                .labelMedium,
                        color =
                            item.color
                                .composeColor()
                                .copy(
                                    alpha =
                                        if (done) .45f
                                        else 1f
                                )
                    )

                    Column(
                        Modifier.weight(1f)
                    ) {
                        Text(
                            item.title,
                            style =
                                MaterialTheme.typography
                                    .bodyLarge,
                            color =
                                MaterialTheme.colorScheme
                                    .onBackground.copy(
                                        alpha =
                                            if (done) {
                                                .45f
                                            } else {
                                                1f
                                            }
                                    )
                        )

                        val meta =
                            spaces.firstOrNull {
                                it.id == item.spaceId
                            }?.name
                                ?: item.calendarName

                        meta?.let {
                            Text(
                                it,
                                style =
                                    MaterialTheme.typography
                                        .bodyMedium,
                                color =
                                    MaterialTheme.colorScheme
                                        .onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
