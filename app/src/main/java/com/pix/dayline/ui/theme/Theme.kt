package com.pix.dayline.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.pix.dayline.data.FontChoice
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = DaylineLightForeground,
    onPrimary = DaylineLightBackground,
    background = DaylineLightBackground,
    onBackground = DaylineLightForeground,
    surface = DaylineLightSurface,
    onSurface = DaylineLightForeground,
    surfaceVariant = DaylineLightSurface,
    onSurfaceVariant = DaylineLightSecondary,
    outline = Color(0xFFD8D5D0)
)

private val DarkColors = darkColorScheme(
    primary = DaylineDarkForeground,
    onPrimary = DaylineDarkBackground,
    background = DaylineDarkBackground,
    onBackground = DaylineDarkForeground,
    surface = DaylineDarkSurface,
    onSurface = DaylineDarkForeground,
    surfaceVariant = DaylineDarkSurface,
    onSurfaceVariant = DaylineDarkSecondary,
    outline = Color(0xFF323232)
)

@Composable
fun DaylineTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    fontChoice: FontChoice = FontChoice.PIXELIFY,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current

    if (!view.isInEditMode) {
        val window = (view.context as Activity).window
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = daylineTypography(fontChoice),
        content = content
    )
}
