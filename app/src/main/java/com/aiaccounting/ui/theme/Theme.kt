package com.aiaccounting.ui.theme

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.aiaccounting.R

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

/**
 * 应用统一字体：HarmonyOS Sans SC（只打包 Regular 一个字重）。
 *
 * 字体授权明确禁止修改字形，所以不能做子集裁剪；为控制包体只带 Regular，
 * 标题/金额需要的粗体由系统的合成加粗（FontSynthesis）提供。
 */
val AppFontFamily = FontFamily(
    Font(R.font.harmonyos_sans_regular, FontWeight.Normal),
)

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

private val BaseTypography = Typography()

/** 所有排版角色都挂上品牌字体，避免任何 Material 组件回落到系统字体。 */
private val AppTypography: Typography = Typography(
    displayLarge = BaseTypography.displayLarge.copy(fontFamily = AppFontFamily),
    displayMedium = BaseTypography.displayMedium.copy(fontFamily = AppFontFamily),
    displaySmall = BaseTypography.displaySmall.copy(
        fontFamily = AppFontFamily,
        fontSize = 36.sp,
        lineHeight = 42.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp,
    ),
    headlineLarge = BaseTypography.headlineLarge.copy(fontFamily = AppFontFamily),
    headlineMedium = BaseTypography.headlineMedium.copy(
        fontFamily = AppFontFamily,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Bold,
    ),
    headlineSmall = BaseTypography.headlineSmall.copy(fontFamily = AppFontFamily),
    titleLarge = BaseTypography.titleLarge.copy(fontFamily = AppFontFamily),
    titleMedium = BaseTypography.titleMedium.copy(
        fontFamily = AppFontFamily,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    titleSmall = BaseTypography.titleSmall.copy(fontFamily = AppFontFamily),
    bodyLarge = BaseTypography.bodyLarge.copy(fontFamily = AppFontFamily),
    bodyMedium = BaseTypography.bodyMedium.copy(
        fontFamily = AppFontFamily,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = BaseTypography.bodySmall.copy(fontFamily = AppFontFamily),
    labelLarge = BaseTypography.labelLarge.copy(fontFamily = AppFontFamily),
    labelMedium = BaseTypography.labelMedium.copy(fontFamily = AppFontFamily),
    labelSmall = BaseTypography.labelSmall.copy(
        fontFamily = AppFontFamily,
        fontSize = 11.sp,
        lineHeight = 14.sp,
    ),
)

@Composable
fun AIAccountingTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = AppTypography,
    ) {
        // 裸 Text 走 LocalTextStyle，这里保证它们也带上品牌字体
        CompositionLocalProvider(LocalTextStyle provides AppTypography.bodyMedium) {
            content()
        }
    }
}
