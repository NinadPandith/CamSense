package com.camsense.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.camsense.BuildConfig
import com.camsense.R
import com.camsense.core.camera.CameraOverlay
import com.camsense.ui.theme.CamSenseColors
import com.camsense.ui.theme.CamSenseType
import com.camsense.ui.theme.Spacing

/** Plain screen: optional back arrow, title, scrolling body, bottom actions. */
@Composable
fun CamSenseScreen(
    title: String?,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {},
    bottom: @Composable ColumnScope.() -> Unit = {},
    scroll: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        Modifier.fillMaxSize().background(CamSenseColors.Background).statusBarsPadding().navigationBarsPadding()
    ) {
        if (onBack != null || title != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp).height(56.dp),
            ) {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back), tint = CamSenseColors.Text)
                    }
                } else Spacer(Modifier.padding(start = 16.dp))
                Text(title.orEmpty(), style = CamSenseType.Title, color = CamSenseColors.Text,
                    modifier = Modifier.weight(1f).semantics { heading() })
                actions()
            }
        }
        val body = Modifier.weight(1f).fillMaxWidth().padding(horizontal = Spacing.screen)
        Column(
            modifier = if (scroll) body.verticalScroll(rememberScrollState()) else body,
            verticalArrangement = Arrangement.spacedBy(Spacing.l),
            content = content,
        )
        Column(
            Modifier.fillMaxWidth().padding(horizontal = Spacing.screen, vertical = Spacing.l),
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
            content = bottom,
        )
    }
}

/** Non-camera guided-scan step: six dots, title, one instruction, one main button. */
@Composable
fun ScanStepScreen(
    step: Int,
    title: String,
    instruction: String,
    onStop: (() -> Unit)?,
    bottom: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize().background(CamSenseColors.Background).statusBarsPadding().navigationBarsPadding()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = Spacing.screen),
        ) {
            StepProgress(step, Modifier.weight(1f))
            if (onStop != null) {
                IconButton(onClick = onStop) {
                    Icon(Icons.Rounded.Close, stringResource(R.string.stop_scan), tint = CamSenseColors.Text)
                }
            }
        }
        Column(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = Spacing.screen).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.l),
        ) {
            Text(title, style = CamSenseType.Title, color = CamSenseColors.Text, modifier = Modifier.semantics { heading() })
            Text(instruction, style = CamSenseType.Body, color = CamSenseColors.TextMuted)
            content()
        }
        Column(
            Modifier.fillMaxWidth().padding(horizontal = Spacing.screen, vertical = Spacing.l),
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
            content = bottom,
        )
    }
}

/**
 * Camera step: full-bleed preview, one instruction pill at the top, one button at
 * the bottom. In the dark sweep ([dim]) everything but the preview, overlays and
 * stop button is dimmed to 70%.
 */
@Composable
fun CameraStepScreen(
    step: Int,
    instruction: String,
    overlay: CameraOverlay,
    mirror: Boolean,
    dim: Boolean,
    onStop: () -> Unit,
    preview: @Composable () -> Unit,
    banner: @Composable ColumnScope.() -> Unit,
    bottom: @Composable ColumnScope.() -> Unit,
    /** Debug builds only: frames/second, brightness, spots, for tuning on real phones. */
    debugLine: String? = null,
) {
    val chromeAlpha = if (dim) 0.7f else 1f
    Box(Modifier.fillMaxSize().background(CamSenseColors.Background)) {
        preview()
        CameraOverlayCanvas(overlay, mirror, Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = Spacing.screen),
            ) {
                StepProgress(step, Modifier.weight(1f).alpha(chromeAlpha))
                IconButton(onClick = onStop) {
                    Icon(Icons.Rounded.Close, stringResource(R.string.stop_scan), tint = CamSenseColors.Text)
                }
            }
            InstructionPill(instruction, Modifier.padding(horizontal = Spacing.screen).alpha(chromeAlpha))
            Column(
                Modifier.padding(horizontal = Spacing.screen, vertical = Spacing.s),
                verticalArrangement = Arrangement.spacedBy(Spacing.s),
                content = banner,
            )
            Spacer(Modifier.weight(1f))
            if (BuildConfig.DEBUG && !debugLine.isNullOrEmpty()) {
                Text(
                    debugLine,
                    style = CamSenseType.Caption,
                    color = CamSenseColors.Text,
                    modifier = Modifier.padding(horizontal = Spacing.screen)
                        .background(CamSenseColors.Background.copy(alpha = 0.7f)).padding(4.dp),
                )
            }
            Column(
                Modifier.fillMaxWidth().padding(horizontal = Spacing.screen, vertical = Spacing.l).alpha(chromeAlpha),
                verticalArrangement = Arrangement.spacedBy(Spacing.m),
                content = bottom,
            )
        }
    }
}
