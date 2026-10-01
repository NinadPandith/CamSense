package com.camsense.modules.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import com.camsense.core.BaseModule
import com.camsense.core.Finding
import com.camsense.core.ModuleId
import com.camsense.core.Reasons
import com.camsense.core.SkipReason
import com.camsense.core.permissions.Permissions
import com.camsense.scoring.ScanConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withTimeoutOrNull

/** M4: 20-second Bluetooth Low Energy scan (PRD FR-4.1/4.2). */
class BleScanModule(
    private val context: Context,
    private val oui: OuiDatabase,
) : BaseModule(ModuleId.BLE) {

    private val _devices = MutableStateFlow<List<BleDevice>>(emptyList())
    val devices: StateFlow<List<BleDevice>> = _devices.asStateFlow()

    private val adapter get() = context.getSystemService(BluetoothManager::class.java)?.adapter

    override fun isSupported(): Boolean = adapter?.isEnabled == true

    @SuppressLint("MissingPermission") // checked through Permissions.hasBluetooth
    override suspend fun start() {
        if (!Permissions.hasBluetooth(context)) return skip(SkipReason.NO_NEARBY_PERMISSION)
        val scanner = adapter?.takeIf { it.isEnabled }?.bluetoothLeScanner ?: return skip(SkipReason.NO_BLUETOOTH)
        markRunning()

        val seen = linkedMapOf<String, BleDevice>()
        val results = callbackFlow {
            val callback = object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: ScanResult) { trySend(result) }
                override fun onBatchScanResults(list: MutableList<ScanResult>) { list.forEach { trySend(it) } }
                override fun onScanFailed(errorCode: Int) { close(IllegalStateException("BLE scan failed: $errorCode")) }
            }
            val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
            scanner.startScan(null, settings, callback)
            awaitClose { try { scanner.stopScan(callback) } catch (_: Exception) {} }
        }

        try {
            withTimeoutOrNull(ScanConfig.BLE_SCAN_MS) {
                results.collect { r ->
                    val address = r.device.address ?: return@collect
                    val record = r.scanRecord
                    // scanRecord.deviceName needs no BLUETOOTH_CONNECT, unlike device.name.
                    val name = record?.deviceName ?: seen[address]?.name
                    val makers = record?.manufacturerSpecificData
                    val manufacturerId = if (makers != null && makers.size() > 0) makers.keyAt(0) else null
                    val rssi = maxOf(r.rssi, seen[address]?.rssi ?: Int.MIN_VALUE)
                    val vendor = oui.lookup(address)
                    val v = BleClassifier.classify(name, rssi, vendor?.cameraMaker == true)
                    seen[address] = BleDevice(address, name, rssi, manufacturerId, vendor?.name, v.score, v.reason,
                        id = seen[address]?.id ?: java.util.UUID.randomUUID().toString())
                    publishDevices(seen.values.toList(), done = false)
                }
            }
            publishDevices(seen.values.toList(), done = true)
        } catch (e: CancellationException) {
            throw e // scan finished or stopped; the result already says DONE
        } catch (e: Exception) {
            fail(SkipReason.MODULE_FAILED, e)
        }
    }

    private fun publishDevices(list: List<BleDevice>, done: Boolean) {
        val sorted = list.sortedByDescending { it.rssi }
        _devices.value = sorted
        val flagged = sorted.filter { it.suspicionScore >= ScanConfig.BLE_FINDING_MIN }
        // Camera-like names and makers are listed one by one; nearby unknown devices
        // (often your own earbuds or watch) become a single finding.
        val (unknown, named) = flagged.partition { it.reason == Reasons.BLE_STRONG_UNKNOWN }
        val findings = named.map {
            Finding(reason = it.reason ?: "", confidence = it.suspicionScore, targetKey = null, deviceAddress = it.address)
        } + listOfNotNull(unknown.takeIf { it.isNotEmpty() }?.let { list ->
            Finding(reason = Reasons.bleStrongUnknown(list.size), confidence = list.maxOf { it.suspicionScore },
                targetKey = null, deviceAddress = list.first().address)
        })
        publish(score = flagged.maxOfOrNull { it.suspicionScore } ?: 0f, findings = findings, done = done)
    }

    override fun stop() = finish()
}
