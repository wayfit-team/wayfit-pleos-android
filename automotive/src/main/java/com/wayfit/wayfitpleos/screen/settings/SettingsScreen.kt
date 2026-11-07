package com.wayfit.wayfitpleos.screen.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PhoneIphone
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wayfit.wayfitpleos.BuildConfig
import com.wayfit.wayfitpleos.model.PairedPhone
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
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale


@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onOpenPairing: () -> Unit,
    onShowSnackbar: (String) -> Unit,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is SettingsEvent.ShowMessage -> onShowSnackbar(event.message)
            }
        }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = WayfitSize.ContentMaxWidth)
                .fillMaxHeight(),
            contentPadding = PaddingValues(WayfitSpace.XL),
            verticalArrangement = Arrangement.spacedBy(WayfitSpace.L),
        ) {
            item {
                Column {
                    SectionLabel("설정")
                    Text("웨이핏 설정", style = MaterialTheme.typography.headlineLarge)
                }
            }
            item { PhonesSection(state.phones, onOpenPairing) }
            item { VoiceSection(state.voiceGuideEnabled, viewModel::setVoiceGuideEnabled) }
            item { AppInfoSection() }
        }
    }
}

@Composable
private fun PhonesSection(
    phones: List<PairedPhone>,
    onOpenPairing: () -> Unit,
) {
    WayfitCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(WayfitSpace.M)) {
            Column {
                Text("연결된 폰", style = MaterialTheme.typography.titleLarge)
                Text(
                    "쿠폰은 연결된 폰의 쿠폰함으로도 들어가요",
                    style = MaterialTheme.typography.labelMedium,
                    color = WayfitColor.TextSecondary,
                )
            }
            if (phones.isEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(WayfitSpace.M),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "연결된 폰이 없어요",
                        style = MaterialTheme.typography.bodyLarge,
                        color = WayfitColor.TextTertiary,
                        modifier = Modifier.weight(1f),
                    )
                    PrimaryButton("폰 연결하기", onOpenPairing, icon = Icons.Rounded.PhoneIphone)
                }
            } else {
                phones.forEach { phone -> PhoneRow(phone) }
                // 차량 쪽 연결 해제 API 가 없어 폰(웨이핏 웹)에서 해제한다
                Text(
                    "연결 해제는 폰의 웨이핏에서 할 수 있어요",
                    style = MaterialTheme.typography.labelMedium,
                    color = WayfitColor.TextTertiary,
                )
                SecondaryButton("다른 폰 연결", onOpenPairing, Modifier.fillMaxWidth(), icon = Icons.Rounded.Add)
            }
        }
    }
}

@Composable
private fun PhoneRow(phone: PairedPhone) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = WayfitSize.ButtonHeight + WayfitSpace.M)
            .background(WayfitColor.SurfaceSunken, WayfitShape.Button)
            .padding(horizontal = WayfitSpace.M),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(WayfitSpace.M),
    ) {
        IconTile(Icons.Rounded.PhoneIphone, tint = WayfitColor.Brand, background = WayfitColor.BrandSoft, size = 56.dp)
        Column(Modifier.weight(1f)) {
            Text(phone.name, style = MaterialTheme.typography.titleLarge)
            phone.connectedAtMillis?.let {
                Text(
                    "연결 ${formatTime(it)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = WayfitColor.TextTertiary,
                )
            }
        }
    }
}

@Composable
private fun VoiceSection(enabled: Boolean, onChange: (Boolean) -> Unit) {
    WayfitCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(WayfitSpace.M)) {
            IconTile(Icons.Rounded.RecordVoiceOver, tint = WayfitColor.Brand, background = WayfitColor.BrandSoft, size = 56.dp)
            Column(Modifier.weight(1f)) {
                Text("음성 안내", style = MaterialTheme.typography.titleLarge)
                Text(
                    if (enabled) "추천을 읽어주고 \"응\" / \"아니\"로 답할 수 있어요" else "추천 카드만 보여드려요",
                    style = MaterialTheme.typography.labelMedium,
                    color = WayfitColor.TextSecondary,
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = onChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = WayfitColor.OnBrand,
                    checkedTrackColor = WayfitColor.Brand,
                    uncheckedThumbColor = WayfitColor.TextSecondary,
                    uncheckedTrackColor = WayfitColor.SurfaceSunken,
                    uncheckedBorderColor = WayfitColor.Border,
                ),
            )
        }
    }
}

@Composable
private fun AppInfoSection() {
    WayfitCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(WayfitSpace.M)) {
            IconTile(Icons.Rounded.Info, tint = WayfitColor.TextSecondary, background = WayfitColor.SurfaceSunken, size = 56.dp)
            Column(Modifier.weight(1f)) {
                Text("앱 정보", style = MaterialTheme.typography.titleLarge)
                Text(
                    "웨이핏 ${BuildConfig.VERSION_NAME} · 가는 길에, 웨이핏",
                    style = MaterialTheme.typography.labelMedium,
                    color = WayfitColor.TextSecondary,
                )
            }
        }
    }
}


private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("M월 d일 HH:mm", Locale.KOREA)

private fun formatTime(millis: Long): String = TIME_FORMAT.format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))
