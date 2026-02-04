package com.example.inteltrace_v3.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "threats")
data class ThreatEntity(
    @PrimaryKey
    val ipAddress: String,
    val threatScore: Int,
    val abuseConfidenceScore: Int = 0,
    val totalReports: Int = 0,
    val country: String? = null,
    val isp: String? = null,
    val domain: String? = null,
    val usageType: String? = null,
    val isMalicious: Boolean = false,
    val categories: String? = null, // Comma-separated categories
    val lastChecked: Long,
    val virusTotalDetections: Int = 0,
    val virusTotalEngines: Int = 0
)
