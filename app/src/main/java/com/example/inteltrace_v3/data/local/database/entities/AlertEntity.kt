package com.example.inteltrace_v3.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alerts")
data class AlertEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val type: String, // CRITICAL, HIGH, MEDIUM, LOW
    val title: String,
    val message: String,
    val ipAddress: String,
    val packageName: String,
    val appName: String,
    val threatScore: Int,
    val isRead: Boolean = false,
    val isDismissed: Boolean = false
)
