package com.pix.dayline.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pix.dayline.ui.DaylineScreen

/**
 * Full-screen navigation hub.
 *
 * This intentionally avoids a drawer/burger list. Dayline destinations live in
 * a stable two-column grid so switching pages feels like choosing a place, not
 * opening an app drawer.
 */
@Composable
fun NavigationSheet(
    current: DaylineScreen,
    onSelect: (DaylineScreen) -> Unit,
    onDismiss: () -> Unit,
    onQuickAdd: () -> Unit = {},
    canUndo: Boolean = false,
    onUndo: () -> Unit = {}
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 26.dp, vertical = 22.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "DAYLINE",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Move around",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Surface(
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onDismiss
                        ),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Text(
                            "×",
                            modifier = Modifier.padding(horizontal = 15.dp, vertical = 9.dp),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }

                Spacer(Modifier.height(28.dp))

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onQuickAdd
                        ),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.onBackground,
                    contentColor = MaterialTheme.colorScheme.background
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Quick Add",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                "Event or task",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.background.copy(alpha = .68f)
                            )
                        }
                        Text("+", style = MaterialTheme.typography.headlineMedium)
                    }
                }

                Spacer(Modifier.height(22.dp))
                Text(
                    "PAGES",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))

                DestinationRow(
                    first = DaylineScreen.TODAY,
                    second = DaylineScreen.CALENDAR,
                    current = current,
                    onSelect = onSelect
                )
                Spacer(Modifier.height(10.dp))
                DestinationRow(
                    first = DaylineScreen.UPCOMING,
                    second = DaylineScreen.TASKS,
                    current = current,
                    onSelect = onSelect
                )
                Spacer(Modifier.height(10.dp))
                DestinationRow(
                    first = DaylineScreen.SEARCH,
                    second = DaylineScreen.SPACES,
                    current = current,
                    onSelect = onSelect
                )
                Spacer(Modifier.height(10.dp))
                DestinationRow(
                    first = DaylineScreen.SETTINGS,
                    second = null,
                    current = current,
                    onSelect = onSelect
                )

                Spacer(Modifier.weight(1f))

                if (canUndo) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onUndo
                            ),
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 15.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Undo last change", style = MaterialTheme.typography.bodyLarge)
                            Text("↶", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                Text(
                    "Current · ${current.label}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .7f)
                )
            }
        }
    }
}

@Composable
private fun DestinationRow(
    first: DaylineScreen,
    second: DaylineScreen?,
    current: DaylineScreen,
    onSelect: (DaylineScreen) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        DestinationTile(
            screen = first,
            selected = first == current,
            modifier = Modifier.weight(1f),
            onClick = { onSelect(first) }
        )
        if (second != null) {
            DestinationTile(
                screen = second,
                selected = second == current,
                modifier = Modifier.weight(1f),
                onClick = { onSelect(second) }
            )
        } else {
            Box(Modifier.weight(1f))
        }
    }
}

@Composable
private fun DestinationTile(
    screen: DaylineScreen,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(104.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(26.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.onBackground
        } else {
            MaterialTheme.colorScheme.surface
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.background
        } else {
            MaterialTheme.colorScheme.onBackground
        }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                screen.mark,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) {
                    MaterialTheme.colorScheme.background.copy(alpha = .68f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Column {
                Text(
                    screen.label,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 19.sp,
                        lineHeight = 22.sp
                    )
                )
                Text(
                    screen.hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected) {
                        MaterialTheme.colorScheme.background.copy(alpha = .66f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

private val DaylineScreen.label: String
    get() = when (this) {
        DaylineScreen.TODAY -> "Today"
        DaylineScreen.CALENDAR -> "Calendar"
        DaylineScreen.UPCOMING -> "Upcoming"
        DaylineScreen.TASKS -> "Tasks"
        DaylineScreen.SEARCH -> "Search"
        DaylineScreen.SPACES -> "Spaces"
        DaylineScreen.SETTINGS -> "Settings"
    }

private val DaylineScreen.hint: String
    get() = when (this) {
        DaylineScreen.TODAY -> "Timeline"
        DaylineScreen.CALENDAR -> "Month"
        DaylineScreen.UPCOMING -> "Next"
        DaylineScreen.TASKS -> "To do"
        DaylineScreen.SEARCH -> "Find"
        DaylineScreen.SPACES -> "Organize"
        DaylineScreen.SETTINGS -> "Tune"
    }

private val DaylineScreen.mark: String
    get() = when (this) {
        DaylineScreen.TODAY -> "01"
        DaylineScreen.CALENDAR -> "02"
        DaylineScreen.UPCOMING -> "03"
        DaylineScreen.TASKS -> "04"
        DaylineScreen.SEARCH -> "05"
        DaylineScreen.SPACES -> "06"
        DaylineScreen.SETTINGS -> "07"
    }
