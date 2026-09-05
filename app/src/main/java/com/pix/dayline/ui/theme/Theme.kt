package com.pix.dayline.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.pix.dayline.data.FontChoice

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
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme ->
            dynamicDarkColorScheme(context)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !darkTheme ->
            dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }

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
        typography = daylineTypography(fontChoice)
    ) {
        CompositionLocalProvider(
            LocalContentColor provides colorScheme.onBackground
        ) {
            content()
        }
    }
}
