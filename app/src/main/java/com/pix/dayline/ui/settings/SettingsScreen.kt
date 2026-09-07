package com.pix.dayline.ui.settings

import android.app.AlarmManager
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pix.dayline.BuildConfig
import com.pix.dayline.data.*
import com.pix.dayline.glyph.GlyphVisualPreferencesStore
import com.pix.dayline.model.*
import com.pix.dayline.ui.components.FloatingControls
import com.pix.dayline.ui.glyph.GlyphMatrixPreview
import com.pix.dayline.updates.GithubBetaUpdater
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private enum class SettingsSheet {
    APPEARANCE,
    APP_FONT,
    CALENDARS,
    GLYPH,
    UPDATE,
    PRIVACY
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    appearance: Appearance,
    fontChoice: FontChoice,
    widgetFontChoice: WidgetFontChoice,
    widgetEmojiChoice: WidgetEmojiChoice,
    widgetAutoSlide: Boolean,
    glyphPreferences: GlyphPreferences,
    glyphHardwareStatus: GlyphHardwareStatus,
    nowActivityEnabled: Boolean,
    calendarSyncEnabled: Boolean,
    calendarPreferences: CalendarPreferences,
    deviceCalendars: List<DeviceCalendar>,
    spaces: List<DaylineSpace>,
    lastCalendarSyncAt: Long?,
    calendarSyncError: String?,
    autoBetaUpdates: Boolean,
    updateState: UpdateUiState,
    showOrb: Boolean,
    weekStartsMonday: Boolean,
    onAppearance: (Appearance) -> Unit,
    onFontChoice: (FontChoice) -> Unit,
    onWidgetFontChoice: (WidgetFontChoice) -> Unit,
    onWidgetEmojiChoice: (WidgetEmojiChoice) -> Unit,
    onWidgetAutoSlide: (Boolean) -> Unit,
    onGlyphPreferences: (GlyphPreferences) -> Unit,
    onGlyphTest: (DaylineGlyphSignal) -> Unit,
    onOpenGlyphManager: () -> Unit,
    onNowActivityEnabled: (Boolean) -> Unit,
    onCalendarSyncEnabled: (Boolean) -> Unit,
    onCalendarPreferences: (CalendarPreferences) -> Unit,
    onAutoBetaUpdates: (Boolean) -> Unit,
    onCheckUpdates: () -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    onExportIcs: () -> Unit,
    onImportIcs: () -> Unit,
    onShowOrb: (Boolean) -> Unit,
    onWeekStart: (Boolean) -> Unit,
    onMenu: () -> Unit,
    onToday: () -> Unit
) {
    val context = LocalContext.current
    var openSheet by remember { mutableStateOf<SettingsSheet?>(null) }

    @Suppress("UNUSED_VARIABLE")
    val widgetSettingsOwnedByWidget = listOf(
        widgetFontChoice,
        widgetEmojiChoice,
        widgetAutoSlide,
        onWidgetFontChoice,
        onWidgetEmojiChoice,
        onWidgetAutoSlide
    )

    LaunchedEffect(updateState.status, updateState.release?.tagName) {
        if (updateState.status == UpdateStatus.AVAILABLE && updateState.release != null) {
            openSheet = SettingsSheet.UPDATE
        }
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
                .padding(start = 30.dp, end = 30.dp, top = 52.dp, bottom = 138.dp)
        ) {
            Text("Settings", style = MaterialTheme.typography.displayLarge)
            Spacer(Modifier.height(40.dp))

            SettingsGroup("General") {
                SelectorRow("Appearance", appearanceLabel(appearance)) {
                    openSheet = SettingsSheet.APPEARANCE
                }
                SelectorRow("App font", fontLabel(fontChoice)) {
                    openSheet = SettingsSheet.APP_FONT
                }
            }

            SectionGap()
            SettingsGroup("Glyph") {
                SelectorRow("Dayline Glyph", glyphModeLabel(glyphPreferences.mode)) {
                    openSheet = SettingsSheet.GLYPH
                }
            }

            SectionGap()
            SettingsGroup("Today") {
                ToggleSettingRow("24-hour horizon", showOrb, onShowOrb)
            }

            SectionGap()
            SettingsGroup("Focus & reminders") {
                ToggleSettingRow("Now activity", nowActivityEnabled, onNowActivityEnabled)
                SelectorRow("Notifications", "Open") {
                    context.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    )
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    SelectorRow("Precise timing", preciseStatus(context)) {
                        runCatching {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                    Uri.parse("package:${context.packageName}")
                                )
                            )
                        }
                    }
                }
            }

            SectionGap()
            SettingsGroup("Calendar") {
                ToggleSettingRow("Android Calendar sync", calendarSyncEnabled, onCalendarSyncEnabled)
                if (calendarSyncEnabled) {
                    InfoRow(
                        "Sync",
                        calendarHealthLabel(calendarSyncEnabled, lastCalendarSyncAt, calendarSyncError)
                    )
                }
                if (!calendarSyncError.isNullOrBlank()) {
                    Text(
                        calendarSyncError,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.height(6.dp))
                }
                SelectorRow(
                    "Calendars",
                    if (deviceCalendars.isEmpty()) "None" else "${deviceCalendars.size} found"
                ) { openSheet = SettingsSheet.CALENDARS }
                ToggleSettingRow("Week starts Monday", weekStartsMonday, onWeekStart)
            }

            SectionGap()
            SettingsGroup("Data") {
                SelectorRow("Backup", "JSON") { onBackup() }
                SelectorRow("Restore", "JSON") { onRestore() }
                SelectorRow("Export calendar", ".ics") { onExportIcs() }
                SelectorRow("Import calendar", ".ics") { onImportIcs() }
            }

            if (BuildConfig.UPDATE_CHANNEL == "GitHub beta") {
                SectionGap()
                SettingsGroup("Beta updates") {
                    ToggleSettingRow("Automatic daily check", autoBetaUpdates, onAutoBetaUpdates)
                    SelectorRow(
                        if (updateState.status == UpdateStatus.AVAILABLE) "Update available" else "Check for updates",
                        updateActionLabel(updateState)
                    ) {
                        if (updateState.status == UpdateStatus.AVAILABLE && updateState.release != null) {
                            openSheet = SettingsSheet.UPDATE
                        } else {
                            onCheckUpdates()
                        }
                    }
                    updateState.checkedAtMillis?.let {
                        Text(
                            "Last checked ${relativeCheckTime(it)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (updateState.status == UpdateStatus.ERROR && !updateState.error.isNullOrBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            updateState.error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            SectionGap()
            SettingsGroup("About") {
                SelectorRow("Privacy", "On-device") { openSheet = SettingsSheet.PRIVACY }
                SelectorRow("Source code", "GitHub") {
                    context.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://github.com/0xpix/dayline")
                        )
                    )
                }
                InfoRow("Version", BuildConfig.VERSION_NAME)
                InfoRow("Build", "${BuildConfig.VERSION_CODE} · ${BuildConfig.GIT_COMMIT}")
            }

            Spacer(Modifier.height(38.dp))
        }

        FloatingControls(
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 28.dp),
            showAdd = false,
            onMenu = onMenu,
            onToday = onToday
        )
    }

    when (openSheet) {
        SettingsSheet.APPEARANCE -> SelectionSheet(
            "Appearance",
            appearanceLabel(appearance),
            listOf(
                "System" to { onAppearance(Appearance.SYSTEM) },
                "Light" to { onAppearance(Appearance.LIGHT) },
                "OLED dark" to { onAppearance(Appearance.DARK) }
            )
        ) { openSheet = null }

        SettingsSheet.APP_FONT -> SelectionSheet(
            "App font",
            fontLabel(fontChoice),
            listOf(
                "Pixelify Sans" to { onFontChoice(FontChoice.PIXELIFY) },
                "Geist · Nothing OS 5" to { onFontChoice(FontChoice.GEIST) },
                "Geist Pixel" to { onFontChoice(FontChoice.GEIST_PIXEL) },
                "System" to { onFontChoice(FontChoice.SYSTEM) }
            )
        ) { openSheet = null }

        SettingsSheet.GLYPH -> GlyphSettingsSheet(
            preferences = glyphPreferences,
            hardware = glyphHardwareStatus,
            onChange = onGlyphPreferences,
            onTest = onGlyphTest,
            onOpenManager = onOpenGlyphManager,
            onDismiss = { openSheet = null }
        )

        SettingsSheet.CALENDARS -> CalendarControlsSheet(
            calendars = deviceCalendars,
            preferences = calendarPreferences,
            spaces = spaces,
            onChange = onCalendarPreferences,
            onDismiss = { openSheet = null }
        )

        SettingsSheet.UPDATE -> {
            updateState.release?.let { release ->
                UpdateSheet(release = release, onDismiss = { openSheet = null })
            }
        }

        SettingsSheet.PRIVACY -> PrivacySheet { openSheet = null }
        null -> Unit
    }
}

@Composable
private fun SettingsGroup(label: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(9.dp))
        content()
    }
}

@Composable
private fun SettingsSubhead(label: String) {
    Text(
        label,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
private fun SectionGap() = Spacer(Modifier.height(30.dp))

@Composable
private fun SelectorRow(title: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(8.dp))
        Text("›", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun InfoRow(title: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ToggleSettingRow(title: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionSheet(
    title: String,
    selected: String,
    options: List<Pair<String, () -> Unit>>,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 28.dp).padding(bottom = 32.dp)) {
            Text(title, style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(24.dp))
            options.forEach { (label, action) ->
                Row(
                    Modifier.fillMaxWidth().clickable {
                        action()
                        onDismiss()
                    }.padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Text(if (label == selected) "●" else "○", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GlyphSettingsSheet(
    preferences: GlyphPreferences,
    hardware: GlyphHardwareStatus,
    onChange: (GlyphPreferences) -> Unit,
    onTest: (DaylineGlyphSignal) -> Unit,
    onOpenManager: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val visualStore = remember(context) { GlyphVisualPreferencesStore(context.applicationContext) }
    var preview by remember { mutableStateOf(DaylineGlyphSignal.CENTER) }
    var brightness by remember { mutableIntStateOf(visualStore.loadBrightness()) }

    fun update(next: GlyphPreferences) {
        onChange(
            next.copy(
                mode = if (next.mode == GlyphMode.OFF) GlyphMode.OFF else GlyphMode.EYES_ONLY,
                showAppStates = false,
                restAnimation = false
            )
        )
    }

    fun test(signal: DaylineGlyphSignal) {
        preview = signal
        onTest(signal)
    }

    fun changeBrightness(delta: Int) {
        brightness = (brightness + delta).coerceIn(
            GlyphVisualPreferencesStore.MIN_BRIGHTNESS,
            GlyphVisualPreferencesStore.MAX_BRIGHTNESS
        )
        visualStore.saveBrightness(brightness)
    }

    LaunchedEffect(preferences.mode, preferences.showAppStates, preferences.restAnimation) {
        if (
            preferences.mode == GlyphMode.EYES_AND_STATES ||
            preferences.showAppStates ||
            preferences.restAnimation
        ) {
            update(preferences)
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 26.dp)
                .padding(bottom = 38.dp)
        ) {
            Text("Dayline Glyph", style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(7.dp))
            Text(
                "Eyes first. Focus time appears only at checkpoints.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(22.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                GlyphMatrixPreview(
                    signal = preview,
                    modifier = Modifier.size(156.dp)
                )
            }
            Spacer(Modifier.height(26.dp))

            SettingsSubhead("Glyph")
            Spacer(Modifier.height(6.dp))
            ToggleSettingRow("Enabled", preferences.enabled) {
                update(preferences.copy(mode = if (it) GlyphMode.EYES_ONLY else GlyphMode.OFF))
            }
            InfoRow(
                "Hardware",
                if (hardware.available) hardware.deviceLabel else "Unavailable"
            )
            if (!hardware.available && !hardware.detail.isNullOrBlank()) {
                Text(
                    hardware.detail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
            }
            Text(
                "OPEN NOTHING SETTINGS  ›",
                modifier = Modifier.clickable(onClick = onOpenManager).padding(vertical = 10.dp),
                style = MaterialTheme.typography.labelLarge
            )

            SectionGap()
            SettingsSubhead("Look")
            Spacer(Modifier.height(6.dp))
            Row(
                Modifier.fillMaxWidth().padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Brightness", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                TinyAction("−") { changeBrightness(-32) }
                Spacer(Modifier.width(12.dp))
                Text(
                    "${((brightness / GlyphVisualPreferencesStore.MAX_BRIGHTNESS.toFloat()) * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(12.dp))
                TinyAction("+") { changeBrightness(32) }
            }
            ToggleSettingRow("Blink", preferences.blinkEnabled) {
                update(preferences.copy(blinkEnabled = it))
            }
            ToggleSettingRow("Expressions", preferences.randomGlancesEnabled) {
                update(preferences.copy(randomGlancesEnabled = it))
            }
            if (preferences.randomGlancesEnabled) {
                SelectorRow(
                    "Motion",
                    preferences.glanceFrequency.name.lowercase().replaceFirstChar { it.titlecase() }
                ) {
                    val entries = GlyphGlanceFrequency.entries
                    val next = entries[(entries.indexOf(preferences.glanceFrequency) + 1) % entries.size]
                    update(preferences.copy(glanceFrequency = next))
                }
            }
            ToggleSettingRow("Reduce motion", preferences.reduceMotion) {
                update(preferences.copy(reduceMotion = it))
            }

            SectionGap()
            SettingsSubhead("Focus")
            Spacer(Modifier.height(6.dp))
            InfoRow("Time check-ins", "30 sec")
            Text(
                "Start · every 5 min · 1 min left",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            SectionGap()
            SettingsSubhead("Night")
            Spacer(Modifier.height(6.dp))
            ToggleSettingRow("Quiet hours", preferences.quietHoursEnabled) {
                update(preferences.copy(quietHoursEnabled = it))
            }
            if (preferences.quietHoursEnabled) {
                SelectorRow("Starts", preferences.quietStart.toString()) {
                    TimePickerDialog(
                        context,
                        { _, hour, minute -> update(preferences.copy(quietStart = java.time.LocalTime.of(hour, minute))) },
                        preferences.quietStart.hour,
                        preferences.quietStart.minute,
                        true
                    ).show()
                }
                SelectorRow("Ends", preferences.quietEnd.toString()) {
                    TimePickerDialog(
                        context,
                        { _, hour, minute -> update(preferences.copy(quietEnd = java.time.LocalTime.of(hour, minute))) },
                        preferences.quietEnd.hour,
                        preferences.quietEnd.minute,
                        true
                    ).show()
                }
                ToggleSettingRow("Dim at night", preferences.dimAtNight) {
                    update(preferences.copy(dimAtNight = it))
                }
            }

            SectionGap()
            SettingsSubhead("Test expressions")
            Spacer(Modifier.height(10.dp))
            val tests = listOf(
                "CENTER" to DaylineGlyphSignal.CENTER,
                "LEFT" to DaylineGlyphSignal.LOOK_LEFT,
                "RIGHT" to DaylineGlyphSignal.LOOK_RIGHT,
                "BLINK" to DaylineGlyphSignal.BLINK,
                "HAPPY" to DaylineGlyphSignal.HAPPY,
                "WINK" to DaylineGlyphSignal.WINK,
                "HEARTS" to DaylineGlyphSignal.HEARTS,
                "SQUINT" to DaylineGlyphSignal.SQUINT,
                "SLEEPY" to DaylineGlyphSignal.SLEEPY
            )
            tests.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { (label, signal) ->
                        Text(
                            label,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { test(signal) }
                                .padding(vertical = 10.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarControlsSheet(
    calendars: List<DeviceCalendar>,
    preferences: CalendarPreferences,
    spaces: List<DaylineSpace>,
    onChange: (CalendarPreferences) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 26.dp)
                .padding(bottom = 34.dp)
        ) {
            Text("Calendars", style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(8.dp))
            Text(
                "Choose what Dayline shows and where new events go.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(22.dp))

            if (calendars.isEmpty()) {
                Text("No Android calendars available yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            calendars.forEach { calendar ->
                val rule = preferences.ruleFor(calendar.id) ?: CalendarRule(calendar.id)
                Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(calendar.name, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                buildString {
                                    append(calendar.accountName)
                                    append(" · ")
                                    append(if (rule.visible) "SYNCED" else "HIDDEN")
                                    if (!calendar.writable) append(" · READ ONLY")
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            if (rule.visible) "●" else "○",
                            modifier = Modifier.clickable {
                                onChange(preferences.withRule(rule.copy(visible = !rule.visible)))
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    if (calendar.writable) {
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            TinyAction(
                                if (preferences.defaultCalendarId == calendar.id) "DEFAULT" else "MAKE DEFAULT"
                            ) {
                                onChange(preferences.copy(defaultCalendarId = calendar.id))
                            }
                            TinyAction(if (rule.editable) "EDITABLE" else "READ ONLY") {
                                onChange(preferences.withRule(rule.copy(editable = !rule.editable)))
                            }
                        }
                        Spacer(Modifier.height(5.dp))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        TinyAction("SPACE · ${spaceName(rule.spaceId, spaces)}") {
                            onChange(preferences.withRule(rule.copy(spaceId = nextSpace(rule.spaceId, spaces))))
                        }
                        TinyAction("COLOR · ${rule.color.name}") {
                            val entries = ItemColor.entries
                            val next = entries[(entries.indexOf(rule.color) + 1) % entries.size]
                            onChange(preferences.withRule(rule.copy(color = next)))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TinyAction(label: String, onClick: () -> Unit) {
    Text(
        label,
        modifier = Modifier.clickable(onClick = onClick).padding(vertical = 4.dp),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

private data class UpdateNoteSection(
    val title: String,
    val items: List<String>
)

private fun cleanUpdateNoteLine(line: String): String = line
    .trim()
    .trimStart('•', '-', '*', ' ')
    .replace("**", "")
    .replace("`", "")
    .trim()

private fun parseUpdateNotes(raw: String): List<UpdateNoteSection> {
    val sections = mutableListOf<UpdateNoteSection>()
    var title: String? = null
    var items = mutableListOf<String>()

    fun flush() {
        val currentTitle = title ?: return
        if (items.isNotEmpty()) {
            sections += UpdateNoteSection(currentTitle, items.toList())
        }
        items = mutableListOf()
    }

    raw.lines().forEach { rawLine ->
        val line = rawLine.trim()
        when {
            line.startsWith("## ") -> {
                flush()
                title = line.removePrefix("## ").trim()
            }
            line.isBlank() -> Unit
            title != null -> {
                cleanUpdateNoteLine(line).takeIf { it.isNotBlank() }?.let(items::add)
            }
        }
    }
    flush()

    val ordered = listOf("Added", "Changed", "Fixed").mapNotNull { expected ->
        sections.firstOrNull { it.title.equals(expected, ignoreCase = true) }
    }
    if (ordered.isNotEmpty()) return ordered

    val fallback = raw.lines()
        .map(::cleanUpdateNoteLine)
        .filter { it.isNotBlank() && !it.startsWith("#") }
    return listOf(
        UpdateNoteSection(
            title = "Changed",
            items = fallback.ifEmpty { listOf("Bug fixes and Dayline polish.") }
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UpdateSheet(release: BetaRelease, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sections = remember(release.notes) { parseUpdateNotes(release.notes) }
    var downloading by remember(release.tagName) { mutableStateOf(false) }
    var downloadedApk by remember(release.tagName) { mutableStateOf<File?>(null) }
    var message by remember(release.tagName) { mutableStateOf<String?>(null) }

    fun installVerified(apk: File) {
        when (val result = GithubBetaUpdater.install(context, apk)) {
            GithubBetaUpdater.InstallResult.Started ->
                message = "Verified · Android installer opened"
            GithubBetaUpdater.InstallResult.PermissionRequested ->
                message = "Allow Dayline β to install apps, then tap Continue update."
            is GithubBetaUpdater.InstallResult.Error ->
                message = result.message
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp)
                .padding(bottom = 32.dp)
        ) {
            Text("Update available", style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(8.dp))
            Text(
                release.versionName,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (release.title.isNotBlank() && !release.title.contains(release.versionName, ignoreCase = true)) {
                Spacer(Modifier.height(4.dp))
                Text(
                    release.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(26.dp))
            Text("What's new", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(14.dp))

            sections.forEachIndexed { sectionIndex, section ->
                Text(
                    section.title.uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                section.items.forEach { item ->
                    Row(
                        Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            "•",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            item,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
                if (sectionIndex != sections.lastIndex) Spacer(Modifier.height(10.dp))
            }

            Spacer(Modifier.height(22.dp))

            val actionLabel = when {
                release.apkUrl.isNullOrBlank() -> "View release"
                downloading -> "Downloading…"
                downloadedApk != null -> "Continue update"
                else -> "Download & update"
            }
            androidx.compose.material3.Button(
                onClick = {
                    if (release.apkUrl.isNullOrBlank()) {
                        GithubBetaUpdater.openRelease(context, release)
                    } else {
                        val readyApk = downloadedApk
                        if (readyApk != null) {
                            installVerified(readyApk)
                        } else {
                            downloading = true
                            message = null
                            scope.launch {
                                GithubBetaUpdater.download(context.applicationContext, release)
                                    .onSuccess { apk ->
                                        downloadedApk = apk
                                        installVerified(apk)
                                    }
                                    .onFailure {
                                        message = it.message ?: "Download failed"
                                    }
                                downloading = false
                            }
                        }
                    }
                },
                enabled = !downloading,
                modifier = Modifier.fillMaxWidth().height(54.dp)
            ) {
                Text(actionLabel, style = MaterialTheme.typography.titleMedium)
            }

            message?.let {
                Spacer(Modifier.height(10.dp))
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (it.contains("failed", ignoreCase = true) || it.contains("error", ignoreCase = true)) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            Spacer(Modifier.height(10.dp))
            Text(
                "Package, version and checksum are verified before Android opens the installer.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (release.htmlUrl.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "View on GitHub",
                    modifier = Modifier
                        .clickable { GithubBetaUpdater.openRelease(context, release) }
                        .padding(vertical = 10.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrivacySheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 26.dp).padding(bottom = 34.dp)) {
            Text("Privacy", style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(20.dp))
            Text(
                "Dayline has no account, advertising SDK, analytics SDK, or Dayline cloud service. Events, tasks, settings and focus state are stored on your device. If Android Calendar sync is enabled, Dayline reads and writes through Android's Calendar Provider; the calendar provider you choose may independently sync that calendar according to its own settings. Backup/export only writes data to a location you explicitly choose. GitHub beta builds can contact the public Dayline GitHub Releases API when you check for updates, or once per day if automatic beta checks are enabled. Play builds hide that update channel and do not request Internet access for it.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(18.dp))
            Text(
                "Open full privacy policy",
                modifier = Modifier.clickable {
                    context.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://0xpix.github.io/dayline/privacy-policy.html")
                        )
                    )
                },
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(14.dp))
            Text("Close", modifier = Modifier.clickable(onClick = onDismiss), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

private fun CalendarPreferences.withRule(rule: CalendarRule): CalendarPreferences =
    copy(rules = rules.filterNot { it.calendarId == rule.calendarId } + rule)

private fun nextSpace(current: String?, spaces: List<DaylineSpace>): String? {
    if (spaces.isEmpty()) return null
    if (current == null) return spaces.first().id
    val index = spaces.indexOfFirst { it.id == current }
    return if (index < 0 || index == spaces.lastIndex) null else spaces[index + 1].id
}

private fun spaceName(id: String?, spaces: List<DaylineSpace>): String =
    spaces.firstOrNull { it.id == id }?.name ?: "NONE"

private fun glyphModeLabel(mode: GlyphMode): String = when (mode) {
    GlyphMode.OFF -> "Off"
    GlyphMode.EYES_ONLY,
    GlyphMode.EYES_AND_STATES -> "On"
}

private fun updateActionLabel(state: UpdateUiState): String = when (state.status) {
    UpdateStatus.IDLE -> "Check"
    UpdateStatus.CHECKING -> "Checking…"
    UpdateStatus.UP_TO_DATE -> "Up to date"
    UpdateStatus.AVAILABLE -> state.release?.versionName ?: "Available"
    UpdateStatus.ERROR -> "Retry"
}

private fun relativeCheckTime(epochMillis: Long): String {
    val elapsed = (System.currentTimeMillis() - epochMillis).coerceAtLeast(0L)
    return when {
        elapsed < 60_000L -> "just now"
        elapsed < 3_600_000L -> "${elapsed / 60_000L}m ago"
        elapsed < 86_400_000L -> "${elapsed / 3_600_000L}h ago"
        else -> DateTimeFormatter.ofPattern("MMM d · HH:mm")
            .withZone(ZoneId.systemDefault())
            .format(Instant.ofEpochMilli(epochMillis))
    }
}

private fun calendarHealthLabel(enabled: Boolean, lastSyncAt: Long?, error: String?): String {
    if (!enabled) return "Off"
    if (!error.isNullOrBlank()) return "Needs attention"
    if (lastSyncAt == null) return "Waiting"
    return "Synced ${DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(lastSyncAt))}"
}

private fun appearanceLabel(appearance: Appearance): String = when (appearance) {
    Appearance.SYSTEM -> "System"
    Appearance.LIGHT -> "Light"
    Appearance.DARK -> "OLED dark"
}

private fun fontLabel(font: FontChoice): String = when (font.name) {
    "PIXELIFY" -> "Pixelify Sans"
    "GEIST" -> "Geist · Nothing OS 5"
    "GEIST_PIXEL" -> "Geist Pixel"
    "INTER" -> "Inter"
    "SPACE_GROTESK" -> "Space Grotesk"
    "IBM_PLEX_MONO" -> "IBM Plex Mono"
    "SYSTEM" -> "System"
    else -> font.name.lowercase().split('_').joinToString(" ") { part ->
        part.replaceFirstChar { ch -> ch.titlecase() }
    }
}

private fun preciseStatus(context: Context): String {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return "On"
    val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    return if (manager.canScheduleExactAlarms()) "On" else "Allow"
}
