package com.aiaccounting.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ── 品牌色：参考图是「浅蓝底 + 纯白卡片 + 亮蓝强调」 ──────────────────────
val BrandBlue = Color(0xFF2F6BFF)
val BrandBlueDeep = Color(0xFF1B4BD8)
val BrandBlueSoft = Color(0xFFE7F0FF)
val BrandBlueSofter = Color(0xFFF3F7FF)
val PageBg = Color(0xFFEFF4FF)
val CardBg = Color(0xFFFFFFFF)
val TextMain = Color(0xFF151A23)
val TextSub = Color(0xFF98A2B3)
val TextFaint = Color(0xFFC3CAD6)
val IncomeGreen = Color(0xFF12B76A)
val AlertRed = Color(0xFFF04438)
val HairLine = Color(0xFFEFF2F7)
val TrackBg = Color(0xFFEDF2FA)

private val LightColors = lightColorScheme(
    primary = BrandBlue,
    onPrimary = Color.White,
    primaryContainer = BrandBlueSoft,
    onPrimaryContainer = BrandBlueDeep,
    secondary = BrandBlueDeep,
    onSecondary = Color.White,
    background = PageBg,
    onBackground = TextMain,
    surface = CardBg,
    onSurface = TextMain,
    surfaceVariant = BrandBlueSofter,
    onSurfaceVariant = TextSub,
    outline = HairLine,
    error = AlertRed,
    onError = Color.White,
)

private val AppTypography = Typography(
    displaySmall = TextStyle(
        fontSize = 36.sp,
        lineHeight = 42.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp,
    ),
    headlineMedium = TextStyle(
        fontSize = 22.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Bold,
    ),
    titleMedium = TextStyle(
        fontSize = 16.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Normal,
    ),
    labelSmall = TextStyle(
        fontSize = 11.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.Normal,
    ),
)

@Composable
fun AIAccountingTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = AppTypography,
        content = content,
    )
}
