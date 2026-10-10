package com.camsense.ui.scan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.camsense.R
import com.camsense.ui.components.NoticeBanner
import com.camsense.ui.components.ScanStepScreen
import com.camsense.ui.components.SecondaryButton
import com.camsense.ui.components.SpeakOnEnter
import com.camsense.ui.components.TextAction
import com.camsense.ui.theme.CamSenseColors
import com.camsense.ui.theme.CamSenseType

/**
 * Bathroom / changing-room step: the fingertip test for two-way mirrors. On a normal
 * mirror there's a small gap between your fingertip and its reflection (the glass is
 * in front of the silver); on a two-way mirror the fingertip touches its reflection.
 */
@Composable
fun MirrorCheckScreen(vm: ScanViewModel, onNext: () -> Unit, onStopped: () -> Unit) {
    val askStop = scanStepGuard(vm, onStopped)
    val title = stringResource(R.string.mirror_title)
    val instruction = stringResource(R.string.mirror_instruction)
    SpeakOnEnter(title, instruction)

    fun answer(a: ScanViewModel.MirrorAnswer) {
        vm.recordMirror(a)
        onNext()
    }

    ScanStepScreen(
        step = ScanSteps.dotNumber(ScanStep.MIRROR),
        title = title,
        instruction = instruction,
        onStop = askStop,
        bottom = {
            SecondaryButton(stringResource(R.string.mirror_gap), { answer(ScanViewModel.MirrorAnswer.NORMAL) })
            SecondaryButton(stringResource(R.string.mirror_no_gap), { answer(ScanViewModel.MirrorAnswer.NO_GAP) })
            TextAction(stringResource(R.string.mirror_none), { answer(ScanViewModel.MirrorAnswer.NO_MIRROR) },
                Modifier.align(Alignment.CenterHorizontally))
        },
    ) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Rounded.TouchApp, null, tint = CamSenseColors.Primary, modifier = Modifier.size(48.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.mirror_how_gap), style = CamSenseType.Body, color = CamSenseColors.Text)
                Text(stringResource(R.string.mirror_how_no_gap), style = CamSenseType.Body, color = CamSenseColors.Text)
            }
        }
        NoticeBanner(stringResource(R.string.mirror_tip))
    }
}
