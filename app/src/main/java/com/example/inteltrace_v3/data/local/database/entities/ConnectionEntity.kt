package com.example.inteltrace_v3.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "connections")
data class ConnectionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val sourceIp: String,
    val destIp: String,
    val sourcePort: Int,
    val destPort: Int,
    val protocol: Int, // 6 = TCP, 17 = UDP
    val packageName: String,
    val appName: String,
    val bytesSent: Long = 0,
    val bytesReceived: Long = 0,
    val threatScore: Int = 0,
    val isBlocked: Boolean = false,
    val country: String? = null,
    val city: String? = null
)
