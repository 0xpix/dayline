package com.pix.dayline.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pix.dayline.data.Appearance
import com.pix.dayline.model.CalendarPreferences
import com.pix.dayline.model.CalendarRule
import com.pix.dayline.model.DeviceCalendar

@Composable
fun OnboardingScreen(
    calendars: List<DeviceCalendar>,
    calendarPreferences: CalendarPreferences,
    appearance: Appearance,
    onRequestCalendar: () -> Unit,
    onCalendarPreferences: (CalendarPreferences) -> Unit,
    onAppearance: (Appearance) -> Unit,
    onDone: () -> Unit
) {
    var step by remember { mutableIntStateOf(0) }

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
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 56.dp)
        ) {
            Text("DAYLINE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(60.dp))

            when (step) {
                0 -> {
                    Text("Your day,\nwithout the noise.", style = MaterialTheme.typography.displayMedium)
                    Spacer(Modifier.height(18.dp))
                    Text(
                        "Dayline can use calendars already on this phone. Your calendar data stays on-device unless your calendar provider syncs it.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(32.dp))
                    PrimaryAction("Allow calendar access") {
                        onRequestCalendar()
                        step = 1
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Not now",
                        modifier = Modifier.clickable { step = 2 },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                1 -> {
                    Text("Choose\nwhat appears.", style = MaterialTheme.typography.displayMedium)
                    Spacer(Modifier.height(20.dp))
                    if (calendars.isEmpty()) {
                        Text(
                            "No calendar list yet. You can configure it later in Settings.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        calendars.take(8).forEach { calendar ->
                            val visible = calendarPreferences.isVisible(calendar.id)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        val existing = calendarPreferences.rules
                                            .firstOrNull { it.calendarId == calendar.id }
                                            ?: CalendarRule(calendar.id)
                                        val nextRules = calendarPreferences.rules
                                            .filterNot { it.calendarId == calendar.id } +
                                            existing.copy(visible = !visible)
                                        onCalendarPreferences(calendarPreferences.copy(rules = nextRules))
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(calendar.name, style = MaterialTheme.typography.bodyLarge)
                                    if (calendar.accountName.isNotBlank()) {
                                        Text(calendar.accountName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Text(if (visible) "●" else "○", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    PrimaryAction("Continue") { step = 2 }
                }

                else -> {
                    Text("Make it\nyours.", style = MaterialTheme.typography.displayMedium)
                    Spacer(Modifier.height(24.dp))
                    listOf(
                        Appearance.SYSTEM to "System",
                        Appearance.LIGHT to "Light",
                        Appearance.DARK to "OLED dark"
                    ).forEach { (value, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAppearance(value) }
                                .padding(vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            Text(if (appearance == value) "●" else "○", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    PrimaryAction("Start Dayline", onDone)
                }
            }
        }
    }
}

@Composable
private fun PrimaryAction(label: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.onBackground,
        contentColor = MaterialTheme.colorScheme.background
    ) {
        Text(label, modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp), style = MaterialTheme.typography.titleMedium)
    }
}
