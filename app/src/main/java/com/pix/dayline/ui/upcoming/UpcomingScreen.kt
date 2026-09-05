package com.pix.dayline.ui.upcoming

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pix.dayline.model.DaylineItem
import com.pix.dayline.model.occursOn
import com.pix.dayline.ui.components.AgendaList
import com.pix.dayline.ui.components.FloatingControls
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun UpcomingScreen(
    items: List<DaylineItem>,
    onMenu: () -> Unit,
    onToday: () -> Unit,
    onAdd: (LocalDate) -> Unit,
    onEdit: (DaylineItem) -> Unit,
    onToggleTask: (DaylineItem, LocalDate) -> Unit
) {
    val today = remember { LocalDate.now() }
    val upcomingDates = remember(items, today) {
        (0L..30L).map { today.plusDays(it) }
            .filter { date -> items.any { it.occursOn(date) } }
    }

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
                text = "Upcoming",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(30.dp))

            if (upcomingDates.isEmpty()) {
                Text(
                    text = "Nothing in the next 30 days.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                upcomingDates.forEachIndexed { index, date ->
                    Text(
                        text = if (date == today) "Today" else date.format(DateTimeFormatter.ofPattern("EEE, MMM d")),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))

                    val dayItems = items
                        .filter { it.occursOn(date) }
                        .sortedWith(compareBy<DaylineItem> { it.time == null }.thenBy { it.time })

                    AgendaList(
                        items = dayItems,
                        date = date,
                        onEdit = onEdit,
                        onToggleTask = onToggleTask
                    )

                    if (index != upcomingDates.lastIndex) Spacer(Modifier.height(28.dp))
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
