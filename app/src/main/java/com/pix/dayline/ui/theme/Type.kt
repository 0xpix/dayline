package com.pix.dayline.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.pix.dayline.data.FontChoice

private fun downloadableFamily(name: String): FontFamily {
    val googleFont = GoogleFont(name, bestEffort = true)
    return FontFamily(
        Font(googleFont = googleFont, weight = FontWeight.Normal),
        Font(googleFont = googleFont, weight = FontWeight.Medium),
        Font(googleFont = googleFont, weight = FontWeight.SemiBold),
        Font(googleFont = googleFont, weight = FontWeight.Bold),
    )
}

private val PixelifySansFamily = downloadableFamily("Pixelify Sans")
private val GeistFamily = downloadableFamily("Geist")
private val InterFamily = downloadableFamily("Inter")
private val SpaceGroteskFamily = downloadableFamily("Space Grotesk")
private val IbmPlexMonoFamily = downloadableFamily("IBM Plex Mono")

private fun familyFor(choice: FontChoice): FontFamily = when (choice) {
    FontChoice.SYSTEM -> FontFamily.SansSerif
    FontChoice.GEIST -> GeistFamily
    FontChoice.INTER -> InterFamily
    FontChoice.SPACE_GROTESK -> SpaceGroteskFamily
    FontChoice.IBM_PLEX_MONO -> IbmPlexMonoFamily
    FontChoice.PIXELIFY -> PixelifySansFamily
    // Migration path for users who previously selected Geist Pixel.
    FontChoice.GEIST_PIXEL -> GeistFamily
}

fun daylineTypography(
    choice: FontChoice
): Typography {
    val family = familyFor(choice)

    return Typography(
        displayLarge = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Bold,
            fontSize = 45.sp,
            lineHeight = 48.sp,
            letterSpacing = (-0.6).sp,
        ),
        displayMedium = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Bold,
            fontSize = 34.sp,
            lineHeight = 36.sp,
            letterSpacing = (-0.5).sp,
        ),
        displaySmall = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Bold,
            fontSize = 31.sp,
            lineHeight = 34.sp,
        ),
        headlineLarge = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Bold,
            fontSize = 30.sp,
            lineHeight = 33.sp,
        ),
        headlineMedium = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.SemiBold,
            fontSize = 26.sp,
            lineHeight = 30.sp,
        ),
        headlineSmall = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.SemiBold,
            fontSize = 23.sp,
            lineHeight = 28.sp,
        ),
        titleLarge = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.SemiBold,
            fontSize = 21.sp,
            lineHeight = 26.sp,
        ),
        titleMedium = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.SemiBold,
            fontSize = 17.sp,
            lineHeight = 21.sp,
        ),
        titleSmall = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            lineHeight = 19.sp,
        ),
        bodyLarge = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Medium,
            fontSize = 17.sp,
            lineHeight = 22.sp,
        ),
        bodyMedium = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp,
            lineHeight = 20.sp,
        ),
        bodySmall = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            lineHeight = 18.sp,
        ),
        labelLarge = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            letterSpacing = 0.1.sp,
        ),
        labelMedium = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.1.sp,
        ),
        labelSmall = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            letterSpacing = 0.1.sp,
        ),
    )
}
