package com.wayfit.wayfitpleos.screen.main

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.wayfit.wayfitpleos.ui.theme.WayfitTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            viewModel.startSdks(fusedLocationGranted = result[FUSED_LOCATION] == true || isGranted(FUSED_LOCATION))
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { WayfitTheme { MainScreen(viewModel) } }

        val missing = PLEOS_PERMISSIONS.filterNot(::isGranted)
        if (missing.isEmpty()) {
            viewModel.startSdks(fusedLocationGranted = true)
        } else {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    private fun isGranted(permission: String) =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    private companion object {
        const val FUSED_LOCATION = "pleos.car.permission.FUSED_LOCATION"

        // 런타임 허용이 필요한 것은 FUSED_LOCATION 뿐이지만, 플랫폼별 보호 수준 차이에 대비해 함께 요청
        val PLEOS_PERMISSIONS = listOf(
            FUSED_LOCATION,
            "pleos.car.permission.CAR_DRIVE",
            "pleos.car.permission.CAR_DOORS",
            "pleos.car.permission.NAVI_ROUTE",
            "pleos.car.permission.NAVI_ROUTE_SEARCH",
            "pleos.car.permission.TTS_SERVICE",
            "pleos.car.permission.STT_SERVICE",
        )
    }
}
