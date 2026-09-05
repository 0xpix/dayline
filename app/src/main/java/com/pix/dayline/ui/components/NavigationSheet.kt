package com.pix.dayline.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
        Column(modifier = Modifier.padding(horizontal = 28.dp, vertical = 10.dp)) {
            NavigationRow("Today", DaylineScreen.TODAY, current, onSelect)
            NavigationRow("Calendar", DaylineScreen.CALENDAR, current, onSelect)
            NavigationRow("Upcoming", DaylineScreen.UPCOMING, current, onSelect)
            NavigationRow("Tasks", DaylineScreen.TASKS, current, onSelect)
            NavigationRow("Spaces", DaylineScreen.SPACES, current, onSelect)
            Spacer(Modifier.height(14.dp))
            NavigationRow("Settings", DaylineScreen.SETTINGS, current, onSelect)
            Spacer(Modifier.height(26.dp))
        }
    }
}

@Composable
private fun NavigationRow(
    label: String,
    screen: DaylineScreen,
    current: DaylineScreen,
    onSelect: (DaylineScreen) -> Unit
) {
    Text(
        text = label,
        modifier = Modifier
            .padding(vertical = 12.dp)
            .alpha(if (screen == current) 1f else 0.64f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onSelect(screen) }
            ),
        style = MaterialTheme.typography.titleMedium.copy(fontSize = 22.sp, lineHeight = 27.sp),
        color = MaterialTheme.colorScheme.onBackground
    )
}
