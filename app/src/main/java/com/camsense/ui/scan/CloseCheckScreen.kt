package com.camsense.ui.scan

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.camsense.R
import com.camsense.ui.components.NoticeBanner
import com.camsense.ui.components.PrimaryButton
import com.camsense.ui.components.ScanStepScreen
import com.camsense.ui.components.SpeakOnEnter

/** Step 5, lights back on: hold the phone near each flagged spot; faster beeps = more electronics. */
@Composable
fun CloseCheckScreen(vm: ScanViewModel, onResults: () -> Unit, onStopped: () -> Unit) {
    val askStop = scanStepGuard(vm, onStopped)
    var supported by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { supported = vm.startCloseCheck() }
    val live by vm.modules.magnet.live.collectAsState()
    val hotCold by vm.hotCold.collectAsState()
    val title = stringResource(R.string.close_title)
    val instruction = stringResource(R.string.close_instruction)
    SpeakOnEnter(title, instruction)
    val passed = stringResource(R.string.hotcold_passed)
    LaunchedEffect(hotCold.passedPeak) { if (hotCold.passedPeak) vm.voice.say(passed) }
    val spots by vm.spots.collectAsState()
    var saving by remember { mutableStateOf(false) }

    ScanStepScreen(
        step = ScanSteps.dotNumber(ScanStep.CLOSE),
        title = title,
        instruction = instruction,
        onStop = askStop,
        bottom = {
            PrimaryButton(stringResource(R.string.close_button), enabled = !saving, onClick = {
                saving = true
                onResults()
            })
        },
    ) {
        if (!supported) NoticeBanner(stringResource(R.string.close_no_magnetometer))
        else CloseCheckContent(live, hotCold, spots, vm::selectSpot, vm.modules.magnet::recalibrate)
    }
}
