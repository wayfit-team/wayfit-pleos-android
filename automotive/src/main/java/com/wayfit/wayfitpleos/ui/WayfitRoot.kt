package com.wayfit.wayfitpleos.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.wayfit.wayfitpleos.ui.components.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wayfit.wayfitpleos.BuildConfig
import com.wayfit.wayfitpleos.WayfitViewModel
import com.wayfit.wayfitpleos.model.PairState
import com.wayfit.wayfitpleos.model.Tab
import com.wayfit.wayfitpleos.model.UiState
import com.wayfit.wayfitpleos.ui.components.DrivingGate
import com.wayfit.wayfitpleos.ui.components.StatusChip
import com.wayfit.wayfitpleos.ui.theme.WayfitColors

@Composable
fun WayfitRoot(vm: WayfitViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WayfitColors.Background)
            .safeDrawingPadding(),
    ) {
        TopBar(state)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.TopCenter,
        ) {
            // Pleos 앱 영역 1칸(세로)을 기준으로, 넓어져도 가독 폭 유지
            Box(Modifier.widthIn(max = 760.dp).fillMaxSize()) {
                when (state.tab) {
                    Tab.DRIVE -> DriveScreen(
                        phase = state.phase,
                        route = state.route,
                        tripRecommendCount = state.tripRecommendCount,
                        phonePaired = state.pair is PairState.Paired,
                        listening = state.listening,
                        onAccept = vm::accept,
                        onDecline = vm::decline,
                        onCancelVisit = vm::cancelVisit,
                        onSendToPhone = { vm.selectTab(Tab.PAIR) },
                        onDismissResult = vm::dismissResult,
                    )
                    Tab.COUPONS -> DrivingGate(state.driving) {
                        CouponScreen(state.coupons, state.pair, onPairClick = { vm.selectTab(Tab.PAIR) })
                    }
                    Tab.PAIR -> DrivingGate(state.driving) {
                        PairScreen(state.pair, onNewCode = vm::startPairing)
                    }
                }
            }
        }
        BottomNav(state, vm::selectTab)
        if (BuildConfig.DEBUG) DemoPanel(vm, state)
    }
}

@Composable
private fun TopBar(state: UiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "웨이핏",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = WayfitColors.Mint,
        )
        if (state.driving) {
            StatusChip("주행 중", WayfitColors.Amber)
        } else {
            StatusChip("정차 중", WayfitColors.Mint)
        }
    }
}

@Composable
private fun BottomNav(state: UiState, onSelect: (Tab) -> Unit) {
    val activeCoupons = state.coupons.count { !it.redeemed }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(WayfitColors.Surface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        NavItem("홈", Icons.Filled.Home, state.tab == Tab.DRIVE, locked = false, Modifier.weight(1f)) {
            onSelect(Tab.DRIVE)
        }
        NavItem("쿠폰 $activeCoupons", Icons.Filled.Star, state.tab == Tab.COUPONS, state.driving, Modifier.weight(1f)) {
            onSelect(Tab.COUPONS)
        }
        NavItem("폰 연결", Icons.Filled.Phone, state.tab == Tab.PAIR, state.driving, Modifier.weight(1f)) {
            onSelect(Tab.PAIR)
        }
    }
}

@Composable
private fun NavItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    locked: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val color = if (selected) WayfitColors.Mint else WayfitColors.TextSecondary
    Row(
        modifier = modifier
            .heightIn(min = 80.dp)
            .background(
                if (selected) WayfitColors.Mint.copy(alpha = 0.12f) else WayfitColors.Surface,
                RoundedCornerShape(20.dp),
            )
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(if (locked) Icons.Filled.Lock else icon, null, tint = color, modifier = Modifier.size(30.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = color)
    }
}

/** PoC 시연용 컨트롤. 실제 앱에서는 Pleos SDK 이벤트가 이 역할을 한다. debug 빌드에만 노출 */
@Composable
private fun DemoPanel(vm: WayfitViewModel, state: UiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(WayfitColors.Background)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("DEMO", style = MaterialTheme.typography.labelMedium, color = WayfitColors.Amber)
        Text(sdkSummary(state), style = MaterialTheme.typography.labelMedium, color = WayfitColors.TextSecondary)
        DemoChip(if (state.driving) "정차로 전환" else "주행으로 전환", vm::demoToggleDriving)
        DemoChip("추천 띄우기", vm::demoRecommend)
        DemoChip("매장 도착·주차", vm::demoArriveAndPark)
        DemoChip("폰 연결됨", vm::demoPhonePaired)
        DemoChip(if (state.route.drivingMinutes >= 120) "장시간 운전 OFF" else "장시간 운전 ON", vm::demoToggleLongDrive)
    }
}

@Composable
private fun DemoChip(label: String, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        colors = AssistChipDefaults.assistChipColors(labelColor = WayfitColors.TextPrimary),
    )
}

private fun sdkSummary(state: UiState): String = with(state.sdk) {
    fun mark(ok: Boolean) = if (ok) "✓" else "✗"
    "SDK 차량${mark(vehicle)} 내비${mark(navi)} 위치${mark(location)} TTS${mark(tts)} STT${mark(stt)}"
}
