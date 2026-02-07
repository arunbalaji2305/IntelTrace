package com.example.inteltrace_v3.core.protocol

import java.nio.ByteBuffer

object TLSParser {
    
    private const val TLS_HANDSHAKE = 22
    private const val CLIENT_HELLO = 1
    private const val EXTENSION_SERVER_NAME = 0
    
    data class TLSInfo(
        val sni: String?,
        val tlsVersion: String,
        val cipherSuites: List<Int>,
        val extensions: List<Int>
    )
    
    fun parseTLSClientHello(tcpPayload: ByteArray): TLSInfo? {
        if (tcpPayload.size < 43) return null
        
        try {
            val buffer = ByteBuffer.wrap(tcpPayload)
            
            val contentType = buffer.get().toInt() and 0xFF
            if (contentType != TLS_HANDSHAKE) return null
            
            val majorVersion = buffer.get().toInt() and 0xFF
            val minorVersion = buffer.get().toInt() and 0xFF
            val tlsVersion = "TLS ${majorVersion}.${minorVersion}"
            
            val recordLength = buffer.short.toInt() and 0xFFFF
            if (recordLength + 5 > tcpPayload.size) return null
            
            val handshakeType = buffer.get().toInt() and 0xFF
            if (handshakeType != CLIENT_HELLO) return null
            
            val handshakeLength = ((buffer.get().toInt() and 0xFF) shl 16) or
                                  ((buffer.get().toInt() and 0xFF) shl 8) or
                                  (buffer.get().toInt() and 0xFF)
            
            buffer.get()
            buffer.get()
            
            val random = ByteArray(32)
            buffer.get(random)
            
            val sessionIdLength = buffer.get().toInt() and 0xFF
            if (sessionIdLength > 0) {
                buffer.position(buffer.position() + sessionIdLength)
            }
            
            val cipherSuitesLength = buffer.short.toInt() and 0xFFFF
            val cipherSuites = mutableListOf<Int>()
            repeat(cipherSuitesLength / 2) {
                cipherSuites.add(buffer.short.toInt() and 0xFFFF)
            }
            
            val compressionMethodsLength = buffer.get().toInt() and 0xFF
            buffer.position(buffer.position() + compressionMethodsLength)
            
            var sni: String? = null
            val extensions = mutableListOf<Int>()
            
            if (buffer.remaining() >= 2) {
                val extensionsLength = buffer.short.toInt() and 0xFFFF
                val extensionsEnd = buffer.position() + extensionsLength
                
                while (buffer.position() < extensionsEnd && buffer.remaining() >= 4) {
                    val extensionType = buffer.short.toInt() and 0xFFFF
                    val extensionLength = buffer.short.toInt() and 0xFFFF
                    
                    extensions.add(extensionType)
                    
                    if (extensionType == EXTENSION_SERVER_NAME && extensionLength > 0) {
                        val serverNameListLength = buffer.short.toInt() and 0xFFFF
                        
                        while (buffer.position() < extensionsEnd && buffer.remaining() >= 3) {
                            val nameType = buffer.get().toInt() and 0xFF
                            val nameLength = buffer.short.toInt() and 0xFFFF
                            
                            if (nameType == 0 && nameLength > 0 && buffer.remaining() >= nameLength) {
                                val nameBytes = ByteArray(nameLength)
                                buffer.get(nameBytes)
                                sni = String(nameBytes)
                                break
                            } else if (nameLength > 0) {
                                buffer.position(buffer.position() + nameLength)
                            }
                        }
                    } else if (extensionLength > 0 && buffer.remaining() >= extensionLength) {
                        buffer.position(buffer.position() + extensionLength)
                    }
                }
            }
            
            return TLSInfo(
                sni = sni,
                tlsVersion = tlsVersion,
                cipherSuites = cipherSuites,
                extensions = extensions
            )
            
        } catch (e: Exception) {
            return null
        }
    }
    
    fun extractSNI(tcpPayload: ByteArray): String? {
        return parseTLSClientHello(tcpPayload)?.sni
    }
}
