package com.example.inteltrace_v3.core.detection

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

/**
 * IOC (Indicator of Compromise) Matcher
 * Checks against known malicious IPs, domains, and patterns
 */
@Singleton
class IOCMatcher @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val maliciousIPs = mutableSetOf<String>()
    private val maliciousCIDRs = mutableListOf<CIDRRange>()
    private val suspiciousPatterns = mutableMapOf<String, String>()
    
    init {
        loadDefaultIOCs()
    }
    
    private fun loadDefaultIOCs() {
        // Known malicious IPs (examples - in production, load from threat feeds)
        maliciousIPs.addAll(
            listOf(
                // C2 Servers (examples)
                "185.220.101.1",
                "185.220.101.2",
                "192.42.116.16",
                // Add more from threat intelligence feeds
            )
        )
        
        // Suspicious CIDR ranges
        maliciousCIDRs.add(CIDRRange("185.220.101.0", 24)) // Example Tor exit nodes
        
        // Pattern matching
        suspiciousPatterns["Tor Exit Node"] = "185\\.220\\.10[0-9]\\.[0-9]{1,3}"
        suspiciousPatterns["Known C2"] = "192\\.42\\.116\\.[0-9]{1,3}"
    }
    
    fun checkIP(ipAddress: String): IOCMatch {
        // Direct IP match
        if (ipAddress in maliciousIPs) {
            return IOCMatch(
                isMatched = true,
                category = "Known Malicious IP",
                description = "IP found in malware database",
                confidence = 100
            )
        }
        
        // CIDR range match
        for (cidr in maliciousCIDRs) {
            if (cidr.contains(ipAddress)) {
                return IOCMatch(
                    isMatched = true,
                    category = "Suspicious Range",
                    description = "IP in known malicious CIDR range",
                    confidence = 85
                )
            }
        }
        
        // Pattern match
        for ((category, pattern) in suspiciousPatterns) {
            if (ipAddress.matches(Regex(pattern))) {
                return IOCMatch(
                    isMatched = true,
                    category = category,
                    description = "IP matches suspicious pattern",
                    confidence = 70
                )
            }
        }
        
        return IOCMatch(isMatched = false)
    }
    
    fun addMaliciousIP(ip: String) {
        maliciousIPs.add(ip)
    }
    
    fun removeMaliciousIP(ip: String) {
        maliciousIPs.remove(ip)
    }
    
    fun updateIOCFeed(ips: List<String>) {
        maliciousIPs.clear()
        maliciousIPs.addAll(ips)
    }
    
    data class IOCMatch(
        val isMatched: Boolean,
        val category: String = "",
        val description: String = "",
        val confidence: Int = 0
    )
    
    private data class CIDRRange(
        val network: String,
        val prefixLength: Int
    ) {
        private val networkLong: Long
        private val maskLong: Long
        
        init {
            networkLong = ipToLong(network)
            maskLong = (-1L shl (32 - prefixLength)) and 0xFFFFFFFFL
        }
        
        fun contains(ip: String): Boolean {
            val ipLong = ipToLong(ip)
            return (ipLong and maskLong) == (networkLong and maskLong)
        }
        
        private fun ipToLong(ip: String): Long {
            val parts = ip.split(".")
            if (parts.size != 4) return 0L
            
            return try {
                ((parts[0].toLong() shl 24) +
                 (parts[1].toLong() shl 16) +
                 (parts[2].toLong() shl 8) +
                 parts[3].toLong()) and 0xFFFFFFFFL
            } catch (e: Exception) {
                0L
            }
        }
    }
}
