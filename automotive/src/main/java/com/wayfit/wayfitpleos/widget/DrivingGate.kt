package com.wayfit.wayfitpleos.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocalParking
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wayfit.wayfitpleos.ui.theme.WayfitColor
import com.wayfit.wayfitpleos.ui.theme.WayfitSpace

/** 주행 중에는 정차 전용 화면을 가리고 안내만 보여준다 (Spec REQ-A17) */
@Composable
fun DrivingGate(parked: Boolean, content: @Composable () -> Unit) {
    if (parked) {
        content()
        return
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(WayfitSpace.XL),
        verticalArrangement = Arrangement.spacedBy(WayfitSpace.L, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconTile(Icons.Rounded.LocalParking, tint = WayfitColor.Amber, background = WayfitColor.AmberSoft, size = 112.dp)
        Text("정차 후 이용할 수 있어요", style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
        Text(
            "안전을 위해 주행 중에는\n추천과 방문 안내만 보여드려요",
            style = MaterialTheme.typography.bodyLarge,
            color = WayfitColor.TextSecondary,
            textAlign = TextAlign.Center,
        )
    }
}
