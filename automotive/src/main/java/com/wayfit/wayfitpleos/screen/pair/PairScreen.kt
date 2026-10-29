package com.wayfit.wayfitpleos.screen.pair

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.PhoneIphone
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.wayfit.wayfitpleos.model.PairingCode
import com.wayfit.wayfitpleos.ui.theme.WayfitColor
import com.wayfit.wayfitpleos.ui.theme.WayfitShape
import com.wayfit.wayfitpleos.ui.theme.WayfitSize
import com.wayfit.wayfitpleos.ui.theme.WayfitSpace
import com.wayfit.wayfitpleos.widget.IconTile
import com.wayfit.wayfitpleos.widget.PrimaryButton
import com.wayfit.wayfitpleos.widget.SecondaryButton
import com.wayfit.wayfitpleos.widget.SectionLabel
import com.wayfit.wayfitpleos.widget.Text
import com.wayfit.wayfitpleos.widget.WayfitCard

@Composable
fun PairScreen(
    viewModel: PairViewModel = hiltViewModel(),
    onBack: () -> Unit,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(WayfitSpace.XL),
        contentAlignment = Alignment.Center,
    ) {
        BackButton(onBack, Modifier.align(Alignment.TopStart))
        Box(Modifier.widthIn(max = WayfitSize.ContentMaxWidth)) {
            val code = state.code
            when {
                state.justPaired -> PairedContent(state.phoneCount, viewModel::requestNewCode)
                code != null -> ShowingCode(code, viewModel::requestNewCode)
                else -> PrimaryButton("연결 코드 받기", viewModel::requestNewCode)
            }
        }
    }
}

@Composable
private fun BackButton(onBack: () -> Unit, modifier: Modifier) {
    Row(
        modifier = modifier
            .clip(WayfitShape.Button)
            .clickable(onClick = onBack)
            .padding(horizontal = WayfitSpace.S, vertical = WayfitSpace.XS),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(WayfitSpace.XS),
    ) {
        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "뒤로", tint = WayfitColor.TextSecondary, modifier = Modifier.size(32.dp))
        Text("뒤로", style = MaterialTheme.typography.labelLarge, color = WayfitColor.TextSecondary)
    }
}

@Composable
private fun ShowingCode(pairing: PairingCode, onNewCode: () -> Unit) {
    val expired = pairing.expired
    Row(horizontalArrangement = Arrangement.spacedBy(WayfitSpace.XL), verticalAlignment = Alignment.CenterVertically) {
        val qr = remember(pairing.code) { qrBitmap(PairingCode.URL_BASE + pairing.code) }
        Box(
            modifier = Modifier
                .size(300.dp)
                .background(Color.White, WayfitShape.Card)
                .padding(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Image(qr, contentDescription = "연결 QR 코드", modifier = Modifier.fillMaxSize(), alpha = if (expired) 0.12f else 1f)
            if (expired) Text("만료됨", style = MaterialTheme.typography.headlineLarge, color = WayfitColor.Rose)
        }
        Column(verticalArrangement = Arrangement.spacedBy(WayfitSpace.M)) {
            SectionLabel("폰 연결")
            Text("폰 카메라로\nQR을 찍어주세요", style = MaterialTheme.typography.headlineLarge)
            Text("앱 설치 없이 바로 쿠폰함이 열려요", style = MaterialTheme.typography.bodyLarge, color = WayfitColor.TextSecondary)
            SectionLabel("또는 코드 입력")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                pairing.code.forEachIndexed { index, digit ->
                    if (index == 4) Box(Modifier.size(width = 8.dp, height = 1.dp))
                    Box(
                        modifier = Modifier
                            .size(width = 44.dp, height = 56.dp)
                            .background(WayfitColor.SurfaceRaised, WayfitShape.Small)
                            .border(1.dp, WayfitColor.Border, WayfitShape.Small),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            digit.toString(),
                            style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Monospace),
                            color = if (expired) WayfitColor.TextTertiary else WayfitColor.TextPrimary,
                        )
                    }
                }
            }
            if (expired) {
                PrimaryButton("새 코드 받기", onNewCode, icon = Icons.Rounded.Refresh)
            } else {
                Text(
                    "남은 시간 %d:%02d".format(pairing.secondsLeft / 60, pairing.secondsLeft % 60),
                    style = MaterialTheme.typography.labelMedium,
                    color = WayfitColor.TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun PairedContent(phoneCount: Int, onAddPhone: () -> Unit) {
    WayfitCard {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(WayfitSpace.L),
        ) {
            IconTile(Icons.Rounded.CheckCircle, tint = WayfitColor.OnBrand, background = WayfitColor.Brand, size = 96.dp)
            Text("폰이 연결됐어요", style = MaterialTheme.typography.headlineLarge)
            Text(
                "연결된 폰 ${phoneCount}대 · 쿠폰은 폰의 웨이핏 쿠폰함에서 확인하세요",
                style = MaterialTheme.typography.bodyLarge,
                color = WayfitColor.TextSecondary,
                textAlign = TextAlign.Center,
            )
            SecondaryButton("다른 폰 추가 연결", onAddPhone, icon = Icons.Rounded.PhoneIphone)
        }
    }
}

private fun qrBitmap(content: String, size: Int = 512): ImageBitmap {
    val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, mapOf(EncodeHintType.MARGIN to 0))
    val pixels = IntArray(size * size) { i ->
        if (matrix[i % size, i / size]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
    }
    return Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888).asImageBitmap()
}
