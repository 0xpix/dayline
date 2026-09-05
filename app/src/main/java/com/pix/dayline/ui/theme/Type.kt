package com.pix.dayline.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.pix.dayline.R

private val provider = GoogleFont.Provider("com.google.android.gms.fonts", "com.google.android.gms", R.array.com_google_android_gms_fonts_certs)
private val pixelify = GoogleFont("Pixelify Sans", bestEffort = true)
val PixelifySansFamily = FontFamily(
    Font(pixelify, provider, FontWeight.Normal), Font(pixelify, provider, FontWeight.Medium), Font(pixelify, provider, FontWeight.SemiBold), Font(pixelify, provider, FontWeight.Bold)
)
val DaylineTypography = Typography(
    displayMedium = TextStyle(fontFamily=PixelifySansFamily,fontWeight=FontWeight.Bold,fontSize=34.sp,lineHeight=36.sp,letterSpacing=(-0.5).sp),
    headlineLarge = TextStyle(fontFamily=PixelifySansFamily,fontWeight=FontWeight.Bold,fontSize=30.sp,lineHeight=33.sp),
    titleMedium = TextStyle(fontFamily=PixelifySansFamily,fontWeight=FontWeight.SemiBold,fontSize=17.sp,lineHeight=21.sp),
    bodyLarge = TextStyle(fontFamily=PixelifySansFamily,fontWeight=FontWeight.Medium,fontSize=17.sp,lineHeight=22.sp),
    bodyMedium = TextStyle(fontFamily=PixelifySansFamily,fontWeight=FontWeight.Normal,fontSize=15.sp,lineHeight=20.sp),
    labelMedium = TextStyle(fontFamily=PixelifySansFamily,fontWeight=FontWeight.SemiBold,fontSize=13.sp,lineHeight=16.sp,letterSpacing=0.1.sp)
)
