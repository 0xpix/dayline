package com.pix.dayline.ui.settings

import android.app.AlarmManager
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.pix.dayline.data.*
import com.pix.dayline.model.*
import com.pix.dayline.ui.components.FloatingControls


private val NotoEmojiFamily = FontFamily(
    Font(
        googleFont = GoogleFont("Noto Emoji", bestEffort = true),
        weight = FontWeight.Normal
    )
)

private enum class SettingsSheet {
    APPEARANCE,
    APP_FONT,
    WIDGET_FONT,
    WIDGET_EMOJI,
    CALENDARS,
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
    nowActivityEnabled: Boolean,
    calendarSyncEnabled: Boolean,
    calendarPreferences: CalendarPreferences,
    deviceCalendars: List<DeviceCalendar>,
    spaces: List<DaylineSpace>,
    showOrb: Boolean,
    weekStartsMonday: Boolean,
    onAppearance: (Appearance) -> Unit,
    onFontChoice: (FontChoice) -> Unit,
    onWidgetFontChoice: (WidgetFontChoice) -> Unit,
    onWidgetEmojiChoice: (WidgetEmojiChoice) -> Unit,
    onWidgetAutoSlide: (Boolean) -> Unit,
    onNowActivityEnabled: (Boolean) -> Unit,
    onCalendarSyncEnabled: (Boolean) -> Unit,
    onCalendarPreferences: (CalendarPreferences) -> Unit,
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
            }

            Spacer(Modifier.height(34.dp))
            Text(
                "Dayline 0.12.2 · Play beta",
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

        SettingsSheet.CALENDARS -> CalendarControlsSheet(
            calendars = deviceCalendars,
            preferences = calendarPreferences,
            spaces = spaces,
            onChange = onCalendarPreferences,
            onDismiss = { openSheet = null }
        )

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
                "Google Noto Emoji · monochrome · changes stay live while this sheet is open.",
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
                            Text(
                                text = emoji.symbol,
                                fontFamily = NotoEmojiFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 31.sp,
                                lineHeight = 32.sp,
                                color = if (emoji == selected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
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
                            Text(calendar.accountName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
private fun PrivacySheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 26.dp).padding(bottom = 32.dp)) {
            Text("Privacy", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(16.dp))
            Text(
                "Dayline has no account, advertising SDK, analytics SDK, or Dayline cloud service. Events, tasks, settings and focus state are stored on your device. If Android Calendar sync is enabled, Dayline reads and writes through Android's Calendar Provider; the calendar provider you choose may independently sync that calendar according to its own settings. Backup/export only writes data to a location you explicitly choose.",
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

private fun appearanceLabel(appearance: Appearance): String = when (appearance) {
    Appearance.SYSTEM -> "System"
    Appearance.LIGHT -> "Light"
    Appearance.DARK -> "OLED dark"
}

private fun fontLabel(font: FontChoice): String = when (font) {
    FontChoice.PIXELIFY -> "Pixelify Sans"
    FontChoice.GEIST -> "Geist"
    FontChoice.GEIST_PIXEL -> "Geist Pixel"
    FontChoice.SYSTEM -> "System"
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
