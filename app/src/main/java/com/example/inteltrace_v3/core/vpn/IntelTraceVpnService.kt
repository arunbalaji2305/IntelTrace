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
import com.example.inteltrace_v3.data.local.database.entities.ConnectionEntity
import com.example.inteltrace_v3.data.local.preferences.SecurityPreferences
import com.example.inteltrace_v3.data.repository.ConnectionRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.delay
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import javax.inject.Inject

@AndroidEntryPoint
class IntelTraceVpnService : VpnService() {
    
    @Inject
    lateinit var detectionEngine: ThreatDetectionEngine
    
    @Inject
    lateinit var connectionRepository: ConnectionRepository
    
    @Inject
    lateinit var prefs: SecurityPreferences
    
    private var vpnInterface: ParcelFileDescriptor? = null
    private var isRunning = false
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val packetBuffer = ByteBuffer.allocate(32767)
    private val connectionCache = mutableMapOf<String, ConnectionInfo>()
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_VPN -> {
                if (!isRunning) {
                    startVPN()
                }
            }
            ACTION_STOP_VPN -> {
                stopVPN()
                stopSelf()
            }
        }
        return START_STICKY
    }
    
    private fun startVPN() {
        try {
            // Create VPN builder with minimal configuration
            // We use setMetered(false) to indicate this is a local monitoring VPN
            val builder = Builder()
                .addAddress("10.0.0.2", 32)  // /32 means only this single IP
                .setSession("IntelTrace")
                .setMetered(false)
                .setBlocking(false)  // Non-blocking mode
            
            // Important: Don't add routes - this prevents intercepting actual traffic
            // which would break internet connectivity
            
            // Exclude this app from VPN to prevent loops
            try {
                builder.addDisallowedApplication(packageName)
            } catch (e: Exception) {
                Log.w(TAG, "Could not exclude own package: ${e.message}")
            }
            
            vpnInterface = builder.establish()
            
            if (vpnInterface != null) {
                isRunning = true
                prefs.isVpnEnabled = true
                
                startForeground(NOTIFICATION_ID, createNotification("VPN Active", "Monitoring network connections"))
                
                // Start monitoring in background
                serviceScope.launch {
                    monitorConnections()
                }
                
                Log.i(TAG, "VPN monitoring started successfully")
            } else {
                Log.e(TAG, "Failed to establish VPN interface")
                stopSelf()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting VPN: ${e.message}", e)
            stopSelf()
        }
    }
    
    private suspend fun monitorConnections() = withContext(Dispatchers.IO) {
        // Instead of packet capture (which breaks internet), we use /proc/net 
        // monitoring which doesn't interfere with traffic
        try {
            while (isRunning && !Thread.currentThread().isInterrupted) {
                try {
                    // Read active TCP connections from /proc/net/tcp and /proc/net/tcp6
                    parseNetworkConnections()
                    
                    // Check every 2 seconds
                    delay(2000)
                } catch (e: Exception) {
                    Log.e(TAG, "Error reading connections: ${e.message}")
                    delay(5000) // Wait longer on error
                }
            }
        } catch (e: Exception) {
            if (isRunning) {
                Log.e(TAG, "Error monitoring connections: ${e.message}")
            }
        }
    }
    
    private suspend fun parseNetworkConnections() {
        try {
            // Read TCP connections
            val tcp4File = java.io.File("/proc/net/tcp")
            val tcp6File = java.io.File("/proc/net/tcp6")
            
            if (tcp4File.exists()) {
                parseProcNetFile(tcp4File, isIpv6 = false)
            }
            
            if (tcp6File.exists()) {
                parseProcNetFile(tcp6File, isIpv6 = true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing network connections: ${e.message}")
        }
    }
    
    private suspend fun parseProcNetFile(file: java.io.File, isIpv6: Boolean) = withContext(Dispatchers.IO) {
        file.readLines().drop(1).forEach { line ->
            try {
                val parts = line.trim().split("\\s+".toRegex())
                if (parts.size < 10) return@forEach
                
                // Parse local and remote addresses
                val localAddr = parts[1].split(":")
                val remoteAddr = parts[2].split(":")
                
                if (remoteAddr[0] == "00000000" || remoteAddr[0] == "00000000000000000000000000000000") {
                    return@forEach // Skip unconnected sockets
                }
                
                val localIp = hexToIp(localAddr[0], isIpv6)
                val localPort = hexToPort(localAddr[1])
                val remoteIp = hexToIp(remoteAddr[0], isIpv6)
                val remotePort = hexToPort(remoteAddr[1])
                val uid = parts[7].toIntOrNull() ?: 0
                
                // Create network packet for analysis
                val packet = com.example.inteltrace_v3.domain.models.NetworkPacket(
                    timestamp = System.currentTimeMillis(),
                    sourceIp = localIp,
                    destIp = remoteIp,
                    sourcePort = localPort,
                    destPort = remotePort,
                    protocol = 6, // TCP
                    packetSize = 0
                )
                
                // Analyze in background
                serviceScope.launch {
                    try {
                        analyzePacket(packet)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error analyzing connection: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                // Skip malformed lines
            }
        }
    }
    
    private fun hexToIp(hex: String, isIpv6: Boolean): String {
        return try {
            if (isIpv6) {
                // IPv6 - 32 hex chars
                val bytes = hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
                java.net.InetAddress.getByAddress(bytes).hostAddress ?: hex
            } else {
                // IPv4 - 8 hex chars in reverse byte order
                val addr = hex.toLong(16)
                "${addr and 0xFF}.${(addr shr 8) and 0xFF}.${(addr shr 16) and 0xFF}.${(addr shr 24) and 0xFF}"
            }
        } catch (e: Exception) {
            hex
        }
    }
    
    private fun hexToPort(hex: String): Int {
        return try {
            hex.toInt(16)
        } catch (e: Exception) {
            0
        }
    }
    
    private suspend fun analyzePacket(packet: com.example.inteltrace_v3.domain.models.NetworkPacket) {
        val connectionKey = "${packet.destIp}:${packet.destPort}:${packet.protocol}"
        
        // Get or create connection info
        val connInfo = synchronized(connectionCache) {
            connectionCache.getOrPut(connectionKey) {
                ConnectionInfo(
                    destIp = packet.destIp,
                    destPort = packet.destPort,
                    protocol = packet.protocol,
                    firstSeen = packet.timestamp,
                    packageName = getPackageNameForConnection(packet),
                    appName = ""
                )
            }
        }
        
        // Update connection stats
        connInfo.packetCount++
        connInfo.lastSeen = packet.timestamp
        connInfo.totalBytes += packet.packetSize
        
        // Analyze threat (rate limited)
        if (connInfo.shouldAnalyze()) {
            val analysis = detectionEngine.analyzeConnection(packet, connInfo.packageName)
            connInfo.lastAnalyzed = packet.timestamp
            connInfo.threatScore = analysis.threatScore
            
            // Save to database
            if (analysis.threatScore > 0) {
                saveConnection(packet, connInfo, analysis.threatScore)
            }
        }
        
        // Cleanup old connections
        if (connectionCache.size > MAX_CACHE_SIZE) {
            cleanupCache()
        }
    }
    
    private suspend fun saveConnection(
        packet: com.example.inteltrace_v3.domain.models.NetworkPacket,
        connInfo: ConnectionInfo,
        threatScore: Int
    ) {
        val appName = if (connInfo.appName.isEmpty()) {
            getAppName(connInfo.packageName)
        } else {
            connInfo.appName
        }
        
        val connection = ConnectionEntity(
            timestamp = packet.timestamp,
            sourceIp = packet.sourceIp,
            destIp = packet.destIp,
            sourcePort = packet.sourcePort,
            destPort = packet.destPort,
            protocol = packet.protocol,
            packageName = connInfo.packageName,
            appName = appName,
            bytesSent = connInfo.totalBytes,
            threatScore = threatScore
        )
        
        connectionRepository.insertConnection(connection)
    }
    
    private fun getPackageNameForConnection(packet: com.example.inteltrace_v3.domain.models.NetworkPacket): String {
        // Note: Getting the UID/package for a connection is complex and requires
        // parsing /proc/net/tcp or /proc/net/udp files
        // For simplicity, we'll return "unknown" here
        // In production, you'd implement proper UID to package name mapping
        return "unknown"
    }
    
    private fun getAppName(packageName: String): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
    }
    
    private fun cleanupCache() {
        val now = System.currentTimeMillis()
        synchronized(connectionCache) {
            connectionCache.entries.removeIf { (_, info) ->
                now - info.lastSeen > CACHE_TIMEOUT_MS
            }
        }
    }
    
    private fun stopVPN() {
        isRunning = false
        prefs.isVpnEnabled = false
        
        try {
            vpnInterface?.close()
            vpnInterface = null
        } catch (e: Exception) {
            Log.e(TAG, "Error closing VPN: ${e.message}")
        }
        
        serviceScope.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        
        Log.i(TAG, "VPN stopped")
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "VPN Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "IntelTrace VPN monitoring service"
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(title: String, message: String): Notification {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
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
    
    private data class ConnectionInfo(
        val destIp: String,
        val destPort: Int,
        val protocol: Int,
        val firstSeen: Long,
        val packageName: String,
        var appName: String,
        var packetCount: Int = 0,
        var totalBytes: Long = 0,
        var lastSeen: Long = firstSeen,
        var lastAnalyzed: Long = 0,
        var threatScore: Int = 0
    ) {
        fun shouldAnalyze(): Boolean {
            // Analyze first packet, then every 50 packets or every 30 seconds
            return lastAnalyzed == 0L ||
                   packetCount % 50 == 0 ||
                   System.currentTimeMillis() - lastAnalyzed > 30000
        }
    }
    
    companion object {
        private const val TAG = "IntelTraceVpnService"
        const val ACTION_START_VPN = "com.example.inteltrace_v3.START_VPN"
        const val ACTION_STOP_VPN = "com.example.inteltrace_v3.STOP_VPN"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "vpn_service_channel"
        private const val MAX_CACHE_SIZE = 1000
        private const val CACHE_TIMEOUT_MS = 5 * 60 * 1000L // 5 minutes
    }
}
