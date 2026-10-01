package com.camsense.ui.results

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.camsense.R
import com.camsense.data.db.UserVerdict
import com.camsense.ui.components.SecondaryButton
import com.camsense.ui.components.icon
import com.camsense.ui.components.label
import com.camsense.ui.theme.CamSenseColors
import com.camsense.ui.theme.CamSenseType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/** Finding detail: snapshot, module, plain reason, confidence; mark it checked or confirmed. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindingSheet(
    finding: FindingUi,
    readOnly: Boolean,
    onVerdict: (UserVerdict) -> Unit,
    onDismiss: () -> Unit,
) {
    val image by produceState<ImageBitmap?>(null, finding.snapshot) {
        value = finding.snapshot?.let { f ->
            withContext(Dispatchers.IO) { BitmapFactory.decodeFile(f.path)?.asImageBitmap() }
        }
    }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = CamSenseColors.Surface) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 16.dp).navigationBarsPadding(),
        ) {
            image?.let {
                Image(it, contentDescription = null, contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 260.dp).clip(RoundedCornerShape(12.dp)))
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(finding.module.icon(), null, tint = CamSenseColors.Text, modifier = Modifier.size(48.dp))
                Column {
                    Text(stringResource(finding.module.label()), style = CamSenseType.Heading, color = CamSenseColors.Text)
                    Text(stringResource(R.string.finding_confidence, (finding.confidence * 100).roundToInt()),
                        style = CamSenseType.Caption, color = CamSenseColors.TextMuted)
                }
            }
            Text(finding.reason, style = CamSenseType.Body, color = CamSenseColors.Text)
            finding.technical?.let {
                Text(stringResource(R.string.finding_details), style = CamSenseType.Caption, color = CamSenseColors.TextMuted)
                Text(it, style = CamSenseType.Data, color = CamSenseColors.TextMuted)
            }
            if (!readOnly || finding.verdict == UserVerdict.UNREVIEWED) {
                SecondaryButton(stringResource(R.string.verdict_checked), { onVerdict(UserVerdict.CHECKED_OK) })
                SecondaryButton(stringResource(R.string.verdict_confirmed), { onVerdict(UserVerdict.CONFIRMED) })
            }
        }
    }
}
