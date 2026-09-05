package com.pix.dayline.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FloatingControls(
    modifier: Modifier = Modifier,
    showToday: Boolean = true,
    showAdd: Boolean = true,
    onMenu: () -> Unit,
    onToday: () -> Unit = {},
    onAdd: () -> Unit = {}
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CircleAction("≡", false, onClick = onMenu)
        if (showToday) CircleAction("◎", false, onClick = onToday)
        if (showAdd) CircleAction("+", true, size = 54, textSize = 31, onClick = onAdd)
    }
}

@Composable
private fun CircleAction(
    label: String,
    filled: Boolean,
    onClick: () -> Unit,
    size: Int = 48,
    textSize: Int = 23
) {
    val background = if (filled) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.surface
    val foreground = if (filled) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurface

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
