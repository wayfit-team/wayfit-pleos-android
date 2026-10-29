package com.wayfit.wayfitpleos.screen.main

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.LocalParking
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.wayfit.wayfitpleos.BuildConfig
import com.wayfit.wayfitpleos.model.SdkStatus
import com.wayfit.wayfitpleos.navigation.BottomNavigationBar
import com.wayfit.wayfitpleos.navigation.MainNavHost
import com.wayfit.wayfitpleos.navigation.navigateToTab
import com.wayfit.wayfitpleos.ui.theme.WayfitColor
import com.wayfit.wayfitpleos.ui.theme.WayfitShape
import com.wayfit.wayfitpleos.ui.theme.WayfitSpace
import com.wayfit.wayfitpleos.widget.StatusChip
import com.wayfit.wayfitpleos.widget.Text
import kotlinx.coroutines.launch

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val snackState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier.safeDrawingPadding(),
        containerColor = WayfitColor.Background,
        topBar = { TopBar(state.parked) },
        bottomBar = {
            Column {
                BottomNavigationBar(
                    currentDestination = backStackEntry?.destination,
                    parked = state.parked,
                    couponCount = state.activeCouponCount,
                    onSelect = navController::navigateToTab,
                )
                if (BuildConfig.DEBUG) DemoPanel(viewModel, state)
            }
        },
        snackbarHost = {
            SnackbarHost(snackState) { data ->
                Snackbar(containerColor = WayfitColor.SurfaceRaised, contentColor = WayfitColor.TextPrimary, shape = WayfitShape.Button) {
                    Text(data.visuals.message, style = MaterialTheme.typography.bodyLarge)
                }
            }
        },
    ) { innerPadding ->
        MainNavHost(
            navController = navController,
            innerPadding = innerPadding,
            parked = state.parked,
            onShowSnackbar = { message -> scope.launch { snackState.showSnackbar(message) } },
        )
    }
}

@Composable
private fun TopBar(parked: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = WayfitSpace.XL, vertical = WayfitSpace.M),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(WayfitSpace.S)) {
            BrandMark()
            Text(
                "웨이핏",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
            )
        }
        if (parked) {
            StatusChip("정차 중", WayfitColor.Brand, icon = Icons.Rounded.LocalParking)
        } else {
            StatusChip("주행 중", WayfitColor.Amber, icon = Icons.Rounded.DirectionsCar)
        }
    }
}

/** 로고 자리: 길(W) 위에 들르는 점 하나 */
@Composable
private fun BrandMark() {
    Box(
        modifier = Modifier
            .size(44.dp)
            .background(WayfitColor.Brand, WayfitShape.Small),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "W",
            color = WayfitColor.OnBrand,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
        )
    }
}

/** 시연용 컨트롤. 실제 앱에서는 Pleos SDK 이벤트가 이 역할을 한다. debug 빌드에만 노출 */
@Composable
private fun DemoPanel(viewModel: MainViewModel, state: MainState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(WayfitColor.Background)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = WayfitSpace.M, vertical = WayfitSpace.XS),
        horizontalArrangement = Arrangement.spacedBy(WayfitSpace.XS),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("DEMO", style = MaterialTheme.typography.labelMedium, color = WayfitColor.Amber)
        Text(sdkSummary(state.sdk), style = MaterialTheme.typography.labelMedium, color = WayfitColor.TextTertiary)
        DemoChip(if (state.parked) "주행으로 전환" else "정차로 전환", viewModel::demoToggleParked)
        DemoChip("추천 띄우기", viewModel::demoRecommend)
        DemoChip("매장 도착·주차", viewModel::demoArriveAndPark)
        DemoChip("폰 연결됨", viewModel::demoPhonePaired)
        DemoChip(if (state.longDrive) "장시간 운전 OFF" else "장시간 운전 ON", viewModel::demoToggleLongDrive)
    }
}

@Composable
private fun DemoChip(label: String, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        colors = AssistChipDefaults.assistChipColors(labelColor = WayfitColor.TextSecondary),
    )
}

private fun sdkSummary(sdk: SdkStatus): String = with(sdk) {
    fun mark(ok: Boolean) = if (ok) "✓" else "✗"
    "SDK 차량${mark(vehicle)} 내비${mark(navi)} 위치${mark(location)} TTS${mark(tts)} STT${mark(stt)}"
}
