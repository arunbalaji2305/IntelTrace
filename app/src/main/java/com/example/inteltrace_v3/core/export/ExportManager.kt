package com.example.inteltrace_v3.core.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.inteltrace_v3.data.local.database.entities.ConnectionEntity
import com.example.inteltrace_v3.data.local.database.entities.AlertEntity
import com.google.gson.GsonBuilder
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ExportManager(private val context: Context) {
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    
    data class ExportData(
        val connections: List<ConnectionEntity> = emptyList(),
        val alerts: List<AlertEntity> = emptyList(),

        val exportTime: Long = System.currentTimeMillis(),
        val appVersion: String = "1.0.0"
    )
    
    suspend fun exportToCSV(data: ExportData): File {
        val csvContent = buildString {
            appendLine("Timestamp,Source IP,Source Port,Destination IP,Destination Port,Protocol,App Name,Package Name,Bytes Sent,Threat Score,Type")
            
            data.connections.forEach { conn ->
                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                    .format(Date(conn.timestamp))
                
                appendLine("$timestamp,${conn.sourceIp},${conn.sourcePort},${conn.destIp},${conn.destPort},${conn.protocol},\"${conn.appName}\",${conn.packageName},${conn.bytesSent},${conn.threatScore},Connection")
            }
            
            data.alerts.forEach { alert ->
                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                    .format(Date(alert.timestamp))
                
                appendLine("$timestamp,,,${alert.ipAddress},,,,\"${alert.appName}\",${alert.packageName},,${alert.threatScore},Alert - ${alert.type}")
            }
        }
        
        return writeToFile(csvContent, "inteltrace_export_${System.currentTimeMillis()}.csv")
    }
    
    suspend fun exportToJSON(data: ExportData): File {
        val gson = GsonBuilder().setPrettyPrinting().create()
        
        val jsonData = mapOf(
            "export_metadata" to mapOf(
                "export_time" to dateFormat.format(Date(data.exportTime)),
                "app_version" to data.appVersion,
                "connections_count" to data.connections.size,
                "alerts_count" to data.alerts.size
            ),
            "connections" to data.connections.map { conn ->
                mapOf(
                    "timestamp" to dateFormat.format(Date(conn.timestamp)),
                    "source_ip" to conn.sourceIp,
                    "source_port" to conn.sourcePort,
                    "destination_ip" to conn.destIp,
                    "destination_port" to conn.destPort,
                    "protocol" to conn.protocol,
                    "app_name" to conn.appName,
                    "package_name" to conn.packageName,
                    "bytes_sent" to conn.bytesSent,
                    "threat_score" to conn.threatScore
                )
            },
            "alerts" to data.alerts.map { alert ->
                mapOf(
                    "timestamp" to dateFormat.format(Date(alert.timestamp)),
                    "threat_level" to alert.type,
                    "title" to alert.title,
                    "message" to alert.message,
                    "ip_address" to alert.ipAddress,
                    "app_name" to alert.appName,
                    "package_name" to alert.packageName,
                    "threat_score" to alert.threatScore,
                    "is_read" to alert.isRead
                )
            }
        )
        
        val jsonContent = gson.toJson(jsonData)
        return writeToFile(jsonContent, "inteltrace_export_${System.currentTimeMillis()}.json")
    }
    
    suspend fun exportToCEF(data: ExportData): File {
        val cefContent = buildString {
            data.connections.forEach { conn ->
                val cefVersion = "CEF:0"
                val deviceVendor = "IntelTrace"
                val deviceProduct = "IntelTrace"
                val deviceVersion = "1.0"
                val signatureId = "NET-001"
                val name = "Network Connection"
                val severity = when {
                    conn.threatScore >= 80 -> "10"
                    conn.threatScore >= 60 -> "7"
                    conn.threatScore >= 40 -> "5"
                    conn.threatScore >= 20 -> "3"
                    else -> "1"
                }
                
                val extensions = buildString {
                    append("src=${conn.sourceIp} ")
                    append("spt=${conn.sourcePort} ")
                    append("dst=${conn.destIp} ")
                    append("dpt=${conn.destPort} ")
                    append("proto=${getProtocolName(conn.protocol)} ")
                    append("app=${conn.appName} ")
                    append("out=${conn.bytesSent} ")
                    append("cs1Label=PackageName cs1=${conn.packageName} ")
                    append("cn1Label=ThreatScore cn1=${conn.threatScore}")
                }
                
                appendLine("$cefVersion|$deviceVendor|$deviceProduct|$deviceVersion|$signatureId|$name|$severity|$extensions")
            }
            
            data.alerts.forEach { alert ->
                val cefVersion = "CEF:0"
                val deviceVendor = "IntelTrace"
                val deviceProduct = "IntelTrace"
                val deviceVersion = "1.0"
                val signatureId = "ALERT-${alert.type}"
                val name = alert.title
                val severity = when (alert.type) {
                    "CRITICAL" -> "10"
                    "HIGH" -> "8"
                    "MEDIUM" -> "5"
                    "LOW" -> "3"
                    else -> "1"
                }
                
                val extensions = buildString {
                    append("dst=${alert.ipAddress} ")
                    append("msg=${alert.message.replace("|", "\\|")} ")
                    append("app=${alert.appName} ")
                    append("cs1Label=PackageName cs1=${alert.packageName} ")
                    append("cn1Label=ThreatScore cn1=${alert.threatScore}")
                }
                
                appendLine("$cefVersion|$deviceVendor|$deviceProduct|$deviceVersion|$signatureId|$name|$severity|$extensions")
            }
        }
        
        return writeToFile(cefContent, "inteltrace_export_${System.currentTimeMillis()}.cef")
    }
    
    suspend fun exportToSTIX(data: ExportData): File {
        val stixBundle = buildString {
            appendLine("{")
            appendLine("  \"type\": \"bundle\",")
            appendLine("  \"id\": \"bundle--${UUID.randomUUID()}\",")
            appendLine("  \"spec_version\": \"2.1\",")
            appendLine("  \"objects\": [")
            
            val objects = mutableListOf<String>()
            
            data.connections.forEachIndexed { index, conn ->
                if (conn.threatScore > 50) {
                    val indicatorId = "indicator--${UUID.randomUUID()}"
                    val observableId = "ipv4-addr--${UUID.randomUUID()}"
                    
                    val indicator = buildString {
                        appendLine("    {")
                        appendLine("      \"type\": \"indicator\",")
                        appendLine("      \"spec_version\": \"2.1\",")
                        appendLine("      \"id\": \"$indicatorId\",")
                        appendLine("      \"created\": \"${dateFormat.format(Date(conn.timestamp))}\",")
                        appendLine("      \"modified\": \"${dateFormat.format(Date(conn.timestamp))}\",")
                        appendLine("      \"name\": \"Suspicious connection to ${conn.destIp}\",")
                        appendLine("      \"pattern\": \"[ipv4-addr:value = '${conn.destIp}']\",")
                        appendLine("      \"pattern_type\": \"stix\",")
                        appendLine("      \"valid_from\": \"${dateFormat.format(Date(conn.timestamp))}\",")
                        appendLine("      \"indicator_types\": [\"malicious-activity\"],")
                        appendLine("      \"confidence\": ${conn.threatScore}")
                        append("    }")
                    }
                    
                    objects.add(indicator)
                }
            }
            
            data.alerts.forEachIndexed { index, alert ->
                val indicatorId = "indicator--${UUID.randomUUID()}"
                
                val indicator = buildString {
                    appendLine("    {")
                    appendLine("      \"type\": \"indicator\",")
                    appendLine("      \"spec_version\": \"2.1\",")
                    appendLine("      \"id\": \"$indicatorId\",")
                    appendLine("      \"created\": \"${dateFormat.format(Date(alert.timestamp))}\",")
                    appendLine("      \"modified\": \"${dateFormat.format(Date(alert.timestamp))}\",")
                    appendLine("      \"name\": \"${alert.title}\",")
                    appendLine("      \"description\": \"${alert.message}\",")
                    appendLine("      \"pattern\": \"[ipv4-addr:value = '${alert.ipAddress}']\",")
                    appendLine("      \"pattern_type\": \"stix\",")
                    appendLine("      \"valid_from\": \"${dateFormat.format(Date(alert.timestamp))}\",")
                    appendLine("      \"indicator_types\": [\"malicious-activity\"],")
                    appendLine("      \"confidence\": ${alert.threatScore}")
                    append("    }")
                }
                
                objects.add(indicator)
            }
            
            appendLine(objects.joinToString(",\n"))
            appendLine("  ]")
            append("}")
        }
        
        return writeToFile(stixBundle, "inteltrace_export_${System.currentTimeMillis()}.stix")
    }
    
    fun shareExportFile(file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = when (file.extension) {
                "csv" -> "text/csv"
                "json" -> "application/json"
                "cef" -> "text/plain"
                "stix" -> "application/json"
                else -> "text/plain"
            }
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(Intent.createChooser(intent, "Share Export File"))
    }
    
    private fun writeToFile(content: String, filename: String): File {
        val exportsDir = File(context.getExternalFilesDir(null), "exports").apply {
            if (!exists()) mkdirs()
        }
        
        val file = File(exportsDir, filename)
        file.writeText(content)
        
        return file
    }
    
    private fun getProtocolName(protocol: Int): String {
        return when (protocol) {
            1 -> "ICMP"
            6 -> "TCP"
            17 -> "UDP"
            else -> protocol.toString()
        }
    }
}
