package com.pix.dayline.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pix.dayline.ui.DaylineScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationSheet(
    current: DaylineScreen,
    onSelect: (DaylineScreen) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .padding(top = 4.dp, bottom = 30.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    "DAYLINE",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    current.label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(22.dp))

            NavigationGroup("PLAN") {
                NavigationRow("Today", DaylineScreen.TODAY, current, onSelect)
                NavigationRow("Calendar", DaylineScreen.CALENDAR, current, onSelect)
                NavigationRow("Upcoming", DaylineScreen.UPCOMING, current, onSelect)
                NavigationRow("Search", DaylineScreen.SEARCH, current, onSelect)
            }

            Spacer(Modifier.height(18.dp))

            NavigationGroup("ORGANIZE") {
                NavigationRow("Tasks", DaylineScreen.TASKS, current, onSelect)
                NavigationRow("Spaces", DaylineScreen.SPACES, current, onSelect)
            }

            Spacer(Modifier.height(18.dp))

            NavigationGroup("DAYLINE") {
                NavigationRow("Settings", DaylineScreen.SETTINGS, current, onSelect)
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

@Composable
private fun NavigationGroup(
    label: String,
    content: @Composable () -> Unit
) {
    Text(
        label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
            alpha = 0.62f
        )
    )

    Spacer(Modifier.height(5.dp))
    content()
}

@Composable
private fun NavigationRow(
    label: String,
    screen: DaylineScreen,
    current: DaylineScreen,
    onSelect: (DaylineScreen) -> Unit
) {
    val selected = screen == current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember {
                    MutableInteractionSource()
                },
                indication = null,
                onClick = { onSelect(screen) }
            )
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(if (selected) 7.dp else 5.dp)
                .background(
                    if (selected) {
                        MaterialTheme.colorScheme.onBackground
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = 0.22f
                        )
                    },
                    CircleShape
                )
        )

        Spacer(Modifier.size(14.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 21.sp,
                lineHeight = 25.sp
            ),
            color = MaterialTheme.colorScheme.onBackground.copy(
                alpha = if (selected) 1f else 0.64f
            )
        )
    }
}
