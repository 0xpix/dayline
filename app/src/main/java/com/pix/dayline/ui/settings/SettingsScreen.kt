package com.pix.dayline.ui.settings

import android.app.AlarmManager
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.pix.dayline.BuildConfig
import com.pix.dayline.R
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
    WIDGET_FONT,
    WIDGET_EMOJI,
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
                .padding(start = 30.dp, end = 30.dp, top = 56.dp, bottom = 138.dp)
        ) {
            Text("Settings", style = MaterialTheme.typography.displayMedium)
            Spacer(Modifier.height(34.dp))

            SettingsGroup("General") {
                SelectorRow("Appearance", appearanceLabel(appearance)) {
                    openSheet = SettingsSheet.APPEARANCE
                }
                SelectorRow("App font", fontLabel(fontChoice)) {
                    openSheet = SettingsSheet.APP_FONT
                }
            }

            SectionGap()
            SettingsGroup("Widgets") {
                SelectorRow("Widget font", widgetFontLabel(widgetFontChoice)) {
                    openSheet = SettingsSheet.WIDGET_FONT
                }
                SelectorRow("Widget emoji", widgetEmojiChoice.label) {
                    openSheet = SettingsSheet.WIDGET_EMOJI
                }
                ToggleSettingRow("Slide long titles", widgetAutoSlide, onWidgetAutoSlide)
                Text(
                    "Each placed widget can also have its own Space, calendar, content and background configuration.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SectionGap()
            SettingsGroup("Glyph") {
                SelectorRow("Dayline Glyph", glyphModeLabel(glyphPreferences.mode)) {
                    openSheet = SettingsSheet.GLYPH
                }
                InfoRow(
                    "Hardware",
                    if (glyphHardwareStatus.available) {
                        "${glyphHardwareStatus.deviceLabel} · ${glyphHardwareStatus.matrixSize ?: 13}×${glyphHardwareStatus.matrixSize ?: 13}"
                    } else {
                        glyphHardwareStatus.deviceLabel
                    }
                )
                Text(
                    "Expressive eyes stay active. Focus cycles add a pixel progress ring around the face.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                InfoRow(
                    "Sync health",
                    calendarHealthLabel(calendarSyncEnabled, lastCalendarSyncAt, calendarSyncError)
                )
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
                SelectorRow("Backup Dayline", "JSON") { onBackup() }
                SelectorRow("Restore backup", "JSON") { onRestore() }
                SelectorRow("Export calendar", ".ics") { onExportIcs() }
                SelectorRow("Import calendar", ".ics") { onImportIcs() }
            }

            if (BuildConfig.UPDATE_CHANNEL == "GitHub beta") {
                SectionGap()
                SettingsGroup("Beta updates") {
                    InfoRow("Channel", "GitHub beta")
                    ToggleSettingRow("Automatic daily check", autoBetaUpdates, onAutoBetaUpdates)
                    SelectorRow("Check for updates", updateActionLabel(updateState)) {
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
                    } else {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Beta releases are checked directly against the public Dayline GitHub Releases feed.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                InfoRow("Build", BuildConfig.VERSION_CODE.toString())
                InfoRow("Commit", BuildConfig.GIT_COMMIT)
            }

            Spacer(Modifier.height(34.dp))
            Text(
                "Dayline ${BuildConfig.VERSION_NAME} · ${BuildConfig.UPDATE_CHANNEL}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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

        SettingsSheet.WIDGET_FONT -> SelectionSheet(
            "Widget font",
            widgetFontLabel(widgetFontChoice),
            listOf(
                "Nothing dots · Bold" to { onWidgetFontChoice(WidgetFontChoice.DOT_BOLD) },
                "Nothing dots · Fine" to { onWidgetFontChoice(WidgetFontChoice.DOT_FINE) },
                "Monospace · Bold" to { onWidgetFontChoice(WidgetFontChoice.MONO) }
            )
        ) { openSheet = null }

        SettingsSheet.WIDGET_EMOJI -> EmojiSheet(
            selected = widgetEmojiChoice,
            onSelect = onWidgetEmojiChoice,
            onDismiss = { openSheet = null }
        )

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
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun SectionGap() = Spacer(Modifier.height(24.dp))

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
            .padding(vertical = 12.dp),
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
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ToggleSettingRow(title: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
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
        Column(Modifier.fillMaxWidth().padding(horizontal = 28.dp).padding(bottom = 28.dp)) {
            Text(title, style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(18.dp))
            options.forEach { (label, action) ->
                Row(
                    Modifier.fillMaxWidth().clickable {
                        action()
                        onDismiss()
                    }.padding(vertical = 13.dp),
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
private fun EmojiSheet(
    selected: WidgetEmojiChoice,
    onSelect: (WidgetEmojiChoice) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 28.dp)) {
            Text("Widget emoji", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(6.dp))
            Text(
                "Changes stay live while this sheet is open.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(20.dp))
            WidgetEmojiChoice.entries.chunked(4).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    row.forEach { emoji ->
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .background(
                                    if (emoji == selected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                    CircleShape
                                )
                                .clickable { onSelect(emoji) },
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(widgetEmojiIconRes(emoji)),
                                contentDescription = widgetEmojiLabel(emoji),
                                modifier = Modifier.size(31.dp),
                                colorFilter = ColorFilter.tint(
                                    if (emoji == selected) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                    repeat(4 - row.size) { Spacer(Modifier.size(68.dp)) }
                }
                Spacer(Modifier.height(14.dp))
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
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
        ) {
            Text("Dayline Glyph", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(6.dp))
            Text(
                "Expressive eyes + Focus progress. Nothing else interrupts the face.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(18.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                GlyphMatrixPreview(
                    signal = preview,
                    modifier = Modifier.size(190.dp)
                )
            }
            Spacer(Modifier.height(18.dp))

            Text("GLYPH", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            ToggleSettingRow("Enable Glyph", preferences.enabled) {
                update(preferences.copy(mode = if (it) GlyphMode.EYES_ONLY else GlyphMode.OFF))
            }
            InfoRow("Hardware", if (hardware.available) hardware.deviceLabel else "Unavailable")
            hardware.detail?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                "ACTIVATE IN NOTHING SETTINGS  ›",
                modifier = Modifier.clickable(onClick = onOpenManager).padding(vertical = 8.dp),
                style = MaterialTheme.typography.labelMedium
            )

            SectionGap()
            Text("LOOK & FEEL", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Brightness", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                TinyAction("−") { changeBrightness(-32) }
                Spacer(Modifier.width(12.dp))
                Text(
                    "${((brightness / 255f) * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(12.dp))
                TinyAction("+") { changeBrightness(32) }
            }
            ToggleSettingRow("Frequent blink", preferences.blinkEnabled) {
                update(preferences.copy(blinkEnabled = it))
            }
            ToggleSettingRow("Expressions & glances", preferences.randomGlancesEnabled) {
                update(preferences.copy(randomGlancesEnabled = it))
            }
            if (preferences.randomGlancesEnabled) {
                SelectorRow(
                    "Motion frequency",
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
            Text("FOCUS MODE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            InfoRow("Progress ring", "Automatic")
            Text(
                "For 25 / 5, 50 / 10 and custom focus cycles, pixels fill around the eyes one by one. Focus runs clockwise; break restarts in the opposite direction while the eye animations keep going.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            SectionGap()
            Text("NIGHT", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            ToggleSettingRow("Quiet hours", preferences.quietHoursEnabled) {
                update(preferences.copy(quietHoursEnabled = it))
            }
            if (preferences.quietHoursEnabled) {
                SelectorRow("Quiet starts", preferences.quietStart.toString()) {
                    TimePickerDialog(
                        context,
                        { _, hour, minute -> update(preferences.copy(quietStart = java.time.LocalTime.of(hour, minute))) },
                        preferences.quietStart.hour,
                        preferences.quietStart.minute,
                        true
                    ).show()
                }
                SelectorRow("Quiet ends", preferences.quietEnd.toString()) {
                    TimePickerDialog(
                        context,
                        { _, hour, minute -> update(preferences.copy(quietEnd = java.time.LocalTime.of(hour, minute))) },
                        preferences.quietEnd.hour,
                        preferences.quietEnd.minute,
                        true
                    ).show()
                }
            }
            ToggleSettingRow("Dim during quiet hours", preferences.dimAtNight) {
                update(preferences.copy(dimAtNight = it))
            }

            SectionGap()
            Text("PREVIEW EXPRESSIONS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
            val tests = listOf(
                "CENTER" to DaylineGlyphSignal.CENTER,
                "LEFT" to DaylineGlyphSignal.LOOK_LEFT,
                "RIGHT" to DaylineGlyphSignal.LOOK_RIGHT,
                "HAPPY" to DaylineGlyphSignal.HAPPY,
                "WINK" to DaylineGlyphSignal.WINK,
                "CURIOUS" to DaylineGlyphSignal.CURIOUS,
                "PLAYFUL" to DaylineGlyphSignal.PLAYFUL,
                "SURPRISED" to DaylineGlyphSignal.SURPRISED,
                "SIDE EYE" to DaylineGlyphSignal.SIDE_EYE,
                "EXCITED" to DaylineGlyphSignal.EXCITED,
                "ROLLING" to DaylineGlyphSignal.ROLLING,
                "HEARTS" to DaylineGlyphSignal.HEARTS,
                "SLEEPY" to DaylineGlyphSignal.SLEEPY
            )
            tests.chunked(4).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { (label, signal) ->
                        Text(
                            label,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { test(signal) }
                                .padding(vertical = 9.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                "Blink runs automatically and more often now. Sleepy is kept as a rare expression rather than a normal idle state.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text("Calendars", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(6.dp))
            Text(
                "Tap the circle to show/hide. Default controls where new Dayline events are published.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(18.dp))

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
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UpdateSheet(release: BetaRelease, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var downloading by remember(release.tagName) { mutableStateOf(false) }
    var downloadedApk by remember(release.tagName) { mutableStateOf<File?>(null) }
    var message by remember(release.tagName) { mutableStateOf<String?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 26.dp)
                .padding(bottom = 34.dp)
        ) {
            Text("Dayline update", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(8.dp))
            Text(
                release.versionName,
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(8.dp))
            Text(
                release.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))
            Text(
                "RELEASE NOTES",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            Text(
                release.notes.ifBlank { "Bug fixes and Dayline polish." },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(28.dp))

            val actionLabel = when {
                release.apkUrl.isNullOrBlank() -> "OPEN GITHUB RELEASE  ›"
                downloading -> "DOWNLOADING…"
                downloadedApk != null -> "INSTALL UPDATE  ›"
                else -> "DOWNLOAD UPDATE  ›"
            }
            Text(
                actionLabel,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !downloading) {
                        if (release.apkUrl.isNullOrBlank()) {
                            GithubBetaUpdater.openRelease(context, release)
                        } else {
                            val apk = downloadedApk
                            if (apk == null) {
                                downloading = true
                                message = null
                                scope.launch {
                                    GithubBetaUpdater.download(context.applicationContext, release)
                                        .onSuccess {
                                            downloadedApk = it
                                            message = "Download verified · ready to install"
                                        }
                                        .onFailure {
                                            message = it.message ?: "Download failed"
                                        }
                                    downloading = false
                                }
                            } else {
                                when (val result = GithubBetaUpdater.install(context, apk)) {
                                    GithubBetaUpdater.InstallResult.Started ->
                                        message = "Android installer opened"
                                    GithubBetaUpdater.InstallResult.PermissionRequested ->
                                        message = "Allow Dayline β to install apps, then tap Install update again"
                                    is GithubBetaUpdater.InstallResult.Error ->
                                        message = result.message
                                }
                            }
                        }
                    }
                    .padding(vertical = 12.dp),
                style = MaterialTheme.typography.titleMedium,
                color = if (downloading) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onBackground
            )

            if (release.htmlUrl.isNotBlank()) {
                Text(
                    "OPEN RELEASE NOTES",
                    modifier = Modifier
                        .clickable { GithubBetaUpdater.openRelease(context, release) }
                        .padding(vertical = 8.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            message?.let {
                Spacer(Modifier.height(8.dp))
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Dayline verifies the downloaded APK package, version code and published SHA-256 checksum when available. Android still performs the final signature check before replacing the installed beta.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrivacySheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 26.dp).padding(bottom = 32.dp)) {
            Text("Privacy", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(16.dp))
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

@DrawableRes
private fun widgetEmojiIconRes(emoji: WidgetEmojiChoice): Int = when (emoji.name) {
    "SMILE" -> R.drawable.emoji_smile
    "GRIN" -> R.drawable.emoji_grin
    "WINK" -> R.drawable.emoji_wink
    "COOL" -> R.drawable.emoji_cool
    "NERD" -> R.drawable.emoji_nerd
    "PARTY" -> R.drawable.emoji_party
    "SLEEPY" -> R.drawable.emoji_sleepy
    "MELT" -> R.drawable.emoji_melt
    "GHOST" -> R.drawable.emoji_ghost
    "ROBOT" -> R.drawable.emoji_robot
    "RELAXED" -> R.drawable.emoji_relaxed
    "HEART_EYES" -> R.drawable.emoji_heart_eyes
    else -> R.drawable.emoji_smile
}

private fun widgetEmojiLabel(emoji: WidgetEmojiChoice): String = when (emoji.name) {
    "HEART_EYES" -> "Heart eyes"
    else -> emoji.name.lowercase().replace('_', ' ').replaceFirstChar { ch -> ch.titlecase() }
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
    GlyphMode.EYES_AND_STATES -> "Eyes + focus"
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

private fun widgetFontLabel(font: WidgetFontChoice): String = when (font) {
    WidgetFontChoice.DOT_BOLD -> "Nothing dots · Bold"
    WidgetFontChoice.DOT_FINE -> "Nothing dots · Fine"
    WidgetFontChoice.MONO -> "Monospace · Bold"
}

private fun preciseStatus(context: Context): String {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return "On"
    val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    return if (manager.canScheduleExactAlarms()) "On" else "Allow"
}
