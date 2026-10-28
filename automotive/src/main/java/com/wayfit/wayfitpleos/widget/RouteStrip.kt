package com.wayfit.wayfitpleos.widget

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.wayfit.wayfitpleos.ui.theme.WayfitColor

/**
 * 현재 위치 → (들를 매장) → 목적지를 한 줄로 그린 경로 그림. 반복 애니메이션 없음 (주행 중 주의 분산 방지)
 * @param stopFraction 들를 매장 위치(0~1). null 이면 표시하지 않음
 */
@Composable
fun RouteStrip(modifier: Modifier = Modifier, stopFraction: Float? = null, progress: Float = 0.08f) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp),
    ) {
        val y = size.height / 2
        val start = 14.dp.toPx()
        val end = size.width - 14.dp.toPx()
        val stroke = 6.dp.toPx()
        val passedX = start + (end - start) * progress

        // 남은 길
        drawLine(
            color = WayfitColor.Border,
            start = Offset(passedX, y),
            end = Offset(end, y),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 14f)),
        )
        // 지나온 길
        drawLine(WayfitColor.Brand, Offset(start, y), Offset(passedX, y), stroke, StrokeCap.Round)

        // 들를 매장
        if (stopFraction != null) {
            val x = start + (end - start) * stopFraction
            drawCircle(WayfitColor.Background, radius = 13.dp.toPx(), center = Offset(x, y))
            drawCircle(WayfitColor.Amber, radius = 9.dp.toPx(), center = Offset(x, y))
        }
        // 현재 위치
        drawCircle(WayfitColor.Brand.copy(alpha = 0.25f), radius = 14.dp.toPx(), center = Offset(passedX, y))
        drawCircle(WayfitColor.Brand, radius = 8.dp.toPx(), center = Offset(passedX, y))
        // 목적지
        drawCircle(WayfitColor.TextPrimary, radius = 11.dp.toPx(), center = Offset(end, y), style = Stroke(4.dp.toPx()))
    }
}
