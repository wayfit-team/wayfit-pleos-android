package com.wayfit.wayfitpleos.screen.drive

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.NearMe
import androidx.compose.material.icons.rounded.PhoneIphone
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wayfit.wayfitpleos.model.RouteSummary
import com.wayfit.wayfitpleos.model.TripPolicy
import com.wayfit.wayfitpleos.model.VisitStep
import com.wayfit.wayfitpleos.ui.theme.WayfitColor
import com.wayfit.wayfitpleos.ui.theme.WayfitShape
import com.wayfit.wayfitpleos.ui.theme.WayfitSize
import com.wayfit.wayfitpleos.ui.theme.WayfitSpace
import com.wayfit.wayfitpleos.widget.CardLevel
import com.wayfit.wayfitpleos.widget.EmojiTile
import com.wayfit.wayfitpleos.widget.IconTile
import com.wayfit.wayfitpleos.widget.InfoChip
import com.wayfit.wayfitpleos.widget.PrimaryButton
import com.wayfit.wayfitpleos.widget.RouteStrip
import com.wayfit.wayfitpleos.widget.SecondaryButton
import com.wayfit.wayfitpleos.widget.SectionLabel
import com.wayfit.wayfitpleos.widget.StatusChip
import com.wayfit.wayfitpleos.widget.Text
import com.wayfit.wayfitpleos.widget.TicketCard
import com.wayfit.wayfitpleos.widget.WayfitCard
import java.util.Locale

@Composable
fun DriveScreen(
    viewModel: DriveViewModel = hiltViewModel(),
    onOpenPairing: () -> Unit,
    onShowSnackbar: (String) -> Unit,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val route by viewModel.routeFlow.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                DriveEvent.OpenPairing -> onOpenPairing()
                is DriveEvent.ShowMessage -> onShowSnackbar(event.message)
            }
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= WayfitSize.WideBreakpoint
        if (wide) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(WayfitSpace.XL),
                horizontalArrangement = Arrangement.spacedBy(WayfitSpace.XL),
            ) {
                RoutePanel(route, state, Modifier.weight(0.85f).fillMaxHeight())
                PhaseContent(state, route, showRoute = false, viewModel, Modifier.weight(1.15f).fillMaxHeight())
            }
        } else {
            PhaseContent(state, route, showRoute = true, viewModel, Modifier.fillMaxSize())
        }
    }
}

// region 경로 패널 (넓은 화면)

@Composable
private fun RoutePanel(route: RouteSummary, state: DriveState, modifier: Modifier) {
    WayfitCard(modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(WayfitSpace.L)) {
            RouteSummaryBlock(route, state)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(WayfitColor.Border),
            )
            SectionLabel("이번 주행")
            TripRow(Icons.Rounded.Search, "추천", (state as? DriveState.Idle)?.let { "${it.tripRecommendCount} / ${TripPolicy.MAX_RECOMMEND_PER_TRIP}" } ?: "진행 중")
            TripRow(Icons.Rounded.Mic, "음성 응답", "\"응\" / \"아니\"")
            TripRow(Icons.Rounded.Coffee, "장시간 운전", "2시간마다 쉬어갈 곳 추천")
        }
    }
}

@Composable
private fun RouteSummaryBlock(route: RouteSummary, state: DriveState) {
    Column(verticalArrangement = Arrangement.spacedBy(WayfitSpace.S)) {
        SectionLabel("목적지")
        Text(route.destination, style = MaterialTheme.typography.headlineLarge)
        Text(
            "${formatKm(route.remainingKm)} · 약 ${route.etaMin}분",
            style = MaterialTheme.typography.bodyLarge,
            color = WayfitColor.TextSecondary,
        )
        RouteStrip(
            modifier = Modifier.padding(top = WayfitSpace.S),
            stopFraction = when (state) {
                is DriveState.Recommending, is DriveState.Visiting -> 0.32f
                else -> null
            },
        )
    }
}

@Composable
private fun TripRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(WayfitSpace.M)) {
        IconTile(icon, tint = WayfitColor.TextSecondary, background = WayfitColor.SurfaceSunken, size = 48.dp)
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.labelMedium, color = WayfitColor.TextSecondary)
    }
}

// endregion

@Composable
private fun PhaseContent(
    state: DriveState,
    route: RouteSummary,
    showRoute: Boolean,
    viewModel: DriveViewModel,
    modifier: Modifier,
) {
    AnimatedContent(
        targetState = state,
        contentKey = { it::class },
        transitionSpec = {
            (fadeIn(tween(220)) + scaleIn(tween(220), initialScale = 0.97f)) togetherWith fadeOut(tween(160))
        },
        label = "drivePhase",
        modifier = modifier,
    ) { target ->
        // 창이 낮아져도 버튼이 잘리지 않게: 들어가면 가운데, 넘치면 스크롤
        BoxWithConstraints(Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = maxHeight)
                    .fillMaxWidth()
                    .padding(horizontal = if (showRoute) WayfitSpace.XL else 0.dp, vertical = WayfitSpace.L),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.widthIn(max = WayfitSize.ContentMaxWidth)) {
                    when (target) {
                        is DriveState.Idle -> IdleContent(target, route, showRoute)
                        is DriveState.Recommending -> RecommendContent(target, viewModel::accept, viewModel::decline)
                        is DriveState.Visiting -> VisitContent(target, viewModel::cancelVisit)
                        is DriveState.Verified -> VerifiedContent(target, viewModel::sendToPhone, viewModel::dismissResult)
                    }
                }
            }
        }
    }
}

@Composable
private fun IdleContent(state: DriveState.Idle, route: RouteSummary, showRoute: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(WayfitSpace.L)) {
        IconTile(Icons.Rounded.NearMe, tint = WayfitColor.Brand, background = WayfitColor.BrandSoft, size = 88.dp)
        Column(verticalArrangement = Arrangement.spacedBy(WayfitSpace.XS)) {
            SectionLabel("가는 길에")
            Text("들를 곳을 찾고 있어요", style = MaterialTheme.typography.headlineLarge)
            Text(
                "경로 근처 제휴 가게가 나오면 한 번만 알려드릴게요",
                style = MaterialTheme.typography.bodyLarge,
                color = WayfitColor.TextSecondary,
            )
        }
        if (showRoute) {
            WayfitCard(Modifier.fillMaxWidth()) { RouteSummaryBlock(route, state) }
        }
        StatusChip(
            "이번 주행 추천 ${state.tripRecommendCount} / ${TripPolicy.MAX_RECOMMEND_PER_TRIP}",
            WayfitColor.TextSecondary,
        )
    }
}

@Composable
private fun RecommendContent(
    state: DriveState.Recommending,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    val store = state.store
    Column(verticalArrangement = Arrangement.spacedBy(WayfitSpace.L)) {
        if (state.reason != null) {
            StatusChip(state.reason, WayfitColor.Amber, icon = Icons.Rounded.Coffee)
        }
        WayfitCard(Modifier.fillMaxWidth(), level = CardLevel.Floating) {
            Column(verticalArrangement = Arrangement.spacedBy(WayfitSpace.M)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(WayfitSpace.M)) {
                    EmojiTile(store.category.emoji)
                    Column {
                        Text(store.name, style = MaterialTheme.typography.titleLarge)
                        Text(store.category.label, style = MaterialTheme.typography.labelMedium, color = WayfitColor.TextTertiary)
                    }
                }
                Text(store.benefit, style = MaterialTheme.typography.displayMedium, color = WayfitColor.Brand)
                Row(horizontalArrangement = Arrangement.spacedBy(WayfitSpace.XS)) {
                    InfoChip(Icons.Rounded.NearMe, "${formatKm(state.distanceKm)} 앞")
                    InfoChip(Icons.Rounded.Schedule, "약 ${store.detourMin}분 우회")
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(WayfitSpace.M)) {
            SecondaryButton("괜찮아요", onDecline, Modifier.weight(1f))
            PrimaryButton("들를게요", onAccept, Modifier.weight(1.3f))
        }
        VoiceHint(state.listening, state.secondsLeft)
    }
}

@Composable
private fun VoiceHint(listening: Boolean, secondsLeft: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(WayfitSpace.S)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(WayfitSpace.XS)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(if (listening) WayfitColor.Brand else WayfitColor.SurfaceRaised, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Mic,
                    null,
                    tint = if (listening) WayfitColor.OnBrand else WayfitColor.TextSecondary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Text(
                if (listening) "듣고 있어요 · \"응\" 또는 \"아니\"" else "말로도 답할 수 있어요 · \"응\" / \"아니\"",
                style = MaterialTheme.typography.labelMedium,
                color = if (listening) WayfitColor.Brand else WayfitColor.TextSecondary,
            )
        }
        LinearProgressIndicator(
            progress = { secondsLeft / TripPolicy.RECOMMEND_TIMEOUT_SEC.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            color = WayfitColor.Brand,
            trackColor = WayfitColor.SurfaceRaised,
            drawStopIndicator = {},
        )
    }
}

@Composable
private fun VisitContent(state: DriveState.Visiting, onCancel: () -> Unit) {
    val arrived = VisitStep.ARRIVED in state.done
    Column(verticalArrangement = Arrangement.spacedBy(WayfitSpace.L)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(WayfitSpace.M)) {
            EmojiTile(state.store.category.emoji, size = 56.dp)
            Column {
                SectionLabel(if (arrived) "도착" else "들르는 중")
                Text(state.store.name, style = MaterialTheme.typography.titleLarge)
            }
        }
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(WayfitSpace.XS)) {
            if (arrived) {
                Text("도착했어요", style = MaterialTheme.typography.displayMedium, color = WayfitColor.Brand)
            } else {
                val (value, unit) = splitDistance(state.remainingM)
                Text(value, style = MaterialTheme.typography.displayMedium, color = WayfitColor.Brand)
                Text("$unit 남음", style = MaterialTheme.typography.titleLarge, color = WayfitColor.TextSecondary)
            }
        }
        WayfitCard(Modifier.fillMaxWidth()) {
            Column {
                VisitStep.entries.forEachIndexed { index, step ->
                    StepRow(
                        label = step.label,
                        done = step in state.done,
                        inProgress = step == VisitStep.ISSUED && state.verifying,
                        showConnector = index < VisitStep.entries.lastIndex,
                    )
                }
            }
        }
        if (state.verifying) {
            Text("방문을 확인하고 있어요…", style = MaterialTheme.typography.bodyLarge, color = WayfitColor.TextSecondary)
        } else {
            SecondaryButton("방문 취소", onCancel, Modifier.fillMaxWidth(), icon = Icons.Rounded.Close)
        }
    }
}

@Composable
private fun StepRow(label: String, done: Boolean, inProgress: Boolean, showConnector: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(WayfitSpace.M)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(if (done) WayfitColor.Brand else WayfitColor.SurfaceSunken, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    done -> Icon(Icons.Rounded.Check, null, tint = WayfitColor.OnBrand, modifier = Modifier.size(26.dp))
                    inProgress -> CircularProgressIndicator(Modifier.size(24.dp), color = WayfitColor.Brand, strokeWidth = 3.dp)
                }
            }
            if (showConnector) {
                Box(
                    Modifier
                        .width(3.dp)
                        .height(20.dp)
                        .background(if (done) WayfitColor.Brand else WayfitColor.Border),
                )
            }
        }
        Text(
            label,
            modifier = Modifier.padding(top = 4.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = if (done) WayfitColor.TextPrimary else WayfitColor.TextTertiary,
        )
    }
}

@Composable
private fun VerifiedContent(state: DriveState.Verified, onSendToPhone: () -> Unit, onDismiss: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(WayfitSpace.L),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconTile(Icons.Rounded.CheckCircle, tint = WayfitColor.OnBrand, background = WayfitColor.Brand, size = 96.dp)
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(WayfitSpace.XS)) {
            Text("쿠폰이 도착했어요", style = MaterialTheme.typography.headlineLarge)
            Text(
                if (state.phonePaired) "연결된 폰의 쿠폰함에도 들어갔어요" else "폰을 연결하면 매장에서 바로 쓸 수 있어요",
                style = MaterialTheme.typography.bodyLarge,
                color = WayfitColor.TextSecondary,
                textAlign = TextAlign.Center,
            )
        }
        TicketCard(state.coupon, highlighted = true)
        Row(horizontalArrangement = Arrangement.spacedBy(WayfitSpace.M)) {
            SecondaryButton("확인", onDismiss, Modifier.weight(1f))
            if (!state.phonePaired) {
                PrimaryButton("폰으로 받기", onSendToPhone, Modifier.weight(1.4f), icon = Icons.Rounded.PhoneIphone)
            }
        }
    }
}

internal fun formatKm(km: Double): String =
    if (km < 1) "${(km * 1000).toInt()}m" else String.format(Locale.KOREA, "%.1fkm", km)

private fun splitDistance(m: Int): Pair<String, String> =
    if (m >= 1000) String.format(Locale.KOREA, "%.1f", m / 1000.0) to "km" else "$m" to "m"
