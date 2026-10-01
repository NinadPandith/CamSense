package com.camsense.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.camsense.ui.theme.CamSenseColors
import com.camsense.ui.theme.CamSenseType

/** Matter-of-fact notice with an optional fix (errors, skipped checks, camera hints). */
@Composable
fun NoticeBanner(text: String, modifier: Modifier = Modifier, actionText: String? = null, onAction: (() -> Unit)? = null) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CamSenseColors.Surface, RoundedCornerShape(12.dp))
            .border(1.dp, CamSenseColors.Border, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Rounded.Info, contentDescription = null, tint = CamSenseColors.TextMuted,
                modifier = Modifier.padding(top = 2.dp).size(20.dp))
            Text(text, style = CamSenseType.Body, color = CamSenseColors.Text, modifier = Modifier.weight(1f))
        }
        if (actionText != null && onAction != null) {
            TextAction(actionText, onAction, Modifier.align(Alignment.End))
        }
    }
}

/** One instruction line in a surface pill, at the top of camera screens. */
@Composable
fun InstructionPill(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = CamSenseType.Body,
        color = CamSenseColors.Text,
        modifier = modifier
            .background(CamSenseColors.Surface.copy(alpha = 0.92f), RoundedCornerShape(24.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
    )
}
