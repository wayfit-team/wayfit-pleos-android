package com.wayfit.wayfitpleos.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/** 곡률은 4단계만 쓴다 */
object WayfitShape {
    val Small = RoundedCornerShape(14.dp)   // 칩 안 타일, 코드 칸
    val Button = RoundedCornerShape(20.dp)  // 버튼, 내비 탭
    val Card = RoundedCornerShape(28.dp)    // 카드, 쿠폰
    val Pill = RoundedCornerShape(50)       // 상태 칩
}

/** 8dp 격자 */
object WayfitSpace {
    val XS = 8.dp
    val S = 12.dp
    val M = 16.dp
    val L = 24.dp
    val XL = 32.dp
    val XXL = 48.dp
}

object WayfitSize {
    /** AAOS 최소 터치 영역 76dp보다 크게 */
    val ButtonHeight = 96.dp
    val NavHeight = 80.dp
    val ContentMaxWidth = 760.dp
    /** 이 폭 이상이면 경로 패널을 옆에 띄운다 (Pleos 가로 2칸) */
    val WideBreakpoint = 1100.dp
}
