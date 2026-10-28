package com.wayfit.wayfitpleos.screen.drive

import com.wayfit.wayfitpleos.screen.base.Event

sealed class DriveEvent : Event {

    data object OpenPairing : DriveEvent()

    data class ShowMessage(val message: String) : DriveEvent()
}
