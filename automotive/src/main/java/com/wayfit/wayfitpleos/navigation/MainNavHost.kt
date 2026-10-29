package com.wayfit.wayfitpleos.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.wayfit.wayfitpleos.screen.coupon.CouponScreen
import com.wayfit.wayfitpleos.screen.drive.DriveScreen
import com.wayfit.wayfitpleos.screen.pair.PairScreen
import com.wayfit.wayfitpleos.screen.settings.SettingsScreen
import com.wayfit.wayfitpleos.widget.DrivingGate

@Composable
fun MainNavHost(
    navController: NavHostController,
    innerPadding: PaddingValues,
    parked: Boolean,
    onShowSnackbar: (String) -> Unit,
) {
    val openPairing = { navController.navigate(Route.Pair.route) { launchSingleTop = true } }
    NavHost(
        navController = navController,
        startDestination = Route.Drive.route,
        modifier = Modifier.padding(innerPadding),
        enterTransition = { fadeIn(tween(200)) },
        exitTransition = { fadeOut(tween(150)) },
    ) {
        // 주행 화면은 시작 지점이라 탭을 옮겨도 ViewModel(방문 진행 상태)이 유지된다
        composable(Route.Drive.route) {
            DriveScreen(onOpenPairing = openPairing, onShowSnackbar = onShowSnackbar)
        }
        composable(Route.Coupons.route) {
            DrivingGate(parked) { CouponScreen(onOpenPairing = openPairing) }
        }
        composable(Route.Pair.route) {
            DrivingGate(parked) { PairScreen(onBack = { navController.popBackStack() }) }
        }
        composable(Route.Settings.route) {
            DrivingGate(parked) { SettingsScreen(onOpenPairing = openPairing, onShowSnackbar = onShowSnackbar) }
        }
    }
}

fun NavHostController.navigateToTab(route: Route) {
    navigate(route.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
