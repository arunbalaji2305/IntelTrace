package com.example.inteltrace_v3.domain.models

data class AppNetworkStats(
    val packageName: String,
    val appName: String,
    val totalConnections: Int,
    val suspiciousConnections: Int,
    val totalBytesSent: Long,
    val totalBytesReceived: Long,
    val uniqueIps: Int,
    val highestThreatScore: Int,
    val lastConnectionTime: Long,
    val isWhitelisted: Boolean = false,
    val isBlacklisted: Boolean = false
) {
    val riskLevel: ThreatLevel
        get() = when {
            isBlacklisted -> ThreatLevel.CRITICAL
            highestThreatScore >= 80 -> ThreatLevel.CRITICAL
            highestThreatScore >= 60 -> ThreatLevel.HIGH
            highestThreatScore >= 40 -> ThreatLevel.MEDIUM
            highestThreatScore >= 20 -> ThreatLevel.LOW
            else -> ThreatLevel.SAFE
        }
}
