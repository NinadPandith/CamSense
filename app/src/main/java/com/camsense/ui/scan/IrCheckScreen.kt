package com.camsense.ui.scan

import androidx.camera.core.CameraSelector
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.camsense.R
import com.camsense.scoring.ScanConfig
import com.camsense.ui.components.CameraPreview
import com.camsense.ui.components.CameraStepScreen
import com.camsense.ui.components.NoticeBanner

/** Step 4a, lights off: the front camera looks for night-vision glows (orange circles). */
@Composable
fun IrCheckScreen(vm: ScanViewModel, onNext: () -> Unit, onStopped: () -> Unit) {
    val askStop = scanStepGuard(vm, onStopped)
    val overlay by vm.modules.infrared.overlay.collectAsState()
    var acceptedBright by remember { mutableStateOf(false) }
    val stats by vm.modules.infrared.stats.collectAsState()

    CameraStepScreen(
        step = 4,
        instruction = stringResource(R.string.ir_instruction),
        overlay = overlay,
        mirror = true,
        dim = true,
        onStop = askStop,
        preview = { CameraPreview(CameraSelector.LENS_FACING_FRONT, vm.modules.infrared.analyzer, Modifier.fillMaxSize()) },
        banner = {
            if (overlay.tooBright && !acceptedBright) {
                NoticeBanner(stringResource(R.string.banner_too_bright),
                    actionText = stringResource(R.string.banner_continue_anyway),
                    onAction = { acceptedBright = true; vm.modules.infrared.brightRoomAccepted = true })
            } else if (overlay.tooFast) {
                NoticeBanner(stringResource(R.string.banner_too_fast))
            }
        },
        bottom = {
            TimedStepButton(stringResource(R.string.action_next), ScanConfig.IR_STEP_MIN_MS) {
                vm.endIrStep()
                onNext()
            }
        },
        debugLine = stats.line(),
    )
}
