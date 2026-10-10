package com.camsense.ui.scan

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.camsense.R
import com.camsense.core.permissions.Permissions
import com.camsense.ui.components.NoticeBanner
import com.camsense.ui.components.PrimaryButton
import com.camsense.ui.components.ScanStepScreen
import com.camsense.ui.components.SecondaryButton

/**
 * Wraps a camera step. Without camera access it explains, offers "Allow", or lets the
 * user continue without the camera checks ([onSkipCamera]). While the step is on
 * screen, the room coverage meter counts where the camera points.
 */
@Composable
fun CameraGate(
    vm: ScanViewModel,
    step: Int,
    title: String,
    instruction: String,
    frontCamera: Boolean,
    onStop: () -> Unit,
    onSkipCamera: () -> Unit,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    var hasCamera by remember { mutableStateOf(Permissions.hasCamera(context)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasCamera = it }

    if (!hasCamera) {
        ScanStepScreen(
            step = step,
            title = title,
            instruction = instruction,
            onStop = onStop,
            bottom = {
                PrimaryButton(stringResource(R.string.action_allow), { launcher.launch(Permissions.camera.first()) })
                SecondaryButton(stringResource(R.string.action_continue), { vm.skipCameraSteps(); onSkipCamera() })
            },
        ) { NoticeBanner(stringResource(R.string.skip_no_camera)) }
        return
    }
    DisposableEffect(frontCamera) {
        vm.startCoverage(frontCamera)
        onDispose { vm.stopCoverage() }
    }
    content()
}
