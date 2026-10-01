package com.camsense.modules.network

import android.content.Context
import android.net.wifi.WifiManager
import com.camsense.core.BaseModule
import com.camsense.core.Finding
import com.camsense.core.ModuleId
import com.camsense.core.SkipReason
import com.camsense.core.permissions.Permissions
import com.camsense.modules.bluetooth.OuiDatabase
import com.camsense.scoring.ScanConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * M3: the Wi-Fi check (PRD FR-3.1–3.5), three parts run together:
 * - LAN sweep of the /24 subnet for camera ports, with service banners, mDNS, SSDP
 *   and ONVIF discovery (only when the phone is on Wi-Fi);
 * - nearby Wi-Fi networks with camera-like names or makers (spy cameras that aren't
 *   joined to the room's Wi-Fi broadcast their own; works without joining anything).
 * Read-only checks; nothing leaves the local network.
 */
class NetworkScanModule(private val context: Context, oui: OuiDatabase) : BaseModule(ModuleId.NETWORK) {

    private val _devices = MutableStateFlow<List<NetworkDevice>>(emptyList())
    /** Live device list for the Network check screen (self and router included, marked). */
    val devices: StateFlow<List<NetworkDevice>> = _devices.asStateFlow()

    private val _hotspots = MutableStateFlow<List<Hotspot>>(emptyList())
    /** Nearby Wi-Fi networks, camera-like first. */
    val hotspots: StateFlow<List<Hotspot>> = _hotspots.asStateFlow()

    private val _lanSkip = MutableStateFlow<SkipReason?>(null)
    /** Why the LAN sweep didn't run (not on Wi-Fi, no permission), while the rest did. */
    val lanSkip: StateFlow<SkipReason?> = _lanSkip.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val hotspotScanner = HotspotScanner(context, oui)

    override fun isSupported(): Boolean = LocalNetworkInfo.read(context) != null || Permissions.hasWifiScan(context)

    override suspend fun start() {
        val info = LocalNetworkInfo.read(context)
        val lanReason = when {
            info == null -> SkipReason.NO_WIFI
            !Permissions.hasLocalNetwork(context) -> SkipReason.NO_NEARBY_PERMISSION
            else -> null
        }
        val canScanHotspots = Permissions.hasWifiScan(context)
        if (lanReason != null && !canScanHotspots) return skip(lanReason)
        _lanSkip.value = lanReason
        markRunning()

        val wifi = context.applicationContext.getSystemService(WifiManager::class.java)
        val lock = wifi?.createMulticastLock("camsense-discovery")?.apply { setReferenceCounted(false); acquire() }
        try {
            coroutineScope {
                val hotspots = if (canScanHotspots) async(Dispatchers.IO) { hotspotScanner.scan() } else null
                if (info != null && lanReason == null) {
                    val mdns = async(Dispatchers.IO) { MdnsDiscovery(context).discover() }
                    val ssdp = async(Dispatchers.IO) { SsdpDiscovery().discover() }
                    val onvif = async(Dispatchers.IO) { OnvifDiscovery().discover() }
                    val swept = sweep(info)
                    val names = mdns.await()
                    val servers = ssdp.await()
                    val cameras = onvif.await()
                    val extraIps = (names.keys + servers.keys + cameras.keys)
                        .filter { ip -> swept.none { it.ip == ip } && SubnetMath.parseIpv4(ip) != null }
                    val merged = swept.map { d ->
                        d.copy(mdnsName = names[d.ip], ssdpServer = servers[d.ip], onvif = cameras[d.ip])
                    } + extraIps.map { ip -> NetworkDevice(ip, mdnsName = names[ip], ssdpServer = servers[ip], onvif = cameras[ip]) }
                    lanDevices = merged.map(::classified)
                    _progress.value = 1f
                }
                hotspots?.await()?.let { hotspotList = it }
                republish(done = true)
            }
        } catch (e: CancellationException) {
            throw e // scan finished or stopped; the result already says DONE
        } catch (e: SecurityException) {
            // Android 17 without ACCESS_LOCAL_NETWORK: sockets fail with a permission error.
            skip(SkipReason.NO_NEARBY_PERMISSION)
        } catch (e: Exception) {
            fail(SkipReason.MODULE_FAILED, e)
        } finally {
            lock?.release()
        }
    }

    @Volatile private var lanDevices: List<NetworkDevice> = emptyList()
    @Volatile private var hotspotList: List<Hotspot> = emptyList()

    private suspend fun sweep(info: LocalNetworkInfo): List<NetworkDevice> = withContext(Dispatchers.IO) {
        val hosts = SubnetMath.hostsToScan(info.ownIp, info.prefixLength)
        val ports = ScanConfig.CAMERA_PORTS + ScanConfig.LIVENESS_PORTS
        val openPorts = ConcurrentHashMap<String, MutableSet<Int>>()
        val semaphore = Semaphore(ScanConfig.NETWORK_PARALLELISM)
        val total = hosts.size * ports.size
        var done = 0

        coroutineScope {
            for (ip in hosts) for (port in ports) launch {
                semaphore.withPermit {
                    if (PortProber.isOpen(ip, port)) {
                        openPorts.getOrPut(ip) { ConcurrentHashMap.newKeySet() } += port
                        publishDevices(snapshot(info, openPorts), done = false)
                    }
                    synchronized(this@NetworkScanModule) { done++; _progress.value = done.toFloat() / total }
                }
            }
        }

        // Hosts with no open port may still be alive (phones, laptops): list them too.
        val alive = ConcurrentHashMap.newKeySet<String>().apply { addAll(openPorts.keys) }
        info.gatewayIp?.let { alive += it }
        coroutineScope {
            for (ip in hosts) if (ip !in alive) launch {
                semaphore.withPermit { if (PortProber.isReachable(ip)) alive += ip }
            }
        }

        coroutineScope {
            alive.map { ip ->
                async {
                    semaphore.withPermit {
                        val cameraPorts = openPorts[ip].orEmpty().filter { it in ScanConfig.CAMERA_PORTS }.sorted()
                        // RTSP first: its reply is the clearest sign of a camera.
                        val banner = cameraPorts.sortedBy { if (it == ScanConfig.RTSP_PORT) 0 else 1 }
                            .firstNotNullOfOrNull { PortProber.banner(ip, it) }
                        NetworkDevice(ip = ip, openPorts = cameraPorts, banner = banner, isGateway = ip == info.gatewayIp)
                    }
                }
            }.map { it.await() }
        } + NetworkDevice(ip = info.ownIp, isSelf = true)
    }

    private fun snapshot(info: LocalNetworkInfo, open: Map<String, Set<Int>>): List<NetworkDevice> =
        open.map { (ip, ports) ->
            classified(NetworkDevice(ip = ip, openPorts = ports.filter { it in ScanConfig.CAMERA_PORTS }.sorted(),
                isGateway = ip == info.gatewayIp))
        }

    private fun classified(d: NetworkDevice): NetworkDevice {
        val v = DeviceClassifier.classify(d)
        return d.copy(suspicionScore = v.score, reason = v.reason)
    }

    private fun publishDevices(list: List<NetworkDevice>, done: Boolean) {
        lanDevices = list
        republish(done)
    }

    /** Publishes LAN devices and nearby networks together, with findings from both. */
    @Synchronized
    private fun republish(done: Boolean) {
        val sorted = lanDevices.sortedWith(compareByDescending<NetworkDevice> { it.suspicionScore }.thenBy {
            SubnetMath.parseIpv4(it.ip)?.toUInt() ?: 0u
        })
        _devices.value = sorted
        _hotspots.value = hotspotList
        val suspicious = sorted.filter { !it.isSelf && !it.isGateway && it.suspicionScore >= ScanConfig.NET_FINDING_MIN }
        val cameraNetworks = hotspotList.filter { it.suspicionScore >= ScanConfig.NET_FINDING_MIN }
        val findings = suspicious.map {
            Finding(reason = it.reason ?: "", confidence = it.suspicionScore, targetKey = null, deviceAddress = it.ip)
        } + cameraNetworks.map {
            Finding(reason = it.reason ?: "", confidence = it.suspicionScore, targetKey = null, deviceAddress = it.bssid)
        }
        val score = (suspicious.map { it.suspicionScore } + cameraNetworks.map { it.suspicionScore }).maxOrNull() ?: 0f
        publish(score, findings, done)
        if (done) _progress.value = 1f
    }

    override fun stop() = finish()
}
