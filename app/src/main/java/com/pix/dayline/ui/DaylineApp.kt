package com.pix.dayline.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.pix.dayline.data.Appearance
import com.pix.dayline.data.DaylineStore
import com.pix.dayline.model.AgendaKind
import com.pix.dayline.model.DaylineItem
import com.pix.dayline.ui.calendar.CalendarScreen
import com.pix.dayline.ui.components.NavigationSheet
import com.pix.dayline.ui.components.QuickAddSheet
import com.pix.dayline.ui.settings.SettingsScreen
import com.pix.dayline.ui.tasks.TasksScreen
import com.pix.dayline.ui.theme.DaylineTheme
import com.pix.dayline.ui.today.TodayScreen
import com.pix.dayline.ui.upcoming.UpcomingScreen
import java.time.LocalDate

enum class DaylineScreen {
    TODAY,
    CALENDAR,
    UPCOMING,
    TASKS,
    SETTINGS
}

private data class AddRequest(
    val date: LocalDate,
    val kind: AgendaKind
)

@Composable
fun DaylineApp() {
    val context = LocalContext.current
    val store = remember(context) { DaylineStore(context.applicationContext) }

    var items by remember { mutableStateOf(store.loadItems()) }
    var appearance by remember { mutableStateOf(store.loadAppearance()) }
    var showOrb by remember { mutableStateOf(store.loadShowOrb()) }
    var weekStartsMonday by remember { mutableStateOf(store.loadWeekStartsMonday()) }

    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (appearance) {
        Appearance.SYSTEM -> systemDark
        Appearance.LIGHT -> false
        Appearance.DARK -> true
    }

    DaylineTheme(darkTheme = darkTheme) {
        var currentScreen by remember { mutableStateOf(DaylineScreen.TODAY) }
        var menuOpen by remember { mutableStateOf(false) }
        var addRequest by remember { mutableStateOf<AddRequest?>(null) }
        var editing by remember { mutableStateOf<DaylineItem?>(null) }

        fun saveAll(next: List<DaylineItem>) {
            items = next
            store.saveItems(next)
        }

        fun saveItem(item: DaylineItem) {
            val exists = items.any { it.id == item.id }
            val next = if (exists) {
                items.map { if (it.id == item.id) item else it }
            } else {
                items + item
            }
            saveAll(next)
            editing = null
            addRequest = null
        }

        fun deleteItem(item: DaylineItem) {
            saveAll(items.filterNot { it.id == item.id })
            editing = null
        }

        fun toggleTask(item: DaylineItem, date: LocalDate) {
            val completed = item.completedDates.toMutableSet().apply {
                if (date in this) remove(date) else add(date)
            }
            saveItem(item.copy(completedDates = completed))
        }

        when (currentScreen) {
            DaylineScreen.TODAY -> TodayScreen(
                items = items,
                showOrb = showOrb,
                onMenu = { menuOpen = true },
                onAdd = { addRequest = AddRequest(it, AgendaKind.EVENT) },
                onEdit = { editing = it },
                onToggleTask = ::toggleTask
            )

            DaylineScreen.CALENDAR -> CalendarScreen(
                items = items,
                weekStartsMonday = weekStartsMonday,
                onMenu = { menuOpen = true },
                onToday = { currentScreen = DaylineScreen.TODAY },
                onAdd = { addRequest = AddRequest(it, AgendaKind.EVENT) },
                onEdit = { editing = it },
                onToggleTask = ::toggleTask
            )

            DaylineScreen.UPCOMING -> UpcomingScreen(
                items = items,
                onMenu = { menuOpen = true },
                onToday = { currentScreen = DaylineScreen.TODAY },
                onAdd = { addRequest = AddRequest(it, AgendaKind.EVENT) },
                onEdit = { editing = it },
                onToggleTask = ::toggleTask
            )

            DaylineScreen.TASKS -> TasksScreen(
                items = items,
                onMenu = { menuOpen = true },
                onToday = { currentScreen = DaylineScreen.TODAY },
                onAdd = { addRequest = AddRequest(it, AgendaKind.TASK) },
                onEdit = { editing = it },
                onToggleTask = ::toggleTask
            )

            DaylineScreen.SETTINGS -> SettingsScreen(
                appearance = appearance,
                showOrb = showOrb,
                weekStartsMonday = weekStartsMonday,
                onAppearance = {
                    appearance = it
                    store.saveAppearance(it)
                },
                onShowOrb = {
                    showOrb = it
                    store.saveShowOrb(it)
                },
                onWeekStart = {
                    weekStartsMonday = it
                    store.saveWeekStartsMonday(it)
                },
                onMenu = { menuOpen = true },
                onToday = { currentScreen = DaylineScreen.TODAY }
            )
        }

        if (menuOpen) {
            NavigationSheet(
                current = currentScreen,
                onSelect = {
                    currentScreen = it
                    menuOpen = false
                },
                onDismiss = { menuOpen = false }
            )
        }

        editing?.let { item ->
            QuickAddSheet(
                initialDate = item.startDate,
                initialKind = item.kind,
                editing = item,
                onSave = ::saveItem,
                onDelete = ::deleteItem,
                onDismiss = { editing = null }
            )
        } ?: addRequest?.let { request ->
            QuickAddSheet(
                initialDate = request.date,
                initialKind = request.kind,
                onSave = ::saveItem,
                onDismiss = { addRequest = null }
            )
        }
    }
}
