package com.camsense.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.camsense.R
import com.camsense.ui.theme.CamSenseColors
import com.camsense.ui.theme.CamSenseType

@Composable
fun ConfirmDialog(
    title: String,
    body: String,
    confirm: String,
    dismiss: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CamSenseColors.SurfaceRaised,
        title = { Text(title, style = CamSenseType.Heading, color = CamSenseColors.Text) },
        text = { Text(body, style = CamSenseType.Body, color = CamSenseColors.Text) },
        confirmButton = { TextAction(confirm, onConfirm) },
        dismissButton = { TextAction(dismiss, onDismiss) },
    )
}

/** Back during steps 2–5. */
@Composable
fun StopScanDialog(onStop: () -> Unit, onKeep: () -> Unit) = ConfirmDialog(
    title = stringResource(R.string.stop_title),
    body = stringResource(R.string.stop_body),
    confirm = stringResource(R.string.stop_confirm),
    dismiss = stringResource(R.string.stop_keep),
    onConfirm = onStop,
    onDismiss = onKeep,
)
