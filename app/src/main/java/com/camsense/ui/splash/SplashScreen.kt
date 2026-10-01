package com.camsense.ui.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.camsense.R
import com.camsense.ui.appContainer
import com.camsense.ui.theme.CamSenseColors
import kotlinx.coroutines.launch

/** Under 1 s: reads settings and starts loading the AI model in the background. */
@Composable
fun SplashScreen(onReady: (onboarded: Boolean) -> Unit) {
    val container = appContainer()
    LaunchedEffect(Unit) {
        container.appScope.launch { container.objectDetector.ensureLoaded() }
        onReady(container.settings.isOnboardingDone())
    }
    Box(Modifier.fillMaxSize().background(CamSenseColors.Background), contentAlignment = Alignment.Center) {
        Image(painterResource(R.drawable.ic_launcher_foreground), contentDescription = null, modifier = Modifier.size(120.dp))
    }
}
