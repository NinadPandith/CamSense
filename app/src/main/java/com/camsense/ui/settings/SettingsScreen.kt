package com.camsense.ui.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material.icons.rounded.WbIridescent
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.unit.dp
import com.camsense.BuildConfig
import com.camsense.R
import com.camsense.core.permissions.Permissions
import com.camsense.ui.appContainer
import com.camsense.ui.components.CamSenseScreen
import com.camsense.ui.components.ConfirmDialog
import com.camsense.ui.theme.CamSenseColors
import com.camsense.ui.theme.CamSenseType
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(onBack: () -> Unit, onRecalibrate: () -> Unit, onAbout: () -> Unit, onGetHelp: () -> Unit) {
    val context = LocalContext.current
    val container = appContainer()
    val scope = rememberCoroutineScope()
    val irSupported by container.settings.irSupported.collectAsState(initial = null)
    val voiceOn by container.settings.voiceGuidance.collectAsState(initial = false)
    val safe by container.settings.safeDevices.collectAsState(initial = emptySet())
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmForget by remember { mutableStateOf(false) }
    val cameraOk = Permissions.hasCamera(context)
    val nearbyOk = Permissions.hasNearby(context)

    CamSenseScreen(title = stringResource(R.string.settings_title), onBack = onBack) {
        SettingsRow(Icons.Rounded.WbIridescent, stringResource(R.string.settings_recalibrate),
            stringResource(when (irSupported) {
                true -> R.string.settings_recalibrate_status_yes
                false -> R.string.settings_recalibrate_status_no
                null -> R.string.settings_recalibrate_status_unknown
            }), onClick = onRecalibrate)
        SettingsRow(
            Icons.Rounded.Lock, stringResource(R.string.settings_permissions),
            "${stringResource(R.string.permission_camera)}: ${stringResource(if (cameraOk) R.string.permission_granted else R.string.permission_denied)} · " +
                "${stringResource(R.string.permission_nearby)}: ${stringResource(if (nearbyOk) R.string.permission_granted else R.string.permission_denied)}",
        ) {
            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
        }
        SwitchRow(Icons.Rounded.RecordVoiceOver, stringResource(R.string.settings_voice),
            stringResource(R.string.settings_voice_about), voiceOn) { on ->
            scope.launch { container.settings.setVoiceGuidance(on) }
        }
        if (safe.isNotEmpty()) {
            SettingsRow(Icons.Rounded.VerifiedUser, stringResource(R.string.settings_forget_safe),
                pluralStringResource(R.plurals.settings_safe_count, safe.size, safe.size)) { confirmForget = true }
        }
        SettingsRow(Icons.Rounded.Info, stringResource(R.string.settings_about), null, onClick = onAbout)
        SettingsRow(Icons.Rounded.SupportAgent, stringResource(R.string.settings_get_help), null, onClick = onGetHelp)
        SettingsRow(Icons.Rounded.DeleteForever, stringResource(R.string.settings_delete_all), null,
            tint = CamSenseColors.HighText) { confirmDelete = true }
        if (BuildConfig.DEBUG) {
            SettingsRow(Icons.Rounded.Science, stringResource(R.string.settings_load_sample), null) {
                scope.launch {
                    val sql = runCatching {
                        context.assets.open("sample_data.sql").bufferedReader().readText()
                    }.getOrNull() ?: return@launch
                    val statements = sql.lines().filterNot { it.trimStart().startsWith("--") }.joinToString("\n")
                        .split(";\n").map { it.trim().removeSuffix(";") }.filter { it.isNotEmpty() }
                    container.repository.runSql(statements)
                    Toast.makeText(context, R.string.settings_sample_loaded, Toast.LENGTH_SHORT).show()
                }
            }
        }
        Text(stringResource(R.string.settings_version, BuildConfig.VERSION_NAME), style = CamSenseType.Caption,
            color = CamSenseColors.TextMuted, modifier = Modifier.padding(top = 8.dp))
    }

    if (confirmForget) {
        ConfirmDialog(
            title = stringResource(R.string.settings_forget_safe_title),
            body = stringResource(R.string.settings_forget_safe_body),
            confirm = stringResource(R.string.action_forget),
            dismiss = stringResource(R.string.action_keep),
            onConfirm = {
                confirmForget = false
                scope.launch { container.settings.clearSafe() }
            },
            onDismiss = { confirmForget = false },
        )
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.settings_delete_all_title),
            body = stringResource(R.string.settings_delete_all_body),
            confirm = stringResource(R.string.action_delete),
            dismiss = stringResource(R.string.action_keep),
            onConfirm = {
                confirmDelete = false
                scope.launch {
                    container.repository.deleteAll()
                    container.settings.clearAllExceptOnboarding()
                    Toast.makeText(context, R.string.settings_deleted_all, Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    tint: Color = CamSenseColors.Text,
    onClick: () -> Unit,
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(onClick = onClick).padding(vertical = 12.dp),
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(24.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = CamSenseType.Body, color = tint)
                subtitle?.let { Text(it, style = CamSenseType.Caption, color = CamSenseColors.TextMuted) }
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = CamSenseColors.TextMuted)
        }
        HorizontalDivider(color = CamSenseColors.Border)
    }
}

@Composable
private fun SwitchRow(icon: ImageVector, title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                .toggleable(value = checked, role = Role.Switch, onValueChange = onChange).padding(vertical = 12.dp),
        ) {
            Icon(icon, null, tint = CamSenseColors.Text, modifier = Modifier.size(24.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = CamSenseType.Body, color = CamSenseColors.Text)
                subtitle?.let { Text(it, style = CamSenseType.Caption, color = CamSenseColors.TextMuted) }
            }
            Switch(checked = checked, onCheckedChange = null, colors = SwitchDefaults.colors(
                checkedTrackColor = CamSenseColors.Primary, checkedThumbColor = CamSenseColors.OnPrimary))
        }
        HorizontalDivider(color = CamSenseColors.Border)
    }
}
