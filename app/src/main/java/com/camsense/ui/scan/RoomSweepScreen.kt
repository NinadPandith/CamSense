package com.camsense.ui.scan

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.camsense.R
import com.camsense.core.permissions.Permissions
import com.camsense.scoring.ScanConfig
import com.camsense.ui.components.CameraPreview
import com.camsense.ui.components.CameraStepScreen
import com.camsense.ui.components.NoticeBanner
import com.camsense.ui.components.PrimaryButton
import com.camsense.ui.components.ScanStepScreen
import com.camsense.ui.components.SecondaryButton
import kotlinx.coroutines.launch

/**
 * Step 3, lights on: the AI marks likely hiding spots with lime "check this" boxes.
 * Without camera access, steps 3, 4a and 4b are skipped and the scan jumps to the
 * close check. If the model fails to load, this step is skipped.
 */
@Composable
fun RoomSweepScreen(vm: ScanViewModel, onNext: (showIr: Boolean) -> Unit, onCameraSkipped: () -> Unit, onStopped: () -> Unit) {
    val context = LocalContext.current
    val askStop = scanStepGuard(vm, onStopped)
    val scope = rememberCoroutineScope()
    var hasCamera by remember { mutableStateOf(Permissions.hasCamera(context)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasCamera = it }

    fun next() {
        vm.endRoomSweep()
        scope.launch { onNext(vm.prepareIrStep()) }
    }

    if (!hasCamera) {
        ScanStepScreen(
            step = 3,
            title = stringResource(R.string.room_title),
            instruction = stringResource(R.string.room_instruction),
            onStop = askStop,
            bottom = {
                PrimaryButton(stringResource(R.string.action_allow), { launcher.launch(Permissions.camera.first()) })
                SecondaryButton(stringResource(R.string.action_continue), { vm.skipCameraSteps(); onCameraSkipped() })
            },
        ) { NoticeBanner(stringResource(R.string.skip_no_camera)) }
        return
    }

    LaunchedEffect(Unit) {
        if (!vm.startRoomSweep()) next() // model failed to load: step skipped
    }
    val overlay by vm.modules.objects.overlay.collectAsState()
    val stats by vm.modules.objects.stats.collectAsState()

    CameraStepScreen(
        step = 3,
        instruction = stringResource(R.string.room_instruction),
        overlay = overlay,
        mirror = false,
        dim = false,
        onStop = askStop,
        preview = { CameraPreview(CameraSelector.LENS_FACING_BACK, vm.modules.objects.analyzer, Modifier.fillMaxSize()) },
        banner = { if (overlay.tooFast) NoticeBanner(stringResource(R.string.banner_too_fast)) },
        bottom = { TimedStepButton(stringResource(R.string.room_button), ScanConfig.ROOM_SWEEP_MIN_MS, ::next) },
        debugLine = stats.line(),
    )
}
