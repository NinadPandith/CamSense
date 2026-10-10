package com.camsense.data

import android.os.Build
import androidx.room.withTransaction
import com.camsense.core.ModuleId
import com.camsense.core.ModuleResult
import com.camsense.core.ScanMode
import com.camsense.data.db.BleDeviceEntity
import com.camsense.data.db.CamSenseDatabase
import com.camsense.data.db.NetworkDeviceEntity
import com.camsense.data.db.ScanEntity
import com.camsense.data.db.ScanStatus
import com.camsense.data.db.ScanWithDetails
import com.camsense.data.db.UserVerdict
import com.camsense.modules.bluetooth.BleDevice
import com.camsense.modules.network.NetworkDevice
import com.camsense.scoring.RiskAssessment
import com.camsense.scoring.RiskEngine
import kotlinx.coroutines.flow.Flow

class ScanRepository(
    private val db: CamSenseDatabase,
    private val evidenceStore: EvidenceStore,
    private val appVersion: String,
) {
    private val dao = db.scanDao()

    fun observeHistory(): Flow<List<ScanEntity>> = dao.observeHistory()
    fun observeRecent(limit: Int = 3): Flow<List<ScanEntity>> = dao.observeRecent(limit)
    fun observeScan(scanId: String): Flow<ScanWithDetails?> = dao.observeScan(scanId)
    suspend fun getScan(scanId: String): ScanWithDetails? = dao.getScanWithDetails(scanId)

    /** Step 1: creates the scan row with status RUNNING. */
    suspend fun createScan(placeName: String?, mode: ScanMode = ScanMode.FULL): ScanEntity {
        val scan = ScanEntity(
            mode = mode,
            placeName = placeName?.trim()?.takeIf { it.isNotEmpty() },
            startedAt = System.currentTimeMillis(),
            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
            appVersion = appVersion,
        )
        dao.insertScan(scan)
        return scan
    }

    /** Step 6: saves every module's result and marks the scan COMPLETED. */
    suspend fun completeScan(
        scanId: String,
        results: List<ModuleResult>,
        assessment: RiskAssessment,
        networkDevices: List<NetworkDevice>,
        bleDevices: List<BleDevice>,
        coverage: Float? = null,
        startYaw: Float? = null,
    ) = db.withTransaction {
        val scan = dao.getScan(scanId) ?: return@withTransaction
        val netEntities = networkDevices.map {
            NetworkDeviceEntity(id = it.id, scanId = scanId, ip = it.ip, hostname = it.hostname,
                mdnsName = it.mdnsName,
                // ONVIF replies share the ssdp_server column.
                ssdpServer = it.serverText,
                openPorts = it.openPorts,
                banner = it.banner, isSelf = it.isSelf, isGateway = it.isGateway,
                suspicionScore = it.suspicionScore)
        }
        dao.insertNetworkDevices(netEntities)
        dao.insertBleDevices(bleDevices.map {
            BleDeviceEntity(id = it.id, scanId = scanId, address = it.address, name = it.name,
                rssi = it.rssi, manufacturerId = it.manufacturerId, suspicionScore = it.suspicionScore)
        })
        val deviceIdByIp = netEntities.associate { it.ip to it.id }
        insertResults(scanId, results, deviceIdByIp)
        dao.updateScan(scan.copy(
            status = ScanStatus.COMPLETED,
            finishedAt = System.currentTimeMillis(),
            riskScore = assessment.score,
            riskLevel = assessment.level,
            coverage = coverage,
            startYaw = startYaw,
        ))
    }

    /** The previous completed scan of the same place, with its devices, if there is one. */
    suspend fun previousScan(scan: ScanEntity): ScanWithDetails? {
        val place = scan.placeName?.takeIf { it.isNotBlank() } ?: return null
        val prev = dao.previousScanOf(place, scan.startedAt) ?: return null
        return dao.getScanWithDetails(prev.id)
    }

    private suspend fun insertResults(scanId: String, results: List<ModuleResult>, deviceIdByIp: Map<String, String>) {
        val runs = results.map { it.toRunEntity(scanId) }
        dao.insertModuleRuns(runs)
        val runIdByModule = runs.associate { it.module to it.id }
        dao.insertFindings(results.flatMap { r ->
            val runId = runIdByModule.getValue(r.moduleId)
            r.findings.map { f ->
                val deviceId = if (r.moduleId == ModuleId.NETWORK) f.deviceAddress?.let(deviceIdByIp::get) else null
                f.toEntity(scanId, runId, deviceId)
            }
        })
    }

    /**
     * "Check flagged spots" from a Medium result: replaces the magnetometer run with
     * the new readings and re-scores the scan from what is saved.
     */
    suspend fun replaceMagnetResult(scanId: String, magnet: ModuleResult): RiskAssessment? = db.withTransaction {
        dao.deleteModuleRun(scanId, ModuleId.MAGNET.name) // cascades to its findings
        insertResults(scanId, listOf(magnet), emptyMap())
        val details = dao.getScanWithDetails(scanId) ?: return@withTransaction null
        val assessment = RiskEngine.assess(details.toModuleResults())
        dao.updateScan(details.scan.copy(riskScore = assessment.score, riskLevel = assessment.level))
        assessment
    }

    suspend fun abortScan(scanId: String) {
        val scan = dao.getScan(scanId) ?: return
        dao.updateScan(scan.copy(status = ScanStatus.ABORTED))
        evidenceStore.deleteScanFiles(scanId)
    }

    suspend fun abortStaleRunning() = dao.abortStaleRunning()

    suspend fun setVerdict(findingId: String, verdict: UserVerdict) = dao.setVerdict(findingId, verdict)

    suspend fun deleteScan(scanId: String) {
        dao.deleteScan(scanId)
        evidenceStore.deleteScanFiles(scanId)
    }

    suspend fun deleteAll() {
        dao.deleteAll()
        evidenceStore.deleteAll()
    }

    /** Debug builds only: runs the sample SQL from app/src/debug/assets. */
    suspend fun runSql(statements: List<String>) = db.withTransaction {
        statements.forEach { db.openHelper.writableDatabase.execSQL(it) }
    }
}
