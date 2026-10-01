package com.camsense.ui.onboarding

import androidx.camera.core.CameraSelector
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.camsense.R
import com.camsense.core.permissions.Permissions
import com.camsense.modules.infrared.IrCalibrator
import com.camsense.scoring.ScanConfig
import com.camsense.ui.appContainer
import com.camsense.ui.components.CamSenseScreen
import com.camsense.ui.components.CameraOverlayCanvas
import com.camsense.ui.components.CameraPreview
import com.camsense.ui.components.NoticeBanner
import com.camsense.ui.components.PrimaryButton
import com.camsense.ui.components.SecondaryButton
import com.camsense.ui.theme.CamSenseColors
import com.camsense.ui.theme.CamSenseType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class CalState { TESTING, DETECTED, NOT_DETECTED }

/**
 * TV-remote test of the front camera (PRD FR-2.1). Glow seen -> ir_supported = true;
 * nothing after 15 s or "Skip" -> false. Can be redone from Settings.
 */
@Composable
fun IrCalibrationScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val container = appContainer()
    val scope = rememberCoroutineScope()
    val calibrator = remember { IrCalibrator() }
    val detected by calibrator.detected.collectAsState()
    val overlay by calibrator.overlay.collectAsState()
    val hasCamera = remember { Permissions.hasCamera(context) }
    var state by remember { mutableStateOf(if (hasCamera) CalState.TESTING else CalState.NOT_DETECTED) }
    var attempt by remember { mutableStateOf(0) }

    LaunchedEffect(attempt) {
        if (!hasCamera) return@LaunchedEffect
        calibrator.reset()
        state = CalState.TESTING
        delay(ScanConfig.IR_CALIBRATION_TIMEOUT_MS)
        if (state == CalState.TESTING) {
            state = CalState.NOT_DETECTED
            container.settings.setIrSupported(false)
        }
    }
    LaunchedEffect(detected) {
        if (detected && state == CalState.TESTING) {
            state = CalState.DETECTED
            container.settings.setIrSupported(true)
        }
    }

    fun finish(supported: Boolean? = null) {
        scope.launch {
            supported?.let { container.settings.setIrSupported(it) }
            container.settings.setOnboardingDone(true)
            onDone()
        }
    }

    CamSenseScreen(
        title = stringResource(R.string.ir_cal_title),
        bottom = {
            when (state) {
                CalState.TESTING -> SecondaryButton(stringResource(R.string.action_skip), { finish(supported = false) })
                CalState.DETECTED -> PrimaryButton(stringResource(R.string.action_continue), { finish() })
                CalState.NOT_DETECTED -> {
                    PrimaryButton(stringResource(R.string.action_continue), { finish(supported = false) })
                    if (hasCamera) SecondaryButton(stringResource(R.string.action_try_again), { attempt++ })
                }
            }
        },
    ) {
        Text(stringResource(R.string.ir_cal_instruction), style = CamSenseType.Body, color = CamSenseColors.Text)
        if (hasCamera && state == CalState.TESTING) {
            Box(
                Modifier.fillMaxWidth().aspectRatio(9f / 16f).clip(RoundedCornerShape(12.dp))
                    .background(CamSenseColors.Surface)
            ) {
                CameraPreview(CameraSelector.LENS_FACING_FRONT, calibrator.analyzer, Modifier.matchParentSize())
                CameraOverlayCanvas(overlay, mirror = true, modifier = Modifier.matchParentSize())
            }
        }
        when (state) {
            CalState.DETECTED -> NoticeBanner(stringResource(R.string.ir_cal_detected))
            CalState.NOT_DETECTED -> NoticeBanner(stringResource(R.string.ir_cal_not_detected))
            CalState.TESTING -> Unit
        }
    }
}


