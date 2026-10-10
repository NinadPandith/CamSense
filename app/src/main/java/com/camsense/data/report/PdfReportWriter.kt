package com.camsense.data.report

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.camsense.R
import com.camsense.core.ScanMode
import com.camsense.data.db.RunStatus
import com.camsense.data.db.ScanWithDetails
import com.camsense.scoring.RiskLevel
import com.camsense.ui.components.label
import com.camsense.ui.components.skipReasonText
import java.io.File
import java.text.DateFormat
import java.util.Date
import kotlin.math.roundToInt

/**
 * Evidence report as a PDF (android.graphics.pdf, no extra library): place, time,
 * result, every finding with its reason, devices, skipped checks, photos with the
 * flagged spot circled, and the SHA-256 of each photo. Plain A4, printable, made
 * entirely on the phone.
 */
class PdfReportWriter(private val context: Context) {

    private val pageW = 595
    private val pageH = 842
    private val margin = 40f
    private val width get() = pageW - 2 * margin

    private val title = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 20f; typeface = Typeface.DEFAULT_BOLD; color = Color.BLACK }
    private val heading = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 13f; typeface = Typeface.DEFAULT_BOLD; color = Color.BLACK }
    private val body = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 10.5f; color = Color.BLACK }
    private val small = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 8.5f; color = Color.DKGRAY }
    private val mono = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 8f; typeface = Typeface.MONOSPACE; color = Color.DKGRAY }
    private val circle = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 3f; color = Color.rgb(0xEF, 0x44, 0x44) }

    private lateinit var doc: PdfDocument
    private var page: PdfDocument.Page? = null
    private var pageNumber = 0
    private var y = 0f
    private val canvas: Canvas get() = page!!.canvas

    /** Writes the report to [out]. [photoHashes] maps photo file → SHA-256. */
    fun write(details: ScanWithDetails, photoHashes: List<Pair<File, String>>, out: File) {
        doc = PdfDocument()
        pageNumber = 0
        newPage()
        val scan = details.scan
        fun s(id: Int, vararg args: Any): String = context.getString(id, *args)

        text(s(R.string.pdf_title), title)
        y += 6
        line("${s(R.string.evidence_place)}: ${scan.placeName ?: s(R.string.history_unnamed)}")
        line("${s(R.string.evidence_time)}: ${DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT).format(Date(scan.startedAt))}")
        line("${s(R.string.pdf_phone)}: ${scan.deviceModel} · CamSense ${scan.appVersion}")
        line("${s(R.string.pdf_scan_type)}: ${s(when (scan.mode ?: ScanMode.FULL) {
            ScanMode.FULL -> R.string.mode_full
            ScanMode.QUICK -> R.string.mode_quick
            ScanMode.SMALL_ROOM -> R.string.mode_small_room
        })}")
        scan.coverage?.let { line(s(R.string.results_coverage, (it * 100).roundToInt())) }

        section(s(R.string.pdf_result))
        val level = scan.riskLevel ?: RiskLevel.LOW
        text(s(when (level) { RiskLevel.LOW -> R.string.risk_low; RiskLevel.MEDIUM -> R.string.risk_medium; RiskLevel.HIGH -> R.string.risk_high }), heading)
        line(s(when (level) { RiskLevel.LOW -> R.string.summary_low; RiskLevel.MEDIUM -> R.string.summary_medium; RiskLevel.HIGH -> R.string.summary_high }))
        line(s(when (level) { RiskLevel.LOW -> R.string.next_low; RiskLevel.MEDIUM -> R.string.next_medium; RiskLevel.HIGH -> R.string.next_high }))

        section(s(R.string.results_why))
        val runById = details.moduleRuns.associateBy { it.id }
        if (details.findings.isEmpty()) line(s(R.string.results_low_all_ran))
        details.findings.sortedByDescending { it.confidence }.forEachIndexed { i, f ->
            val name = runById[f.moduleRunId]?.module?.label()?.let { s(it) } ?: ""
            line("${i + 1}. $name: ${f.reason} (${(f.confidence * 100).roundToInt()}%)")
        }

        val devices = details.networkDevices.filter { !it.isSelf && !it.isGateway && it.suspicionScore >= 0.3f }
        if (devices.isNotEmpty()) {
            section(s(R.string.pdf_devices))
            devices.forEach { d ->
                val name = listOfNotNull(d.hostname, d.mdnsName, d.ssdpServer).joinToString(" · ")
                line(listOf(d.ip, d.openPorts.joinToString(", ").ifEmpty { null }, name.ifEmpty { null }).filterNotNull().joinToString("  "), mono)
                d.banner?.let { line(it, mono) }
            }
        }

        val skipped = details.moduleRuns.filter { it.status != RunStatus.DONE }
        if (skipped.isNotEmpty()) {
            section(s(R.string.results_skipped))
            skipped.forEach { r ->
                line("${s(r.module.label())}: ${s(skipReasonText(r.skipReason))}")
            }
        }

        if (photoHashes.isNotEmpty()) {
            section(s(R.string.pdf_photos))
            val findingById = details.findings.associateBy { it.id }
            photoHashes.forEach { (file, hash) -> photo(file, hash, findingById[file.nameWithoutExtension]) }
        }

        section(s(R.string.pdf_about))
        line(s(R.string.pdf_integrity), small)
        line(s(R.string.limits_caption), small)

        finishPage()
        out.parentFile?.mkdirs()
        out.outputStream().use { doc.writeTo(it) }
        doc.close()
    }

    private fun photo(file: File, hash: String, finding: com.camsense.data.db.FindingEntity?) {
        val bmp = decodeScaled(file, 1200) ?: return
        val scale = minOf(width / bmp.width, 330f / bmp.height)
        val w = bmp.width * scale
        val h = bmp.height * scale
        ensureRoom(h + 40)
        val rect = RectF(margin, y, margin + w, y + h)
        canvas.drawBitmap(bmp, null, rect, null)
        if (finding?.boxLeft != null && finding.boxRight != null && finding.boxTop != null && finding.boxBottom != null) {
            val cx = rect.left + (finding.boxLeft + finding.boxRight) / 2 * w
            val cy = rect.top + (finding.boxTop + finding.boxBottom) / 2 * h
            val r = maxOf(14f, (finding.boxRight - finding.boxLeft) * w)
            canvas.drawCircle(cx, cy, r, circle)
        }
        bmp.recycle()
        y += h + 4
        line(finding?.reason ?: file.name, small)
        line("SHA-256 ${hash}", mono)
        y += 8
    }

    private fun decodeScaled(file: File, maxSide: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > maxSide) sample *= 2
        return BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
    }

    private fun section(name: String) {
        y += 10
        ensureRoom(40f)
        text(name, heading)
    }

    private fun line(text: String, paint: Paint = body) = text(text, paint)

    private fun text(text: String, paint: Paint) {
        val lineHeight = paint.textSize * 1.4f
        for (l in TextWrap.wrap(text, width, paint::measureText)) {
            ensureRoom(lineHeight)
            y += lineHeight
            canvas.drawText(l, margin, y - paint.textSize * 0.3f, paint)
        }
    }

    private fun ensureRoom(height: Float) {
        if (y + height > pageH - margin - 20) {
            finishPage()
            newPage()
        }
    }

    private fun newPage() {
        pageNumber++
        page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, pageNumber).create())
        y = margin
    }

    private fun finishPage() {
        val p = page ?: return
        p.canvas.drawText(context.getString(R.string.pdf_footer, pageNumber), margin, pageH - margin / 2, small)
        doc.finishPage(p)
        page = null
    }
}
