package com.camsense

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.camsense.ui.CamSenseNavHost
import com.camsense.ui.theme.CamSenseColors
import com.camsense.ui.theme.CamSenseTheme
import androidx.compose.ui.graphics.toArgb

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Dark by default so the screen doesn't light up the room during a scan.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(CamSenseColors.Background.toArgb()),
            navigationBarStyle = SystemBarStyle.dark(CamSenseColors.Background.toArgb()),
        )
        setContent {
            CamSenseTheme {
                CamSenseNavHost()
            }
        }
    }
}
