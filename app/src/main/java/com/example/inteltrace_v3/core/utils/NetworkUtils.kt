package com.example.inteltrace_v3.core.utils

import java.text.SimpleDateFormat
import java.util.*

object NetworkUtils {
    
    fun isPrivateIP(ip: String): Boolean {
        val parts = ip.split(".")
        if (parts.size != 4) return false
        
        val first = parts[0].toIntOrNull() ?: return false
        val second = parts[1].toIntOrNull() ?: return false
        
        return when {
            first == 10 -> true
            first == 172 && second in 16..31 -> true
            first == 192 && second == 168 -> true
            first == 127 -> true
            else -> false
        }
    }
    
    fun getProtocolName(protocol: Int): String {
        return when (protocol) {
            1 -> "ICMP"
            6 -> "TCP"
            17 -> "UDP"
            41 -> "IPv6"
            47 -> "GRE"
            50 -> "ESP"
            51 -> "AH"
            else -> "Protocol-$protocol"
        }
    }
    
    fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.2f KB", bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> String.format("%.2f MB", bytes / (1024.0 * 1024))
            else -> String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024))
        }
    }
    
    fun formatTimestamp(timestamp: Long, pattern: String = "MMM dd, HH:mm:ss"): String {
        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
    
    fun getCommonPortService(port: Int): String? {
        return when (port) {
            20, 21 -> "FTP"
            22 -> "SSH"
            23 -> "Telnet"
            25 -> "SMTP"
            53 -> "DNS"
            80 -> "HTTP"
            110 -> "POP3"
            143 -> "IMAP"
            443 -> "HTTPS"
            3306 -> "MySQL"
            3389 -> "RDP"
            5432 -> "PostgreSQL"
            6379 -> "Redis"
            8080 -> "HTTP Alt"
            8443 -> "HTTPS Alt"
            else -> null
        }
    }
}
