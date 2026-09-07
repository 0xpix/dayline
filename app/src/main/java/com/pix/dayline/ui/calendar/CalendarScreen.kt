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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pix.dayline.model.AgendaKind
import com.pix.dayline.model.DaylineItem
import com.pix.dayline.model.occursOn
import com.pix.dayline.planning.PlanningEngine
import com.pix.dayline.ui.components.FloatingControls
import com.pix.dayline.ui.theme.composeColor
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    items: List<DaylineItem>,
    weekStartsMonday: Boolean,
    onMenu: () -> Unit,
    onToday: () -> Unit,
    onAdd: (LocalDate) -> Unit,
    onEdit: (DaylineItem, LocalDate) -> Unit,
    onToggleTask: (DaylineItem, LocalDate) -> Unit
) {
    val today = remember { LocalDate.now() }
    var month by remember { mutableStateOf(YearMonth.from(today)) }
    var previewDate by remember { mutableStateOf<LocalDate?>(null) }

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
                .padding(start = 30.dp, end = 30.dp, top = 56.dp, bottom = 138.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = month.format(DateTimeFormatter.ofPattern("MMMM", Locale.getDefault())),
                        style = MaterialTheme.typography.displayMedium.copy(fontSize = 32.sp, lineHeight = 34.sp)
                    )
                    Text(
                        month.year.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    MonthArrow("‹") { month = month.minusMonths(1) }
                    MonthArrow("›") { month = month.plusMonths(1) }
                }
            }

            Spacer(Modifier.height(28.dp))
            WeekHeader(weekStartsMonday)
            Spacer(Modifier.height(10.dp))
            MonthGrid(
                month = month,
                today = today,
                weekStartsMonday = weekStartsMonday,
                items = items,
                onSelect = { previewDate = it }
            )

            Spacer(Modifier.height(28.dp))
            Text(
                "Tap a day for its agenda and free time.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        FloatingControls(
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 28.dp),
            onMenu = onMenu,
            onToday = onToday,
            onAdd = { onAdd(previewDate ?: today) }
        )
    }

    previewDate?.let { date ->
        DayPreviewSheet(
            date = date,
            items = items,
            onAdd = { onAdd(date) },
            onEdit = { onEdit(it, date) },
            onToggleTask = { onToggleTask(it, date) },
            onDismiss = { previewDate = null }
        )
    }
}

@Composable
private fun MonthArrow(label: String, onClick: () -> Unit) {
    Text(
        label,
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        ),
        style = MaterialTheme.typography.titleMedium.copy(fontSize = 28.sp)
    )
}

@Composable
private fun WeekHeader(weekStartsMonday: Boolean) {
    val days = if (weekStartsMonday) listOf("M", "T", "W", "T", "F", "S", "S")
    else listOf("S", "M", "T", "W", "T", "F", "S")
    Row(Modifier.fillMaxWidth()) {
        days.forEach { day ->
            Text(
                day,
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
    today: LocalDate,
    weekStartsMonday: Boolean,
    items: List<DaylineItem>,
    onSelect: (LocalDate) -> Unit
) {
    val first = month.atDay(1)
    val firstIndex = if (weekStartsMonday) first.dayOfWeek.value - 1 else first.dayOfWeek.value % 7
    val cells = List<LocalDate?>(42) { index ->
        val day = index - firstIndex + 1
        if (day in 1..month.lengthOfMonth()) month.atDay(day) else null
    }

    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    Box(
                        Modifier.weight(1f).height(52.dp),
                        contentAlignment = Alignment.TopStart
                    ) {
                        if (date != null) {
                            val dayItems = items.filter { it.occursOn(date) }
                            Surface(
                                modifier = Modifier.size(42.dp).clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { onSelect(date) }
                                ),
                                shape = CircleShape,
                                color = if (date == today) MaterialTheme.colorScheme.onBackground
                                else MaterialTheme.colorScheme.background,
                                contentColor = if (date == today) MaterialTheme.colorScheme.background
                                else MaterialTheme.colorScheme.onBackground
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(date.dayOfMonth.toString(), style = MaterialTheme.typography.bodyMedium)
                                    Row(
                                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        dayItems.take(3).forEach { item ->
                                            Box(
                                                Modifier.size(3.dp).background(
                                                    if (date == today) MaterialTheme.colorScheme.background
                                                    else item.color.composeColor(),
                                                    CircleShape
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayPreviewSheet(
    date: LocalDate,
    items: List<DaylineItem>,
    onAdd: () -> Unit,
    onEdit: (DaylineItem) -> Unit,
    onToggleTask: (DaylineItem) -> Unit,
    onDismiss: () -> Unit
) {
    val dayItems = items.filter { it.occursOn(date) }
        .sortedWith(compareBy<DaylineItem> { it.startTime == null }.thenBy { it.startTime })
    val events = dayItems.count { it.kind == AgendaKind.EVENT }
    val tasks = dayItems.count { it.kind == AgendaKind.TASK }
    val free = PlanningEngine.freeSlots(items, date, minMinutes = 30).take(4)

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp).padding(bottom = 36.dp)
        ) {
            Text(
                date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault())),
                style = MaterialTheme.typography.displaySmall
            )
            Spacer(Modifier.height(7.dp))
            Text(
                "$events ${if (events == 1) "event" else "events"} · $tasks ${if (tasks == 1) "task" else "tasks"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(24.dp))
            if (dayItems.isEmpty()) {
                Text("Nothing planned.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                dayItems.forEach { item ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onEdit(item) }.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            item.startTime?.format(TIME) ?: "—",
                            modifier = Modifier.padding(end = 16.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(item.title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                        if (item.kind == AgendaKind.TASK && !item.calendarReadOnly) {
                            Text(
                                if (date in item.completedDates) "●" else "○",
                                modifier = Modifier.clickable { onToggleTask(item) }.padding(8.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Text("FREE TIME", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            if (free.isEmpty()) {
                Text("No 30-minute gaps between 07:00 and 22:00.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                free.forEach { slot ->
                    Text(
                        "${slot.start.format(TIME)} — ${slot.end.format(TIME)}  ·  ${slot.durationMinutes} min",
                        modifier = Modifier.padding(vertical = 6.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            Text(
                "ADD TO THIS DAY  ›",
                modifier = Modifier.clickable { onAdd(); onDismiss() }.padding(vertical = 10.dp),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

private val TIME = DateTimeFormatter.ofPattern("HH:mm")
