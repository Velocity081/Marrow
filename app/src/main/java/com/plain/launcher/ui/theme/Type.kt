package com.plain.launcher.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.plain.launcher.R

import com.plain.launcher.data.model.ClockFont
import com.plain.launcher.data.model.LauncherFont

val GeistFamily = FontFamily(
    Font(R.font.geist_regular, FontWeight.Normal),
    Font(R.font.geist_regular, FontWeight.Medium),
    Font(R.font.geist_regular, FontWeight.Bold)
)

val GeistMonoFamily = FontFamily(
    Font(R.font.geist_mono_regular, FontWeight.Normal),
    Font(R.font.geist_mono_medium, FontWeight.Medium),
    Font(R.font.geist_mono_bold, FontWeight.Bold)
)

val FiraMonoFamily = FontFamily(
    Font(R.font.fira_mono_regular, FontWeight.Normal),
    Font(R.font.fira_mono_medium, FontWeight.Medium),
    Font(R.font.fira_mono_bold, FontWeight.Bold)
)

val DotoFamily = FontFamily(
    Font(R.font.doto_regular, FontWeight.Normal)
)

val FrauncesFamily = FontFamily(
    Font(R.font.fraunces_regular, FontWeight.Normal)
)

val UnboundedFamily = FontFamily(
    Font(R.font.unbounded_regular, FontWeight.Normal)
)

val BigShouldersDisplayFamily = FontFamily(
    Font(R.font.big_shoulders_display_regular, FontWeight.Normal)
)

fun getAppFontFamily(font: LauncherFont): FontFamily {
    return when (font) {
        LauncherFont.GEIST -> GeistFamily
        LauncherFont.GEIST_MONO -> GeistMonoFamily
        LauncherFont.FIRA_MONO -> FiraMonoFamily
    }
}

fun getClockFontFamily(font: ClockFont): FontFamily {
    return when (font) {
        ClockFont.GEIST -> GeistFamily
        ClockFont.GEIST_MONO -> GeistMonoFamily
        ClockFont.FIRA_MONO -> FiraMonoFamily
        ClockFont.DOTO -> DotoFamily
        ClockFont.FRAUNCES -> FrauncesFamily
        ClockFont.UNBOUNDED -> UnboundedFamily
        ClockFont.BIG_SHOULDERS -> BigShouldersDisplayFamily
    }
}

val PlainTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = GeistFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 57.sp,
        lineHeight = 64.sp
    ),
    displayMedium = TextStyle(
        fontFamily = GeistFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 45.sp,
        lineHeight = 52.sp
    ),
    displaySmall = TextStyle(
        fontFamily = GeistFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp,
        lineHeight = 44.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = GeistFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 40.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = GeistFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
        lineHeight = 36.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = GeistFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),
    titleLarge = TextStyle(
        fontFamily = GeistFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontFamily = GeistFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    titleSmall = TextStyle(
        fontFamily = GeistFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = GeistFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = GeistFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = GeistFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = GeistFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = GeistFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = GeistFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp
    )
)
