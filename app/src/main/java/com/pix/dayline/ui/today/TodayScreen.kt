package com.pix.dayline.ui.today

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.pix.dayline.model.AgendaItem
import com.pix.dayline.model.AgendaKind
import com.pix.dayline.ui.theme.OrbBlue
import com.pix.dayline.ui.theme.OrbLavender
import com.pix.dayline.ui.theme.OrbPink
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen() {
    val now = remember { LocalTime.now() }
    val today = remember { LocalDate.now() }

    var addOpen by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }

    val items = remember {
        listOf(
            AgendaItem(AgendaKind.TASK, null, "Water plants"),
            AgendaItem(AgendaKind.EVENT, "09:30", "LUMEN meeting"),
            AgendaItem(AgendaKind.EVENT, "13:00", "Lunch"),
            AgendaItem(AgendaKind.EVENT, "18:30", "Gym")
        )
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
                .align(Alignment.CenterStart)
                .padding(start = 42.dp, end = 84.dp)
                .padding(bottom = 22.dp)
        ) {
            DawnOrb()

            Spacer(Modifier.height(34.dp))

            Text(
                text = greetingText(now, today),
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(32.dp))

            Agenda(items)
        }

        FloatingActions(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 24.dp),
            onMenu = { menuOpen = true },
            onToday = { /* already on Today */ },
            onAdd = { addOpen = true }
        )
    }

    if (addOpen) {
        QuickAddSheet(onDismiss = { addOpen = false })
    }

    if (menuOpen) {
        NavigationSheet(onDismiss = { menuOpen = false })
    }
}

@Composable
private fun DawnOrb() {
    Canvas(modifier = Modifier.size(74.dp)) {
        drawCircle(
            brush = Brush.linearGradient(
                colors = listOf(OrbLavender, OrbPink, OrbBlue),
                start = Offset(0f, size.height),
                end = Offset(size.width, 0f)
            )
        )
    }
}

@Composable
private fun Agenda(items: List<AgendaItem>) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        items.forEach { item ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = when (item.kind) {
                        AgendaKind.TASK -> "TODO"
                        AgendaKind.EVENT -> item.timeLabel.orEmpty()
                    },
                    modifier = Modifier.width(64.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}

@Composable
private fun FloatingActions(
    modifier: Modifier = Modifier,
    onMenu: () -> Unit,
    onToday: () -> Unit,
    onAdd: () -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CircleAction(
            label = "≡",
            filled = false,
            onClick = onMenu
        )

        CircleAction(
            label = "◎",
            filled = false,
            onClick = onToday
        )

        CircleAction(
            label = "+",
            filled = true,
            size = 54,
            textSize = 31,
            onClick = onAdd
        )
    }
}

@Composable
private fun CircleAction(
    label: String,
    filled: Boolean,
    size: Int = 48,
    textSize: Int = 23,
    onClick: () -> Unit
) {
    val background =
        if (filled) MaterialTheme.colorScheme.onBackground
        else MaterialTheme.colorScheme.surface

    val foreground =
        if (filled) MaterialTheme.colorScheme.background
        else MaterialTheme.colorScheme.onSurface

    Surface(
        modifier = Modifier
            .size(size.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        shape = CircleShape,
        color = background,
        contentColor = foreground,
        shadowElevation = if (filled) 0.dp else 1.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = foreground,
                fontSize = textSize.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickAddSheet(onDismiss: () -> Unit) {
    var value by remember { mutableStateOf(TextFieldValue("")) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 12.dp)
        ) {
            Text(
                "New",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(18.dp))

            BasicTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = false,
                textStyle = MaterialTheme.typography.titleMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 23.sp,
                    lineHeight = 28.sp
                ),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    Box {
                        if (value.text.isEmpty()) {
                            Text(
                                "What are you doing?",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 23.sp,
                                    lineHeight = 28.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                            )
                        }
                        innerTextField()
                    }
                }
            )

            Spacer(Modifier.height(28.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                SheetChip("Today")
                SheetChip("09:00")
                SheetChip("Event")
            }

            Spacer(Modifier.height(34.dp))
        }
    }
}

@Composable
private fun SheetChip(text: String) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NavigationSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            NavigationRow("Today", active = true)
            NavigationRow("Calendar")
            NavigationRow("Upcoming")
            NavigationRow("Tasks")
            Spacer(Modifier.height(14.dp))
            NavigationRow("Settings")
            Spacer(Modifier.height(26.dp))
        }
    }
}

@Composable
private fun NavigationRow(
    label: String,
    active: Boolean = false
) {
    Text(
        text = label,
        modifier = Modifier
            .padding(vertical = 12.dp)
            .alpha(if (active) 1f else 0.64f),
        style = MaterialTheme.typography.titleMedium.copy(
            fontSize = 22.sp,
            lineHeight = 27.sp
        ),
        color = MaterialTheme.colorScheme.onBackground
    )
}

private fun greetingText(now: LocalTime, date: LocalDate): String {
    val greeting = when (now.hour) {
        in 5..11 -> "Good morning!"
        in 12..16 -> "Good afternoon!"
        in 17..21 -> "Good evening!"
        else -> "Good night!"
    }

    val dayName = date.dayOfWeek
        .getDisplayName(JavaTextStyle.FULL, Locale.getDefault())

    val monthName = date.month
        .getDisplayName(JavaTextStyle.FULL, Locale.getDefault())

    return "$greeting\nIt's $dayName\n$monthName ${ordinal(date.dayOfMonth)}."
}

private fun ordinal(day: Int): String {
    val suffix = if (day in 11..13) {
        "th"
    } else {
        when (day % 10) {
            1 -> "st"
            2 -> "nd"
            3 -> "rd"
            else -> "th"
        }
    }
    return "$day$suffix"
}
