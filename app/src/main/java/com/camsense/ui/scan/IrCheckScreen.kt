package com.camsense.ui.scan

import androidx.camera.core.CameraSelector
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.camsense.ui.components.SpeakOnEnter
import com.camsense.util.PositionWords

/** Step 4a, lights off: the front camera looks for night-vision glows (orange circles). */
@Composable
fun IrCheckScreen(vm: ScanViewModel, onNext: () -> Unit, onSkipCamera: () -> Unit, onStopped: () -> Unit) {
    val askStop = scanStepGuard(vm, onStopped)
    val title = stringResource(R.string.ir_title)
    val instruction = stringResource(R.string.ir_instruction)
    CameraGate(vm, ScanSteps.dotNumber(ScanStep.IR), title, instruction, frontCamera = true, askStop, onSkipCamera) {
        SpeakOnEnter(title, instruction)
        val overlay by vm.modules.infrared.overlay.collectAsState()
        val stats by vm.modules.infrared.stats.collectAsState()
        var acceptedBright by remember { mutableStateOf(false) }
        val glow = overlay.marks.firstOrNull()?.let { stringResource(R.string.overlay_ir_found, PositionWords.of(it.x, it.y)) }
        LaunchedEffect(glow != null) { glow?.let { vm.voice.say(it) } }

        CameraStepScreen(
            step = ScanSteps.dotNumber(ScanStep.IR),
            instruction = instruction,
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
            coverage = coverageSlot(vm),
        )
    }
}
