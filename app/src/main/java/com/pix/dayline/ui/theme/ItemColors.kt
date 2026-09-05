package com.pix.dayline.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.pix.dayline.model.ItemColor

private val Blue = Color(0xFF7F9FC8)
private val Sage = Color(0xFF87A58C)
private val Amber = Color(0xFFBC945E)
private val Rose = Color(0xFFC27D89)
private val Violet = Color(0xFF9889BD)

@Composable
fun ItemColor.composeColor(): Color = when (this) {
    ItemColor.MONO -> androidx.compose.material3.MaterialTheme.colorScheme.onBackground
    ItemColor.BLUE -> Blue
    ItemColor.SAGE -> Sage
    ItemColor.AMBER -> Amber
    ItemColor.ROSE -> Rose
    ItemColor.VIOLET -> Violet
}
