package com.wayfit.wayfitpleos.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wayfit.wayfitpleos.ui.theme.WayfitColor
import com.wayfit.wayfitpleos.ui.theme.WayfitShape
import com.wayfit.wayfitpleos.ui.theme.WayfitSpace

/** 카드는 2단계만: 화면에 붙은 Flat, 사용자 결정이 필요한 Floating(추천·쿠폰 도착) */
enum class CardLevel { Flat, Floating }

@Composable
fun WayfitCard(
    modifier: Modifier = Modifier,
    level: CardLevel = CardLevel.Flat,
    contentPadding: PaddingValues = PaddingValues(WayfitSpace.L),
    content: @Composable BoxScope.() -> Unit,
) {
    val base = when (level) {
        CardLevel.Flat -> modifier
            .background(WayfitColor.Surface, WayfitShape.Card)
            .border(1.dp, WayfitColor.Border, WayfitShape.Card)
        CardLevel.Floating -> modifier
            .shadow(24.dp, WayfitShape.Card, ambientColor = WayfitColor.Brand, spotColor = WayfitColor.Brand)
            .background(WayfitColor.SurfaceRaised, WayfitShape.Card)
            .border(1.dp, WayfitColor.Brand.copy(alpha = 0.35f), WayfitShape.Card)
    }
    Box(base.padding(contentPadding), content = content)
}

@Composable
fun StatusChip(text: String, color: Color, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    Row(
        modifier = modifier
            .background(color.copy(alpha = 0.14f), WayfitShape.Pill)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
        Text(text, color = color, style = MaterialTheme.typography.labelMedium)
    }
}

/** 아이콘 + 짧은 정보 (예: "1.2km 앞", "3분 우회") */
@Composable
fun InfoChip(icon: ImageVector, text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(WayfitColor.SurfaceSunken, WayfitShape.Pill)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = WayfitColor.TextSecondary, modifier = Modifier.size(20.dp))
        Text(text, color = WayfitColor.TextSecondary, style = MaterialTheme.typography.labelMedium)
    }
}

/** 카테고리 이모지를 브랜드 톤 타일에 담는다 */
@Composable
fun EmojiTile(emoji: String, size: Dp = 64.dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .background(WayfitColor.BrandSoft, WayfitShape.Small),
        contentAlignment = Alignment.Center,
    ) {
        Text(emoji, style = MaterialTheme.typography.titleLarge.copy(fontSize = (size.value * 0.48f).sp))
    }
}

@Composable
fun IconTile(icon: ImageVector, tint: Color, background: Color, size: Dp = 64.dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .background(background, WayfitShape.Small),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(size * 0.5f))
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier, color = WayfitColor.TextTertiary, style = MaterialTheme.typography.labelMedium)
}
