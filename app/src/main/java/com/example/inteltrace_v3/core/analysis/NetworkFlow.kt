package com.example.inteltrace_v3.core.analysis

data class NetworkFlow(
    val flowId: String,
    val sourceIp: String,
    val destIp: String,
    val sourcePort: Int,
    val destPort: Int,
    val protocol: Int,
    val startTime: Long,
    var endTime: Long,
    var packetCount: Int = 0,
    var bytesSent: Long = 0,
    var bytesReceived: Long = 0,
    val packetTimestamps: MutableList<Long> = mutableListOf(),
    val packetSizes: MutableList<Int> = mutableListOf(),
    var dnsQueries: MutableList<String> = mutableListOf(),
    var tlsSni: String? = null,
    var httpHeaders: MutableMap<String, String> = mutableMapOf()
) {
    fun getDuration(): Long = endTime - startTime
    
    fun getAveragePacketSize(): Double {
        return if (packetCount > 0) bytesSent.toDouble() / packetCount else 0.0
    }
    
    fun getPacketsPerSecond(): Double {
        val durationSeconds = getDuration() / 1000.0
        return if (durationSeconds > 0) packetCount / durationSeconds else 0.0
    }
    
    fun getBytesPerSecond(): Double {
        val durationSeconds = getDuration() / 1000.0
        return if (durationSeconds > 0) bytesSent / durationSeconds else 0.0
    }
    
    fun addPacket(timestamp: Long, size: Int, isOutgoing: Boolean) {
        packetCount++
        packetTimestamps.add(timestamp)
        packetSizes.add(size)
        endTime = timestamp
        
        if (isOutgoing) {
            bytesSent += size
        } else {
            bytesReceived += size
        }
    }
    
    fun isActive(currentTime: Long, timeoutMs: Long = 30000): Boolean {
        return currentTime - endTime < timeoutMs
    }
    
    companion object {
        fun generateFlowId(
            sourceIp: String,
            destIp: String,
            sourcePort: Int,
            destPort: Int,
            protocol: Int
        ): String {
            return "$sourceIp:$sourcePort-$destIp:$destPort:$protocol"
        }
    }
}
