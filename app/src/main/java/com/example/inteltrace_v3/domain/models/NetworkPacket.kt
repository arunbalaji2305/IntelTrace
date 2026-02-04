package com.example.inteltrace_v3.domain.models

data class NetworkPacket(
    val sourceIp: String,
    val destIp: String,
    val sourcePort: Int,
    val destPort: Int,
    val protocol: Int,
    val timestamp: Long,
    val packetSize: Int = 0,
    val uid: Int = -1
)
