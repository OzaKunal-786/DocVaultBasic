package com.docvaultbasic.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.docvaultbasic.R

val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val MontserratFont = GoogleFont("Montserrat")
val RobotoFont = GoogleFont("Roboto")

val MontserratFamily = FontFamily(Font(googleFont = MontserratFont, fontProvider = provider))
val RobotoFamily = FontFamily(Font(googleFont = RobotoFont, fontProvider = provider))

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = MontserratFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp, // Reduced from 57
        lineHeight = 56.sp,
    ),
    displayMedium = TextStyle(
        fontFamily = MontserratFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp, // Reduced from 45
        lineHeight = 44.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = MontserratFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp, // Reduced from 32
        lineHeight = 36.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = MontserratFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp, // Reduced from 28
        lineHeight = 32.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = MontserratFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp, // Reduced from 22
        lineHeight = 28.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = RobotoFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp, // Reduced from 16
        lineHeight = 22.sp,
        letterSpacing = 0.4.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = RobotoFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp, // Reduced from 14
        lineHeight = 18.sp,
        letterSpacing = 0.2.sp,
    )
)
