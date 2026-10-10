package com.camsense.ui.evidence

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.camsense.R
import com.camsense.data.db.EvidenceItemEntity
import com.camsense.data.db.EvidenceType
import com.camsense.ui.appContainer
import com.camsense.ui.components.CamSenseScreen
import com.camsense.ui.components.PrimaryButton
import com.camsense.ui.components.SecondaryButton
import com.camsense.ui.components.shareFile
import com.camsense.ui.components.formatTime
import com.camsense.ui.theme.CamSenseColors
import com.camsense.ui.theme.CamSenseType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Saved photos and report for a scan, with the time, place and SHA-256 of each file. */
@Composable
fun EvidenceScreen(scanId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val container = appContainer()
    val pdf = remember(scanId) { container.evidenceStore.pdfFile(scanId) }
    val details by remember(scanId) { container.repository.observeScan(scanId) }.collectAsState(initial = null)

    CamSenseScreen(
        title = stringResource(R.string.evidence_title),
        onBack = onBack,
        bottom = {
            val d = details
            val hasPdf = d?.evidenceItems?.any { it.type == EvidenceType.REPORT_PDF } == true && pdf.exists()
            if (hasPdf) {
                val text = stringResource(R.string.share_report_text, d?.scan?.placeName ?: stringResource(R.string.history_unnamed))
                val title = stringResource(R.string.share_report_title)
                PrimaryButton(stringResource(R.string.evidence_share), { shareFile(context, pdf, "application/pdf", text, title) })
                SecondaryButton(stringResource(R.string.evidence_back), onBack)
            } else {
                PrimaryButton(stringResource(R.string.evidence_back), onBack)
            }
        },
    ) {
        val d = details ?: return@CamSenseScreen
        Label(stringResource(R.string.evidence_place))
        Text(d.scan.placeName ?: stringResource(R.string.history_unnamed), style = CamSenseType.Body, color = CamSenseColors.Text)
        Label(stringResource(R.string.evidence_time))
        Text(formatTime(d.scan.startedAt), style = CamSenseType.Body, color = CamSenseColors.Text)
        Text(stringResource(R.string.evidence_where), style = CamSenseType.Caption, color = CamSenseColors.TextMuted)

        if (d.evidenceItems.isEmpty()) {
            Text(stringResource(R.string.evidence_none), style = CamSenseType.Body, color = CamSenseColors.TextMuted)
        }
        d.evidenceItems.sortedBy { it.type.ordinal }.forEach { item ->
            EvidenceCard(item, container.evidenceStore.file(item.filePath))
        }
    }
}

@Composable
private fun Label(text: String) = Text(text, style = CamSenseType.Caption, color = CamSenseColors.TextMuted)

@Composable
private fun EvidenceCard(item: EvidenceItemEntity, file: File) {
    val image by produceState<ImageBitmap?>(null, file) {
        if (item.type == EvidenceType.PHOTO && file.exists()) {
            value = withContext(Dispatchers.IO) { BitmapFactory.decodeFile(file.path)?.asImageBitmap() }
        }
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, CamSenseColors.Border, RoundedCornerShape(12.dp)).padding(16.dp),
    ) {
        image?.let {
            Image(it, contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp).clip(RoundedCornerShape(12.dp)))
        }
        if (item.type != EvidenceType.PHOTO) {
            Text(stringResource(if (item.type == EvidenceType.REPORT_PDF) R.string.evidence_report_pdf else R.string.evidence_report), style = CamSenseType.Heading, color = CamSenseColors.Text)
        }
        Text(file.name, style = CamSenseType.Data, color = CamSenseColors.Text)
        Label(stringResource(R.string.evidence_hash))
        Text(item.sha256, style = CamSenseType.Data, color = CamSenseColors.TextMuted)
    }
}
