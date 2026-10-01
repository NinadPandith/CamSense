package com.camsense.modules.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.net.Inet4Address

/** Own IPv4 address, prefix length and router address on the current Wi-Fi. */
data class LocalNetworkInfo(val ownIp: String, val prefixLength: Int, val gatewayIp: String?) {
    companion object {
        /** null when the phone isn't on Wi-Fi (the network check is then skipped). */
        fun read(context: Context): LocalNetworkInfo? {
            val cm = context.getSystemService(ConnectivityManager::class.java) ?: return null
            @Suppress("DEPRECATION")
            val candidates = listOfNotNull(cm.activeNetwork) + cm.allNetworks.toList()
            val network = candidates.firstOrNull { n ->
                cm.getNetworkCapabilities(n)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
            } ?: return null
            val props = cm.getLinkProperties(network) ?: return null
            val link = props.linkAddresses.firstOrNull { it.address is Inet4Address } ?: return null
            val gateway = props.routes
                .firstOrNull { it.isDefaultRoute && it.gateway is Inet4Address }
                ?.gateway?.hostAddress
            return LocalNetworkInfo(link.address.hostAddress ?: return null, link.prefixLength, gateway)
        }
    }
}
