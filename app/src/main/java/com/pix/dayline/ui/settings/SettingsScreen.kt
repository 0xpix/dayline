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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pix.dayline.data.Appearance
import com.pix.dayline.data.FontChoice
import com.pix.dayline.data.WidgetFontChoice
import com.pix.dayline.ui.components.FloatingControls

@Composable
fun SettingsScreen(
    appearance: Appearance,
    fontChoice: FontChoice,
    widgetFontChoice: WidgetFontChoice,
    showOrb: Boolean,
    weekStartsMonday: Boolean,
    onAppearance: (Appearance) -> Unit,
    onFontChoice: (FontChoice) -> Unit,
    onWidgetFontChoice: (WidgetFontChoice) -> Unit,
    onShowOrb: (Boolean) -> Unit,
    onWeekStart: (Boolean) -> Unit,
    onMenu: () -> Unit,
    onToday: () -> Unit
) {
    val context = LocalContext.current

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
            SectionLabel("Appearance")
            Spacer(Modifier.height(10.dp))
            ChoiceRow("System", appearance == Appearance.SYSTEM) { onAppearance(Appearance.SYSTEM) }
            ChoiceRow("Light", appearance == Appearance.LIGHT) { onAppearance(Appearance.LIGHT) }
            ChoiceRow("OLED dark", appearance == Appearance.DARK) { onAppearance(Appearance.DARK) }

            Spacer(Modifier.height(28.dp))
            SectionLabel("Typography")
            Spacer(Modifier.height(10.dp))
            ChoiceRow("Pixelify Sans", fontChoice == FontChoice.PIXELIFY) { onFontChoice(FontChoice.PIXELIFY) }
            ChoiceRow("Geist · Nothing OS 5", fontChoice == FontChoice.GEIST) { onFontChoice(FontChoice.GEIST) }
            ChoiceRow("Geist Pixel", fontChoice == FontChoice.GEIST_PIXEL) { onFontChoice(FontChoice.GEIST_PIXEL) }
            ChoiceRow("System", fontChoice == FontChoice.SYSTEM) { onFontChoice(FontChoice.SYSTEM) }

            Spacer(Modifier.height(24.dp))
            SectionLabel("Widget typography")
            Spacer(Modifier.height(10.dp))
            ChoiceRow(
                "Nothing dots · Bold",
                widgetFontChoice == WidgetFontChoice.DOT_BOLD
            ) { onWidgetFontChoice(WidgetFontChoice.DOT_BOLD) }
            ChoiceRow(
                "Nothing dots · Fine",
                widgetFontChoice == WidgetFontChoice.DOT_FINE
            ) { onWidgetFontChoice(WidgetFontChoice.DOT_FINE) }
            ChoiceRow(
                "Monospace · Bold",
                widgetFontChoice == WidgetFontChoice.MONO
            ) { onWidgetFontChoice(WidgetFontChoice.MONO) }
            Text(
                "Bold dots is the default and is designed for better contrast on the home screen.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(28.dp))
            SectionLabel("Today")
            Spacer(Modifier.height(8.dp))
            ToggleRow("24-hour day dial", showOrb, onShowOrb)
            Text(
                "Shows a 24-segment dial on Today. Scheduled hours light up; the small marker shows the current hour.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(28.dp))
            SectionLabel("Reminders")
            Spacer(Modifier.height(8.dp))
            ActionRow("Notification settings", "Open") {
                context.startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                )
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActionRow("Precise reminder timing", preciseStatus(context)) {
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
            Text(
                "Enable precise timing so 5, 10 and 15 minute reminders arrive at the requested minute.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(28.dp))
            SectionLabel("Calendar")
            Spacer(Modifier.height(8.dp))
            ToggleRow("Week starts Monday", weekStartsMonday, onWeekStart)

            Spacer(Modifier.height(34.dp))
            Text(
                "Dayline 0.8.6",
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
}

private fun preciseStatus(context: Context): String {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return "On"
    val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    return if (manager.canScheduleExactAlarms()) "On" else "Allow"
}

@Composable
private fun SectionLabel(label: String) {
    Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ChoiceRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(
            if (selected) "●" else "○",
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@Composable
private fun ActionRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
