package com.camsense

import com.camsense.core.Finding
import com.camsense.core.ModuleId
import com.camsense.core.ModuleResult
import com.camsense.data.SafeDevices
import com.camsense.data.ScanDiff
import com.camsense.data.db.BleDeviceEntity
import com.camsense.data.db.NetworkDeviceEntity
import com.camsense.data.report.TextWrap
import com.camsense.modules.magnetometer.HotCold
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeatureLogicTest {

    // ---- Hot–cold finder ----
    private fun hotCold() = HotCold(fastAlpha = 0.4f, slowAlpha = 0.05f, trendThreshold = 1.5f,
        passedDrop = 0.35f, peakWindowMs = 3_000, minPeak = 10f)

    @Test
    fun approachingIsWarmerLeavingIsColder() {
        val h = hotCold()
        var t = 0L
        repeat(10) { h.update(2f, t); t += 50 }
        var s = h.update(2f, t)
        repeat(10) { i -> t += 50; s = h.update(2f + i * 3f, t) }
        assertEquals(HotCold.Trend.WARMER, s.trend)
        repeat(20) { i -> t += 50; s = h.update(30f - i * 1.5f, t) }
        assertEquals(HotCold.Trend.COLDER, s.trend)
    }

    @Test
    fun passingThePeakIsReported() {
        val h = hotCold()
        var t = 0L
        listOf(5f, 12f, 25f, 40f).forEach { t += 100; h.update(it, t) }
        t += 300
        assertTrue(h.update(18f, t).passedPeak)
        assertEquals(40f, h.update(18f, t).strongest, 0f)
    }

    @Test
    fun weakFieldNeverClaimsAPeak() {
        val h = hotCold()
        var t = 0L
        listOf(3f, 6f, 8f).forEach { t += 100; h.update(it, t) }
        assertFalse(h.update(2f, t + 100).passedPeak)
    }

    // ---- Compare with the last scan ----
    private fun net(ip: String, mdns: String? = null, self: Boolean = false) =
        NetworkDeviceEntity(scanId = "s", ip = ip, mdnsName = mdns, openPorts = emptyList(), isSelf = self,
            isGateway = false, suspicionScore = 0f)
    private fun ble(name: String?) = BleDeviceEntity(scanId = "s", address = "AA", name = name, rssi = -70, suspicionScore = 0f)

    @Test
    fun newDevicesSinceLastScan() {
        val before = listOf(net("192.168.1.5", "Printer"), net("192.168.1.9", self = true))
        val now = listOf(net("192.168.1.7", "printer"), net("192.168.1.30", "IPCam-3F2A"), net("a4:cf:12:00:11:22"))
        val r = ScanDiff.diff(before, listOf(ble("Buds")), now, listOf(ble("Buds"), ble("Mini Cam")))
        assertEquals(1, r.newWifi)      // the printer moved IP but kept its name
        assertEquals(1, r.newHotspots)
        assertEquals(1, r.newBluetooth)
        assertEquals(3, r.total)
    }

    // ---- Remembered safe devices ----
    @Test
    fun rememberedDevicesAreDroppedAndScoreRecomputed() {
        val results = listOf(
            ModuleResult(ModuleId.NETWORK, ModuleResult.Status.DONE, 0.9f, listOf(
                Finding("stream", 0.9f, null, deviceAddress = "192.168.1.30"),
                Finding("web", 0.3f, null, deviceAddress = "192.168.1.40"),
            )),
            ModuleResult(ModuleId.GLINT, ModuleResult.Status.DONE, 0.8f, listOf(Finding("glint", 0.8f, "glint-1"))),
        )
        val safe = setOf(SafeDevices.lan("192.168.1.30", "tv", null, null))
        val (out, dropped) = SafeDevices.filter(results, safe) { _, address ->
            if (address == "192.168.1.30") SafeDevices.lan("192.168.1.30", "TV", null, null) else null
        }
        assertEquals(1, dropped)
        assertEquals(0.3f, out[0].score, 0f)
        assertEquals(results[1], out[1]) // camera findings untouched
    }

    @Test
    fun groupedBluetoothFindingKeepsOnlyDevicesNotMarkedSafe() {
        val group = Finding("3 unknown", 0.5f, null, deviceAddress = "AA:01, AA:02,AA:03")
        val results = listOf(ModuleResult(ModuleId.BLE, ModuleResult.Status.DONE, 0.5f, listOf(group)))
        val regroup = { f: Finding, left: List<String> ->
            f.copy(reason = "${left.size} unknown", deviceAddress = SafeDevices.joinAddresses(left))
        }

        val one = SafeDevices.filter(results, setOf(SafeDevices.ble("aa:02")), regroup) { _, a -> SafeDevices.ble(a) }
        assertEquals(0, one.second)
        assertEquals("2 unknown", one.first[0].findings.single().reason)
        assertEquals("AA:01,AA:03", one.first[0].findings.single().deviceAddress)

        val all = setOf("AA:01", "AA:02", "AA:03").map(SafeDevices::ble).toSet()
        val none = SafeDevices.filter(results, all, regroup) { _, a -> SafeDevices.ble(a) }
        assertEquals(1, none.second)
        assertTrue(none.first[0].findings.isEmpty())
        assertEquals(0f, none.first[0].score, 0f)
    }

    @Test
    fun hotspotAddressesAreRecognised() {
        assertTrue(SafeDevices.isHotspotAddress("a4:cf:12:00:11:22"))
        assertFalse(SafeDevices.isHotspotAddress("192.168.1.30"))
    }

    // ---- PDF word wrap ----
    @Test
    fun wrapsOnWordsAndSplitsLongWords() {
        val measure = { s: String -> s.length.toFloat() }
        assertEquals(listOf("one two", "three"), TextWrap.wrap("one two three", 8f, measure))
        assertEquals(listOf("abcd", "efgh", "ij"), TextWrap.wrap("abcdefghij", 4f, measure))
        assertEquals(listOf("a", "b c"), TextWrap.wrap("a\nb c", 10f, measure))
    }
}
