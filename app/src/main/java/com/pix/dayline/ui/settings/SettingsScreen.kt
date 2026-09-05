package com.pix.dayline.ui.settings

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.sp
import com.pix.dayline.data.Appearance
import com.pix.dayline.data.FontChoice
import com.pix.dayline.data.WidgetEmojiCategory
import com.pix.dayline.data.WidgetEmojiChoice
import com.pix.dayline.data.WidgetFontChoice
import com.pix.dayline.data.category
import com.pix.dayline.data.icon
import com.pix.dayline.data.iconRes
import com.pix.dayline.data.label
import com.pix.dayline.ui.components.FloatingControls

private enum class SettingsSheet {
    APPEARANCE,
    APP_FONT,
    WIDGET_FONT,
    WIDGET_EMOJI
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
    showOrb: Boolean,
    weekStartsMonday: Boolean,
    onAppearance: (Appearance) -> Unit,
    onFontChoice: (FontChoice) -> Unit,
    onWidgetFontChoice: (WidgetFontChoice) -> Unit,
    onWidgetEmojiChoice: (WidgetEmojiChoice) -> Unit,
    onWidgetAutoSlide: (Boolean) -> Unit,
    onNowActivityEnabled: (Boolean) -> Unit,
    onCalendarSyncEnabled: (Boolean) -> Unit,
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
                SelectorRow(
                    title = "Appearance",
                    value = appearanceLabel(appearance)
                ) { openSheet = SettingsSheet.APPEARANCE }

                SelectorRow(
                    title = "App font",
                    value = fontLabel(fontChoice)
                ) { openSheet = SettingsSheet.APP_FONT }
            }

            Spacer(Modifier.height(24.dp))

            SettingsGroup("Widgets") {
                SelectorRow(
                    title = "Widget font",
                    value = widgetFontLabel(widgetFontChoice)
                ) { openSheet = SettingsSheet.WIDGET_FONT }

                SelectorRow(
                    title = "Widget emoji",
                    value = widgetEmojiChoice.label
                ) { openSheet = SettingsSheet.WIDGET_EMOJI }

                ToggleSettingRow(
                    title = "Slide long event titles",
                    checked = widgetAutoSlide,
                    onChecked = onWidgetAutoSlide
                )
            }

            Spacer(Modifier.height(24.dp))

            SettingsGroup("Today") {
                ToggleSettingRow(
                    title = "Day signal",
                    checked = showOrb,
                    onChecked = onShowOrb
                )
            }

            Spacer(Modifier.height(24.dp))

            SettingsGroup("Reminders") {
                ToggleSettingRow(
                    title = "Now activity",
                    checked = nowActivityEnabled,
                    onChecked = onNowActivityEnabled
                )

                Text(
                    "Shows the active timed block as a persistent notification. Android 16 can promote it as a Live Update on supported system surfaces.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(6.dp))

                SelectorRow(
                    title = "Notifications",
                    value = "Open"
                ) {
                    context.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    )
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    SelectorRow(
                        title = "Precise timing",
                        value = preciseStatus(context)
                    ) {
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

            Spacer(Modifier.height(24.dp))

            SettingsGroup("Calendar") {
                ToggleSettingRow(
                    title = "Android Calendar sync",
                    checked = calendarSyncEnabled,
                    onChecked = onCalendarSyncEnabled
                )

                Text(
                    "Shows events from calendars on this phone. New Dayline events are published to the first writable visible calendar.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(8.dp))

                ToggleSettingRow(
                    title = "Week starts Monday",
                    checked = weekStartsMonday,
                    onChecked = onWeekStart
                )
            }

            Spacer(Modifier.height(34.dp))

            Text(
                "Dayline 0.11.0",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        FloatingControls(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 28.dp),
            showAdd = false,
            onMenu = onMenu,
            onToday = onToday
        )
    }

    when (openSheet) {
        SettingsSheet.APPEARANCE -> SelectionSheet(
            title = "Appearance",
            selected = appearanceLabel(appearance),
            options = listOf(
                "System" to { onAppearance(Appearance.SYSTEM) },
                "Light" to { onAppearance(Appearance.LIGHT) },
                "OLED dark" to { onAppearance(Appearance.DARK) }
            ),
            onDismiss = { openSheet = null }
        )

        SettingsSheet.APP_FONT -> SelectionSheet(
            title = "App font",
            selected = fontLabel(fontChoice),
            options = listOf(
                "Pixelify Sans" to { onFontChoice(FontChoice.PIXELIFY) },
                "Geist · Nothing OS 5" to { onFontChoice(FontChoice.GEIST) },
                "Geist Pixel" to { onFontChoice(FontChoice.GEIST_PIXEL) },
                "System" to { onFontChoice(FontChoice.SYSTEM) }
            ),
            onDismiss = { openSheet = null }
        )

        SettingsSheet.WIDGET_FONT -> SelectionSheet(
            title = "Widget font",
            selected = widgetFontLabel(widgetFontChoice),
            options = listOf(
                "Nothing dots · Bold" to {
                    onWidgetFontChoice(WidgetFontChoice.DOT_BOLD)
                },
                "Nothing dots · Fine" to {
                    onWidgetFontChoice(WidgetFontChoice.DOT_FINE)
                },
                "Monospace · Bold" to {
                    onWidgetFontChoice(WidgetFontChoice.MONO)
                }
            ),
            onDismiss = { openSheet = null }
        )

        SettingsSheet.WIDGET_EMOJI -> EmojiSheet(
            selected = widgetEmojiChoice,
            onSelect = {
                // Keep the picker open so the preview and placed widget
                // can update while the user tries different icons.
                onWidgetEmojiChoice(it)
            },
            onDismiss = { openSheet = null }
        )

        null -> Unit
    }
}

@Composable
private fun SettingsGroup(
    label: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun SelectorRow(
    title: String,
    value: String,
    onClick: () -> Unit
) {
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
        Text(
            title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )

        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.width(8.dp))

        Text(
            "›",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ToggleSettingRow(
    title: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onChecked
        )
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
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(title, style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(18.dp))

            options.forEach { (label, action) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            action()
                            onDismiss()
                        }
                        .padding(vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        if (label == selected) "●" else "○",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (label == selected) {
                            MaterialTheme.colorScheme.onBackground
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
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
    var category by remember { mutableStateOf(selected.category) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(bottom = 28.dp)
        ) {
            Text("Widget emoji", style = MaterialTheme.typography.headlineLarge)

            Spacer(Modifier.height(6.dp))

            Text(
                "Monochrome icons inspired by the Nothing OS emoji style.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(18.dp))

            WidgetEmojiPreview(selected)

            Spacer(Modifier.height(18.dp))

            EmojiCategoryStrip(
                selected = category,
                onSelect = { category = it }
            )

            Spacer(Modifier.height(20.dp))

            val visible = WidgetEmojiChoice.entries.filter { it.category == category }

            visible.chunked(4).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    row.forEach { emoji ->
                        EmojiChoice(
                            emoji = emoji,
                            selected = emoji == selected,
                            onClick = { onSelect(emoji) }
                        )
                    }

                    repeat(4 - row.size) {
                        Spacer(Modifier.size(68.dp))
                    }
                }
                Spacer(Modifier.height(14.dp))
            }
        }
    }
}

@Composable
private fun WidgetEmojiPreview(selected: WidgetEmojiChoice) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.shapes.extraLarge
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    MaterialTheme.colorScheme.secondaryContainer,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(selected.iconRes),
                contentDescription = selected.label,
                modifier = Modifier.size(28.dp),
                colorFilter = ColorFilter.tint(
                    MaterialTheme.colorScheme.onSecondaryContainer
                )
            )
        }

        Spacer(Modifier.width(12.dp))

        Box(
            modifier = Modifier
                .width(1.dp)
                .height(46.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )

        Spacer(Modifier.width(12.dp))

        Column {
            Text(
                "SAT  5  SEPT",
                style = MaterialTheme.typography.labelMedium,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
            Spacer(Modifier.height(5.dp))
            Text(
                "YOUR DAY IS CLEAR",
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
            Spacer(Modifier.height(5.dp))
            Text(
                "AM  · · ·  ◉  · · ·  PM",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun EmojiCategoryStrip(
    selected: WidgetEmojiCategory,
    onSelect: (WidgetEmojiCategory) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                CircleShape
            )
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        WidgetEmojiCategory.entries.forEach { category ->
            val active = category == selected
            Row(
                modifier = Modifier
                    .background(
                        if (active) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                        CircleShape
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onSelect(category) }
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(category.icon.iconRes),
                    contentDescription = category.label,
                    modifier = Modifier.size(20.dp),
                    colorFilter = ColorFilter.tint(
                        if (active) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    category.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (active) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmojiChoice(
    emoji: WidgetEmojiChoice,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(68.dp)
            .border(
                width = if (selected) 2.dp else 0.dp,
                color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant,
                shape = CircleShape
            )
            .padding(if (selected) 4.dp else 0.dp)
            .background(
                color = if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                shape = CircleShape
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(emoji.iconRes),
            contentDescription = emoji.label,
            modifier = Modifier.size(31.dp),
            colorFilter = ColorFilter.tint(
                if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}


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
