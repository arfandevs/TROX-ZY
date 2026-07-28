package com.troxzy.xploit.util

import java.net.InetAddress
import java.net.NetworkInterface
import java.util.Enumeration

object NetworkUtils {

    fun getLocalIpAddress(): String {
        try {
            val interfaces: Enumeration<NetworkInterface> = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val intf = interfaces.nextElement()
                val addresses: Enumeration<InetAddress> = intf.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr.hostAddress?.contains('.') == true) {
                        return addr.hostAddress ?: "Unknown"
                    }
                }
            }
        } catch (e: Exception) {
            return "Unknown"
        }
        return "Unknown"
    }

    fun subnetFromIp(ip: String): String {
        val parts = ip.split(".")
        if (parts.size >= 3) {
            return "${parts[0]}.${parts[1]}.${parts[2]}.0/24"
        }
        return ip
    }

    fun ipToLong(ip: String): Long {
        val parts = ip.split(".")
        if (parts.size != 4) return 0L
        var result = 0L
        for (part in parts) {
            result = (result shl 8) + (part.toIntOrNull() ?: 0)
        }
        return result
    }

    fun longToIp(value: Long): String {
        return "${(value shr 24) and 0xFF}.${(value shr 16) and 0xFF}.${(value shr 8) and 0xFF}.${value and 0xFF}"
    }

    fun cidrToRange(cidr: String): Pair<String, String> {
        val parts = cidr.split("/")
        if (parts.size != 2) return Pair(cidr, cidr)
        val ip = parts[0]
        val prefix = parts[1].toIntOrNull() ?: 24
        val ipLong = ipToLong(ip)
        val mask = if (prefix == 0) 0L else (-1L shl (32 - prefix))
        val network = ipLong and mask
        val broadcast = network or (mask xor 0xFFFFFFFFL)
        return Pair(longToIp(network + 1), longToIp(broadcast - 1))
    }

    fun isValidIp(ip: String): Boolean {
        val parts = ip.split(".")
        if (parts.size != 4) return false
        return parts.all { part ->
            val num = part.toIntOrNull()
            num != null && num in 0..255
        }
    }

    fun isValidDomain(domain: String): Boolean {
        return domain.matches(Regex("^[a-zA-Z0-9][a-zA-Z0-9\\-._]*[a-zA-Z0-9]$")) && domain.contains(".")
    }

    fun getLocalSubnet(): String {
        val ip = getLocalIpAddress()
        return if (ip != "Unknown") subnetFromIp(ip) else "192.168.1.0/24"
    }

    fun getMacAddress(): String {
        try {
            val interfaces: Enumeration<NetworkInterface> = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val intf = interfaces.nextElement()
                val mac = intf.hardwareAddress
                if (mac != null) {
                    return mac.joinToString(":") { "%02x".format(it) }
                }
            }
        } catch (e: Exception) {
            return "Unknown"
        }
        return "Unknown"
    }

    fun pingHost(ip: String, timeout: Int = 2000): Boolean {
        try {
            val address = InetAddress.getByName(ip)
            return address.isReachable(timeout)
        } catch (e: Exception) {
            return false
        }
    }
}
