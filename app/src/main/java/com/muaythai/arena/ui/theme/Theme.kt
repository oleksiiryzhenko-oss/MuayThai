package com.muaythai.arena.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** The Muay Thai palette: red and white first, with near-black for text and gold kept for elite ranks only. */
object ArenaColors {
    val Black = Color(0xFF08080A)
    val Charcoal = Color(0xFF121216)
    val Card = Color(0xFF1A1A20)
    val CardHigh = Color(0xFF25252D)
    val Line = Color(0xFF32323C)
    val Crimson = Color(0xFFD4102F)
    val CrimsonDeep = Color(0xFF7A0A1C)
    val White = Color(0xFFFFFFFF)
    val Ink = Color(0xFF1A1114)
    val Gray = Color(0xFF9C9CA8)
    val Gold = Color(0xFFD4AF37)
    val Win = Color(0xFF2FBF71)
    val Warn = Color(0xFFE0A030)
}

private val DarkScheme = darkColorScheme(
    primary = ArenaColors.Crimson,
    onPrimary = ArenaColors.White,
    primaryContainer = ArenaColors.CrimsonDeep,
    onPrimaryContainer = ArenaColors.White,
    secondary = ArenaColors.Gray,
    onSecondary = ArenaColors.Black,
    tertiary = ArenaColors.Gold,
    onTertiary = ArenaColors.Black,
    background = ArenaColors.Black,
    onBackground = ArenaColors.White,
    surface = ArenaColors.Card,
    onSurface = ArenaColors.White,
    surfaceVariant = ArenaColors.CardHigh,
    onSurfaceVariant = ArenaColors.Gray,
    outline = ArenaColors.Line,
    outlineVariant = ArenaColors.Line,
    error = Color(0xFFFF9A9A),
    onError = ArenaColors.Black
)

/** The default look: white surfaces, red for everything that matters. */
private val LightScheme = lightColorScheme(
    primary = ArenaColors.Crimson,
    onPrimary = ArenaColors.White,
    primaryContainer = Color(0xFFFFE3E7),
    onPrimaryContainer = Color(0xFF5A0010),
    secondary = Color(0xFF6B5A5D),
    onSecondary = ArenaColors.White,
    tertiary = Color(0xFF9A7B12),
    onTertiary = ArenaColors.White,
    background = ArenaColors.White,
    onBackground = ArenaColors.Ink,
    surface = ArenaColors.White,
    onSurface = ArenaColors.Ink,
    surfaceVariant = Color(0xFFF8ECEE),
    onSurfaceVariant = Color(0xFF6B5A5D),
    outline = Color(0xFFE9CFD4),
    outlineVariant = Color(0xFFE9CFD4),
    // Errors are a deep maroon so that "wrong" never looks like the brand red used for "selected".
    error = ArenaColors.CrimsonDeep,
    onError = ArenaColors.White
)

private val Display = FontFamily.SansSerif

private val ArenaTypography = Typography(
    displayLarge = TextStyle(fontFamily = Display, fontWeight = FontWeight.Black, fontSize = 56.sp, lineHeight = 58.sp, letterSpacing = (-1).sp),
    displayMedium = TextStyle(fontFamily = Display, fontWeight = FontWeight.Black, fontSize = 44.sp, lineHeight = 46.sp, letterSpacing = (-0.5).sp),
    displaySmall = TextStyle(fontFamily = Display, fontWeight = FontWeight.Black, fontSize = 34.sp, lineHeight = 38.sp),
    headlineLarge = TextStyle(fontFamily = Display, fontWeight = FontWeight.Black, fontSize = 30.sp, lineHeight = 34.sp),
    headlineMedium = TextStyle(fontFamily = Display, fontWeight = FontWeight.Black, fontSize = 26.sp, lineHeight = 30.sp),
    headlineSmall = TextStyle(fontFamily = Display, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, lineHeight = 26.sp),
    titleLarge = TextStyle(fontFamily = Display, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp, lineHeight = 24.sp),
    titleMedium = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 21.sp),
    titleSmall = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 19.sp),
    bodyLarge = TextStyle(fontFamily = Display, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontFamily = Display, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = Display, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontFamily = Display, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, lineHeight = 18.sp, letterSpacing = 1.2.sp),
    labelMedium = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 1.4.sp),
    labelSmall = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 10.sp, lineHeight = 14.sp, letterSpacing = 1.4.sp)
)

/** Sharp corners throughout: fight cards, not pillows. */
private val ArenaShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(2.dp),
    medium = RoundedCornerShape(4.dp),
    large = RoundedCornerShape(6.dp),
    extraLarge = RoundedCornerShape(8.dp)
)

@Composable
fun ArenaTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) DarkScheme else LightScheme,
        typography = ArenaTypography,
        shapes = ArenaShapes,
        content = content
    )
}
