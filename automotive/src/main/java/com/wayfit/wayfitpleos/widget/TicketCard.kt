package com.wayfit.wayfitpleos.widget

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.wayfit.wayfitpleos.model.Coupon
import com.wayfit.wayfitpleos.ui.theme.WayfitColor
import com.wayfit.wayfitpleos.ui.theme.WayfitSpace

/** 오른쪽 스텁 앞에 위·아래 반원 홈이 파인 쿠폰 모양 */
private class TicketShape(private val stubWidthPx: Float, private val notchPx: Float, private val cornerPx: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val body = Path().apply {
            addRoundRect(RoundRect(Rect(Offset.Zero, size), CornerRadius(cornerPx)))
        }
        val x = size.width - stubWidthPx
        val notches = Path().apply {
            addOval(Rect(Offset(x, 0f), notchPx))
            addOval(Rect(Offset(x, size.height), notchPx))
        }
        return Outline.Generic(Path.combine(PathOperation.Difference, body, notches))
    }
}

@Composable
fun TicketCard(coupon: Coupon, modifier: Modifier = Modifier, highlighted: Boolean = false) {
    val dimmed = coupon.redeemed
    val stubWidth = 168.dp
    val shape = TicketShape(stubWidthPx = with(androidx.compose.ui.platform.LocalDensity.current) { stubWidth.toPx() },
        notchPx = with(androidx.compose.ui.platform.LocalDensity.current) { 14.dp.toPx() },
        cornerPx = with(androidx.compose.ui.platform.LocalDensity.current) { 24.dp.toPx() })
    val accent = if (dimmed) WayfitColor.TextTertiary else WayfitColor.Brand

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .background(if (highlighted) WayfitColor.SurfaceRaised else WayfitColor.Surface, shape)
            .border(1.dp, if (highlighted) WayfitColor.Brand.copy(alpha = 0.4f) else WayfitColor.Border, shape),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(WayfitSpace.L),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(coupon.storeName, color = WayfitColor.TextSecondary, style = MaterialTheme.typography.labelMedium)
            Text(
                coupon.benefit,
                color = if (dimmed) WayfitColor.TextSecondary else WayfitColor.TextPrimary,
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                when {
                    dimmed -> "사용 완료"
                    coupon.expiresInDays <= 1 -> "오늘까지 사용"
                    else -> "${coupon.expiresInDays}일 남음"
                },
                color = if (!dimmed && coupon.expiresInDays <= 1) WayfitColor.Rose else WayfitColor.TextTertiary,
                style = MaterialTheme.typography.labelMedium,
            )
        }
        DashedDivider(color = WayfitColor.Border)
        Box(
            modifier = Modifier
                .width(stubWidth)
                .fillMaxHeight()
                .padding(vertical = WayfitSpace.L),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("쿠폰 코드", color = WayfitColor.TextTertiary, style = MaterialTheme.typography.labelMedium)
                Text(
                    coupon.code.chunked(3).joinToString(" "),
                    color = accent,
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Monospace),
                )
            }
        }
    }
}

@Composable
private fun DashedDivider(color: Color) {
    Canvas(
        modifier = Modifier
            .width(2.dp)
            .fillMaxHeight()
            .padding(vertical = 18.dp),
    ) {
        drawLine(
            color = color,
            start = Offset(size.width / 2, 0f),
            end = Offset(size.width / 2, size.height),
            strokeWidth = size.width,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f)),
        )
    }
}
