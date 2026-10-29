package com.wayfit.wayfitpleos.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import com.wayfit.wayfitpleos.ui.theme.WayfitColor
import com.wayfit.wayfitpleos.ui.theme.WayfitShape
import com.wayfit.wayfitpleos.ui.theme.WayfitSize
import com.wayfit.wayfitpleos.ui.theme.WayfitSpace
import com.wayfit.wayfitpleos.widget.Text

@Composable
fun BottomNavigationBar(
    currentDestination: NavDestination?,
    parked: Boolean,
    couponCount: Int,
    onSelect: (Route) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(WayfitColor.Surface)
            .padding(horizontal = WayfitSpace.M, vertical = WayfitSpace.S),
        horizontalArrangement = Arrangement.spacedBy(WayfitSpace.S),
    ) {
        BottomNavItem.items.forEach { item ->
            val current = currentDestination?.route
            // 폰 연결 화면은 설정 아래에 있으므로 설정 탭을 선택 상태로 둔다
            val selected = current == item.route.route || (item is BottomNavItem.Settings && current == Route.Pair.route)
            val locked = item.parkedOnly && !parked
            NavTab(
                item = item,
                label = if (item is BottomNavItem.Coupons && couponCount > 0) "${item.label} $couponCount" else item.label,
                selected = selected,
                locked = locked,
                modifier = Modifier.weight(1f),
                onClick = { onSelect(item.route) },
            )
        }
    }
}

@Composable
private fun NavTab(
    item: BottomNavItem,
    label: String,
    selected: Boolean,
    locked: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val container by animateColorAsState(if (selected) WayfitColor.BrandSoft else WayfitColor.Surface, label = "tab")
    val content = when {
        selected -> WayfitColor.Brand
        locked -> WayfitColor.TextTertiary
        else -> WayfitColor.TextSecondary
    }
    Box(
        modifier = modifier
            .heightIn(min = WayfitSize.NavHeight)
            .clip(WayfitShape.Button)
            .background(container)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (locked) Icons.Rounded.Lock else item.icon, null, tint = content, modifier = Modifier.size(30.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = content)
        }
    }
}
