package com.wayfit.wayfitpleos.screen.settings

import com.wayfit.wayfitpleos.screen.base.Event

sealed class SettingsEvent : Event {

    data class ShowMessage(val message: String) : SettingsEvent()
}
