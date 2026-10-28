package com.wayfit.wayfitpleos.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ConfirmationNumber
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/** Pair 는 탭이 아니라 설정 아래 화면 (설정·쿠폰함·쿠폰 도착에서 연다) */
sealed class Route(val route: String) {
    data object Drive : Route("drive")
    data object Coupons : Route("coupons")
    data object Pair : Route("pair")
    data object Settings : Route("settings")
}

/** [parkedOnly] 탭은 주행 중 잠금 아이콘 + DrivingGate */
sealed class BottomNavItem(val route: Route, val icon: ImageVector, val label: String, val parkedOnly: Boolean) {
    data object Drive : BottomNavItem(Route.Drive, Icons.Rounded.Navigation, "주행", parkedOnly = false)
    data object Coupons : BottomNavItem(Route.Coupons, Icons.Rounded.ConfirmationNumber, "쿠폰함", parkedOnly = true)
    data object Settings : BottomNavItem(Route.Settings, Icons.Rounded.Settings, "설정", parkedOnly = true)

    companion object {
        val items = listOf(Drive, Coupons, Settings)
    }
}
