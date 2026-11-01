package com.example.thailandweather.data.remote

import okhttp3.Dns
import java.net.InetAddress
import java.net.UnknownHostException

/**
 * Resilient DNS resolver for OkHttp.
 * First queries system DNS. If system DNS fails (such as in emulator environments
 * with IPv6 slirp DNS proxy issues or transient DNS failures), it falls back
 * to known Anycast and direct IPs for critical app endpoints.
 */
object ResilientDns : Dns {
    private val fallbackMap = mapOf(
        "api.groq.com" to listOf("104.18.38.236", "172.64.149.20"),
        "api.open-meteo.com" to listOf("157.90.83.168"),
        "air-quality-api.open-meteo.com" to listOf("152.53.84.73"),
        "marine-api.open-meteo.com" to listOf("152.53.84.37"),
        "geocoding-api.open-meteo.com" to listOf("202.61.206.6"),
        "news.google.com" to listOf("142.250.143.139", "142.250.143.138", "142.250.143.102")
    )

    override fun lookup(hostname: String): List<InetAddress> {
        return try {
            val addresses = Dns.SYSTEM.lookup(hostname)
            if (addresses.isNotEmpty()) addresses else resolveFallback(hostname)
        } catch (_: UnknownHostException) {
            resolveFallback(hostname)
        } catch (_: Exception) {
            resolveFallback(hostname)
        }
    }

    private fun resolveFallback(hostname: String): List<InetAddress> {
        val ips = fallbackMap[hostname.lowercase()]
        if (!ips.isNullOrEmpty()) {
            return ips.map { InetAddress.getByName(it) }
        }
        throw UnknownHostException("Unable to resolve host \"$hostname\" via system or resilient fallback DNS")
    }
}
