package com.camsense.ui.scan

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.camsense.R
import com.camsense.ui.components.KeepScreenOn
import com.camsense.ui.components.PrimaryButton
import com.camsense.ui.components.StopScanDialog
import com.camsense.ui.components.rememberSecondsLeft

/**
 * Shared behaviour of steps 2–5: screen kept awake, Back asks "Stop this scan?",
 * and the main button unlocks only after the step's minimum time.
 * Returns a function that opens the stop dialog (for the X button).
 */
@Composable
fun scanStepGuard(vm: ScanViewModel, onStopped: () -> Unit): () -> Unit {
    KeepScreenOn()
    var askStop by remember { mutableStateOf(false) }
    BackHandler { askStop = true }
    if (askStop) {
        StopScanDialog(
            onStop = { askStop = false; vm.abort(); onStopped() },
            onKeep = { askStop = false },
        )
    }
    return { askStop = true }
}

/** Main step button with "Ready in N s" until it unlocks. */
@Composable
fun TimedStepButton(text: String, minMs: Long, onClick: () -> Unit) {
    val left = rememberSecondsLeft(minMs)
    PrimaryButton(
        text = if (left > 0) stringResource(R.string.unlocks_in, left) else text,
        onClick = onClick,
        enabled = left == 0,
    )
}
