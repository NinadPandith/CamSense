package com.camsense.ui.results

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.camsense.R
import com.camsense.data.db.UserVerdict
import com.camsense.scoring.RiskLevel
import com.camsense.ui.appContainer
import com.camsense.ui.components.CamSenseScreen
import com.camsense.ui.components.ConfirmDialog
import com.camsense.ui.components.PrimaryButton
import com.camsense.ui.components.SecondaryButton
import com.camsense.ui.components.formatTime
import com.camsense.ui.components.icon
import com.camsense.ui.components.label
import com.camsense.ui.components.skipReasonText
import com.camsense.ui.components.style
import com.camsense.ui.components.vibrateOnce
import com.camsense.ui.theme.CamSenseColors
import com.camsense.ui.theme.CamSenseType

/**
 * Same layout for every level: risk level (icon + word), one-line summary, "Why"
 * findings, "What to do", skipped checks, the limits caption, then two buttons.
 * Past scans reopen this screen read-only (Scan detail), with delete.
 */
@Composable
fun ResultsScreen(
    scanId: String,
    readOnly: Boolean,
    onHome: () -> Unit,
    onBack: () -> Unit,
    onScanAgain: (place: String?) -> Unit,
    onCheckSpots: () -> Unit,
    onEvidence: () -> Unit,
    onGetHelp: () -> Unit,
    onDeleted: () -> Unit,
) {
    val context = LocalContext.current
    val container = appContainer()
    val vm: ResultsViewModel = viewModel(factory = viewModelFactory { initializer { ResultsViewModel(container, scanId) } })
    val ui by vm.state.collectAsState()
    var openFinding by remember { mutableStateOf<String?>(null) }
    var menu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    BackHandler(onBack = onBack)
    val state = ui ?: return
    LaunchedEffect(state.scanId) {
        // One short vibration when a High result opens.
        if (!readOnly && state.level == RiskLevel.HIGH) vibrateOnce(context)
    }

    fun saveEvidence() = vm.saveEvidence(
        onSaved = {
            Toast.makeText(context, R.string.evidence_saved, Toast.LENGTH_SHORT).show()
            onEvidence()
        },
        onError = { Toast.makeText(context, R.string.evidence_storage_full, Toast.LENGTH_LONG).show() },
    )

    CamSenseScreen(
        title = state.placeName ?: stringResource(R.string.history_unnamed),
        onBack = onBack,
        actions = {
            if (readOnly) {
                IconButton(onClick = { menu = true }) {
                    Icon(Icons.Rounded.MoreVert, stringResource(R.string.results_more), tint = CamSenseColors.Text)
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.results_delete_scan)) },
                        onClick = { menu = false; confirmDelete = true })
                }
            }
        },
        bottom = { Buttons(state, readOnly, onHome, onScanAgain, onCheckSpots, onEvidence, onGetHelp, ::saveEvidence) },
    ) {
        RiskHeader(state)
        Text(formatTime(state.startedAt), style = CamSenseType.Caption, color = CamSenseColors.TextMuted)

        Section(stringResource(R.string.results_why))
        if (state.findings.isEmpty()) {
            Text(
                if (state.modulesRan >= 6) stringResource(R.string.results_low_all_ran)
                else stringResource(R.string.results_low_some_ran, state.modulesRan),
                style = CamSenseType.Body, color = CamSenseColors.Text,
            )
        } else {
            Text(stringResource(R.string.results_checked_count, state.checkedCount, state.findings.size),
                style = CamSenseType.Caption, color = CamSenseColors.TextMuted)
            state.findings.forEach { f -> FindingRow(f) { openFinding = f.id } }
        }

        Section(stringResource(R.string.results_what_to_do))
        Text(stringResource(nextSteps(state.level)), style = CamSenseType.Body, color = CamSenseColors.Text)

        if (state.skipped.isNotEmpty()) {
            Section(stringResource(R.string.results_skipped))
            state.skipped.forEach { s ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(s.module.icon(), null, tint = CamSenseColors.TextMuted, modifier = Modifier.size(24.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(s.module.label()), style = CamSenseType.Label, color = CamSenseColors.Text)
                        Text(stringResource(skipReasonText(s.skipReason)), style = CamSenseType.Body, color = CamSenseColors.TextMuted)
                    }
                }
            }
        }
        Text(stringResource(R.string.limits_caption), style = CamSenseType.Caption, color = CamSenseColors.TextMuted)
    }

    state.findings.firstOrNull { it.id == openFinding }?.let { f ->
        FindingSheet(f, readOnly, onVerdict = { v -> vm.setVerdict(f.id, v); openFinding = null }, onDismiss = { openFinding = null })
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.results_delete_title),
            body = stringResource(R.string.results_delete_body),
            confirm = stringResource(R.string.action_delete),
            dismiss = stringResource(R.string.action_keep),
            onConfirm = {
                confirmDelete = false
                vm.delete {
                    Toast.makeText(context, R.string.results_deleted, Toast.LENGTH_SHORT).show()
                    onDeleted()
                }
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun RiskHeader(state: ResultsUi) {
    val s = state.level.style()
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().background(s.tint, RoundedCornerShape(12.dp)).padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(s.icon, contentDescription = null, tint = s.text, modifier = Modifier.size(48.dp))
            Text(stringResource(s.word), style = CamSenseType.Display, color = s.text, modifier = Modifier.semantics { heading() })
        }
        Text(stringResource(summary(state.level)), style = CamSenseType.Body, color = CamSenseColors.Text)
    }
}

@Composable
private fun Section(title: String) {
    Text(title, style = CamSenseType.Heading, color = CamSenseColors.Text, modifier = Modifier.padding(top = 8.dp).semantics { heading() })
}

@Composable
private fun FindingRow(f: FindingUi, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(shape)
            .border(1.dp, CamSenseColors.Border, shape).background(CamSenseColors.Surface)
            .clickable(onClick = onClick).padding(16.dp),
    ) {
        Icon(f.module.icon(), contentDescription = stringResource(f.module.label()), tint = CamSenseColors.Text,
            modifier = Modifier.size(24.dp))
        Column(Modifier.weight(1f)) {
            Text(f.reason, style = CamSenseType.Body, color = CamSenseColors.Text)
            when (f.verdict) {
                UserVerdict.CHECKED_OK -> Text(stringResource(R.string.verdict_badge_checked), style = CamSenseType.Caption, color = CamSenseColors.TextMuted)
                UserVerdict.CONFIRMED -> Text(stringResource(R.string.verdict_badge_confirmed), style = CamSenseType.Caption, color = CamSenseColors.HighText)
                UserVerdict.UNREVIEWED -> Unit
            }
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = CamSenseColors.TextMuted)
    }
}

@Composable
private fun Buttons(
    state: ResultsUi,
    readOnly: Boolean,
    onHome: () -> Unit,
    onScanAgain: (String?) -> Unit,
    onCheckSpots: () -> Unit,
    onEvidence: () -> Unit,
    onGetHelp: () -> Unit,
    onSaveEvidence: () -> Unit,
) {
    if (readOnly) {
        when {
            state.evidenceSaved -> PrimaryButton(stringResource(R.string.results_evidence), onEvidence)
            state.level == RiskLevel.HIGH -> PrimaryButton(stringResource(R.string.results_save_evidence), onSaveEvidence)
        }
        if (state.level == RiskLevel.HIGH) SecondaryButton(stringResource(R.string.results_get_help), onGetHelp)
        return
    }
    when (state.level) {
        RiskLevel.LOW -> {
            PrimaryButton(stringResource(R.string.results_done), onHome)
            SecondaryButton(stringResource(R.string.results_scan_another), { onScanAgain(null) })
        }
        RiskLevel.MEDIUM -> {
            PrimaryButton(stringResource(R.string.results_check_spots), onCheckSpots)
            SecondaryButton(stringResource(R.string.results_scan_again), { onScanAgain(state.placeName) })
        }
        RiskLevel.HIGH -> {
            // Evidence is not saved until the user taps.
            if (state.evidenceSaved) PrimaryButton(stringResource(R.string.results_evidence), onEvidence)
            else PrimaryButton(stringResource(R.string.results_save_evidence), onSaveEvidence)
            SecondaryButton(stringResource(R.string.results_get_help), onGetHelp)
        }
    }
}

private fun summary(level: RiskLevel) = when (level) {
    RiskLevel.LOW -> R.string.summary_low
    RiskLevel.MEDIUM -> R.string.summary_medium
    RiskLevel.HIGH -> R.string.summary_high
}

private fun nextSteps(level: RiskLevel) = when (level) {
    RiskLevel.LOW -> R.string.next_low
    RiskLevel.MEDIUM -> R.string.next_medium
    RiskLevel.HIGH -> R.string.next_high
}
