package com.pix.dayline.ui

import android.Manifest
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.pix.dayline.data.*
import com.pix.dayline.model.*
import com.pix.dayline.notifications.NotificationScheduler
import com.pix.dayline.notifications.NowActivityScheduler
import com.pix.dayline.ui.calendar.CalendarScreen
import com.pix.dayline.ui.components.NavigationSheet
import com.pix.dayline.ui.components.QuickAddSheet
import com.pix.dayline.ui.settings.SettingsScreen
import com.pix.dayline.ui.spaces.SpaceEditSheet
import com.pix.dayline.ui.spaces.SpacesScreen
import com.pix.dayline.ui.tasks.TaskDetailScreen
import com.pix.dayline.ui.tasks.TasksScreen
import com.pix.dayline.ui.theme.DaylineTheme
import com.pix.dayline.ui.today.TodayScreen
import com.pix.dayline.ui.upcoming.UpcomingScreen
import com.pix.dayline.widgets.DaylineWidgetUpdater
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class DaylineScreen { TODAY, CALENDAR, UPCOMING, TASKS, SPACES, SETTINGS }
private data class AddRequest(val date: LocalDate, val kind: AgendaKind)

@Composable
fun DaylineApp() {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val store = remember(context) { DaylineStore(appContext) }
    val widgetScope = rememberCoroutineScope()

    var items by remember { mutableStateOf(store.loadItems()) }
    var spaces by remember { mutableStateOf(store.loadSpaces()) }
    var appearance by remember { mutableStateOf(store.loadAppearance()) }
    var fontChoice by remember { mutableStateOf(store.loadFontChoice()) }
    var widgetFontChoice by remember { mutableStateOf(store.loadWidgetFontChoice()) }
    var widgetEmojiChoice by remember { mutableStateOf(store.loadWidgetEmojiChoice()) }
    var widgetAutoSlide by remember { mutableStateOf(store.loadWidgetAutoSlide()) }
    var nowActivityEnabled by remember { mutableStateOf(store.loadNowActivityEnabled()) }
    var calendarSyncEnabled by remember { mutableStateOf(store.loadCalendarSyncEnabled()) }
    var calendarItems by remember { mutableStateOf(emptyList<DaylineItem>()) }
    var showOrb by remember { mutableStateOf(store.loadShowOrb()) }
    var weekStartsMonday by remember { mutableStateOf(store.loadWeekStartsMonday()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    fun refreshCalendarOverlay() {
        calendarItems = if (
            calendarSyncEnabled &&
            AndroidCalendarSync.hasReadPermission(appContext)
        ) {
            AndroidCalendarSync.loadOccurrences(appContext)
        } else {
            emptyList()
        }

        widgetScope.launch {
            DaylineWidgetUpdater.updateAll(appContext)
        }
    }

    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted =
            grants[Manifest.permission.READ_CALENDAR] == true &&
            grants[Manifest.permission.WRITE_CALENDAR] == true

        calendarSyncEnabled = granted
        store.saveCalendarSyncEnabled(granted)

        if (granted) {
            val published = AndroidCalendarSync.publishExisting(
                appContext,
                items
            )
            items = published
            store.saveItems(published)
        }

        refreshCalendarOverlay()
    }

    LaunchedEffect(Unit) {
        NotificationScheduler.syncAll(appContext, items)

        if (nowActivityEnabled) {
            NowActivityScheduler.syncAll(appContext, items)
        } else {
            NowActivityScheduler.cancelAll(appContext, items)
        }

        if (
            calendarSyncEnabled &&
            AndroidCalendarSync.hasReadPermission(appContext)
        ) {
            calendarItems = AndroidCalendarSync.loadOccurrences(appContext)
        }
    }

    DisposableEffect(calendarSyncEnabled) {
        if (
            calendarSyncEnabled &&
            AndroidCalendarSync.hasReadPermission(appContext)
        ) {
            val observer = object : ContentObserver(
                Handler(Looper.getMainLooper())
            ) {
                override fun onChange(selfChange: Boolean) {
                    calendarItems =
                        AndroidCalendarSync.loadOccurrences(appContext)

                    widgetScope.launch {
                        DaylineWidgetUpdater.updateAll(appContext)
                    }
                }
            }

            appContext.contentResolver.registerContentObserver(
                CalendarContract.Events.CONTENT_URI,
                true,
                observer
            )

            onDispose {
                appContext.contentResolver.unregisterContentObserver(
                    observer
                )
            }
        } else {
            onDispose { }
        }
    }

    val visibleItems = remember(
        items,
        calendarItems,
        calendarSyncEnabled
    ) {
        if (!calendarSyncEnabled) {
            items
        } else {
            val mapped = items
                .mapNotNull { it.calendarEventId }
                .toSet()

            items + calendarItems.filterNot {
                it.calendarEventId in mapped
            }
        }
    }

    val dark = when (appearance) {
        Appearance.SYSTEM -> isSystemInDarkTheme()
        Appearance.LIGHT -> false
        Appearance.DARK -> true
    }

    DaylineTheme(
        darkTheme = dark,
        fontChoice = fontChoice,
        dynamicColor = appearance == Appearance.SYSTEM
    ) {
        var screen by remember { mutableStateOf(DaylineScreen.TODAY) }
        var history by remember { mutableStateOf(emptyList<DaylineScreen>()) }
        var menuOpen by remember { mutableStateOf(false) }
        var addRequest by remember { mutableStateOf<AddRequest?>(null) }
        var editing by remember { mutableStateOf<DaylineItem?>(null) }
        var taskDetail by remember { mutableStateOf<DaylineItem?>(null) }
        var spaceEditing by remember { mutableStateOf<DaylineSpace?>(null) }
        var newSpace by remember { mutableStateOf(false) }

        fun navigateTo(next: DaylineScreen) {
            if (next == screen) return
            history = history + screen
            screen = next
            taskDetail = null
        }

        fun goToday() {
            history = emptyList()
            taskDetail = null
            screen = DaylineScreen.TODAY
        }

        fun goBack() {
            if (history.isNotEmpty()) {
                screen = history.last()
                history = history.dropLast(1)
            } else if (screen != DaylineScreen.TODAY) {
                screen = DaylineScreen.TODAY
            }
        }

        fun saveAll(next: List<DaylineItem>) {
            items = next
            store.saveItems(next)
            NotificationScheduler.syncAll(appContext, next)

            if (nowActivityEnabled) {
                NowActivityScheduler.syncAll(appContext, next)
            } else {
                NowActivityScheduler.cancelAll(appContext, next)
            }

            widgetScope.launch { DaylineWidgetUpdater.updateAll(appContext) }
        }

        fun saveItem(item: DaylineItem) {
            val savedItem = if (
                calendarSyncEnabled &&
                AndroidCalendarSync.hasWritePermission(appContext) &&
                item.kind == AgendaKind.EVENT &&
                !item.calendarReadOnly
            ) {
                AndroidCalendarSync.upsert(appContext, item)
            } else {
                item
            }

            saveAll(
                if (items.any { it.id == savedItem.id }) {
                    items.map {
                        if (it.id == savedItem.id) savedItem else it
                    }
                } else {
                    items + savedItem
                }
            )

            if (taskDetail?.id == savedItem.id) {
                taskDetail = savedItem
            }

            if (calendarSyncEnabled) {
                refreshCalendarOverlay()
            }

            if (
                (
                    savedItem.reminderMinutes != null ||
                    (
                        nowActivityEnabled &&
                        savedItem.startTime != null &&
                        savedItem.endTime != null
                    )
                ) &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }

            editing = null
            addRequest = null
        }

        fun deleteItem(item: DaylineItem) {
            NotificationScheduler.cancel(appContext, item)
            NowActivityScheduler.cancel(appContext, item)

            if (
                calendarSyncEnabled &&
                !item.calendarReadOnly
            ) {
                AndroidCalendarSync.deleteMappedEvent(
                    appContext,
                    item
                )
            }
            saveAll(items.filterNot { it.id == item.id })
            editing = null
            taskDetail = null
        }

        fun toggleTask(item: DaylineItem, date: LocalDate) {
            saveItem(
                item.copy(
                    completedDates = item.completedDates.toMutableSet().apply {
                        if (date in this) remove(date) else add(date)
                    }
                )
            )
        }

        fun saveSpaces(next: List<DaylineSpace>) {
            spaces = next
            store.saveSpaces(next)
            widgetScope.launch { DaylineWidgetUpdater.updateAll(appContext) }
        }

        fun openItem(item: DaylineItem) {
            if (item.calendarReadOnly) return

            if (item.kind == AgendaKind.TASK) {
                taskDetail = item
            } else {
                editing = item
            }
        }

        val hasOverlay =
            menuOpen || addRequest != null || editing != null || newSpace || spaceEditing != null || taskDetail != null

        BackHandler(enabled = hasOverlay || screen != DaylineScreen.TODAY) {
            when {
                menuOpen -> menuOpen = false
                editing != null -> editing = null
                addRequest != null -> addRequest = null
                newSpace -> newSpace = false
                spaceEditing != null -> spaceEditing = null
                taskDetail != null -> taskDetail = null
                else -> goBack()
            }
        }

        val detail = taskDetail
        if (detail != null) {
            val live = items.firstOrNull { it.id == detail.id } ?: detail
            TaskDetailScreen(
                item = live,
                spaces = spaces,
                onBack = { taskDetail = null },
                onSave = ::saveItem,
                onDelete = ::deleteItem
            )
        } else {
            when (screen) {
                DaylineScreen.TODAY -> TodayScreen(
                    items = visibleItems,
                    showOrb = showOrb,
                    onMenu = { menuOpen = true },
                    onAdd = { addRequest = AddRequest(it, AgendaKind.EVENT) },
                    onEdit = ::openItem,
                    onToggleTask = ::toggleTask,
                    onReschedule = ::saveItem
                )

                DaylineScreen.CALENDAR -> CalendarScreen(
                    items = visibleItems,
                    weekStartsMonday = weekStartsMonday,
                    onMenu = { menuOpen = true },
                    onToday = ::goToday,
                    onAdd = { addRequest = AddRequest(it, AgendaKind.EVENT) },
                    onEdit = ::openItem,
                    onToggleTask = ::toggleTask
                )

                DaylineScreen.UPCOMING -> UpcomingScreen(
                    items = visibleItems,
                    spaces = spaces,
                    onMenu = { menuOpen = true },
                    onToday = ::goToday,
                    onAdd = { addRequest = AddRequest(it, AgendaKind.EVENT) },
                    onEdit = ::openItem,
                    onToggleTask = ::toggleTask
                )

                DaylineScreen.TASKS -> TasksScreen(
                    items = items,
                    spaces = spaces,
                    onMenu = { menuOpen = true },
                    onToday = ::goToday,
                    onAdd = { addRequest = AddRequest(it, AgendaKind.TASK) },
                    onOpenTask = { taskDetail = it },
                    onToggleTask = ::toggleTask
                )

                DaylineScreen.SPACES -> SpacesScreen(
                    spaces = spaces,
                    items = items,
                    onMenu = { menuOpen = true },
                    onToday = ::goToday,
                    onAdd = { newSpace = true },
                    onSpace = { spaceEditing = it }
                )

                DaylineScreen.SETTINGS -> SettingsScreen(
                    appearance = appearance,
                    fontChoice = fontChoice,
                    widgetFontChoice = widgetFontChoice,
                    widgetEmojiChoice = widgetEmojiChoice,
                    widgetAutoSlide = widgetAutoSlide,
                    nowActivityEnabled = nowActivityEnabled,
                    calendarSyncEnabled = calendarSyncEnabled,
                    showOrb = showOrb,
                    weekStartsMonday = weekStartsMonday,
                    onAppearance = {
                        appearance = it
                        store.saveAppearance(it)
                    },
                    onFontChoice = {
                        fontChoice = it
                        store.saveFontChoice(it)
                    },
                    onWidgetFontChoice = {
                        widgetFontChoice = it
                        store.saveWidgetFontChoice(it)
                        widgetScope.launch { DaylineWidgetUpdater.updateAll(appContext) }
                    },
                    onWidgetEmojiChoice = {
                        widgetEmojiChoice = it
                        store.saveWidgetEmojiChoice(it)
                        widgetScope.launch { DaylineWidgetUpdater.updateAll(appContext) }
                    },
                    onWidgetAutoSlide = {
                        widgetAutoSlide = it
                        store.saveWidgetAutoSlide(it)
                        widgetScope.launch { DaylineWidgetUpdater.updateAll(appContext) }
                    },
                    onNowActivityEnabled = {
                        nowActivityEnabled = it
                        store.saveNowActivityEnabled(it)

                        if (
                            it &&
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }

                        if (it) {
                            NowActivityScheduler.syncAll(appContext, items)
                        } else {
                            NowActivityScheduler.cancelAll(appContext, items)
                        }
                    },
                    onCalendarSyncEnabled = { enabled ->
                        if (!enabled) {
                            calendarSyncEnabled = false
                            store.saveCalendarSyncEnabled(false)
                            calendarItems = emptyList()

                            widgetScope.launch {
                                DaylineWidgetUpdater.updateAll(
                                    appContext
                                )
                            }
                        } else if (
                            AndroidCalendarSync.hasPermissions(
                                appContext
                            )
                        ) {
                            calendarSyncEnabled = true
                            store.saveCalendarSyncEnabled(true)

                            val published =
                                AndroidCalendarSync.publishExisting(
                                    appContext,
                                    items
                                )

                            items = published
                            store.saveItems(published)
                            refreshCalendarOverlay()
                        } else {
                            calendarPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.READ_CALENDAR,
                                    Manifest.permission.WRITE_CALENDAR
                                )
                            )
                        }
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
                    onToday = ::goToday
                )
            }
        }

        if (menuOpen) {
            NavigationSheet(
                current = screen,
                onSelect = {
                    navigateTo(it)
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
                spaces = spaces,
                onSave = ::saveItem,
                onDelete = ::deleteItem,
                onDismiss = { editing = null }
            )
        } ?: addRequest?.let { req ->
            QuickAddSheet(
                initialDate = req.date,
                initialKind = req.kind,
                spaces = spaces,
                onSave = ::saveItem,
                onDismiss = { addRequest = null }
            )
        }

        if (newSpace) {
            SpaceEditSheet(
                onSave = {
                    saveSpaces(spaces + it)
                    newSpace = false
                },
                onDismiss = { newSpace = false }
            )
        }

        spaceEditing?.let { space ->
            SpaceEditSheet(
                editing = space,
                onSave = { updated ->
                    saveSpaces(spaces.map { if (it.id == updated.id) updated else it })
                    spaceEditing = null
                },
                onDelete = { doomed ->
                    saveSpaces(spaces.filterNot { it.id == doomed.id })
                    saveAll(
                        items.map {
                            if (it.spaceId == doomed.id) it.copy(spaceId = null) else it
                        }
                    )
                    spaceEditing = null
                },
                onDismiss = { spaceEditing = null }
            )
        }
    }
}
