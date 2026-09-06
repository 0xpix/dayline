package com.pix.dayline.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.pix.dayline.BuildConfig
import com.pix.dayline.data.*
import com.pix.dayline.model.*
import com.pix.dayline.notifications.NotificationScheduler
import com.pix.dayline.notifications.NowActivityScheduler
import com.pix.dayline.ui.calendar.CalendarScreen
import com.pix.dayline.ui.components.NavigationSheet
import com.pix.dayline.ui.components.QuickAddSheet
import com.pix.dayline.ui.onboarding.OnboardingScreen
import com.pix.dayline.ui.search.SearchScreen
import com.pix.dayline.ui.settings.SettingsScreen
import com.pix.dayline.ui.spaces.SpaceEditSheet
import com.pix.dayline.ui.spaces.SpacesScreen
import com.pix.dayline.ui.tasks.TaskDetailScreen
import com.pix.dayline.ui.tasks.TasksScreen
import com.pix.dayline.ui.theme.DaylineTheme
import com.pix.dayline.ui.today.TodayScreen
import com.pix.dayline.ui.upcoming.UpcomingScreen
import com.pix.dayline.widgets.DaylineWidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime

enum class DaylineScreen { TODAY, CALENDAR, UPCOMING, TASKS, SEARCH, SPACES, SETTINGS }
private data class AddRequest(
    val date: LocalDate,
    val kind: AgendaKind,
    val time: LocalTime? = null
)

@Composable
fun DaylineApp() {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val haptics = LocalHapticFeedback.current
    val store = remember(appContext) { DaylineStore(appContext) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var items by remember { mutableStateOf(store.loadItems()) }
    var spaces by remember { mutableStateOf(store.loadSpaces()) }
    var templates by remember { mutableStateOf(store.loadTemplates()) }
    var appearance by remember { mutableStateOf(store.loadAppearance()) }
    var fontChoice by remember { mutableStateOf(store.loadFontChoice()) }
    var widgetFontChoice by remember { mutableStateOf(store.loadWidgetFontChoice()) }
    var widgetEmojiChoice by remember { mutableStateOf(store.loadWidgetEmojiChoice()) }
    var widgetAutoSlide by remember { mutableStateOf(store.loadWidgetAutoSlide()) }
    var nowActivityEnabled by remember { mutableStateOf(store.loadNowActivityEnabled()) }
    var calendarSyncEnabled by remember { mutableStateOf(store.loadCalendarSyncEnabled()) }
    var calendarPreferences by remember { mutableStateOf(store.loadCalendarPreferences()) }
    var deviceCalendars by remember { mutableStateOf(emptyList<DeviceCalendar>()) }
    var calendarItems by remember { mutableStateOf(emptyList<DaylineItem>()) }
    var showOrb by remember { mutableStateOf(store.loadShowOrb()) }
    var weekStartsMonday by remember { mutableStateOf(store.loadWeekStartsMonday()) }
    var onboardingComplete by remember { mutableStateOf(store.loadOnboardingComplete()) }
    var autoBetaUpdates by remember { mutableStateOf(store.loadAutoBetaUpdates()) }
    var updateState by remember {
        val lastError = store.loadLastUpdateCheckError()
        val cachedRelease = store.loadAvailableBetaRelease()?.takeIf {
            BuildConfig.UPDATE_CHANNEL == "GitHub beta" &&
                DaylineVersion.compare(it.versionName, BuildConfig.VERSION_NAME) > 0
        }
        mutableStateOf(
            UpdateUiState(
                status = when {
                    cachedRelease != null -> UpdateStatus.AVAILABLE
                    lastError != null -> UpdateStatus.ERROR
                    else -> UpdateStatus.IDLE
                },
                release = cachedRelease,
                error = lastError,
                checkedAtMillis = store.loadLastUpdateCheckAt()
            )
        )
    }
    var lastCalendarSyncAt by remember { mutableStateOf(store.loadLastCalendarSyncAt()) }
    var calendarSyncError by remember { mutableStateOf(store.loadLastCalendarSyncError()) }

    fun updateWidgets() {
        scope.launch { DaylineWidgetUpdater.updateAll(appContext) }
    }

    fun refreshCalendarOverlay() {
        if (calendarSyncEnabled && AndroidCalendarSync.hasReadPermission(appContext)) {
            val probe = AndroidCalendarSync.probe(appContext)
            if (probe.isSuccess) {
                // A Dayline-created event can also be deleted from Google Calendar,
                // Outlook, etc. Reconcile mapped rows before loading the overlay so a
                // provider-side deletion cannot reappear as a local Dayline event.
                val reconciled = AndroidCalendarSync.reconcileDeletedMappedItems(
                    appContext,
                    items
                )

                if (reconciled.size != items.size) {
                    val removedIds = items.map { it.id }.toSet() - reconciled.map { it.id }.toSet()
                    items.filter { it.id in removedIds }.forEach {
                        NotificationScheduler.cancel(appContext, it)
                        NowActivityScheduler.cancel(appContext, it)
                    }
                    items = reconciled
                    store.saveItems(reconciled)
                }

                deviceCalendars = AndroidCalendarSync.listCalendars(appContext)
                calendarItems = AndroidCalendarSync.loadOccurrences(
                    appContext,
                    calendarPreferences
                )
                lastCalendarSyncAt = System.currentTimeMillis()
                calendarSyncError = null
                store.saveCalendarSyncHealth(lastCalendarSyncAt, null)
            } else {
                deviceCalendars = emptyList()
                calendarItems = emptyList()
                calendarSyncError = probe.exceptionOrNull()?.message
                    ?: "Calendar provider could not be reached."
                store.saveCalendarSyncHealth(lastCalendarSyncAt, calendarSyncError)
            }
        } else {
            deviceCalendars = emptyList()
            calendarItems = emptyList()
            if (calendarSyncEnabled) {
                calendarSyncError = "Calendar permission is needed."
                store.saveCalendarSyncHealth(lastCalendarSyncAt, calendarSyncError)
            } else {
                calendarSyncError = null
            }
        }

        updateWidgets()
    }

    fun checkForUpdates(announce: Boolean = false) {
        if (BuildConfig.UPDATE_CHANNEL != "GitHub beta") return
        if (updateState.status == UpdateStatus.CHECKING) return

        updateState = updateState.copy(status = UpdateStatus.CHECKING, error = null)
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                BetaUpdateChecker.check(BuildConfig.VERSION_NAME)
            }
            updateState = result
            result.checkedAtMillis?.let { store.saveUpdateCheckResult(it, result.error) }
            store.saveAvailableBetaRelease(
                if (result.status == UpdateStatus.AVAILABLE) result.release else null
            )

            if (announce) {
                when (result.status) {
                    UpdateStatus.AVAILABLE -> {
                        val release = result.release ?: return@launch
                        val action = snackbarHostState.showSnackbar(
                            "Dayline ${release.versionName} is available",
                            actionLabel = "DOWNLOAD"
                        )
                        if (action == SnackbarResult.ActionPerformed) {
                            val url = release.apkUrl ?: release.htmlUrl
                            if (url.isNotBlank()) {
                                appContext.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }
                        }
                    }
                    UpdateStatus.ERROR -> snackbarHostState.showSnackbar(
                        result.error ?: "Couldn't check for Dayline updates."
                    )
                    else -> Unit
                }
            }
        }
    }

    fun persistItems(next: List<DaylineItem>) {
        items = next
        store.saveItems(next)
        NotificationScheduler.syncAll(appContext, next)
        if (nowActivityEnabled) {
            NowActivityScheduler.syncAll(appContext, next)
        } else {
            NowActivityScheduler.cancelAll(appContext, next)
        }
        updateWidgets()
    }

    fun reloadState() {
        items = store.loadItems()
        spaces = store.loadSpaces()
        templates = store.loadTemplates()
        appearance = store.loadAppearance()
        fontChoice = store.loadFontChoice()
        widgetFontChoice = store.loadWidgetFontChoice()
        widgetEmojiChoice = store.loadWidgetEmojiChoice()
        widgetAutoSlide = store.loadWidgetAutoSlide()
        nowActivityEnabled = store.loadNowActivityEnabled()
        calendarSyncEnabled = store.loadCalendarSyncEnabled()
        calendarPreferences = store.loadCalendarPreferences()
        showOrb = store.loadShowOrb()
        weekStartsMonday = store.loadWeekStartsMonday()
        onboardingComplete = store.loadOnboardingComplete()
        autoBetaUpdates = store.loadAutoBetaUpdates()
        lastCalendarSyncAt = store.loadLastCalendarSyncAt()
        calendarSyncError = store.loadLastCalendarSyncError()
        val updateError = store.loadLastUpdateCheckError()
        val cachedRelease = store.loadAvailableBetaRelease()?.takeIf {
            BuildConfig.UPDATE_CHANNEL == "GitHub beta" &&
                DaylineVersion.compare(it.versionName, BuildConfig.VERSION_NAME) > 0
        }
        updateState = UpdateUiState(
            status = when {
                cachedRelease != null -> UpdateStatus.AVAILABLE
                updateError != null -> UpdateStatus.ERROR
                else -> UpdateStatus.IDLE
            },
            release = cachedRelease,
            error = updateError,
            checkedAtMillis = store.loadLastUpdateCheckAt()
        )
        refreshCalendarOverlay()
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.READ_CALENDAR] == true &&
            grants[Manifest.permission.WRITE_CALENDAR] == true

        calendarSyncEnabled = granted
        store.saveCalendarSyncEnabled(granted)

        if (granted) {
            deviceCalendars = AndroidCalendarSync.listCalendars(appContext)
            if (calendarPreferences.defaultCalendarId == null) {
                calendarPreferences = calendarPreferences.copy(
                    defaultCalendarId = deviceCalendars.firstOrNull { it.primary && it.writable }?.id
                        ?: deviceCalendars.firstOrNull { it.writable }?.id
                )
                store.saveCalendarPreferences(calendarPreferences)
            }
            val published = AndroidCalendarSync.publishExisting(
                context = appContext,
                items = items,
                preferences = calendarPreferences,
                spaces = spaces
            )
            items = published
            store.saveItems(published)
        }
        refreshCalendarOverlay()
    }

    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            appContext.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use {
                it.write(store.exportState())
            }
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val raw = runCatching {
            appContext.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        }.getOrNull() ?: return@rememberLauncherForActivityResult
        if (store.importState(raw)) reloadState()
    }

    val exportIcsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/calendar")
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            appContext.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use {
                it.write(DaylineTransfer.exportIcs(items))
            }
        }
    }

    val importIcsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val raw = runCatching {
            appContext.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        }.getOrNull() ?: return@rememberLauncherForActivityResult
        val imported = DaylineTransfer.importIcs(raw)
        if (imported.isNotEmpty()) persistItems(items + imported)
    }

    LaunchedEffect(Unit) {
        NotificationScheduler.syncAll(appContext, items)
        if (nowActivityEnabled) NowActivityScheduler.syncAll(appContext, items)
        refreshCalendarOverlay()
        BetaUpdateScheduler.sync(appContext)
        val lastCheck = store.loadLastUpdateCheckAt() ?: 0L
        val stale = System.currentTimeMillis() - lastCheck >= 24L * 60L * 60L * 1000L
        if (BuildConfig.UPDATE_CHANNEL == "GitHub beta" && autoBetaUpdates && stale) {
            checkForUpdates(announce = true)
        }
    }

    DisposableEffect(calendarSyncEnabled, calendarPreferences) {
        if (calendarSyncEnabled && AndroidCalendarSync.hasReadPermission(appContext)) {
            val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    refreshCalendarOverlay()
                }
            }
            appContext.contentResolver.registerContentObserver(
                CalendarContract.Events.CONTENT_URI,
                true,
                observer
            )
            onDispose { appContext.contentResolver.unregisterContentObserver(observer) }
        } else {
            onDispose { }
        }
    }

    val visibleItems = remember(items, calendarItems, calendarSyncEnabled) {
        if (!calendarSyncEnabled) {
            items
        } else {
            val mappedIds = items.mapNotNull { it.calendarEventId }.toSet()
            items + calendarItems.filterNot { it.calendarEventId in mappedIds }
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
        if (!onboardingComplete) {
            OnboardingScreen(
                calendars = deviceCalendars,
                calendarPreferences = calendarPreferences,
                appearance = appearance,
                onRequestCalendar = {
                    calendarPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.READ_CALENDAR,
                            Manifest.permission.WRITE_CALENDAR
                        )
                    )
                },
                onCalendarPreferences = {
                    calendarPreferences = it
                    store.saveCalendarPreferences(it)
                    refreshCalendarOverlay()
                },
                onAppearance = {
                    appearance = it
                    store.saveAppearance(it)
                },
                onDone = {
                    onboardingComplete = true
                    store.saveOnboardingComplete(true)
                }
            )
            return@DaylineTheme
        }

        var screen by remember { mutableStateOf(DaylineScreen.TODAY) }
        var history by remember { mutableStateOf(emptyList<DaylineScreen>()) }
        var menuOpen by remember { mutableStateOf(false) }
        var addRequest by remember { mutableStateOf<AddRequest?>(null) }
        var editing by remember { mutableStateOf<DaylineItem?>(null) }
        var editingDate by remember { mutableStateOf<LocalDate?>(null) }
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

        fun requestNotificationIfNeeded(item: DaylineItem) {
            if (
                (item.reminderMinutes != null ||
                    (nowActivityEnabled && item.startTime != null && item.endTime != null)) &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        fun publishIfNeeded(item: DaylineItem): DaylineItem {
            if (!calendarSyncEnabled || !AndroidCalendarSync.hasWritePermission(appContext)) return item
            if (item.kind != AgendaKind.EVENT || item.calendarReadOnly) return item
            return AndroidCalendarSync.upsert(
                context = appContext,
                item = item,
                preferences = calendarPreferences,
                spaces = spaces
            )
        }

        fun saveItem(
            item: DaylineItem,
            scopeValue: RecurrenceEditScope = RecurrenceEditScope.ENTIRE_SERIES
        ) {
            // Editable provider-only events are changed in place and remain external.
            if (item.id.startsWith("android:") && item.calendarEventId != null) {
                AndroidCalendarSync.upsert(
                    appContext,
                    item.copy(calendarReadOnly = false),
                    calendarPreferences,
                    spaces
                )
                refreshCalendarOverlay()
                editing = null
                editingDate = null
                return
            }

            val original = items.firstOrNull { it.id == item.id }
            val occurrenceDate = editingDate ?: item.startDate
            var next = if (original != null && original.recurrence != Recurrence.ONCE) {
                SeriesEditor.apply(
                    existingItems = items,
                    original = original,
                    edited = item,
                    occurrenceDate = occurrenceDate,
                    scope = scopeValue
                )
            } else if (items.any { it.id == item.id }) {
                items.map { if (it.id == item.id) item else it }
            } else {
                items + item
            }

            if (calendarSyncEnabled && AndroidCalendarSync.hasWritePermission(appContext)) {
                next = next.map { candidate ->
                    if (
                        candidate.kind == AgendaKind.EVENT &&
                        !candidate.calendarReadOnly &&
                        (candidate.id == item.id || candidate.seriesParentId == item.id)
                    ) {
                        publishIfNeeded(candidate)
                    } else candidate
                }
            }

            persistItems(next)
            requestNotificationIfNeeded(item)
            if (calendarSyncEnabled) refreshCalendarOverlay()
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            editing = null
            editingDate = null
            addRequest = null
        }

        fun changeWithUndo(updated: DaylineItem, message: String) {
            val previous = items.firstOrNull { it.id == updated.id }
            if (previous == null || updated.calendarReadOnly) return
            val saved = publishIfNeeded(updated)
            persistItems(items.map { if (it.id == updated.id) saved else it })
            if (calendarSyncEnabled) refreshCalendarOverlay()
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = message,
                    actionLabel = "UNDO",
                    duration = SnackbarDuration.Short
                )
                if (result == SnackbarResult.ActionPerformed) {
                    val restored = publishIfNeeded(previous)
                    persistItems(items.map { if (it.id == previous.id) restored else it })
                    if (calendarSyncEnabled) refreshCalendarOverlay()
                }
            }
        }

        fun deleteItem(item: DaylineItem) {
            val externalOnly = item.id.startsWith("android:")
            if (externalOnly) {
                if (!item.calendarReadOnly) {
                    AndroidCalendarSync.deleteMappedEvent(appContext, item.copy(calendarReadOnly = false))
                    refreshCalendarOverlay()
                }
                editing = null
                return
            }

            val snapshot = item
            NotificationScheduler.cancel(appContext, item)
            NowActivityScheduler.cancel(appContext, item)
            persistItems(items.filterNot { it.id == item.id })
            editing = null
            taskDetail = null

            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = "Deleted ${item.title}",
                    actionLabel = "UNDO",
                    duration = SnackbarDuration.Short
                )
                if (result == SnackbarResult.ActionPerformed) {
                    persistItems(items + snapshot)
                } else if (calendarSyncEnabled) {
                    AndroidCalendarSync.deleteMappedEvent(appContext, snapshot)
                    refreshCalendarOverlay()
                }
            }
        }

        fun toggleTask(item: DaylineItem, date: LocalDate) {
            if (item.calendarReadOnly) return
            val updated = item.copy(
                completedDates = if (date in item.completedDates) {
                    item.completedDates - date
                } else {
                    item.completedDates + date
                }
            )
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            persistItems(items.map { if (it.id == item.id) updated else it })
            if (taskDetail?.id == item.id) taskDetail = updated
        }

        fun saveSpaces(next: List<DaylineSpace>) {
            spaces = next
            store.saveSpaces(next)
            updateWidgets()
        }

        fun saveTemplate(template: EventTemplate) {
            templates = (templates + template).distinctBy { it.id }
            store.saveTemplates(templates)
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }

        fun openItem(item: DaylineItem, occurrenceDate: LocalDate = item.startDate) {
            if (item.calendarReadOnly) return
            editingDate = occurrenceDate
            if (item.kind == AgendaKind.TASK && !item.id.startsWith("android:")) {
                taskDetail = item
            } else {
                editing = item
            }
        }

        val hasOverlay = menuOpen || addRequest != null || editing != null ||
            newSpace || spaceEditing != null || taskDetail != null

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

        Box(Modifier.fillMaxSize()) {
            val detail = taskDetail
            if (detail != null) {
                val live = items.firstOrNull { it.id == detail.id } ?: detail
                TaskDetailScreen(
                    item = live,
                    spaces = spaces,
                    onBack = { taskDetail = null },
                    onSave = { updated ->
                        if (live.kind == AgendaKind.TASK && updated.kind == AgendaKind.EVENT) {
                            changeWithUndo(updated, "Converted ${updated.title} to event")
                            taskDetail = null
                        } else {
                            saveItem(updated)
                        }
                    },
                    onDelete = ::deleteItem
                )
            } else {
                when (screen) {
                    DaylineScreen.TODAY -> TodayScreen(
                        items = visibleItems,
                        showOrb = showOrb,
                        onMenu = { menuOpen = true },
                        onAdd = { addRequest = AddRequest(it, AgendaKind.EVENT) },
                        onAddAt = { date, time -> addRequest = AddRequest(date, AgendaKind.EVENT, time) },
                        onEdit = { openItem(it, LocalDate.now()) },
                        onToggleTask = ::toggleTask,
                        onReschedule = { changeWithUndo(it, "Moved ${it.title} to ${it.startTime}") },
                        onResize = { changeWithUndo(it, "Resized ${it.title} to ${it.endTime}") },
                        onScheduleTask = { changeWithUndo(it, "Scheduled ${it.title} at ${it.startTime}") }
                    )

                    DaylineScreen.CALENDAR -> CalendarScreen(
                        items = visibleItems,
                        weekStartsMonday = weekStartsMonday,
                        onMenu = { menuOpen = true },
                        onToday = ::goToday,
                        onAdd = { addRequest = AddRequest(it, AgendaKind.EVENT) },
                        onEdit = { item, date -> openItem(item, date) },
                        onToggleTask = ::toggleTask
                    )

                    DaylineScreen.UPCOMING -> UpcomingScreen(
                        items = visibleItems,
                        spaces = spaces,
                        onMenu = { menuOpen = true },
                        onToday = ::goToday,
                        onAdd = { addRequest = AddRequest(it, AgendaKind.EVENT) },
                        onEdit = { item, date -> openItem(item, date) },
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

                    DaylineScreen.SEARCH -> SearchScreen(
                        items = visibleItems,
                        spaces = spaces,
                        onMenu = { menuOpen = true },
                        onToday = ::goToday,
                        onOpen = { item, date -> openItem(item, date) }
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
                        nowActivityEnabled = nowActivityEnabled,
                        calendarSyncEnabled = calendarSyncEnabled,
                        calendarPreferences = calendarPreferences,
                        deviceCalendars = deviceCalendars,
                        spaces = spaces,
                        lastCalendarSyncAt = lastCalendarSyncAt,
                        calendarSyncError = calendarSyncError,
                        autoBetaUpdates = autoBetaUpdates,
                        updateState = updateState,
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
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            if (it) NowActivityScheduler.syncAll(appContext, items)
                            else NowActivityScheduler.cancelAll(appContext, items)
                        },
                        onCalendarSyncEnabled = { enabled ->
                            if (!enabled) {
                                calendarSyncEnabled = false
                                store.saveCalendarSyncEnabled(false)
                                calendarItems = emptyList()
                                updateWidgets()
                            } else if (AndroidCalendarSync.hasPermissions(appContext)) {
                                calendarSyncEnabled = true
                                store.saveCalendarSyncEnabled(true)
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
                        onCalendarPreferences = {
                            calendarPreferences = it
                            store.saveCalendarPreferences(it)
                            refreshCalendarOverlay()
                        },
                        onAutoBetaUpdates = {
                            autoBetaUpdates = it
                            store.saveAutoBetaUpdates(it)
                            BetaUpdateScheduler.sync(appContext)
                            if (it) checkForUpdates(announce = false)
                        },
                        onCheckUpdates = { checkForUpdates(announce = false) },
                        onBackup = { backupLauncher.launch("dayline-backup.json") },
                        onRestore = { restoreLauncher.launch(arrayOf("application/json", "text/plain")) },
                        onExportIcs = { exportIcsLauncher.launch("dayline-calendar.ics") },
                        onImportIcs = { importIcsLauncher.launch(arrayOf("text/calendar", "text/plain", "text/*")) },
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

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 92.dp)
            )
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
                initialDate = editingDate ?: item.startDate,
                initialKind = item.kind,
                editing = item,
                spaces = spaces,
                templates = templates,
                onSave = ::saveItem,
                onSaveTemplate = ::saveTemplate,
                onDelete = ::deleteItem,
                onDismiss = {
                    editing = null
                    editingDate = null
                }
            )
        } ?: addRequest?.let { request ->
            QuickAddSheet(
                initialDate = request.date,
                initialKind = request.kind,
                initialTime = request.time,
                spaces = spaces,
                templates = templates,
                onSave = ::saveItem,
                onSaveTemplate = ::saveTemplate,
                onDismiss = { addRequest = null }
            )
        }

        if (newSpace) {
            SpaceEditSheet(
                calendars = deviceCalendars,
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
                calendars = deviceCalendars,
                onSave = { updated ->
                    saveSpaces(spaces.map { if (it.id == updated.id) updated else it })
                    spaceEditing = null
                },
                onDelete = { doomed ->
                    saveSpaces(spaces.filterNot { it.id == doomed.id })
                    persistItems(
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
