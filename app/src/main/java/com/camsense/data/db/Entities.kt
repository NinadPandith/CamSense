package com.camsense.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.camsense.core.ModuleId
import com.camsense.core.ScanMode
import com.camsense.scoring.RiskLevel
import java.util.UUID

// Tables from docs/backend-schema.md. Primary keys are UUID strings made on the
// phone; timestamps are Unix milliseconds (UTC).

@Entity(tableName = "scans")
data class ScanEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "place_name") val placeName: String?,
    @ColumnInfo(name = "started_at") val startedAt: Long,
    @ColumnInfo(name = "finished_at") val finishedAt: Long? = null,
    val status: ScanStatus = ScanStatus.RUNNING,
    @ColumnInfo(name = "risk_score") val riskScore: Float? = null,
    @ColumnInfo(name = "risk_level") val riskLevel: RiskLevel? = null,
    @ColumnInfo(name = "device_model") val deviceModel: String,
    @ColumnInfo(name = "app_version") val appVersion: String,
    @ColumnInfo(name = "cloud_report_id") val cloudReportId: String? = null,
    // Added in version 2 (nullable, no default, so the migration is plain ALTER TABLE).
    /** null on scans saved before version 2: treat as FULL. */
    val mode: ScanMode? = ScanMode.FULL,
    /** Share of the room the camera pointed at during the camera steps, 0–1. */
    val coverage: Float? = null,
    /** Compass heading (degrees) the phone pointed at when the camera steps began: "ahead" on the room map. */
    @ColumnInfo(name = "start_yaw") val startYaw: Float? = null,
)

@Entity(
    tableName = "module_runs",
    foreignKeys = [ForeignKey(entity = ScanEntity::class, parentColumns = ["id"],
        childColumns = ["scan_id"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["scan_id", "module"], unique = true)],
)
data class ModuleRunEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "scan_id") val scanId: String,
    val module: ModuleId,
    val status: RunStatus,
    val score: Float?,
    val weight: Float,
    @ColumnInfo(name = "skip_reason") val skipReason: String? = null,
    @ColumnInfo(name = "duration_ms") val durationMs: Long? = null,
)

@Entity(
    tableName = "network_devices",
    foreignKeys = [ForeignKey(entity = ScanEntity::class, parentColumns = ["id"],
        childColumns = ["scan_id"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("scan_id")],
)
data class NetworkDeviceEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "scan_id") val scanId: String,
    val ip: String,
    val hostname: String? = null,
    @ColumnInfo(name = "mdns_name") val mdnsName: String? = null,
    @ColumnInfo(name = "ssdp_server") val ssdpServer: String? = null,
    @ColumnInfo(name = "open_ports") val openPorts: List<Int>,
    val banner: String? = null,
    @ColumnInfo(name = "is_self") val isSelf: Boolean,
    @ColumnInfo(name = "is_gateway") val isGateway: Boolean,
    @ColumnInfo(name = "suspicion_score") val suspicionScore: Float,
)

@Entity(
    tableName = "findings",
    foreignKeys = [
        ForeignKey(entity = ScanEntity::class, parentColumns = ["id"],
            childColumns = ["scan_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = ModuleRunEntity::class, parentColumns = ["id"],
            childColumns = ["module_run_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = NetworkDeviceEntity::class, parentColumns = ["id"],
            childColumns = ["network_device_id"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("scan_id"), Index("module_run_id"), Index("target_key"), Index("network_device_id")],
)
data class FindingEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "scan_id") val scanId: String,
    @ColumnInfo(name = "module_run_id") val moduleRunId: String,
    @ColumnInfo(name = "target_key") val targetKey: String?,
    val reason: String,
    val confidence: Float,
    @ColumnInfo(name = "box_left") val boxLeft: Float? = null,
    @ColumnInfo(name = "box_top") val boxTop: Float? = null,
    @ColumnInfo(name = "box_right") val boxRight: Float? = null,
    @ColumnInfo(name = "box_bottom") val boxBottom: Float? = null,
    @ColumnInfo(name = "network_device_id") val networkDeviceId: String? = null,
    @ColumnInfo(name = "user_verdict") val userVerdict: UserVerdict = UserVerdict.UNREVIEWED,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    // Added in version 2.
    /** Compass heading (degrees) of the spot, for the room map; camera findings only. */
    val yaw: Float? = null,
    /** Up/down angle (degrees, up positive) of the spot. */
    val pitch: Float? = null,
    /** Wi-Fi IP/BSSID or Bluetooth address, for "remembered safe" devices. */
    @ColumnInfo(name = "device_address") val deviceAddress: String? = null,
)

@Entity(
    tableName = "ble_devices",
    foreignKeys = [ForeignKey(entity = ScanEntity::class, parentColumns = ["id"],
        childColumns = ["scan_id"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("scan_id")],
)
data class BleDeviceEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "scan_id") val scanId: String,
    val address: String,
    val name: String? = null,
    val rssi: Int,
    @ColumnInfo(name = "manufacturer_id") val manufacturerId: Int? = null,
    @ColumnInfo(name = "suspicion_score") val suspicionScore: Float,
)

@Entity(
    tableName = "evidence_items",
    foreignKeys = [
        ForeignKey(entity = ScanEntity::class, parentColumns = ["id"],
            childColumns = ["scan_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = FindingEntity::class, parentColumns = ["id"],
            childColumns = ["finding_id"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("scan_id"), Index("finding_id")],
)
data class EvidenceItemEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "scan_id") val scanId: String,
    @ColumnInfo(name = "finding_id") val findingId: String? = null,
    val type: EvidenceType,
    /** Relative to the app's files/evidence folder, e.g. "<scanId>/<findingId>.jpg". */
    @ColumnInfo(name = "file_path") val filePath: String,
    val sha256: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)
