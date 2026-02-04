package com.example.inteltrace_v3.core.detection

import android.util.Log
import com.example.inteltrace_v3.data.local.preferences.SecurityPreferences
import com.example.inteltrace_v3.data.repository.AlertRepository
import com.example.inteltrace_v3.data.repository.ThreatRepository
import com.example.inteltrace_v3.domain.models.NetworkPacket
import com.example.inteltrace_v3.domain.models.ThreatLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThreatDetectionEngine @Inject constructor(
    private val threatRepository: ThreatRepository,
    private val alertRepository: AlertRepository,
    private val iocMatcher: IOCMatcher,
    private val prefs: SecurityPreferences
) {
    
    suspend fun analyzeConnection(
        packet: NetworkPacket,
        packageName: String
    ): ThreatAnalysis = withContext(Dispatchers.IO) {
        
        try {
            // Check against known malicious IOCs
            val iocMatch = iocMatcher.checkIP(packet.destIp)
            if (iocMatch.isMatched) {
                val analysis = ThreatAnalysis(
                    threatLevel = ThreatLevel.CRITICAL,
                    threatScore = 100,
                    reason = "Known ${iocMatch.category}: ${iocMatch.description}",
                    shouldBlock = prefs.isAutoBlockEnabled
                )
                
                // Create alert
                createAlert(analysis, packet, packageName)
                
                return@withContext analysis
            }
            
            // Check port-based threats
            val portThreat = checkSuspiciousPort(packet.destPort, packet.protocol)
            if (portThreat != null) {
                Log.d(TAG, "Suspicious port detected: ${packet.destPort}")
            }
            
            // Query OSINT for IP reputation
            val threatResult = threatRepository.checkIPReputation(packet.destIp)
            
            // Calculate final threat score
            val finalScore = calculateFinalScore(
                threatResult.threatScore,
                portThreat?.score ?: 0,
                packet
            )
            
            val threatLevel = getThreatLevel(finalScore)
            
            val analysis = ThreatAnalysis(
                threatLevel = threatLevel,
                threatScore = finalScore,
                reason = buildReasonString(threatResult.message, portThreat?.reason),
                shouldBlock = shouldBlock(finalScore),
                country = threatResult.country,
                isp = threatResult.isp
            )
            
            // Create alert if threat score exceeds threshold
            if (finalScore >= prefs.threatThreshold) {
                createAlert(analysis, packet, packageName)
            }
            
            analysis
            
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing connection: ${e.message}")
            ThreatAnalysis(
                threatLevel = ThreatLevel.UNKNOWN,
                threatScore = 0,
                reason = "Analysis failed",
                shouldBlock = false
            )
        }
    }
    
    private fun calculateFinalScore(
        osintScore: Int,
        portScore: Int,
        packet: NetworkPacket
    ): Int {
        var score = osintScore
        
        // Add port threat score
        score += portScore
        
        // Penalty for common malware ports
        if (isMalwarePort(packet.destPort)) {
            score += 15
        }
        
        // Penalty for unusual protocols
        if (packet.protocol !in listOf(6, 17)) { // Not TCP or UDP
            score += 5
        }
        
        return score.coerceIn(0, 100)
    }
    
    private fun checkSuspiciousPort(port: Int, protocol: Int): PortThreat? {
        return when {
            // Remote access trojans
            port in listOf(1337, 31337, 12345, 27374) -> 
                PortThreat(30, "Common RAT port")
            
            // Tor/Proxy
            port in listOf(9050, 9051, 1080) -> 
                PortThreat(15, "Proxy/Tor port")
            
            // IRC (often used by botnets)
            port in 6660..6669 -> 
                PortThreat(20, "IRC port (potential botnet)")
            
            // Cryptocurrency miners
            port in listOf(3333, 4444, 5555, 14433) && protocol == 6 -> 
                PortThreat(25, "Potential crypto miner")
            
            // Unusual high ports for common protocols
            port > 49152 && protocol == 6 -> 
                PortThreat(5, "Dynamic/private port")
            
            else -> null
        }
    }
    
    private fun isMalwarePort(port: Int): Boolean {
        return port in listOf(
            1337, 31337, 12345, 27374, // RATs
            4444, 5555, 6666, 7777, 8888, // Generic malware
            6660, 6661, 6662, 6663, 6664, 6665, 6666, 6667, 6668, 6669 // IRC
        )
    }
    
    private fun getThreatLevel(score: Int): ThreatLevel {
        return when {
            score >= 80 -> ThreatLevel.CRITICAL
            score >= 60 -> ThreatLevel.HIGH
            score >= 40 -> ThreatLevel.MEDIUM
            score >= 20 -> ThreatLevel.LOW
            else -> ThreatLevel.SAFE
        }
    }
    
    private fun shouldBlock(score: Int): Boolean {
        return prefs.isAutoBlockEnabled && score >= 70
    }
    
    private fun buildReasonString(osintReason: String, portReason: String?): String {
        return if (portReason != null) {
            "$osintReason; $portReason"
        } else {
            osintReason
        }
    }
    
    private suspend fun createAlert(
        analysis: ThreatAnalysis,
        packet: NetworkPacket,
        packageName: String
    ) {
        if (!prefs.notificationsEnabled) return
        
        if (prefs.criticalAlertsOnly && analysis.threatLevel != ThreatLevel.CRITICAL) {
            return
        }
        
        val title = when (analysis.threatLevel) {
            ThreatLevel.CRITICAL -> "🚨 Critical Threat Detected"
            ThreatLevel.HIGH -> "⚠️ High Risk Connection"
            ThreatLevel.MEDIUM -> "⚡ Suspicious Activity"
            ThreatLevel.LOW -> "ℹ️ Low Risk Detected"
            else -> "Unknown Threat"
        }
        
        val message = "${packet.destIp}:${packet.destPort} - ${analysis.reason}"
        
        alertRepository.createAlert(
            threatLevel = analysis.threatLevel,
            title = title,
            message = message,
            ipAddress = packet.destIp,
            packageName = packageName,
            appName = packageName,
            threatScore = analysis.threatScore
        )
    }
    
    data class ThreatAnalysis(
        val threatLevel: ThreatLevel,
        val threatScore: Int,
        val reason: String,
        val shouldBlock: Boolean,
        val country: String? = null,
        val isp: String? = null
    )
    
    private data class PortThreat(
        val score: Int,
        val reason: String
    )
    
    companion object {
        private const val TAG = "ThreatDetectionEngine"
    }
}
