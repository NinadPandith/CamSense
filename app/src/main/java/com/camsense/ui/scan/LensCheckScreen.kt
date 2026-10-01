package com.camsense.ui.scan

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.camsense.R
import com.camsense.core.camera.CameraOverlay
import com.camsense.scoring.ScanConfig
import com.camsense.ui.components.CameraPreview
import com.camsense.ui.components.CameraStepScreen
import com.camsense.ui.components.NoticeBanner
import com.camsense.ui.components.TextAction

/**
 * Step 4b: torch + rear camera; confirmed glints get red circles and a snapshot.
 * The AI model also runs on every few frames so a glint inside, say, a smoke
 * detector shares that object's target_key. The torch turns off on leaving.
 */
@Composable
fun LensCheckScreen(vm: ScanViewModel, onNext: () -> Unit, onStopped: () -> Unit) {
    val context = LocalContext.current
    val askStop = scanStepGuard(vm, onStopped)
    LaunchedEffect(Unit) { vm.startLensStep() }
    val glint by vm.modules.glint.overlay.collectAsState()
    val objects by vm.modules.objects.overlay.collectAsState()
    var acceptedBright by remember { mutableStateOf(false) }
    val overlay = if (objects.frameWidth == glint.frameWidth && objects.frameHeight == glint.frameHeight)
        glint.copy(marks = objects.marks + glint.marks) else glint

    CameraStepScreen(
        step = 4,
        instruction = stringResource(R.string.lens_instruction),
        overlay = overlay,
        mirror = false,
        dim = true,
        onStop = askStop,
        preview = {
            CameraPreview(CameraSelector.LENS_FACING_BACK, vm.modules.glint.analyzer, Modifier.fillMaxSize(), torch = true)
        },
        banner = {
            if (glint.tooBright && !acceptedBright) {
                NoticeBanner(stringResource(R.string.banner_too_bright),
                    actionText = stringResource(R.string.banner_continue_anyway),
                    onAction = { acceptedBright = true; vm.modules.glint.brightRoomAccepted = true })
            } else if (glint.tooFast) {
                NoticeBanner(stringResource(R.string.banner_too_fast))
            }
        },
        bottom = {
            if (overlay.marks.any { it.kind == CameraOverlay.Kind.GLINT }) {
                TextAction(stringResource(R.string.lens_take_photo), {
                    vm.modules.glint.requestSnapshot()
                    Toast.makeText(context, R.string.lens_photo_saved, Toast.LENGTH_SHORT).show()
                })
            }
            TimedStepButton(stringResource(R.string.lens_button), ScanConfig.LENS_STEP_MIN_MS) {
                vm.endLensStep()
                onNext()
            }
        },
    )
}
