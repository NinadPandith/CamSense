package com.camsense.ui.help

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LocalPhone
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.camsense.R
import com.camsense.ui.components.CamSenseScreen
import com.camsense.ui.components.NoticeBanner
import com.camsense.ui.theme.CamSenseColors
import com.camsense.ui.theme.CamSenseType

/**
 * 112 (emergency), 1930 (cybercrime helpline) and cybercrime.gov.in, each tappable.
 * Verify these numbers again before release (Content Guidelines).
 */
@Composable
fun GetHelpScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    fun dial(number: String) = context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
    fun open(site: String) = context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://$site")))

    CamSenseScreen(title = stringResource(R.string.help_title), onBack = onBack) {
        val emergency = stringResource(R.string.help_emergency_number)
        val cyber = stringResource(R.string.help_cyber_number)
        val site = stringResource(R.string.help_report_site)
        HelpRow(Icons.Rounded.LocalPhone, stringResource(R.string.help_emergency), emergency) { dial(emergency) }
        HelpRow(Icons.Rounded.SupportAgent, stringResource(R.string.help_cyber), cyber) { dial(cyber) }
        HelpRow(Icons.Rounded.Language, stringResource(R.string.help_report), site) { open(site) }
        NoticeBanner(stringResource(R.string.help_tip))
    }
}

@Composable
private fun HelpRow(icon: ImageVector, title: String, value: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).clip(shape).border(1.dp, CamSenseColors.Border, shape)
            .clickable(onClick = onClick).padding(16.dp),
    ) {
        Icon(icon, null, tint = CamSenseColors.Primary, modifier = Modifier.size(24.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = CamSenseType.Label, color = CamSenseColors.TextMuted)
            Text(value, style = CamSenseType.Title, color = CamSenseColors.Primary)
        }
    }
}
