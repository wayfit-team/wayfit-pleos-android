package com.wayfit.wayfitpleos.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.sp

object WayfitColors {
    val Background = Color(0xFF0E1013)
    val Surface = Color(0xFF171A1F)
    val SurfaceHigh = Color(0xFF22262D)
    val Outline = Color(0xFF353A43)
    val Mint = Color(0xFF34E0A1)
    val OnMint = Color(0xFF04261A)
    val TextPrimary = Color(0xFFF2F4F7)
    val TextSecondary = Color(0xFF9AA3AE)
    val Amber = Color(0xFFFFB547)
    val Danger = Color(0xFFFF6B6B)
}

private val colorScheme = darkColorScheme(
    primary = WayfitColors.Mint,
    onPrimary = WayfitColors.OnMint,
    background = WayfitColors.Background,
    onBackground = WayfitColors.TextPrimary,
    surface = WayfitColors.Surface,
    onSurface = WayfitColors.TextPrimary,
    surfaceVariant = WayfitColors.SurfaceHigh,
    onSurfaceVariant = WayfitColors.TextSecondary,
    outline = WayfitColors.Outline,
    error = WayfitColors.Danger,
)

// 줄 길이를 고르게 (어절 단위 줄바꿈 자체는 ui.components.Text 가 처리)
private val Base = TextStyle(lineBreak = LineBreak.Paragraph.copy(strategy = LineBreak.Strategy.Balanced))

// 차량 화면 기준: 운전자가 한눈에 읽을 수 있도록 일반 앱보다 크게
private val typography = Typography(
    displayLarge = Base.copy(fontSize = 56.sp, fontWeight = FontWeight.Bold, lineHeight = 64.sp),
    displayMedium = Base.copy(fontSize = 44.sp, fontWeight = FontWeight.Bold, lineHeight = 52.sp),
    headlineLarge = Base.copy(fontSize = 36.sp, fontWeight = FontWeight.Bold, lineHeight = 44.sp),
    headlineMedium = Base.copy(fontSize = 30.sp, fontWeight = FontWeight.SemiBold, lineHeight = 38.sp),
    titleLarge = Base.copy(fontSize = 26.sp, fontWeight = FontWeight.SemiBold, lineHeight = 34.sp),
    bodyLarge = Base.copy(fontSize = 24.sp, lineHeight = 32.sp),
    bodyMedium = Base.copy(fontSize = 20.sp, lineHeight = 28.sp),
    labelLarge = Base.copy(fontSize = 26.sp, fontWeight = FontWeight.Bold),
    labelMedium = Base.copy(fontSize = 18.sp, fontWeight = FontWeight.Medium),
)

@Composable
fun WayfitTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colorScheme, typography = typography) {
        Surface(color = WayfitColors.Background, contentColor = WayfitColors.TextPrimary, content = content)
    }
}
