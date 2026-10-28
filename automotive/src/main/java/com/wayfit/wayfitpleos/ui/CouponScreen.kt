package com.wayfit.wayfitpleos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import com.wayfit.wayfitpleos.ui.components.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wayfit.wayfitpleos.model.Coupon
import com.wayfit.wayfitpleos.model.PairState
import com.wayfit.wayfitpleos.ui.components.Card
import com.wayfit.wayfitpleos.ui.components.PrimaryBigButton
import com.wayfit.wayfitpleos.ui.components.StatusChip
import com.wayfit.wayfitpleos.ui.theme.WayfitColors

@Composable
fun CouponScreen(coupons: List<Coupon>, pair: PairState, onPairClick: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("받은 쿠폰", style = MaterialTheme.typography.headlineLarge)
                if (pair is PairState.Paired) {
                    StatusChip("폰 ${pair.phoneCount}대 연결됨", WayfitColors.Mint)
                } else {
                    StatusChip("폰 미연결", WayfitColors.Amber)
                }
            }
        }
        if (pair !is PairState.Paired) {
            item {
                Card {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            "쿠폰은 폰에서 보여주면 돼요",
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            "QR 한 번이면 연결돼요. 앱 설치는 필요 없어요.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = WayfitColors.TextSecondary,
                        )
                        PrimaryBigButton("폰 연결하기", onPairClick, Modifier.fillMaxWidth())
                    }
                }
            }
        }
        if (coupons.isEmpty()) {
            item {
                Text(
                    "아직 쿠폰이 없어요.\n가는 길에 웨이핏 매장에 들러보세요",
                    style = MaterialTheme.typography.bodyLarge,
                    color = WayfitColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        items(coupons, key = { it.id }) { CouponRow(it) }
    }
}

@Composable
private fun CouponRow(coupon: Coupon) {
    Card {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(coupon.storeName, style = MaterialTheme.typography.bodyMedium, color = WayfitColors.TextSecondary)
                Text(
                    coupon.benefit,
                    style = MaterialTheme.typography.titleLarge,
                    color = if (coupon.redeemed) WayfitColors.TextSecondary else WayfitColors.TextPrimary,
                )
            }
            when {
                coupon.redeemed -> StatusChip("사용 완료", WayfitColors.TextSecondary)
                coupon.expiresInDays <= 1 -> StatusChip("오늘까지", WayfitColors.Danger)
                else -> StatusChip("${coupon.expiresInDays}일 남음", WayfitColors.Mint)
            }
        }
    }
}
