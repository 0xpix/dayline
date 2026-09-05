package com.pix.dayline.ui.calendar

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pix.dayline.model.DaylineItem
import com.pix.dayline.model.occursOn
import com.pix.dayline.ui.components.AgendaList
import com.pix.dayline.ui.components.FloatingControls
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun CalendarScreen(
    items: List<DaylineItem>,
    weekStartsMonday: Boolean,
    onMenu: () -> Unit,
    onToday: () -> Unit,
    onAdd: (LocalDate) -> Unit,
    onEdit: (DaylineItem) -> Unit,
    onToggleTask: (DaylineItem, LocalDate) -> Unit
) {
    val today = remember { LocalDate.now() }
    var month by remember { mutableStateOf(YearMonth.from(today)) }
    var selected by remember { mutableStateOf(today) }

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
                .padding(start = 28.dp, end = 80.dp, top = 34.dp, bottom = 130.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = month.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                    style = MaterialTheme.typography.displayMedium.copy(fontSize = 29.sp, lineHeight = 32.sp),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    MonthArrow("‹") {
                        month = month.minusMonths(1)
                        selected = month.atDay(1)
                    }
                    MonthArrow("›") {
                        month = month.plusMonths(1)
                        selected = month.atDay(1)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            WeekHeader(weekStartsMonday)
            Spacer(Modifier.height(8.dp))

            MonthGrid(
                month = month,
                selected = selected,
                today = today,
                weekStartsMonday = weekStartsMonday,
                items = items,
                onSelect = { selected = it }
            )

            Spacer(Modifier.height(28.dp))

            Text(
                text = selected.format(DateTimeFormatter.ofPattern("EEEE, MMMM d")),
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(16.dp))

            val selectedItems = items
                .filter { it.occursOn(selected) }
                .sortedWith(compareBy<DaylineItem> { it.time == null }.thenBy { it.time })

            AgendaList(
                items = selectedItems,
                date = selected,
                emptyText = "Nothing on this day.",
                onEdit = onEdit,
                onToggleTask = onToggleTask
            )
        }

        FloatingControls(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 24.dp),
            onMenu = onMenu,
            onToday = onToday,
            onAdd = { onAdd(selected) }
        )
    }
}

@Composable
private fun MonthArrow(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        ),
        style = MaterialTheme.typography.titleMedium.copy(fontSize = 28.sp),
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
private fun WeekHeader(weekStartsMonday: Boolean) {
    val days = if (weekStartsMonday) {
        listOf("M", "T", "W", "T", "F", "S", "S")
    } else {
        listOf("S", "M", "T", "W", "T", "F", "S")
    }

    Row(modifier = Modifier.fillMaxWidth()) {
        days.forEach { day ->
            Text(
                text = day,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    selected: LocalDate,
    today: LocalDate,
    weekStartsMonday: Boolean,
    items: List<DaylineItem>,
    onSelect: (LocalDate) -> Unit
) {
    val first = month.atDay(1)
    val firstDayIndex = if (weekStartsMonday) {
        first.dayOfWeek.value - 1
    } else {
        first.dayOfWeek.value % 7
    }

    val cells = List<LocalDate?>(42) { index ->
        val day = index - firstDayIndex + 1
        if (day in 1..month.lengthOfMonth()) month.atDay(day) else null
    }

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        cells.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (date != null) {
                            val active = date == selected
                            val hasItems = items.any { it.occursOn(date) }

                            Surface(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = { onSelect(date) }
                                    ),
                                shape = CircleShape,
                                color = if (active) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.background,
                                contentColor = if (active) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onBackground
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = date.dayOfMonth.toString(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (active) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onBackground
                                    )

                                    if (hasItems) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .padding(bottom = 3.dp)
                                                .size(3.dp)
                                                .background(
                                                    if (active) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    CircleShape
                                                )
                                        )
                                    }
                                }
                            }

                            if (date == today && !active) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(start = 16.dp, bottom = 2.dp)
                                        .size(3.dp)
                                        .background(MaterialTheme.colorScheme.onBackground, CircleShape)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
