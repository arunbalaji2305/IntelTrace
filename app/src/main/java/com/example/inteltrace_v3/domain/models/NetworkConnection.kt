package com.example.inteltrace_v3.domain.models

data class NetworkConnection(
    val id: Long = 0,
    val timestamp: Long,
    val sourceIp: String,
    val destIp: String,
    val sourcePort: Int,
    val destPort: Int,
    val protocol: Int,
    val packageName: String,
    val appName: String,
    val bytesSent: Long = 0,
    val bytesReceived: Long = 0,
    val threatScore: Int = 0,
    val isBlocked: Boolean = false,
    val country: String? = null,
    val city: String? = null
) {
    val protocolName: String
        get() = when (protocol) {
            6 -> "TCP"
            17 -> "UDP"
            1 -> "ICMP"
            else -> "Unknown($protocol)"
        }
    
    val timestampFormatted: String
        get() = java.text.SimpleDateFormat(
            "MMM dd, HH:mm:ss",
            java.util.Locale.getDefault()
        ).format(java.util.Date(timestamp))
}
