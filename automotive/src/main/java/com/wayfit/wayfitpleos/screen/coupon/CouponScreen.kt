package com.wayfit.wayfitpleos.screen.coupon

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ConfirmationNumber
import androidx.compose.material.icons.rounded.PhoneIphone
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wayfit.wayfitpleos.ui.theme.WayfitColor
import com.wayfit.wayfitpleos.ui.theme.WayfitSize
import com.wayfit.wayfitpleos.ui.theme.WayfitSpace
import com.wayfit.wayfitpleos.widget.IconTile
import com.wayfit.wayfitpleos.widget.PrimaryButton
import com.wayfit.wayfitpleos.widget.SectionLabel
import com.wayfit.wayfitpleos.widget.StatusChip
import com.wayfit.wayfitpleos.widget.Text
import com.wayfit.wayfitpleos.widget.TicketCard
import com.wayfit.wayfitpleos.widget.WayfitCard

@Composable
fun CouponScreen(
    viewModel: CouponViewModel = hiltViewModel(),
    onOpenPairing: () -> Unit,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    val (active, used) = state.coupons.partition { it.usable }

    // 넓은 화면에서도 읽기 좋은 폭으로 가운데 정렬
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
        modifier = Modifier
            .widthIn(max = WayfitSize.ContentMaxWidth)
            .fillMaxHeight(),
        contentPadding = PaddingValues(WayfitSpace.XL),
        verticalArrangement = Arrangement.spacedBy(WayfitSpace.M),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    SectionLabel("쿠폰함")
                    Text("받은 쿠폰 ${active.size}장", style = MaterialTheme.typography.headlineLarge)
                }
                if (state.phoneCount > 0) {
                    StatusChip("폰 ${state.phoneCount}대 연결됨", WayfitColor.Brand, icon = Icons.Rounded.CheckCircle)
                } else {
                    StatusChip("폰 미연결", WayfitColor.Amber, icon = Icons.Rounded.PhoneIphone)
                }
            }
        }
        if (state.phoneCount == 0) {
            item {
                WayfitCard(Modifier.fillMaxWidth()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(WayfitSpace.L), verticalAlignment = Alignment.CenterVertically) {
                        IconTile(Icons.Rounded.QrCode2, tint = WayfitColor.Brand, background = WayfitColor.BrandSoft)
                        Column(Modifier.weight(1f)) {
                            Text("쿠폰은 폰에서 보여주면 돼요", style = MaterialTheme.typography.titleLarge)
                            Text(
                                "QR 한 번이면 연결돼요 · 앱 설치 없음",
                                style = MaterialTheme.typography.labelMedium,
                                color = WayfitColor.TextSecondary,
                            )
                        }
                        PrimaryButton("연결", onOpenPairing, Modifier.widthIn(min = 140.dp))
                    }
                }
            }
        }
        if (state.coupons.isEmpty()) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(WayfitSpace.M),
                ) {
                    IconTile(Icons.Rounded.ConfirmationNumber, tint = WayfitColor.TextTertiary, background = WayfitColor.Surface)
                    Text(
                        "아직 쿠폰이 없어요. 가는 길에 웨이핏 매장에 들러보세요",
                        style = MaterialTheme.typography.bodyLarge,
                        color = WayfitColor.TextSecondary,
                    )
                }
            }
        }
        items(active, key = { it.id }) { TicketCard(it) }
        if (used.isNotEmpty()) {
            item { SectionLabel("사용 완료 · 만료", Modifier.padding(top = WayfitSpace.S)) }
            items(used, key = { it.id }) { TicketCard(it) }
        }
    }
    }
}
