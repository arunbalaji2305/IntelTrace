package com.example.inteltrace_v3.core.vpn.packet

import android.net.VpnService
import android.util.Log
import kotlinx.coroutines.*
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.SocketChannel
import java.util.concurrent.ConcurrentHashMap

/**
 * Tracks and forwards network sessions (TCP/UDP) through protected sockets.
 * This allows the VPN to capture packets while still allowing traffic to flow.
 */
class SessionManager(
    private val vpnService: VpnService,
    private val onSessionCreated: (Session) -> Unit,
    private val onSessionClosed: (Session) -> Unit,
    private val onDataReceived: (Session, ByteArray) -> Unit
) {
    private val tcpSessions = ConcurrentHashMap<String, TcpSession>()
    private val udpSessions = ConcurrentHashMap<String, UdpSession>()
    
    private val selector = Selector.open()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var running = false
    
    companion object {
        private const val TAG = "SessionManager"
        private const val UDP_TIMEOUT_MS = 60_000L
        private const val TCP_TIMEOUT_MS = 300_000L
    }
    
    fun start() {
        running = true
        scope.launch { selectorLoop() }
        scope.launch { cleanupLoop() }
    }
    
    fun stop() {
        running = false
        scope.cancel()
        
        tcpSessions.values.forEach { it.close() }
        udpSessions.values.forEach { it.close() }
        tcpSessions.clear()
        udpSessions.clear()
        
        try { selector.close() } catch (_: Exception) {}
    }
    
    /**
     * Process an outgoing IP packet captured from the VPN.
     * Creates or updates the appropriate session and forwards the packet.
     */
    suspend fun processOutgoingPacket(packet: IpPacket, vpnOutput: java.io.FileOutputStream) {
        when (packet.protocol) {
            IpPacket.PROTOCOL_TCP -> processTcpPacket(packet, vpnOutput)
            IpPacket.PROTOCOL_UDP -> processUdpPacket(packet, vpnOutput)
        }
    }
    
    private suspend fun processTcpPacket(packet: IpPacket, vpnOutput: java.io.FileOutputStream) {
        val sessionKey = "${packet.sourceIp}:${packet.sourcePort}->${packet.destIp}:${packet.destPort}"
        
        var session = tcpSessions[sessionKey]
        
        // Handle connection lifecycle
        if (packet.isSyn && !packet.isAck && session == null) {
            // New connection attempt
            session = createTcpSession(packet, vpnOutput)
            if (session != null) {
                tcpSessions[sessionKey] = session
                onSessionCreated(session)
                Log.d(TAG, "New TCP session: $sessionKey")
            }
        } else if (session != null) {
            if (packet.isFin || packet.isRst) {
                // Connection closing
                session.close()
                tcpSessions.remove(sessionKey)
                onSessionClosed(session)
                Log.d(TAG, "TCP session closed: $sessionKey")
                return
            }
            
            // Forward payload data
            val payload = packet.getPayload()
            if (payload.isNotEmpty()) {
                session.sendData(payload)
            }
        }
    }
    
    private suspend fun processUdpPacket(packet: IpPacket, vpnOutput: java.io.FileOutputStream) {
        val sessionKey = "${packet.sourceIp}:${packet.sourcePort}->${packet.destIp}:${packet.destPort}"
        
        var session = udpSessions[sessionKey]
        
        if (session == null) {
            session = createUdpSession(packet, vpnOutput)
            if (session != null) {
                udpSessions[sessionKey] = session
                onSessionCreated(session)
                Log.d(TAG, "New UDP session: $sessionKey -> ${packet.destIp}:${packet.destPort}")
            }
        }
        
        // Forward the UDP payload
        val payload = packet.getPayload()
        if (session != null && payload.isNotEmpty()) {
            session.sendData(payload)
            session.lastActivity = System.currentTimeMillis()
        }
    }
    
    private fun createTcpSession(packet: IpPacket, vpnOutput: java.io.FileOutputStream): TcpSession? {
        return try {
            val channel = SocketChannel.open()
            channel.configureBlocking(false)
            
            // Protect socket from VPN to avoid loop
            if (!vpnService.protect(channel.socket())) {
                Log.e(TAG, "Failed to protect TCP socket")
                channel.close()
                return null
            }
            
            val remoteAddress = InetSocketAddress(packet.destAddress, packet.destPort)
            channel.connect(remoteAddress)
            
            val session = TcpSession(
                sessionKey = "${packet.sourceIp}:${packet.sourcePort}->${packet.destIp}:${packet.destPort}",
                sourceIp = packet.sourceIp,
                sourcePort = packet.sourcePort,
                destIp = packet.destIp,
                destPort = packet.destPort,
                channel = channel,
                vpnOutput = vpnOutput,
                localPort = packet.sourcePort,
                remoteAddress = remoteAddress
            )
            
            // Register with selector for reading responses
            channel.register(selector, SelectionKey.OP_CONNECT or SelectionKey.OP_READ, session)
            selector.wakeup()
            
            session
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create TCP session: ${e.message}")
            null
        }
    }
    
    private fun createUdpSession(packet: IpPacket, vpnOutput: java.io.FileOutputStream): UdpSession? {
        return try {
            val channel = DatagramChannel.open()
            channel.configureBlocking(false)
            
            // Protect socket from VPN to avoid loop
            if (!vpnService.protect(channel.socket())) {
                Log.e(TAG, "Failed to protect UDP socket")
                channel.close()
                return null
            }
            
            val remoteAddress = InetSocketAddress(packet.destAddress, packet.destPort)
            channel.connect(remoteAddress)
            
            val session = UdpSession(
                sessionKey = "${packet.sourceIp}:${packet.sourcePort}->${packet.destIp}:${packet.destPort}",
                sourceIp = packet.sourceIp,
                sourcePort = packet.sourcePort,
                destIp = packet.destIp,
                destPort = packet.destPort,
                channel = channel,
                vpnOutput = vpnOutput,
                remoteAddress = remoteAddress
            )
            
            // Register with selector for reading responses
            channel.register(selector, SelectionKey.OP_READ, session)
            selector.wakeup()
            
            session
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create UDP session: ${e.message}")
            null
        }
    }
    
    private suspend fun selectorLoop() = withContext(Dispatchers.IO) {
        val readBuffer = ByteBuffer.allocate(65535)
        
        while (running && isActive) {
            try {
                val readyCount = selector.select(1000)
                if (readyCount == 0) continue
                
                val keys = selector.selectedKeys().iterator()
                while (keys.hasNext()) {
                    val key = keys.next()
                    keys.remove()
                    
                    if (!key.isValid) continue
                    
                    val session = key.attachment()
                    
                    when {
                        key.isConnectable -> handleConnect(key, session)
                        key.isReadable -> handleRead(key, session, readBuffer)
                    }
                }
            } catch (e: Exception) {
                if (running) {
                    Log.e(TAG, "Selector error: ${e.message}")
                }
            }
        }
    }
    
    private fun handleConnect(key: SelectionKey, session: Any?) {
        if (session !is TcpSession) return
        
        try {
            if (session.channel.finishConnect()) {
                key.interestOps(SelectionKey.OP_READ)
                Log.d(TAG, "TCP connected: ${session.sessionKey}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "TCP connect failed: ${e.message}")
            session.close()
            tcpSessions.remove(session.sessionKey)
            onSessionClosed(session)
        }
    }
    
    private fun handleRead(key: SelectionKey, session: Any?, buffer: ByteBuffer) {
        buffer.clear()
        
        try {
            when (session) {
                is TcpSession -> {
                    val bytesRead = session.channel.read(buffer)
                    if (bytesRead > 0) {
                        buffer.flip()
                        val data = ByteArray(bytesRead)
                        buffer.get(data)
                        session.lastActivity = System.currentTimeMillis()
                        
                        // Send response back through VPN
                        writeResponseToVpn(session, data)
                        onDataReceived(session, data)
                    } else if (bytesRead == -1) {
                        // Connection closed
                        session.close()
                        tcpSessions.remove(session.sessionKey)
                        onSessionClosed(session)
                    }
                }
                is UdpSession -> {
                    val bytesRead = session.channel.read(buffer)
                    if (bytesRead > 0) {
                        buffer.flip()
                        val data = ByteArray(bytesRead)
                        buffer.get(data)
                        session.lastActivity = System.currentTimeMillis()
                        
                        // Send response back through VPN
                        writeResponseToVpn(session, data)
                        onDataReceived(session, data)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Read error: ${e.message}")
            when (session) {
                is TcpSession -> {
                    session.close()
                    tcpSessions.remove(session.sessionKey)
                    onSessionClosed(session)
                }
                is UdpSession -> {
                    session.close()
                    udpSessions.remove(session.sessionKey)
                    onSessionClosed(session)
                }
            }
        }
    }
    
    private fun writeResponseToVpn(session: Session, data: ByteArray) {
        // Build IP packet header and write back to VPN
        // This is a simplified version - in production you'd reconstruct proper headers
        try {
            val packet = buildResponsePacket(session, data)
            session.vpnOutput.write(packet)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write VPN response: ${e.message}")
        }
    }
    
    private fun buildResponsePacket(session: Session, payload: ByteArray): ByteArray {
        val isTcp = session is TcpSession
        val headerSize = 20 + if (isTcp) 20 else 8 // IP + TCP/UDP
        val totalSize = headerSize + payload.size
        
        val packet = ByteArray(totalSize)
        val buffer = ByteBuffer.wrap(packet)
        
        // IPv4 header
        buffer.put((0x45).toByte()) // Version 4, IHL 5
        buffer.put(0) // TOS
        buffer.putShort(totalSize.toShort()) // Total length
        buffer.putShort(0) // ID
        buffer.putShort(0x4000.toShort()) // Don't fragment
        buffer.put(64) // TTL
        buffer.put((if (isTcp) 6 else 17).toByte()) // Protocol
        buffer.putShort(0) // Checksum (will be calculated by kernel)
        
        // Source = remote, Dest = local (response direction)
        val srcAddr = InetAddress.getByName(session.destIp).address
        val dstAddr = InetAddress.getByName(session.sourceIp).address
        buffer.put(srcAddr)
        buffer.put(dstAddr)
        
        if (isTcp) {
            // TCP header (simplified)
            buffer.putShort(session.destPort.toShort()) // Source port
            buffer.putShort(session.sourcePort.toShort()) // Dest port
            buffer.putInt(0) // Sequence
            buffer.putInt(0) // Ack
            buffer.putShort(0x5010.toShort()) // Data offset + ACK flag
            buffer.putShort(65535.toShort()) // Window
            buffer.putShort(0) // Checksum
            buffer.putShort(0) // Urgent
        } else {
            // UDP header
            buffer.putShort(session.destPort.toShort())
            buffer.putShort(session.sourcePort.toShort())
            buffer.putShort((8 + payload.size).toShort())
            buffer.putShort(0) // Checksum
        }
        
        buffer.put(payload)
        
        return packet
    }
    
    private suspend fun cleanupLoop() = withContext(Dispatchers.IO) {
        while (running && isActive) {
            delay(30_000)
            
            val now = System.currentTimeMillis()
            
            // Clean up stale UDP sessions
            val staleUdp = udpSessions.entries.filter {
                now - it.value.lastActivity > UDP_TIMEOUT_MS
            }
            staleUdp.forEach { (key, session) ->
                session.close()
                udpSessions.remove(key)
                onSessionClosed(session)
            }
            
            // Clean up stale TCP sessions
            val staleTcp = tcpSessions.entries.filter {
                now - it.value.lastActivity > TCP_TIMEOUT_MS
            }
            staleTcp.forEach { (key, session) ->
                session.close()
                tcpSessions.remove(key)
                onSessionClosed(session)
            }
            
            if (staleUdp.isNotEmpty() || staleTcp.isNotEmpty()) {
                Log.d(TAG, "Cleaned up ${staleUdp.size} UDP and ${staleTcp.size} TCP sessions")
            }
        }
    }
    
    fun getActiveSessionCount(): Int = tcpSessions.size + udpSessions.size
    fun getTcpSessionCount(): Int = tcpSessions.size
    fun getUdpSessionCount(): Int = udpSessions.size
}

/**
 * Base class for network sessions
 */
abstract class Session(
    val sessionKey: String,
    val sourceIp: String,
    val sourcePort: Int,
    val destIp: String,
    val destPort: Int,
    val vpnOutput: java.io.FileOutputStream
) {
    var lastActivity: Long = System.currentTimeMillis()
    var bytesOut: Long = 0
    var bytesIn: Long = 0
    var domain: String? = null
    
    abstract fun sendData(data: ByteArray)
    abstract fun close()
    abstract val protocol: Int
}

class TcpSession(
    sessionKey: String,
    sourceIp: String,
    sourcePort: Int,
    destIp: String,
    destPort: Int,
    val channel: SocketChannel,
    vpnOutput: java.io.FileOutputStream,
    val localPort: Int,
    val remoteAddress: InetSocketAddress
) : Session(sessionKey, sourceIp, sourcePort, destIp, destPort, vpnOutput) {
    
    override val protocol = 6
    
    override fun sendData(data: ByteArray) {
        try {
            val buffer = ByteBuffer.wrap(data)
            while (buffer.hasRemaining()) {
                channel.write(buffer)
            }
            bytesOut += data.size
        } catch (e: Exception) {
            // Connection may be closed
        }
    }
    
    override fun close() {
        try {
            channel.close()
        } catch (_: Exception) {}
    }
}

class UdpSession(
    sessionKey: String,
    sourceIp: String,
    sourcePort: Int,
    destIp: String,
    destPort: Int,
    val channel: DatagramChannel,
    vpnOutput: java.io.FileOutputStream,
    val remoteAddress: InetSocketAddress
) : Session(sessionKey, sourceIp, sourcePort, destIp, destPort, vpnOutput) {
    
    override val protocol = 17
    
    override fun sendData(data: ByteArray) {
        try {
            channel.write(ByteBuffer.wrap(data))
            bytesOut += data.size
        } catch (e: Exception) {
            // Socket may be closed
        }
    }
    
    override fun close() {
        try {
            channel.close()
        } catch (_: Exception) {}
    }
}
