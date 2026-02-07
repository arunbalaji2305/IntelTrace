package com.example.inteltrace_v3.core.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import com.example.inteltrace_v3.R
import com.example.inteltrace_v3.core.detection.ThreatDetectionEngine
import com.example.inteltrace_v3.core.protocol.DNSParser
import com.example.inteltrace_v3.core.protocol.TLSParser
import com.example.inteltrace_v3.core.vpn.packet.IpPacket
import com.example.inteltrace_v3.core.vpn.packet.Session
import com.example.inteltrace_v3.core.vpn.packet.SessionManager
import com.example.inteltrace_v3.data.local.database.entities.ConnectionEntity
import com.example.inteltrace_v3.data.local.preferences.SecurityPreferences
import com.example.inteltrace_v3.data.repository.ConnectionRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

/**
 * VPN Service that performs real packet capture and analysis.
 * 
 * This service:
 * 1. Routes all device traffic through a VPN tunnel
 * 2. Captures and parses every IP packet
 * 3. Extracts connection info, DNS queries, and TLS SNI (for URLs/domains)
 * 4. Forwards packets via protected sockets to maintain connectivity
 * 5. Runs threat detection on connections in real-time
 */
@AndroidEntryPoint
class IntelTraceVpnService : VpnService() {

    @Inject lateinit var detectionEngine: ThreatDetectionEngine
    @Inject lateinit var connectionRepository: ConnectionRepository
    @Inject lateinit var prefs: SecurityPreferences

    private var vpnInterface: ParcelFileDescriptor? = null
    private var vpnInput: FileInputStream? = null
    private var vpnOutput: FileOutputStream? = null
    
    private var sessionManager: SessionManager? = null
    private var isRunning = false
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Connection tracking
    private val activeConnections = ConcurrentHashMap<String, ConnectionInfo>()
    private val dnsCache = ConcurrentHashMap<String, String>() // IP -> Domain
    
    // Stats
    private var totalPackets = 0L
    private var totalBytes = 0L

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_VPN -> if (!isRunning) startVPN()
            ACTION_STOP_VPN -> { stopVPN(); stopSelf() }
        }
        return START_STICKY
    }

    private fun startVPN() {
        try {
            // Build VPN with routes to capture ALL traffic
            val builder = Builder()
                .setSession("IntelTrace")
                .setMtu(MTU)
                .addAddress("10.0.0.2", 32)
                .addRoute("0.0.0.0", 0)           // Capture all IPv4 traffic
                .addRoute("::", 0)                 // Capture all IPv6 traffic
                .addDnsServer("8.8.8.8")
                .addDnsServer("8.8.4.4")
                .setBlocking(true)

            // Exclude our own app from VPN to prevent loops
            try {
                builder.addDisallowedApplication(packageName)
            } catch (e: Exception) {
                Log.w(TAG, "Could not exclude self: ${e.message}")
            }

            vpnInterface = builder.establish()
            
            if (vpnInterface == null) {
                Log.e(TAG, "Failed to establish VPN interface")
                stopSelf()
                return
            }

            vpnInput = FileInputStream(vpnInterface!!.fileDescriptor)
            vpnOutput = FileOutputStream(vpnInterface!!.fileDescriptor)

            // Initialize session manager for packet forwarding
            sessionManager = SessionManager(
                vpnService = this,
                onSessionCreated = { session -> onNewSession(session) },
                onSessionClosed = { session -> onSessionClosed(session) },
                onDataReceived = { session, data -> onSessionData(session, data) }
            ).also { it.start() }

            isRunning = true
            prefs.isVpnEnabled = true

            startForeground(NOTIFICATION_ID, createNotification(
                "VPN Active", "Capturing network traffic..."
            ))

            // Start packet capture loop
            serviceScope.launch { packetCaptureLoop() }
            
            // Start periodic stats update
            serviceScope.launch { statsUpdateLoop() }

            Log.i(TAG, "VPN started - capturing all traffic")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting VPN: ${e.message}", e)
            stopSelf()
        }
    }

    /**
     * Main packet capture loop - reads packets from VPN, parses and processes them.
     */
    private suspend fun packetCaptureLoop() = withContext(Dispatchers.IO) {
        val packet = ByteArray(MTU)
        
        while (isRunning && isActive) {
            try {
                val length = vpnInput?.read(packet) ?: -1
                if (length <= 0) continue

                totalPackets++
                totalBytes += length

                // Parse the IP packet
                val ipPacket = IpPacket.parse(packet, length)
                if (ipPacket == null) {
                    Log.v(TAG, "Failed to parse packet of length $length")
                    continue
                }

                // Process the packet
                processPacket(ipPacket)

                // Forward packet via session manager to maintain connectivity
                vpnOutput?.let { output ->
                    sessionManager?.processOutgoingPacket(ipPacket, output)
                }

            } catch (e: CancellationException) {
                break
            } catch (e: Exception) {
                if (isRunning) {
                    Log.e(TAG, "Packet capture error: ${e.message}")
                    delay(100) // Brief delay on error
                }
            }
        }
    }

    /**
     * Process a captured packet - extract connection info and run analysis.
     */
    private fun processPacket(packet: IpPacket) {
        // Skip packets to/from localhost
        if (packet.destIp.startsWith("127.") || packet.destIp == "::1") return
        if (packet.sourceIp.startsWith("127.") || packet.sourceIp == "::1") return
        if (packet.destIp == "10.0.0.2") return // Our VPN address

        val connKey = "${packet.sourceIp}:${packet.sourcePort}->${packet.destIp}:${packet.destPort}:${packet.protocol}"
        
        // Check for DNS query/response (UDP port 53)
        if (packet.isUdp && (packet.destPort == 53 || packet.sourcePort == 53)) {
            processDnsPacket(packet)
        }

        // Check for new connections
        val existingConn = activeConnections[connKey]
        if (existingConn == null) {
            // This is a new connection
            val conn = ConnectionInfo(
                sourceIp = packet.sourceIp,
                sourcePort = packet.sourcePort,
                destIp = packet.destIp,
                destPort = packet.destPort,
                protocol = packet.protocol,
                firstSeen = System.currentTimeMillis(),
                domain = dnsCache[packet.destIp] // Try to get domain from DNS cache
            )
            activeConnections[connKey] = conn
            
            // Log and analyze new connection
            serviceScope.launch {
                onNewConnection(conn, packet)
            }
        } else {
            // Update existing connection
            existingConn.packetCount++
            existingConn.bytesOut += packet.totalLength
            existingConn.lastSeen = System.currentTimeMillis()
        }

        // Try to extract TLS SNI for HTTPS connections
        if (packet.isTcp && packet.destPort == 443 && packet.isSyn) {
            // First packet to 443 - we'll capture SNI on the actual ClientHello
        } else if (packet.isTcp && packet.destPort == 443 && packet.payloadLength > 0) {
            extractTlsSni(packet, connKey)
        }
    }

    /**
     * Process DNS packets to build IP->domain mapping.
     */
    private fun processDnsPacket(packet: IpPacket) {
        try {
            val payload = packet.getPayload()
            if (payload.isEmpty()) return

            val dns = DNSParser.parse(payload) ?: return

            // For DNS responses, cache the IP->domain mapping
            if (!dns.isQuery && dns.answers.isNotEmpty()) {
                val domain = dns.questions.firstOrNull()?.name ?: return
                
                dns.answers.forEach { answer ->
                    if (answer.type == 1 || answer.type == 28) { // A or AAAA record
                        dnsCache[answer.data] = domain
                        Log.d(TAG, "DNS: $domain -> ${answer.data}")
                        
                        // Update any existing connections to this IP with the domain
                        activeConnections.values
                            .filter { it.destIp == answer.data && it.domain == null }
                            .forEach { it.domain = domain }
                    }
                }
            } else if (dns.isQuery) {
                // Log DNS query
                val queryDomain = dns.questions.firstOrNull()?.name
                if (queryDomain != null) {
                    Log.d(TAG, "DNS Query: $queryDomain")
                }
            }
        } catch (e: Exception) {
            Log.v(TAG, "DNS parse error: ${e.message}")
        }
    }

    /**
     * Extract TLS Server Name Indication (SNI) from TLS ClientHello.
     */
    private fun extractTlsSni(packet: IpPacket, connKey: String) {
        try {
            val payload = packet.getPayload()
            if (payload.size < 43) return

            val tlsInfo = TLSParser.parseTLSClientHello(payload) ?: return
            
            val sni = tlsInfo.sni
            if (sni != null && sni.isNotEmpty()) {
                Log.d(TAG, "TLS SNI: $sni (${packet.destIp})")
                
                // Cache and update connection
                dnsCache[packet.destIp] = sni
                activeConnections[connKey]?.domain = sni
            }
        } catch (e: Exception) {
            // TLS parsing errors are expected for non-TLS traffic
        }
    }

    /**
     * Handle new connection - save to DB and run threat analysis.
     */
    private suspend fun onNewConnection(conn: ConnectionInfo, packet: IpPacket) {
        try {
            // Get app info (simplified - in real impl would track per-socket)
            val appName = conn.domain ?: conn.destIp
            val packageName = "network" // Would need UID tracking for actual app

            // Save to database
            val entity = ConnectionEntity(
                timestamp = conn.firstSeen,
                sourceIp = conn.sourceIp,
                destIp = conn.destIp,
                sourcePort = conn.sourcePort,
                destPort = conn.destPort,
                protocol = conn.protocol,
                packageName = packageName,
                appName = appName,
                bytesSent = packet.totalLength.toLong(),
                bytesReceived = 0,
                threatScore = 0
            )
            val dbId = connectionRepository.insertConnection(entity)
            conn.dbId = dbId

            Log.d(TAG, "New ${if (packet.isTcp) "TCP" else "UDP"}: ${conn.domain ?: conn.destIp}:${conn.destPort}")

            // Run threat analysis in background
            serviceScope.launch {
                analyzeConnection(conn, packet)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling new connection: ${e.message}")
        }
    }

    /**
     * Run threat detection on a connection.
     */
    private suspend fun analyzeConnection(conn: ConnectionInfo, packet: IpPacket) {
        try {
            val networkPacket = com.example.inteltrace_v3.domain.models.NetworkPacket(
                timestamp = conn.firstSeen,
                sourceIp = conn.sourceIp,
                destIp = conn.destIp,
                sourcePort = conn.sourcePort,
                destPort = conn.destPort,
                protocol = conn.protocol,
                packetSize = packet.totalLength
            )

            val analysis = detectionEngine.analyzeConnection(
                packet = networkPacket,
                packageName = "network",
                domain = conn.domain
            )

            conn.threatScore = analysis.threatScore

            // Update DB with threat score
            if (conn.dbId > 0 && analysis.threatScore > 0) {
                connectionRepository.updateThreatScore(conn.dbId, analysis.threatScore)
                Log.d(TAG, "Threat: ${conn.domain ?: conn.destIp} = ${analysis.threatScore}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Analysis error: ${e.message}")
        }
    }

    // Session manager callbacks
    private fun onNewSession(session: Session) {
        Log.d(TAG, "Session: ${session.destIp}:${session.destPort}")
    }

    private fun onSessionClosed(session: Session) {
        // Remove from active connections
        val key = "${session.sourceIp}:${session.sourcePort}->${session.destIp}:${session.destPort}:${session.protocol}"
        activeConnections.remove(key)
    }

    private fun onSessionData(session: Session, data: ByteArray) {
        // Update bytes received
        val key = "${session.sourceIp}:${session.sourcePort}->${session.destIp}:${session.destPort}:${session.protocol}"
        activeConnections[key]?.let {
            it.bytesIn += data.size
        }
    }

    /**
     * Periodic stats update and notification refresh.
     */
    private suspend fun statsUpdateLoop() = withContext(Dispatchers.IO) {
        while (isRunning && isActive) {
            delay(5_000)
            
            val activeCount = activeConnections.size
            val suspicious = activeConnections.values.count { it.threatScore > 30 }
            
            updateNotification(
                "Monitoring: $activeCount connections",
                if (suspicious > 0) "⚠️ $suspicious suspicious" else "All clear • ${totalPackets} packets"
            )
            
            // Update byte counts in DB periodically
            activeConnections.values.forEach { conn ->
                if (conn.dbId > 0) {
                    try {
                        connectionRepository.updateBytes(conn.dbId, conn.bytesOut, conn.bytesIn)
                    } catch (_: Exception) {}
                }
            }
            
            // Cleanup old connections
            val cutoff = System.currentTimeMillis() - 300_000 // 5 min
            activeConnections.entries.removeIf { it.value.lastSeen < cutoff }
        }
    }

    private fun stopVPN() {
        isRunning = false
        prefs.isVpnEnabled = false
        
        sessionManager?.stop()
        sessionManager = null
        
        try { vpnInput?.close() } catch (_: Exception) {}
        try { vpnOutput?.close() } catch (_: Exception) {}
        try { vpnInterface?.close() } catch (_: Exception) {}
        
        vpnInput = null
        vpnOutput = null
        vpnInterface = null
        
        serviceScope.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        
        Log.i(TAG, "VPN stopped")
    }

    private fun updateNotification(title: String, message: String) {
        try {
            val nm = getSystemService(NotificationManager::class.java)
            nm.notify(NOTIFICATION_ID, createNotification(title, message))
        } catch (_: Exception) {}
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "VPN Service", NotificationManager.IMPORTANCE_LOW
            ).apply { description = "IntelTrace VPN monitoring" }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun createNotification(title: String, message: String): Notification {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .build()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopVPN()
    }

    override fun onRevoke() {
        super.onRevoke()
        stopVPN()
        stopSelf()
    }

    /**
     * Internal connection tracking data.
     */
    private data class ConnectionInfo(
        val sourceIp: String,
        val sourcePort: Int,
        val destIp: String,
        val destPort: Int,
        val protocol: Int,
        val firstSeen: Long,
        var lastSeen: Long = firstSeen,
        var domain: String? = null,
        var packetCount: Int = 1,
        var bytesOut: Long = 0,
        var bytesIn: Long = 0,
        var threatScore: Int = 0,
        var dbId: Long = 0
    )

    companion object {
        private const val TAG = "IntelTraceVPN"
        const val ACTION_START_VPN = "com.example.inteltrace_v3.START_VPN"
        const val ACTION_STOP_VPN = "com.example.inteltrace_v3.STOP_VPN"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "vpn_service_channel"
        private const val MTU = 1500
    }
}
