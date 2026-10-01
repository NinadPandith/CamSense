package com.camsense.modules.network

/**
 * Reads a WS-Discovery ProbeMatch reply. Devices that answer an ONVIF probe for
 * "NetworkVideoTransmitter" are IP cameras or video encoders. The scopes often carry
 * the product name and hardware model. Pure Kotlin for unit tests.
 */
object OnvifParser {

    /** The probe CamSense sends to 239.255.255.250:3702. */
    fun probe(messageId: String): String = """<?xml version="1.0" encoding="UTF-8"?>
<e:Envelope xmlns:e="http://www.w3.org/2003/05/soap-envelope" xmlns:w="http://schemas.xmlsoap.org/ws/2004/08/addressing" xmlns:d="http://schemas.xmlsoap.org/ws/2005/04/discovery" xmlns:dn="http://www.onvif.org/ver10/network/wsdl">
<e:Header><w:MessageID>uuid:$messageId</w:MessageID><w:To e:mustUnderstand="true">urn:schemas-xmlsoap-org:ws:2005:04:discovery</w:To><w:Action e:mustUnderstand="true">http://schemas.xmlsoap.org/ws/2005/04/discovery/Probe</w:Action></e:Header>
<e:Body><d:Probe><d:Types>dn:NetworkVideoTransmitter</d:Types></d:Probe></e:Body></e:Envelope>"""

    /** "ONVIF camera: <name> <hardware>", or null when the reply isn't an ONVIF ProbeMatch. */
    fun summary(reply: String): String? {
        if (!reply.contains("ProbeMatch", ignoreCase = true)) return null
        val scopes = Regex("<[^>]*Scopes[^>]*>([^<]*)<", RegexOption.IGNORE_CASE).find(reply)?.groupValues?.get(1).orEmpty()
        fun scope(kind: String) = Regex("onvif://www\\.onvif\\.org/$kind/(\\S+)", RegexOption.IGNORE_CASE)
            .find(scopes)?.groupValues?.get(1)?.let(::decode)
        val parts = listOfNotNull(scope("name"), scope("hardware")).distinct()
        return ("ONVIF camera" + if (parts.isEmpty()) "" else ": " + parts.joinToString(" ")).take(120)
    }

    private fun decode(s: String) = s.replace("%20", " ").replace("_", " ").trim()
}
