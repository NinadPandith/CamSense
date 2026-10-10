package com.camsense.ui.scan

import androidx.camera.core.CameraSelector
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.camsense.R
import com.camsense.scoring.ScanConfig
import com.camsense.ui.appContainer
import com.camsense.ui.components.CameraPreview
import com.camsense.ui.components.CameraStepScreen
import com.camsense.ui.components.NoticeBanner
import com.camsense.ui.components.SpeakOnEnter

/**
 * Step 3, lights on: the AI marks likely hiding spots with lime "check this" boxes.
 * Without camera access, the camera steps are skipped. If the model fails to load,
 * this step is skipped.
 */
@Composable
fun RoomSweepScreen(vm: ScanViewModel, onNext: () -> Unit, onSkipCamera: () -> Unit, onStopped: () -> Unit) {
    val askStop = scanStepGuard(vm, onStopped)
    val title = stringResource(R.string.room_title)
    val instruction = stringResource(R.string.room_instruction)
    CameraGate(vm, ScanSteps.dotNumber(ScanStep.ROOM), title, instruction, frontCamera = false, askStop, onSkipCamera) {
        SpeakOnEnter(title, instruction)
        LaunchedEffect(Unit) {
            if (!vm.startRoomSweep()) onNext() // model failed to load: step skipped
        }
        val overlay by vm.modules.objects.overlay.collectAsState()
        val stats by vm.modules.objects.stats.collectAsState()
        val voice = appContainer().voice
        val checkThis = stringResource(R.string.voice_check_this)
        // Say the name of each new hiding spot once ("Check the clock").
        val labels = overlay.marks.mapNotNull { it.label }.toSet()
        LaunchedEffect(labels) { labels.forEach { voice.say("$checkThis: $it") } }

        CameraStepScreen(
            step = ScanSteps.dotNumber(ScanStep.ROOM),
            instruction = instruction,
            overlay = overlay,
            mirror = false,
            dim = false,
            onStop = askStop,
            preview = { CameraPreview(CameraSelector.LENS_FACING_BACK, vm.modules.objects.analyzer, Modifier.fillMaxSize()) },
            banner = { if (overlay.tooFast) NoticeBanner(stringResource(R.string.banner_too_fast)) },
            bottom = {
                TimedStepButton(stringResource(R.string.room_button), ScanConfig.ROOM_SWEEP_MIN_MS) {
                    vm.endRoomSweep()
                    onNext()
                }
            },
            debugLine = stats.line(),
            coverage = coverageSlot(vm),
        )
    }
}
