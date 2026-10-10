package com.camsense.data

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.camsense.data.db.EvidenceDao
import com.camsense.data.db.EvidenceItemEntity
import com.camsense.data.db.EvidenceType
import com.camsense.data.db.ScanWithDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.security.MessageDigest

/**
 * Evidence lives only in app-private storage: files/evidence/<scanId>/.
 * Glint snapshots are written during the scan as <findingId>.jpg so the finding
 * sheet can show them; they only become evidence items (with SHA-256 hashes)
 * when the user taps "Save evidence".
 */
class EvidenceStore(private val context: Context, private val evidenceDao: EvidenceDao) {

    private val root = File(context.filesDir, "evidence")
    private val json = Json { prettyPrint = true; encodeDefaults = true }

    fun scanDir(scanId: String): File = File(root, scanId)

    fun snapshotFile(scanId: String, findingId: String): File = File(scanDir(scanId), "$findingId.jpg")

    fun file(relativePath: String): File = File(root, relativePath)

    /** The PDF report, written by saveEvidence. */
    fun pdfFile(scanId: String): File = File(scanDir(scanId), "report.pdf")

    suspend fun saveSnapshot(scanId: String, findingId: String, bitmap: Bitmap) = withContext(Dispatchers.IO) {
        try {
            val f = snapshotFile(scanId, findingId)
            f.parentFile?.mkdirs()
            f.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        } catch (e: IOException) {
            Log.w(TAG, "Snapshot not saved", e)
        }
    }

    /**
     * Writes report.json, hashes every file and records them as evidence items.
     * Throws IOException when storage is full; the caller shows the error copy.
     */
    suspend fun saveEvidence(details: ScanWithDetails, appVersion: String): List<EvidenceItemEntity> =
        withContext(Dispatchers.IO) {
            val scanId = details.scan.id
            val dir = scanDir(scanId).apply { mkdirs() }
            val now = System.currentTimeMillis()
            val findingIds = details.findings.map { it.id }.toSet()

            val photos = dir.listFiles { f -> f.extension == "jpg" }.orEmpty().sortedBy { it.name }.map { f ->
                val findingId = f.nameWithoutExtension.takeIf { it in findingIds }
                EvidenceItemEntity(scanId = scanId, findingId = findingId, type = EvidenceType.PHOTO,
                    filePath = "$scanId/${f.name}", sha256 = sha256(f), createdAt = now)
            }

            val runById = details.moduleRuns.associateBy { it.id }
            val report = EvidenceReport(
                appVersion = appVersion,
                scanId = scanId,
                placeName = details.scan.placeName,
                startedAt = details.scan.startedAt,
                finishedAt = details.scan.finishedAt,
                deviceModel = details.scan.deviceModel,
                riskLevel = details.scan.riskLevel?.name,
                riskScore = details.scan.riskScore,
                modules = details.moduleRuns.map {
                    EvidenceReport.ModuleEntry(it.module.name, it.status.name, it.score, it.skipReason)
                },
                findings = details.findings.map {
                    EvidenceReport.FindingEntry(it.id, runById[it.moduleRunId]?.module?.name ?: "?", it.reason,
                        it.confidence, it.targetKey, it.userVerdict.name, it.createdAt)
                },
                networkDevices = details.networkDevices.filter { !it.isSelf }.map {
                    EvidenceReport.NetworkEntry(it.ip, it.openPorts, it.banner, it.mdnsName, it.ssdpServer, it.suspicionScore)
                },
                bluetoothDevices = details.bleDevices.map {
                    EvidenceReport.BleEntry(it.address, it.name, it.rssi, it.suspicionScore)
                },
                photos = photos.map { EvidenceReport.PhotoEntry(File(it.filePath).name, it.findingId, it.sha256) },
            )
            val reportFile = File(dir, "report.json")
            reportFile.writeText(json.encodeToString(EvidenceReport.serializer(), report))
            val reportItem = EvidenceItemEntity(scanId = scanId, type = EvidenceType.REPORT_JSON,
                filePath = "$scanId/report.json", sha256 = sha256(reportFile), createdAt = now)

            // PDF report: photos with the spot circled, every finding, and the photo hashes.
            val pdf = pdfFile(scanId)
            com.camsense.data.report.PdfReportWriter(context)
                .write(details, photos.map { file(it.filePath) to it.sha256 }, pdf)
            val pdfItem = EvidenceItemEntity(scanId = scanId, type = EvidenceType.REPORT_PDF,
                filePath = "$scanId/report.pdf", sha256 = sha256(pdf), createdAt = now)

            // Saving twice replaces the earlier record instead of duplicating it.
            evidenceDao.deleteForScan(scanId)
            val items = photos + reportItem + pdfItem
            evidenceDao.insert(items)
            items
        }

    fun deleteScanFiles(scanId: String) {
        scanDir(scanId).deleteRecursively()
    }

    fun deleteAll() {
        root.deleteRecursively()
    }

    companion object {
        private const val TAG = "CamSense/Evidence"

        fun sha256(file: File): String {
            val md = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buf = ByteArray(8192)
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    md.update(buf, 0, n)
                }
            }
            return md.digest().joinToString("") { "%02x".format(it) }
        }
    }
}
