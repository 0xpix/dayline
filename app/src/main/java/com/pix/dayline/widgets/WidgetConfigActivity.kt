package com.pix.dayline.widgets

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.pix.dayline.data.AndroidCalendarSync
import com.pix.dayline.data.DaylineStore
import com.pix.dayline.data.WidgetEmojiChoice
import com.pix.dayline.data.WidgetFontChoice
import com.pix.dayline.model.*
import com.pix.dayline.ui.theme.DaylineTheme
import kotlinx.coroutines.launch

class WidgetConfigActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setResult(Activity.RESULT_CANCELED)
        val store = DaylineStore(applicationContext)

        setContent {
            DaylineTheme(
                darkTheme = androidx.compose.foundation.isSystemInDarkTheme(),
                fontChoice = store.loadFontChoice(),
                dynamicColor = true
            ) {
                WidgetConfigScreen(
                    initial = store.loadWidgetInstancePrefs(appWidgetId),
                    spaces = store.loadSpaces(),
                    calendars = AndroidCalendarSync.listCalendars(applicationContext),
                    globalEmoji = store.loadWidgetEmojiChoice(),
                    globalFont = store.loadWidgetFontChoice(),
                    onSave = { value ->
                        store.saveWidgetInstancePrefs(value)
                        lifecycleScope.launch {
                            DaylineWidgetUpdater.updateAll(applicationContext)
                        }
                        setResult(
                            Activity.RESULT_OK,
                            Intent().putExtra(
                                AppWidgetManager.EXTRA_APPWIDGET_ID,
                                appWidgetId
                            )
                        )
                        finish()
                    },
                    onCancel = { finish() }
                )
            }
        }
    }
}

@Composable
private fun WidgetConfigScreen(
    initial: WidgetInstancePrefs,
    spaces: List<DaylineSpace>,
    calendars: List<DeviceCalendar>,
    globalEmoji: WidgetEmojiChoice,
    globalFont: WidgetFontChoice,
    onSave: (WidgetInstancePrefs) -> Unit,
    onCancel: () -> Unit
) {
    var value by remember { mutableStateOf(initial) }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            )
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 48.dp)
        ) {
            Text("Widget", style = MaterialTheme.typography.displayMedium)
            Spacer(Modifier.height(28.dp))

            ConfigCycleRow("Space", spaces.firstOrNull { it.id == value.spaceId }?.name ?: "All") {
                value = value.copy(spaceId = cycleSpace(value.spaceId, spaces))
            }
            ConfigCycleRow("Calendar", calendars.firstOrNull { it.id == value.calendarId }?.name ?: "All") {
                value = value.copy(calendarId = cycleCalendar(value.calendarId, calendars))
            }
            ConfigCycleRow("Emoji", value.emoji ?: "Global · ${globalEmoji.name.lowercase()}") {
                val choices = WidgetEmojiChoice.entries.map { it.name }
                value = value.copy(emoji = cycleString(value.emoji, choices))
            }
            ConfigCycleRow("Font", value.font ?: "Global · ${globalFont.name.lowercase()}") {
                val choices = WidgetFontChoice.entries.map { it.name }
                value = value.copy(font = cycleString(value.font, choices))
            }
            ConfigCycleRow("Background", value.backgroundMode.name.lowercase()) {
                value = value.copy(
                    backgroundMode = if (value.backgroundMode == WidgetBackgroundMode.SYSTEM) {
                        WidgetBackgroundMode.TRANSPARENT
                    } else WidgetBackgroundMode.SYSTEM
                )
            }
            ConfigCycleRow("Content", value.contentMode.name.lowercase()) {
                val all = WidgetContentMode.entries
                value = value.copy(
                    contentMode = all[(all.indexOf(value.contentMode) + 1) % all.size]
                )
            }
            ConfigToggleRow("Events", value.showEvents) { value = value.copy(showEvents = it) }
            ConfigToggleRow("Tasks", value.showTasks) { value = value.copy(showTasks = it) }
            ConfigToggleRow("Focus state", value.showFocusState) { value = value.copy(showFocusState = it) }

            Spacer(Modifier.height(34.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    modifier = Modifier.clickable { onSave(value) },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.onBackground,
                    contentColor = MaterialTheme.colorScheme.background
                ) {
                    Text("Save", modifier = Modifier.padding(horizontal = 22.dp, vertical = 11.dp))
                }
                Text(
                    "Cancel",
                    modifier = Modifier.clickable { onCancel() }.padding(11.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ConfigCycleRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(8.dp))
        Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ConfigToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

private fun cycleSpace(current: String?, spaces: List<DaylineSpace>): String? {
    if (spaces.isEmpty()) return null
    if (current == null) return spaces.first().id
    val index = spaces.indexOfFirst { it.id == current }
    return if (index < 0 || index == spaces.lastIndex) null else spaces[index + 1].id
}

private fun cycleCalendar(current: Long?, calendars: List<DeviceCalendar>): Long? {
    if (calendars.isEmpty()) return null
    if (current == null) return calendars.first().id
    val index = calendars.indexOfFirst { it.id == current }
    return if (index < 0 || index == calendars.lastIndex) null else calendars[index + 1].id
}

private fun cycleString(current: String?, choices: List<String>): String? {
    if (choices.isEmpty()) return null
    if (current == null) return choices.first()
    val index = choices.indexOf(current)
    return if (index < 0 || index == choices.lastIndex) null else choices[index + 1]
}
