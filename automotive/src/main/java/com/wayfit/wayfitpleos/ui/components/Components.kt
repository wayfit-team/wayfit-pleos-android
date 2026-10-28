package com.wayfit.wayfitpleos.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wayfit.wayfitpleos.ui.theme.WayfitColors

/** AAOS 권장 최소 터치 영역(76dp)보다 크게 */
private val BigButtonHeight = 96.dp

@Composable
fun PrimaryBigButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = BigButtonHeight),
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = WayfitColors.Mint,
            contentColor = WayfitColors.OnMint,
        ),
        contentPadding = PaddingValues(horizontal = 12.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1, softWrap = false)
    }
}

@Composable
fun SecondaryBigButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = BigButtonHeight),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(2.dp, WayfitColors.Outline),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = WayfitColors.TextPrimary),
        contentPadding = PaddingValues(horizontal = 12.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1, softWrap = false)
    }
}

@Composable
fun StatusChip(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(color.copy(alpha = 0.16f), RoundedCornerShape(50))
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(text, color = color, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun Card(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(WayfitColors.Surface, RoundedCornerShape(28.dp))
            .padding(28.dp),
    ) { content() }
}

/** 주행 중에는 정차 전용 화면을 가리고 안내만 보여준다 (Spec REQ-A17) */
@Composable
fun DrivingGate(driving: Boolean, content: @Composable () -> Unit) {
    if (!driving) {
        content()
        return
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(WayfitColors.SurfaceHigh, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Lock, contentDescription = null, tint = WayfitColors.Amber, modifier = Modifier.size(48.dp))
        }
        Text("정차 후 이용할 수 있어요", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Text(
            "안전을 위해 주행 중에는\n추천과 방문 안내만 보여드려요",
            style = MaterialTheme.typography.bodyLarge,
            color = WayfitColors.TextSecondary,
            textAlign = TextAlign.Center,
        )
    }
}
