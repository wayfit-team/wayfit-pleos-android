package com.wayfit.wayfitpleos.screen.settings

import com.wayfit.wayfitpleos.model.PairedPhone
import com.wayfit.wayfitpleos.screen.base.State

data class SettingsState(
    val phones: List<PairedPhone> = emptyList(),
    val voiceGuideEnabled: Boolean = true,
) : State
