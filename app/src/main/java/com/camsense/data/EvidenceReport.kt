package com.camsense.data

import kotlinx.serialization.Serializable

/** What report.json holds. Written when the user taps "Save evidence". */
@Serializable
data class EvidenceReport(
    val app: String = "CamSense",
    val appVersion: String,
    val scanId: String,
    val placeName: String?,
    val startedAt: Long,
    val finishedAt: Long?,
    val deviceModel: String,
    val riskLevel: String?,
    val riskScore: Float?,
    val modules: List<ModuleEntry>,
    val findings: List<FindingEntry>,
    val networkDevices: List<NetworkEntry>,
    val bluetoothDevices: List<BleEntry>,
    val photos: List<PhotoEntry>,
    val note: String = "CamSense raises the chance of finding a camera but can't guarantee it.",
) {
    @Serializable data class ModuleEntry(val module: String, val status: String, val score: Float?, val skipReason: String?)
    @Serializable data class FindingEntry(val id: String, val module: String, val reason: String, val confidence: Float,
                                          val targetKey: String?, val userVerdict: String, val createdAt: Long)
    @Serializable data class NetworkEntry(val ip: String, val openPorts: List<Int>, val banner: String?,
                                          val mdnsName: String?, val ssdpServer: String?, val suspicionScore: Float)
    @Serializable data class BleEntry(val address: String, val name: String?, val rssi: Int, val suspicionScore: Float)
    @Serializable data class PhotoEntry(val file: String, val findingId: String?, val sha256: String)
}
