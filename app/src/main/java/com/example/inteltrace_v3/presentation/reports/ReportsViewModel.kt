package com.example.inteltrace_v3.presentation.reports

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.inteltrace_v3.data.repository.ConnectionRepository
import com.example.inteltrace_v3.data.repository.AlertRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class ReportStatistics(
    val totalConnections: Int = 0,
    val suspiciousConnections: Int = 0,
    val maliciousIps: Int = 0,
    val totalAlerts: Int = 0,
    val totalBytesMonitored: Long = 0,
    val firstConnectionTime: Long = 0
)

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val connectionRepository: ConnectionRepository,
    private val alertRepository: AlertRepository,
    private val exportManager: com.example.inteltrace_v3.core.export.ExportManager
) : ViewModel() {
    
    val statistics: StateFlow<ReportStatistics> = combine(
        connectionRepository.getAllConnections(),
        alertRepository.getAllAlerts()
    ) { connections, alerts ->
        ReportStatistics(
            totalConnections = connections.size,
            suspiciousConnections = connections.count { it.threatScore > 30 },
            maliciousIps = connections.distinctBy { it.destIp }.count { it.threatScore > 70 },
            totalAlerts = alerts.size,
            totalBytesMonitored = connections.sumOf { it.bytesReceived + it.bytesSent },
            firstConnectionTime = connections.minOfOrNull { it.timestamp } ?: 0L
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ReportStatistics()
    )
    
    suspend fun exportConnections(context: Context): Pair<Boolean, String> {
        return try {
            val connections = connectionRepository.getAllConnections().first()
            val csv = buildString {
                appendLine("Timestamp,App,Destination IP,Port,Protocol,Threat Score,Bytes Sent,Bytes Received,Country")
                connections.forEach { conn ->
                    appendLine("${formatTimestamp(conn.timestamp)},${conn.appName},${conn.destIp},${conn.destPort},${conn.protocol},${conn.threatScore},${conn.bytesSent},${conn.bytesReceived},${conn.country}")
                }
            }
            
            val fileName = "IntelTrace_Connections_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.csv"
            val file = createAndShareFile(context, fileName, csv)
            shareFile(context, file, "IntelTrace Connection History", "text/csv")
            Pair(true, "Opening share menu...")
        } catch (e: Exception) {
            Pair(false, "Export failed: ${e.message}")
        }
    }
    
    suspend fun exportThreats(context: Context): Pair<Boolean, String> {
        return try {
            val connections = connectionRepository.getSuspiciousConnections(30).first()
            val csv = buildString {
                appendLine("Timestamp,App,IP Address,Port,Threat Score,Country")
                connections.forEach { conn ->
                    appendLine("${formatTimestamp(conn.timestamp)},${conn.appName},${conn.destIp},${conn.destPort},${conn.threatScore},${conn.country}")
                }
            }
            
            val fileName = "IntelTrace_Threats_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.csv"
            val file = createAndShareFile(context, fileName, csv)
            shareFile(context, file, "IntelTrace Threat Report", "text/csv")
            Pair(true, "Opening share menu...")
        } catch (e: Exception) {
            Pair(false, "Export failed: ${e.message}")
        }
    }
    
    suspend fun exportFullReport(context: Context): Pair<Boolean, String> {
        return try {
            val stats = statistics.value
            val connections = connectionRepository.getAllConnections().first()
            val alerts = alertRepository.getAllAlerts().first()
            
            val report = buildString {
                appendLine("=".repeat(60))
                appendLine("INTELTRACE SECURITY REPORT")
                appendLine("Generated: ${formatTimestamp(System.currentTimeMillis())}")
                appendLine("=".repeat(60))
                appendLine()
                
                appendLine("SUMMARY")
                appendLine("-".repeat(60))
                appendLine("Total Connections: ${stats.totalConnections}")
                appendLine("Suspicious Activity: ${stats.suspiciousConnections}")
                appendLine("Malicious IPs: ${stats.maliciousIps}")
                appendLine("Security Alerts: ${stats.totalAlerts}")
                appendLine("Data Monitored: ${formatBytes(stats.totalBytesMonitored)}")
                appendLine()
                
                appendLine("TOP THREATS")
                appendLine("-".repeat(60))
                connections.sortedByDescending { it.threatScore }
                    .take(10)
                    .forEach { conn ->
                        appendLine("${conn.destIp} - Score: ${conn.threatScore} - ${conn.appName}")
                    }
                appendLine()
                
                appendLine("RECENT ALERTS")
                appendLine("-".repeat(60))
                alerts.sortedByDescending { it.timestamp }
                    .take(10)
                    .forEach { alert ->
                        appendLine("[${alert.type}] ${alert.title} - ${formatTimestamp(alert.timestamp)}")
                    }
                appendLine()
                
                appendLine("=".repeat(60))
                appendLine("End of Report")
                appendLine("=".repeat(60))
            }
            
            val fileName = "IntelTrace_FullReport_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.txt"
            val file = createAndShareFile(context, fileName, report)
            shareFile(context, file, "IntelTrace Full Security Report", "text/plain")
            Pair(true, "Opening share menu...")
        } catch (e: Exception) {
            Pair(false, "Export failed: ${e.message}")
        }
    }
    
    suspend fun generateTextReport(): String {
        val stats = statistics.value
        return """
            IntelTrace Security Report
            
            Total Connections: ${stats.totalConnections}
            Suspicious Activity: ${stats.suspiciousConnections}
            Malicious IPs Detected: ${stats.maliciousIps}
            Total Alerts: ${stats.totalAlerts}
            Data Monitored: ${formatBytes(stats.totalBytesMonitored)}
            
            Generated by IntelTrace
        """.trimIndent()
    }
    
    fun clearOldData() {
        viewModelScope.launch {
            val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
            connectionRepository.deleteOldConnections(thirtyDaysAgo)
        }
    }
    
    private fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
    
    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.2f KB", bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> String.format("%.2f MB", bytes / (1024.0 * 1024))
            else -> String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024))
        }
    }
    
    private fun createAndShareFile(context: Context, fileName: String, content: String): File {
        // Create file in cache directory for sharing
        val file = File(context.cacheDir, fileName)
        file.writeText(content)
        return file
    }
    
    private fun shareFile(context: Context, file: File, title: String, mimeType: String) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, "IntelTrace Security Report - $title")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        val chooserIntent = Intent.createChooser(shareIntent, "Share via")
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooserIntent)
    }
}
