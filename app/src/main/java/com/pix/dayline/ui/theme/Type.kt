package com.pix.dayline.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp

private val pixelifySans = GoogleFont("Pixelify Sans")

val PixelifySansFamily = FontFamily(
    Font(googleFont = pixelifySans, weight = FontWeight.Normal),
    Font(googleFont = pixelifySans, weight = FontWeight.Medium),
    Font(googleFont = pixelifySans, weight = FontWeight.SemiBold),
    Font(googleFont = pixelifySans, weight = FontWeight.Bold),
)

val DaylineTypography = Typography(
    displayMedium = TextStyle(
        fontFamily = PixelifySansFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = PixelifySansFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 33.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = PixelifySansFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 21.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = PixelifySansFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 17.sp,
        lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = PixelifySansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = PixelifySansFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.1.sp,
    ),
)
