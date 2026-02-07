package com.example.inteltrace_v3.domain.models

data class NetworkPacket(
    val sourceIp: String,
    val destIp: String,
    val sourcePort: Int,
    val destPort: Int,
    val protocol: Int,
    val timestamp: Long,
    val packetSize: Int = 0,
    val uid: Int = -1,
    val payload: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as NetworkPacket
        
        if (sourceIp != other.sourceIp) return false
        if (destIp != other.destIp) return false
        if (sourcePort != other.sourcePort) return false
        if (destPort != other.destPort) return false
        if (protocol != other.protocol) return false
        if (timestamp != other.timestamp) return false
        if (packetSize != other.packetSize) return false
        if (uid != other.uid) return false
        if (payload != null) {
            if (other.payload == null) return false
            if (!payload.contentEquals(other.payload)) return false
        } else if (other.payload != null) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = sourceIp.hashCode()
        result = 31 * result + destIp.hashCode()
        result = 31 * result + sourcePort
        result = 31 * result + destPort
        result = 31 * result + protocol
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + packetSize
        result = 31 * result + uid
        result = 31 * result + (payload?.contentHashCode() ?: 0)
        return result
    }
}
