package com.example.inteltrace_v3.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_info")
data class AppInfoEntity(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val isWhitelisted: Boolean = false,
    val isBlacklisted: Boolean = false,
    val totalConnections: Int = 0,
    val suspiciousConnections: Int = 0,
    val lastConnectionTime: Long = 0,
    val totalBytesSent: Long = 0,
    val totalBytesReceived: Long = 0
)
