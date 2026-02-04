package com.example.inteltrace_v3.core.vpn

import com.example.inteltrace_v3.domain.models.NetworkPacket
import java.nio.ByteBuffer

object PacketParser {
    
    private const val IPV4_VERSION = 4
    private const val IPV6_VERSION = 6
    private const val PROTOCOL_TCP = 6
    private const val PROTOCOL_UDP = 17
    private const val PROTOCOL_ICMP = 1
    
    fun parse(buffer: ByteBuffer): NetworkPacket? {
        try {
            buffer.position(0)
            val versionAndIHL = buffer.get().toInt() and 0xFF
            val version = (versionAndIHL shr 4) and 0xF
            
            return when (version) {
                IPV4_VERSION -> parseIPv4(buffer, versionAndIHL)
                IPV6_VERSION -> parseIPv6(buffer)
                else -> null
            }
        } catch (e: Exception) {
            return null
        }
    }
    
    private fun parseIPv4(buffer: ByteBuffer, versionAndIHL: Int): NetworkPacket {
        buffer.position(0)
        
        // Skip version and IHL
        buffer.get()
        
        // Skip DSCP and ECN
        buffer.get()
        
        // Total length
        val totalLength = buffer.short.toInt() and 0xFFFF
        
        // Skip identification, flags, fragment offset
        buffer.getShort()
        buffer.getShort()
        
        // TTL
        buffer.get()
        
        // Protocol
        val protocol = buffer.get().toInt() and 0xFF
        
        // Skip checksum
        buffer.getShort()
        
        // Source IP
        val srcAddress = extractIPv4Address(buffer)
        
        // Destination IP
        val dstAddress = extractIPv4Address(buffer)
        
        // Calculate header length
        val ihl = versionAndIHL and 0x0F
        val headerLength = ihl * 4
        
        // Skip to transport layer
        buffer.position(headerLength)
        
        // Extract ports
        val (srcPort, dstPort) = when (protocol) {
            PROTOCOL_TCP, PROTOCOL_UDP -> extractPorts(buffer)
            else -> Pair(0, 0)
        }
        
        return NetworkPacket(
            sourceIp = srcAddress,
            destIp = dstAddress,
            sourcePort = srcPort,
            destPort = dstPort,
            protocol = protocol,
            timestamp = System.currentTimeMillis(),
            packetSize = totalLength
        )
    }
    
    private fun parseIPv6(buffer: ByteBuffer): NetworkPacket {
        buffer.position(0)
        
        // Skip version, traffic class, flow label
        buffer.getInt()
        
        // Payload length
        val payloadLength = buffer.short.toInt() and 0xFFFF
        
        // Next header (protocol)
        val protocol = buffer.get().toInt() and 0xFF
        
        // Skip hop limit
        buffer.get()
        
        // Source address (16 bytes)
        val srcAddress = extractIPv6Address(buffer)
        
        // Destination address (16 bytes)
        val dstAddress = extractIPv6Address(buffer)
        
        // Extract ports
        val (srcPort, dstPort) = when (protocol) {
            PROTOCOL_TCP, PROTOCOL_UDP -> extractPorts(buffer)
            else -> Pair(0, 0)
        }
        
        return NetworkPacket(
            sourceIp = srcAddress,
            destIp = dstAddress,
            sourcePort = srcPort,
            destPort = dstPort,
            protocol = protocol,
            timestamp = System.currentTimeMillis(),
            packetSize = payloadLength + 40 // IPv6 header is 40 bytes
        )
    }
    
    private fun extractIPv4Address(buffer: ByteBuffer): String {
        val bytes = ByteArray(4)
        buffer.get(bytes)
        return "${bytes[0].toInt() and 0xFF}.${bytes[1].toInt() and 0xFF}." +
               "${bytes[2].toInt() and 0xFF}.${bytes[3].toInt() and 0xFF}"
    }
    
    private fun extractIPv6Address(buffer: ByteBuffer): String {
        val bytes = ByteArray(16)
        buffer.get(bytes)
        
        val sb = StringBuilder()
        for (i in 0 until 16 step 2) {
            if (i > 0) sb.append(":")
            val value = ((bytes[i].toInt() and 0xFF) shl 8) or (bytes[i + 1].toInt() and 0xFF)
            sb.append(String.format("%x", value))
        }
        
        return sb.toString()
    }
    
    private fun extractPorts(buffer: ByteBuffer): Pair<Int, Int> {
        val srcPort = buffer.short.toInt() and 0xFFFF
        val dstPort = buffer.short.toInt() and 0xFFFF
        return Pair(srcPort, dstPort)
    }
}
