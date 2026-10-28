package com.wayfit.wayfitpleos.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.wayfit.wayfitpleos.ui.components.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.wayfit.wayfitpleos.model.PAIR_URL_BASE
import com.wayfit.wayfitpleos.model.PairState
import com.wayfit.wayfitpleos.ui.components.PrimaryBigButton
import com.wayfit.wayfitpleos.ui.components.SecondaryBigButton
import com.wayfit.wayfitpleos.ui.theme.WayfitColors

@Composable
fun PairScreen(pair: PairState, onNewCode: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (pair) {
            PairState.NotPaired -> PrimaryBigButton("연결 코드 받기", onNewCode, Modifier.fillMaxWidth())
            is PairState.Showing -> ShowingCode(pair, onNewCode)
            is PairState.Paired -> PairedContent(pair, onNewCode)
        }
    }
}

@Composable
private fun ShowingCode(pair: PairState.Showing, onNewCode: () -> Unit) {
    val expired = pair.secondsLeft <= 0
    Text("폰 카메라로 QR을 찍어주세요", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)

    val qr = remember(pair.code) { qrBitmap(PAIR_URL_BASE + pair.code) }
    Box(
        modifier = Modifier
            .size(300.dp)
            .background(androidx.compose.ui.graphics.Color.White, RoundedCornerShape(24.dp))
            .padding(18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Image(qr, contentDescription = "연결 QR 코드", modifier = Modifier.fillMaxSize(), alpha = if (expired) 0.15f else 1f)
        if (expired) Text("만료됨", style = MaterialTheme.typography.headlineMedium, color = WayfitColors.Danger)
    }

    Text("또는 코드 입력", style = MaterialTheme.typography.bodyMedium, color = WayfitColors.TextSecondary)
    Text(
        pair.code.chunked(4).joinToString(" "),
        style = MaterialTheme.typography.displayMedium.copy(fontFamily = FontFamily.Monospace),
    )
    if (expired) {
        PrimaryBigButton("새 코드 받기", onNewCode, Modifier.fillMaxWidth())
    } else {
        Text(
            "남은 시간 %d:%02d".format(pair.secondsLeft / 60, pair.secondsLeft % 60),
            style = MaterialTheme.typography.bodyLarge,
            color = WayfitColors.TextSecondary,
        )
    }
}

@Composable
private fun PairedContent(pair: PairState.Paired, onAddPhone: () -> Unit) {
    Icon(Icons.Filled.CheckCircle, null, tint = WayfitColors.Mint, modifier = Modifier.size(104.dp))
    Text("폰이 연결됐어요", style = MaterialTheme.typography.headlineLarge)
    Text(
        "연결된 폰 ${pair.phoneCount}대\n쿠폰은 폰의 웨이핏 쿠폰함에서 확인하세요",
        style = MaterialTheme.typography.bodyLarge,
        color = WayfitColors.TextSecondary,
        textAlign = TextAlign.Center,
    )
    SecondaryBigButton("다른 폰 추가 연결", onAddPhone, Modifier.fillMaxWidth())
}

private fun qrBitmap(content: String, size: Int = 512): ImageBitmap {
    val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, mapOf(EncodeHintType.MARGIN to 0))
    val pixels = IntArray(size * size) { i ->
        if (matrix[i % size, i / size]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
    }
    return Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888).asImageBitmap()
}
