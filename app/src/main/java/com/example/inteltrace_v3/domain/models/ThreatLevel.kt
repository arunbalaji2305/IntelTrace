package com.example.inteltrace_v3.domain.models

enum class ThreatLevel {
    SAFE,
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL,
    UNKNOWN;
    
    val color: Long
        get() = when (this) {
            SAFE -> 0xFF4CAF50
            LOW -> 0xFF8BC34A
            MEDIUM -> 0xFFFFC107
            HIGH -> 0xFFFF9800
            CRITICAL -> 0xFFF44336
            UNKNOWN -> 0xFF9E9E9E
        }
    
    val displayName: String
        get() = when (this) {
            SAFE -> "Safe"
            LOW -> "Low Risk"
            MEDIUM -> "Medium Risk"
            HIGH -> "High Risk"
            CRITICAL -> "Critical"
            UNKNOWN -> "Unknown"
        }
}

data class ThreatResult(
    val ipAddress: String,
    val threatLevel: ThreatLevel,
    val threatScore: Int,
    val message: String,
    val country: String? = null,
    val isp: String? = null,
    val detectionTime: Long = System.currentTimeMillis()
)
