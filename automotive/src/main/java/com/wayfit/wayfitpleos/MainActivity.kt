package com.wayfit.wayfitpleos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.wayfit.wayfitpleos.ui.WayfitRoot
import com.wayfit.wayfitpleos.ui.theme.WayfitTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WayfitTheme {
                WayfitRoot()
            }
        }
    }
}
