package com.camsense.data.db

import androidx.room.Embedded
import androidx.room.Relation

/** A scan with everything under it; what Results, Evidence and the report read. */
data class ScanWithDetails(
    @Embedded val scan: ScanEntity,
    @Relation(parentColumn = "id", entityColumn = "scan_id") val moduleRuns: List<ModuleRunEntity>,
    @Relation(parentColumn = "id", entityColumn = "scan_id") val findings: List<FindingEntity>,
    @Relation(parentColumn = "id", entityColumn = "scan_id") val networkDevices: List<NetworkDeviceEntity>,
    @Relation(parentColumn = "id", entityColumn = "scan_id") val bleDevices: List<BleDeviceEntity>,
    @Relation(parentColumn = "id", entityColumn = "scan_id") val evidenceItems: List<EvidenceItemEntity>,
)
