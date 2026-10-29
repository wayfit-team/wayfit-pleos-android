package com.wayfit.wayfitpleos.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.sp

private val Base = TextStyle(lineBreak = LineBreak.Paragraph.copy(strategy = LineBreak.Strategy.Balanced))

/**
 * 타이포 6단계. 운전자가 한눈에 읽도록 일반 앱보다 크게.
 * display = 혜택 문구, headline = 화면 제목, title = 매장명, body = 본문, label = 버튼, meta = 보조 정보
 */
val WayfitTypography = Typography(
    displayMedium = Base.copy(fontSize = 46.sp, lineHeight = 54.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
    headlineLarge = Base.copy(fontSize = 34.sp, lineHeight = 42.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
    titleLarge = Base.copy(fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = Base.copy(fontSize = 22.sp, lineHeight = 30.sp),
    labelLarge = Base.copy(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold),
    labelMedium = Base.copy(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium),
)
