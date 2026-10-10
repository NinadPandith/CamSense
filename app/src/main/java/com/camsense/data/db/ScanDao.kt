package com.camsense.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanDao {
    @Insert suspend fun insertScan(scan: ScanEntity)
    @Update suspend fun updateScan(scan: ScanEntity)
    @Insert suspend fun insertModuleRuns(runs: List<ModuleRunEntity>)
    @Insert suspend fun insertFindings(findings: List<FindingEntity>)
    @Insert suspend fun insertNetworkDevices(devices: List<NetworkDeviceEntity>)
    @Insert suspend fun insertBleDevices(devices: List<BleDeviceEntity>)

    @Query("SELECT * FROM scans WHERE id = :scanId")
    suspend fun getScan(scanId: String): ScanEntity?

    @Query("SELECT * FROM scans WHERE status = 'COMPLETED' ORDER BY started_at DESC")
    fun observeHistory(): Flow<List<ScanEntity>>

    @Query("SELECT * FROM scans WHERE status = 'COMPLETED' ORDER BY started_at DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<ScanEntity>>

    @Transaction
    @Query("SELECT * FROM scans WHERE id = :scanId")
    fun observeScan(scanId: String): Flow<ScanWithDetails?>

    @Transaction
    @Query("SELECT * FROM scans WHERE id = :scanId")
    suspend fun getScanWithDetails(scanId: String): ScanWithDetails?

    /** The latest completed scan of the same place before [before], for "since your last scan". */
    @Query("SELECT * FROM scans WHERE status = 'COMPLETED' AND place_name = :place COLLATE NOCASE AND started_at < :before ORDER BY started_at DESC LIMIT 1")
    suspend fun previousScanOf(place: String, before: Long): ScanEntity?

    @Query("UPDATE findings SET user_verdict = :verdict WHERE id = :findingId")
    suspend fun setVerdict(findingId: String, verdict: UserVerdict)

    /** [module] is a ModuleId name, e.g. "MAGNET". */
    @Query("DELETE FROM module_runs WHERE scan_id = :scanId AND module = :module")
    suspend fun deleteModuleRun(scanId: String, module: String)

    /** Scans left RUNNING because the app was killed; they become ABORTED on next launch. */
    @Query("UPDATE scans SET status = 'ABORTED' WHERE status = 'RUNNING'")
    suspend fun abortStaleRunning(): Int

    @Query("DELETE FROM scans WHERE id = :scanId")
    suspend fun deleteScan(scanId: String)   // also delete files in files/evidence/<scanId>/

    @Query("DELETE FROM scans")
    suspend fun deleteAll()
}
