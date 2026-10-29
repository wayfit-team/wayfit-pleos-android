package com.wayfit.wayfitpleos.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val colorScheme = darkColorScheme(
    primary = WayfitColor.Brand,
    onPrimary = WayfitColor.OnBrand,
    background = WayfitColor.Background,
    onBackground = WayfitColor.TextPrimary,
    surface = WayfitColor.Surface,
    onSurface = WayfitColor.TextPrimary,
    surfaceVariant = WayfitColor.SurfaceRaised,
    onSurfaceVariant = WayfitColor.TextSecondary,
    outline = WayfitColor.Border,
    error = WayfitColor.Rose,
)

/** 차량 화면은 다크 단일 테마. 라이트 모드는 두지 않는다 */
@Composable
fun WayfitTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colorScheme, typography = WayfitTypography) {
        Surface(color = WayfitColor.Background, contentColor = WayfitColor.TextPrimary, content = content)
    }
}
