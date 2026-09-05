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
private val GeistPixelFamily = downloadableFamily("Geist Pixel")

private fun familyFor(choice: FontChoice): FontFamily = when (choice) {
    FontChoice.PIXELIFY -> PixelifySansFamily
    FontChoice.GEIST -> GeistFamily
    FontChoice.GEIST_PIXEL -> GeistPixelFamily
    FontChoice.SYSTEM -> FontFamily.SansSerif
}

fun daylineTypography(choice: FontChoice): Typography {
    val family = familyFor(choice)
    return Typography(
        displayMedium = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Bold,
            fontSize = 34.sp,
            lineHeight = 36.sp,
            letterSpacing = (-0.5).sp,
        ),
        headlineLarge = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Bold,
            fontSize = 30.sp,
            lineHeight = 33.sp,
        ),
        titleMedium = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.SemiBold,
            fontSize = 17.sp,
            lineHeight = 21.sp,
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
        labelMedium = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.1.sp,
        ),
    )
}
