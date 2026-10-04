package com.pix.dayline.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pix.dayline.ui.DaylineScreen

/**
 * Compact bottom navigation tray.
 *
 * Dayline deliberately avoids a side drawer, burger menu and full-screen
 * launcher. The tray stays close to the thumb, keeps the current page visible
 * and gives the four everyday destinations the strongest hierarchy.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationSheet(
    current: DaylineScreen,
    onSelect: (DaylineScreen) -> Unit,
    onDismiss: () -> Unit,
    onQuickAdd: () -> Unit = {},
    canUndo: Boolean = false,
    onUndo: () -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        dragHandle = {
            Surface(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 8.dp)
                    .width(34.dp)
                    .height(4.dp),
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .28f)
            ) {}
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(bottom = 28.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        "Switch view",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "Currently ${current.label}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    "Swipe down to close",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .62f)
                )
            }

            Spacer(Modifier.height(22.dp))

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

            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SecondaryDestination(
                    screen = DaylineScreen.SEARCH,
                    selected = current == DaylineScreen.SEARCH,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelect(DaylineScreen.SEARCH) }
                )
                SecondaryDestination(
                    screen = DaylineScreen.SPACES,
                    selected = current == DaylineScreen.SPACES,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelect(DaylineScreen.SPACES) }
                )
                SecondaryDestination(
                    screen = DaylineScreen.PLANS,
                    selected = current == DaylineScreen.PLANS,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelect(DaylineScreen.PLANS) }
                )
                SecondaryDestination(
                    screen = DaylineScreen.SETTINGS,
                    selected = current == DaylineScreen.SETTINGS,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelect(DaylineScreen.SETTINGS) }
                )
            }

            Spacer(Modifier.height(22.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onQuickAdd
                    ),
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.onBackground,
                contentColor = MaterialTheme.colorScheme.background
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Quick add", style = MaterialTheme.typography.titleMedium)
                    Text("+", style = MaterialTheme.typography.titleLarge)
                }
            }

            if (canUndo) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Undo last change",
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 46.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onUndo
                        )
                        .padding(horizontal = 4.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DestinationRow(
    first: DaylineScreen,
    second: DaylineScreen,
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
        DestinationTile(
            screen = second,
            selected = second == current,
            modifier = Modifier.weight(1f),
            onClick = { onSelect(second) }
        )
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
            .height(84.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(24.dp),
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
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    screen.label,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 18.sp,
                        lineHeight = 21.sp
                    )
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    screen.hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected) {
                        MaterialTheme.colorScheme.background.copy(alpha = .64f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            if (selected) {
                Surface(
                    modifier = Modifier
                        .width(7.dp)
                        .height(7.dp),
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.background
                ) {}
            }
        }
    }
}

@Composable
private fun SecondaryDestination(
    screen: DaylineScreen,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(999.dp),
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
        Box(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 13.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(screen.label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

private val DaylineScreen.label: String
    get() = when (this) {
        DaylineScreen.TODAY -> "Today"
        DaylineScreen.PLANS -> "Plans"
        DaylineScreen.CALENDAR -> "Calendar"
        DaylineScreen.UPCOMING -> "Upcoming"
        DaylineScreen.TASKS -> "Tasks"
        DaylineScreen.SEARCH -> "Search"
        DaylineScreen.SPACES -> "Spaces"
        DaylineScreen.SETTINGS -> "Settings"
    }

private val DaylineScreen.hint: String
    get() = when (this) {
        DaylineScreen.TODAY -> "Your timeline"
        DaylineScreen.PLANS -> "Things you follow"
        DaylineScreen.CALENDAR -> "Month view"
        DaylineScreen.UPCOMING -> "What is next"
        DaylineScreen.TASKS -> "Things to do"
        DaylineScreen.SEARCH -> "Find anything"
        DaylineScreen.SPACES -> "Organize"
        DaylineScreen.SETTINGS -> "Preferences"
    }
