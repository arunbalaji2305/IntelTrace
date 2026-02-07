package com.example.inteltrace_v3.core.vpn.packet

import java.net.InetAddress
import java.nio.ByteBuffer

/**
 * Represents a parsed IPv4/IPv6 packet with transport layer (TCP/UDP) info.
 */
data class IpPacket(
    val version: Int,
    val protocol: Int,           // 6 = TCP, 17 = UDP, 1 = ICMP
    val sourceAddress: InetAddress,
    val destAddress: InetAddress,
    val sourcePort: Int,
    val destPort: Int,
    val headerLength: Int,       // IP header length
    val totalLength: Int,        // Total packet length
    val transportHeaderLength: Int,
    val payloadOffset: Int,      // Offset to transport payload
    val payloadLength: Int,
    val tcpFlags: Int = 0,       // TCP flags (SYN, ACK, FIN, etc.)
    val rawData: ByteArray
) {
    val sourceIp: String get() = sourceAddress.hostAddress ?: ""
    val destIp: String get() = destAddress.hostAddress ?: ""
    
    val isTcp: Boolean get() = protocol == PROTOCOL_TCP
    val isUdp: Boolean get() = protocol == PROTOCOL_UDP
    val isIcmp: Boolean get() = protocol == PROTOCOL_ICMP
    
    val isSyn: Boolean get() = (tcpFlags and TCP_SYN) != 0
    val isAck: Boolean get() = (tcpFlags and TCP_ACK) != 0
    val isFin: Boolean get() = (tcpFlags and TCP_FIN) != 0
    val isRst: Boolean get() = (tcpFlags and TCP_RST) != 0
    
    fun getPayload(): ByteArray {
        return if (payloadLength > 0 && payloadOffset + payloadLength <= rawData.size) {
            rawData.copyOfRange(payloadOffset, payloadOffset + payloadLength)
        } else {
            ByteArray(0)
        }
    }
    
    companion object {
        const val PROTOCOL_ICMP = 1
        const val PROTOCOL_TCP = 6
        const val PROTOCOL_UDP = 17
        
        const val TCP_FIN = 0x01
        const val TCP_SYN = 0x02
        const val TCP_RST = 0x04
        const val TCP_PSH = 0x08
        const val TCP_ACK = 0x10
        
        /**
         * Parse raw packet data from tun interface.
         * Returns null if packet is malformed or unsupported.
         */
        fun parse(data: ByteArray, length: Int): IpPacket? {
            if (length < 20) return null
            
            val buffer = ByteBuffer.wrap(data, 0, length)
            val versionIhl = buffer.get().toInt() and 0xFF
            val version = versionIhl shr 4
            
            return when (version) {
                4 -> parseIPv4(data, length, buffer, versionIhl)
                6 -> parseIPv6(data, length, buffer)
                else -> null
            }
        }
        
        private fun parseIPv4(data: ByteArray, length: Int, buffer: ByteBuffer, versionIhl: Int): IpPacket? {
            val ihl = versionIhl and 0x0F
            val ipHeaderLength = ihl * 4
            
            if (length < ipHeaderLength) return null
            
            buffer.get() // TOS
            val totalLength = buffer.short.toInt() and 0xFFFF
            buffer.short // ID
            buffer.short // Flags + Fragment offset
            buffer.get() // TTL
            val protocol = buffer.get().toInt() and 0xFF
            buffer.short // Checksum
            
            val srcBytes = ByteArray(4)
            val dstBytes = ByteArray(4)
            buffer.get(srcBytes)
            buffer.get(dstBytes)
            
            val sourceAddress = InetAddress.getByAddress(srcBytes)
            val destAddress = InetAddress.getByAddress(dstBytes)
            
            // Skip IP options if present
            buffer.position(ipHeaderLength)
            
            return parseTransportLayer(
                data, length, buffer, 4, protocol,
                sourceAddress, destAddress, ipHeaderLength, totalLength
            )
        }
        
        private fun parseIPv6(data: ByteArray, length: Int, buffer: ByteBuffer): IpPacket? {
            if (length < 40) return null
            
            val ipHeaderLength = 40
            
            buffer.int // Version, Traffic Class, Flow Label
            val payloadLength = buffer.short.toInt() and 0xFFFF
            val nextHeader = buffer.get().toInt() and 0xFF
            buffer.get() // Hop Limit
            
            val srcBytes = ByteArray(16)
            val dstBytes = ByteArray(16)
            buffer.get(srcBytes)
            buffer.get(dstBytes)
            
            val sourceAddress = InetAddress.getByAddress(srcBytes)
            val destAddress = InetAddress.getByAddress(dstBytes)
            
            val totalLength = ipHeaderLength + payloadLength
            
            return parseTransportLayer(
                data, length, buffer, 6, nextHeader,
                sourceAddress, destAddress, ipHeaderLength, totalLength
            )
        }
        
        private fun parseTransportLayer(
            data: ByteArray,
            length: Int,
            buffer: ByteBuffer,
            version: Int,
            protocol: Int,
            sourceAddress: InetAddress,
            destAddress: InetAddress,
            ipHeaderLength: Int,
            totalLength: Int
        ): IpPacket? {
            var sourcePort = 0
            var destPort = 0
            var transportHeaderLength = 0
            var tcpFlags = 0
            
            when (protocol) {
                PROTOCOL_TCP -> {
                    if (buffer.remaining() < 20) return null
                    sourcePort = buffer.short.toInt() and 0xFFFF
                    destPort = buffer.short.toInt() and 0xFFFF
                    buffer.int // Sequence number
                    buffer.int // Ack number
                    val dataOffsetFlags = buffer.short.toInt() and 0xFFFF
                    transportHeaderLength = ((dataOffsetFlags shr 12) and 0x0F) * 4
                    tcpFlags = dataOffsetFlags and 0x3F
                }
                PROTOCOL_UDP -> {
                    if (buffer.remaining() < 8) return null
                    sourcePort = buffer.short.toInt() and 0xFFFF
                    destPort = buffer.short.toInt() and 0xFFFF
                    transportHeaderLength = 8
                }
                PROTOCOL_ICMP -> {
                    transportHeaderLength = 8
                }
                else -> {
                    // Unknown protocol
                    return null
                }
            }
            
            val payloadOffset = ipHeaderLength + transportHeaderLength
            val payloadLength = (totalLength - payloadOffset).coerceAtLeast(0)
            
            return IpPacket(
                version = version,
                protocol = protocol,
                sourceAddress = sourceAddress,
                destAddress = destAddress,
                sourcePort = sourcePort,
                destPort = destPort,
                headerLength = ipHeaderLength,
                totalLength = totalLength,
                transportHeaderLength = transportHeaderLength,
                payloadOffset = payloadOffset,
                payloadLength = payloadLength,
                tcpFlags = tcpFlags,
                rawData = data.copyOf(length)
            )
        }
    }
}
