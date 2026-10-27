package com.jarvis.app

import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/** Sab internet calls ke liye ek hi client. Sirf HTTPS (network_security_config bhi cleartext rokta hai). */
object Net {

    private val bootstrapClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()

    private val doh: DnsOverHttps = DnsOverHttps.Builder()
        .client(bootstrapClient)
        .url("https://dns.google/dns-query".toHttpUrl())
        .bootstrapDnsHosts(
            InetAddress.getByAddress(byteArrayOf(8, 8, 8, 8)),
            InetAddress.getByAddress(byteArrayOf(8, 8, 4, 4))
        )
        .build()

    private val cache = ConcurrentHashMap<String, Pair<Long, List<InetAddress>>>()

    // Pehle Google DNS, na chale to phone ka DNS (kuch networks par DNS atakta hai)
    private val smartDns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            val now = System.currentTimeMillis()
            val c = cache[hostname]
            if (c != null && c.second.isNotEmpty() && now - c.first < 300000) return c.second
            var res: List<InetAddress> = emptyList()
            try {
                res = doh.lookup(hostname)
            } catch (e: Exception) {
            }
            if (res.isEmpty()) res = Dns.SYSTEM.lookup(hostname)
            cache[hostname] = Pair(now, res)
            return res
        }
    }

    val api: OkHttpClient = OkHttpClient.Builder()
        .dns(smartDns)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .callTimeout(60, TimeUnit.SECONDS)
        .build()

    /** Bade model downloads: koi total time limit nahi, bas data aata rahe */
    val download: OkHttpClient = api.newBuilder()
        .readTimeout(30, TimeUnit.SECONDS)
        .callTimeout(0, TimeUnit.SECONDS)
        .build()

    fun isHttps(url: String): Boolean = url.trim().lowercase().startsWith("https://")
}
