package com.wayfit.wayfitpleos.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import com.wayfit.wayfitpleos.ui.components.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wayfit.wayfitpleos.model.DrivePhase
import com.wayfit.wayfitpleos.model.MAX_RECOMMEND_PER_TRIP
import com.wayfit.wayfitpleos.model.RECOMMEND_TIMEOUT_SEC
import com.wayfit.wayfitpleos.model.RouteSummary
import com.wayfit.wayfitpleos.model.VisitStep
import com.wayfit.wayfitpleos.ui.components.Card
import com.wayfit.wayfitpleos.ui.components.PrimaryBigButton
import com.wayfit.wayfitpleos.ui.components.SecondaryBigButton
import com.wayfit.wayfitpleos.ui.theme.WayfitColors
import java.util.Locale

@Composable
fun DriveScreen(
    phase: DrivePhase,
    route: RouteSummary,
    tripRecommendCount: Int,
    phonePaired: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onCancelVisit: () -> Unit,
    onSendToPhone: () -> Unit,
    onDismissResult: () -> Unit,
) {
    AnimatedContent(
        targetState = phase,
        contentKey = { it::class },
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "drivePhase",
        modifier = Modifier.fillMaxSize(),
    ) { target ->
        // 창이 낮아져도(창 크기 조절) 버튼이 잘리지 않게: 들어가면 가운데 정렬, 넘치면 스크롤
        BoxWithConstraints(Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = maxHeight)
                    .fillMaxWidth()
                    .padding(28.dp),
                contentAlignment = Alignment.Center,
            ) {
                when (target) {
                    DrivePhase.Idle -> IdleContent(route, tripRecommendCount)
                    is DrivePhase.Recommending -> RecommendContent(target, onAccept, onDecline)
                    is DrivePhase.Visiting -> VisitContent(target, onCancelVisit)
                    is DrivePhase.Verified -> VerifiedContent(target, phonePaired, onSendToPhone, onDismissResult)
                }
            }
        }
    }
}

@Composable
private fun IdleContent(route: RouteSummary, tripRecommendCount: Int) {
    Column(
        verticalArrangement = Arrangement.spacedBy(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(WayfitColors.Mint.copy(alpha = 0.14f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.LocationOn, null, tint = WayfitColors.Mint, modifier = Modifier.size(60.dp))
        }
        Text(
            "가는 길에 들를 곳을\n찾고 있어요",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
        )
        Card {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("목적지", style = MaterialTheme.typography.labelMedium, color = WayfitColors.TextSecondary)
                Text(route.destination, style = MaterialTheme.typography.headlineMedium)
                Text(
                    "${formatKm(route.remainingKm)} · 약 ${route.etaMin}분",
                    style = MaterialTheme.typography.bodyLarge,
                    color = WayfitColors.TextSecondary,
                )
            }
        }
        Text(
            "이번 주행 추천 $tripRecommendCount / $MAX_RECOMMEND_PER_TRIP",
            style = MaterialTheme.typography.bodyMedium,
            color = WayfitColors.TextSecondary,
        )
    }
}

@Composable
private fun RecommendContent(
    phase: DrivePhase.Recommending,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    val store = phase.store
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        if (phase.reason != null) {
            Text(
                phase.reason,
                style = MaterialTheme.typography.bodyLarge,
                color = WayfitColors.Amber,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(WayfitColors.Amber.copy(alpha = 0.14f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            )
        }
        Card {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "${store.category.emoji} ${store.name}",
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    store.benefit,
                    style = MaterialTheme.typography.displayMedium,
                    color = WayfitColors.Mint,
                )
                Text(
                    "${formatKm(store.aheadKm)} 앞 · 약 ${store.detourMin}분 우회",
                    style = MaterialTheme.typography.bodyLarge,
                    color = WayfitColors.TextSecondary,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            SecondaryBigButton("괜찮아요", onDecline, Modifier.weight(1f))
            PrimaryBigButton("들를게요", onAccept, Modifier.weight(1.2f))
        }
        LinearProgressIndicator(
            progress = { phase.secondsLeft / RECOMMEND_TIMEOUT_SEC.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = WayfitColors.Mint,
            trackColor = WayfitColors.SurfaceHigh,
            drawStopIndicator = {},
        )
    }
}

@Composable
private fun VisitContent(phase: DrivePhase.Visiting, onCancel: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Text(
            "${phase.store.category.emoji} ${phase.store.name}",
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            if (phase.remainingM > 0) "들르는 중 · ${formatMeters(phase.remainingM)}" else "도착했어요",
            style = MaterialTheme.typography.displayMedium,
            color = WayfitColors.Mint,
        )
        Card {
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                VisitStep.entries.forEach { step ->
                    StepRow(
                        label = step.label,
                        done = step in phase.done,
                        inProgress = step == VisitStep.ISSUED && phase.verifying,
                    )
                }
            }
        }
        if (!phase.verifying) {
            SecondaryBigButton("방문 취소", onCancel, Modifier.fillMaxWidth())
        } else {
            Text(
                "방문을 확인하고 있어요…",
                style = MaterialTheme.typography.bodyLarge,
                color = WayfitColors.TextSecondary,
            )
        }
    }
}

@Composable
private fun StepRow(label: String, done: Boolean, inProgress: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(if (done) WayfitColors.Mint else WayfitColors.SurfaceHigh, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            when {
                done -> Icon(Icons.Filled.Check, null, tint = WayfitColors.OnMint, modifier = Modifier.size(26.dp))
                inProgress -> CircularProgressIndicator(
                    modifier = Modifier.size(26.dp),
                    color = WayfitColors.Mint,
                    strokeWidth = 3.dp,
                )
            }
        }
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (done) WayfitColors.TextPrimary else WayfitColors.TextSecondary,
        )
    }
}

@Composable
private fun VerifiedContent(
    phase: DrivePhase.Verified,
    phonePaired: Boolean,
    onSendToPhone: () -> Unit,
    onDismiss: () -> Unit,
) {
    val coupon = phase.coupon
    Column(
        verticalArrangement = Arrangement.spacedBy(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Filled.CheckCircle, null, tint = WayfitColors.Mint, modifier = Modifier.size(80.dp))
        Text("쿠폰이 도착했어요", style = MaterialTheme.typography.headlineLarge)
        Card {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(coupon.storeName, style = MaterialTheme.typography.titleLarge, color = WayfitColors.TextSecondary)
                Text(coupon.benefit, style = MaterialTheme.typography.headlineLarge, color = WayfitColors.Mint)
                Text(
                    "${coupon.expiresInDays}일 동안 사용할 수 있어요",
                    style = MaterialTheme.typography.bodyMedium,
                    color = WayfitColors.TextSecondary,
                )
            }
        }
        Text(
            if (phonePaired) "연결된 폰의 쿠폰함에도 들어갔어요" else "폰을 연결하면 매장에서 바로 쓸 수 있어요",
            style = MaterialTheme.typography.bodyMedium,
            color = WayfitColors.TextSecondary,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            SecondaryBigButton("확인", onDismiss, Modifier.weight(1f))
            if (!phonePaired) PrimaryBigButton("폰으로 받기", onSendToPhone, Modifier.weight(1.4f))
        }
    }
}

internal fun formatKm(km: Double): String = String.format(Locale.KOREA, "%.1fkm", km)

private fun formatMeters(m: Int): String = if (m >= 1000) formatKm(m / 1000.0) else "${m}m"
