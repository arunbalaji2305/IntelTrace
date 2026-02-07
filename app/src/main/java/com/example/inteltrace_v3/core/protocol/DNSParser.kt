package com.example.inteltrace_v3.core.protocol

import java.nio.ByteBuffer

object DNSParser {
    
    data class DNSPacket(
        val transactionId: Int,
        val isQuery: Boolean,
        val opcode: Int,
        val questions: List<DNSQuestion>,
        val answers: List<DNSAnswer>
    )
    
    data class DNSQuestion(
        val name: String,
        val type: Int,
        val qClass: Int
    )
    
    data class DNSAnswer(
        val name: String,
        val type: Int,
        val ttl: Int,
        val data: String
    )
    
    fun parse(udpPayload: ByteArray): DNSPacket? {
        if (udpPayload.size < 12) return null
        
        try {
            val buffer = ByteBuffer.wrap(udpPayload)
            
            val transactionId = buffer.short.toInt() and 0xFFFF
            val flags = buffer.short.toInt() and 0xFFFF
            
            val isQuery = (flags and 0x8000) == 0
            val opcode = (flags shr 11) and 0x0F
            
            val questionCount = buffer.short.toInt() and 0xFFFF
            val answerCount = buffer.short.toInt() and 0xFFFF
            buffer.short
            buffer.short
            
            val questions = mutableListOf<DNSQuestion>()
            repeat(questionCount) {
                val name = parseDomainName(buffer, udpPayload)
                val type = buffer.short.toInt() and 0xFFFF
                val qClass = buffer.short.toInt() and 0xFFFF
                
                questions.add(DNSQuestion(name, type, qClass))
            }
            
            val answers = mutableListOf<DNSAnswer>()
            repeat(answerCount) {
                val name = parseDomainName(buffer, udpPayload)
                val type = buffer.short.toInt() and 0xFFFF
                buffer.short
                val ttl = buffer.int
                val dataLength = buffer.short.toInt() and 0xFFFF
                
                val data = when (type) {
                    1 -> {
                        val ipBytes = ByteArray(4)
                        buffer.get(ipBytes)
                        ipBytes.joinToString(".") { (it.toInt() and 0xFF).toString() }
                    }
                    28 -> {
                        val ipBytes = ByteArray(16)
                        buffer.get(ipBytes)
                        ipBytes.joinToString(":") { String.format("%02x", it) }
                    }
                    5 -> parseDomainName(buffer, udpPayload)
                    else -> {
                        val dataBytes = ByteArray(dataLength)
                        buffer.get(dataBytes)
                        dataBytes.joinToString("") { String.format("%02x", it) }
                    }
                }
                
                answers.add(DNSAnswer(name, type, ttl, data))
            }
            
            return DNSPacket(
                transactionId = transactionId,
                isQuery = isQuery,
                opcode = opcode,
                questions = questions,
                answers = answers
            )
            
        } catch (e: Exception) {
            return null
        }
    }
    
    private fun parseDomainName(buffer: ByteBuffer, fullPacket: ByteArray): String {
        val labels = mutableListOf<String>()
        var jumped = false
        var jumpPosition = -1
        val originalPosition = buffer.position()
        var position = originalPosition
        
        while (true) {
            if (position >= fullPacket.size) break
            
            val length = fullPacket[position].toInt() and 0xFF
            
            if (length == 0) {
                if (!jumped) buffer.position(position + 1)
                break
            }
            
            if ((length and 0xC0) == 0xC0) {
                if (!jumped) {
                    jumpPosition = position + 2
                    jumped = true
                }
                
                val pointer = ((length and 0x3F) shl 8) or (fullPacket[position + 1].toInt() and 0xFF)
                position = pointer
                continue
            }
            
            position++
            if (position + length > fullPacket.size) break
            
            val label = String(fullPacket, position, length)
            labels.add(label)
            position += length
        }
        
        if (jumped && jumpPosition > 0) {
            buffer.position(jumpPosition)
        } else if (!jumped) {
            buffer.position(position + 1)
        }
        
        return labels.joinToString(".")
    }
    
    fun extractQueriesFromPacket(dnsPacket: DNSPacket): List<String> {
        return dnsPacket.questions.map { it.name }.filter { it.isNotBlank() }
    }
}
