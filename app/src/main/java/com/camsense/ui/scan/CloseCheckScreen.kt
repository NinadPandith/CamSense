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

/** Step 5, lights back on: hold the phone near each flagged spot; faster beeps = more electronics. */
@Composable
fun CloseCheckScreen(vm: ScanViewModel, onResults: (String) -> Unit, onStopped: () -> Unit) {
    val askStop = scanStepGuard(vm, onStopped)
    var supported by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { supported = vm.startCloseCheck() }
    val live by vm.modules.magnet.live.collectAsState()
    val spots by vm.spots.collectAsState()
    var saving by remember { mutableStateOf(false) }

    ScanStepScreen(
        step = 5,
        title = stringResource(R.string.close_title),
        instruction = stringResource(R.string.close_instruction),
        onStop = askStop,
        bottom = {
            PrimaryButton(stringResource(R.string.close_button), enabled = !saving, onClick = {
                saving = true
                vm.finish(onResults)
            })
        },
    ) {
        if (!supported) NoticeBanner(stringResource(R.string.close_no_magnetometer))
        else CloseCheckContent(live, spots, vm::selectSpot, vm.modules.magnet::recalibrate)
    }
}
