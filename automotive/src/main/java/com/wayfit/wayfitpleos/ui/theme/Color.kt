package com.wayfit.wayfitpleos.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 색 토큰. 규칙은 docs/DESIGN_SYSTEM.md
 * - 브랜드는 민트 하나 (주요 동작·활성·진행)
 * - 중립은 Ink 계열 하나
 * - 위험은 Rose 하나, 주의(주행 중·장시간 운전)는 Amber 하나 — 의미가 정해진 자리에서만
 */
object WayfitColor {
    // Brand
    val Brand = Color(0xFF2FD8A3)
    val BrandStrong = Color(0xFF1DB386)
    val BrandSoft = Color(0x242FD8A3)
    val OnBrand = Color(0xFF032A1C)

    // Ink (중립) — 어두운 차량 화면 기준
    val Background = Color(0xFF0B0D10)
    val Surface = Color(0xFF15181D)
    val SurfaceRaised = Color(0xFF1D2128)
    val SurfaceSunken = Color(0xFF101318)
    val Border = Color(0xFF2A2F37)
    val TextPrimary = Color(0xFFF3F5F7)
    val TextSecondary = Color(0xFFA4ACB7)
    val TextTertiary = Color(0xFF6C7480)

    // 의미 색
    val Amber = Color(0xFFFFB547)
    val AmberSoft = Color(0x24FFB547)
    val Rose = Color(0xFFFF6B7A)
}
