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
    private val prefs: SecurityPreferences,
    private val flowAnalyzer: com.example.inteltrace_v3.core.analysis.FlowAnalyzer,
    private val blocklistManager: com.example.inteltrace_v3.data.local.preferences.BlocklistManager
) {
    
    private val domainIpCache = mutableMapOf<String, MutableList<String>>()
    private val MAX_CACHE_SIZE = 1000
    
    suspend fun analyzeConnection(
        packet: NetworkPacket,
        packageName: String,
        domain: String? = null
    ): ThreatAnalysis = withContext(Dispatchers.IO) {
        
        try {
            // Check blocklist first (allowlist has priority)
            if (checkAllowlist(packet.destIp, domain, packageName)) {
                return@withContext ThreatAnalysis(
                    threatLevel = ThreatLevel.SAFE,
                    threatScore = 0,
                    reason = "Allowlisted",
                    shouldBlock = false,
                    detectionMethod = "Allowlist"
                )
            }
            
            if (checkBlocklist(packet.destIp, domain, packageName)) {
                return@withContext ThreatAnalysis(
                    threatLevel = ThreatLevel.CRITICAL,
                    threatScore = 100,
                    reason = "Blocklisted",
                    shouldBlock = true,
                    detectionMethod = "Blocklist"
                )
            }
            
            // Add packet to flow analyzer
            val flow = flowAnalyzer.processPacket(packet)
            
            // Check against known malicious IOCs
            val iocMatch = iocMatcher.checkIP(packet.destIp)
            if (iocMatch.isMatched) {
                val analysis = ThreatAnalysis(
                    threatLevel = ThreatLevel.CRITICAL,
                    threatScore = 100,
                    reason = "Known ${iocMatch.category}: ${iocMatch.description}",
                    shouldBlock = prefs.isAutoBlockEnabled,
                    detectionMethod = "IOC Match"
                )
                
                // Create alert
                createAlert(analysis, packet, packageName)
                
                return@withContext analysis
            }
            
            // DGA Detection if domain is available
            var dgaScore = 0
            var dgaAnalysis: DGADetector.DGAAnalysis? = null
            if (domain != null) {
                dgaAnalysis = DGADetector.analyzeDomain(domain)
                if (dgaAnalysis.isDGA) {
                    dgaScore = (dgaAnalysis.confidence * 50).toInt()
                    Log.d(TAG, "DGA detected: $domain (confidence: ${dgaAnalysis.confidence})")
                    
                    // Track domain-IP mapping for Fast Flux detection
                    trackDomainIP(domain, packet.destIp)
                }
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
                osintScore = threatResult.threatScore,
                portScore = portThreat?.score ?: 0,
                dgaScore = dgaScore,
                packet = packet
            )
            
            val threatLevel = getThreatLevel(finalScore)
            
            val analysis = ThreatAnalysis(
                threatLevel = threatLevel,
                threatScore = finalScore,
                reason = buildReasonString(threatResult.message, portThreat?.reason, dgaAnalysis),
                shouldBlock = shouldBlock(finalScore),
                country = threatResult.country,
                isp = threatResult.isp,
                detectionMethod = buildDetectionMethod(threatResult.threatScore, portThreat, dgaAnalysis)
            )
            
            // Create alert if threat score exceeds threshold
            if (finalScore >= prefs.threatThreshold) {
                createAlert(analysis, packet, packageName, domain)
            }
            
            analysis
            
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing connection: ${e.message}")
            ThreatAnalysis(
                threatLevel = ThreatLevel.UNKNOWN,
                threatScore = 0,
                reason = "Analysis failed",
                shouldBlock = false,
                detectionMethod = "Error"
            )
        }
    }
    
    suspend fun analyzeFlow(
        flow: com.example.inteltrace_v3.core.analysis.NetworkFlow,
        packageName: String,
        domain: String? = null
    ): FlowThreatAnalysis = withContext(Dispatchers.IO) {
        try {
            val heuristicResults = mutableMapOf<String, AdvancedHeuristics.HeuristicResult>()
            var totalHeuristicScore = 0
            
            // Run advanced heuristics
            val beaconingResult = AdvancedHeuristics.detectBeaconing(flow)
            heuristicResults["beaconing"] = beaconingResult
            if (beaconingResult.detected) {
                totalHeuristicScore += (beaconingResult.confidence * 30).toInt()
                Log.d(TAG, "Beaconing detected: ${beaconingResult.reason}")
            }
            
            // DNS Tunneling detection
            val tunneling = AdvancedHeuristics.detectDNSTunneling(flow)
            heuristicResults["dns_tunneling"] = tunneling
            if (tunneling.detected) {
                totalHeuristicScore += (tunneling.confidence * 40).toInt()
                Log.d(TAG, "DNS Tunneling detected: ${tunneling.reason}")
            }
            
            // Fast Flux detection
            if (domain != null) {
                val previousIps = domainIpCache[domain] ?: emptyList()
                val fastFlux = AdvancedHeuristics.detectFastFlux(flow.destIp, previousIps, domain)
                heuristicResults["fast_flux"] = fastFlux
                if (fastFlux.detected) {
                    totalHeuristicScore += (fastFlux.confidence * 25).toInt()
                    Log.d(TAG, "Fast Flux detected: ${fastFlux.reason}")
                }
            }
            
            // Port Scanning detection
            val portScanning = AdvancedHeuristics.detectPortScan(
                flow.destIp,
                listOf(flow.destPort),
                flow.getDuration()
            )
            heuristicResults["port_scanning"] = portScanning
            if (portScanning.detected) {
                totalHeuristicScore += (portScanning.confidence * 20).toInt()
            }
            
            // Data Exfiltration detection
            val exfiltration = AdvancedHeuristics.detectDataExfiltration(flow)
            heuristicResults["data_exfiltration"] = exfiltration
            if (exfiltration.detected) {
                totalHeuristicScore += (exfiltration.confidence * 35).toInt()
                Log.d(TAG, "Data Exfiltration detected: ${exfiltration.reason}")
            }
            
            // Crypto Mining detection
            val cryptoMining = AdvancedHeuristics.detectCryptoMining(flow)
            heuristicResults["crypto_mining"] = cryptoMining
            if (cryptoMining.detected) {
                totalHeuristicScore += (cryptoMining.confidence * 30).toInt()
                Log.d(TAG, "Crypto Mining detected: ${cryptoMining.reason}")
            }
            
            // Get OSINT threat score
            val threatResult = threatRepository.checkIPReputation(flow.destIp)
            
            // Calculate combined score
            val combinedScore = ((threatResult.threatScore * 0.6) + (totalHeuristicScore * 0.4)).toInt()
                .coerceIn(0, 100)
            
            val threatLevel = getThreatLevel(combinedScore)
            
            // Build explanation
            val explanation = ThreatExplainer.explainThreat(
                threatScore = combinedScore,
                threatLevel = threatLevel,
                osintScore = threatResult.threatScore,
                portScore = 0,
                dgaAnalysis = if (domain != null) DGADetector.analyzeDomain(domain) else null,
                heuristicResults = heuristicResults,
                flow = flow,
                iocMatched = false
            )
            
            FlowThreatAnalysis(
                threatLevel = threatLevel,
                threatScore = combinedScore,
                heuristicResults = heuristicResults,
                explanation = explanation,
                shouldBlock = shouldBlock(combinedScore)
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing flow: ${e.message}")
            FlowThreatAnalysis(
                threatLevel = ThreatLevel.UNKNOWN,
                threatScore = 0,
                heuristicResults = emptyMap(),
                explanation = null,
                shouldBlock = false
            )
        }
    }
    
    private fun trackDomainIP(domain: String, ip: String) {
        if (domainIpCache.size >= MAX_CACHE_SIZE) {
            domainIpCache.remove(domainIpCache.keys.first())
        }
        domainIpCache.getOrPut(domain) { mutableListOf() }.add(ip)
    }
    
    private fun buildDetectionMethod(
        osintScore: Int,
        portThreat: PortThreat?,
        dgaAnalysis: DGADetector.DGAAnalysis?
    ): String {
        val methods = mutableListOf<String>()
        if (osintScore > 0) methods.add("OSINT")
        if (portThreat != null) methods.add("Port Analysis")
        if (dgaAnalysis?.isDGA == true) methods.add("DGA Detection")
        return methods.joinToString(", ").ifEmpty { "None" }
    }
    
    private fun calculateFinalScore(
        osintScore: Int,
        portScore: Int,
        dgaScore: Int = 0,
        packet: NetworkPacket
    ): Int {
        var score = osintScore
        
        // Add port threat score
        score += portScore
        
        // Add DGA score
        score += dgaScore
        
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
    
    private fun buildReasonString(
        osintReason: String,
        portReason: String?,
        dgaAnalysis: DGADetector.DGAAnalysis?
    ): String {
        val reasons = mutableListOf(osintReason)
        
        if (portReason != null) {
            reasons.add(portReason)
        }
        
        if (dgaAnalysis?.isDGA == true) {
            reasons.add("DGA detected (${dgaAnalysis.reason})")
        }
        
        return reasons.joinToString("; ")
    }
    
    private suspend fun createAlert(
        analysis: ThreatAnalysis,
        packet: NetworkPacket,
        packageName: String,
        domain: String? = null
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
        
        val destination = domain ?: "${packet.destIp}:${packet.destPort}"
        val message = "$destination - ${analysis.reason}"
        
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
        val isp: String? = null,
        val detectionMethod: String = "Unknown"
    )
    
    data class FlowThreatAnalysis(
        val threatLevel: ThreatLevel,
        val threatScore: Int,
        val heuristicResults: Map<String, AdvancedHeuristics.HeuristicResult>,
        val explanation: ThreatExplainer.ThreatExplanation?,
        val shouldBlock: Boolean
    )
    
    private data class PortThreat(
        val score: Int,
        val reason: String
    )
    
    private fun checkAllowlist(
        ip: String, 
        domain: String?, 
        pkgName: String
    ): Boolean {
        if (blocklistManager.isAllowed(ip, com.example.inteltrace_v3.data.local.preferences.BlocklistManager.EntryType.IP_ADDRESS)) return true
        if (domain != null && blocklistManager.isAllowed(domain, com.example.inteltrace_v3.data.local.preferences.BlocklistManager.EntryType.DOMAIN)) return true
        if (blocklistManager.isAllowed(pkgName, com.example.inteltrace_v3.data.local.preferences.BlocklistManager.EntryType.PACKAGE_NAME)) return true
        return false
    }
    
    private fun checkBlocklist(
        ip: String, 
        domain: String?, 
        pkgName: String
    ): Boolean {
        if (blocklistManager.isBlocked(ip, com.example.inteltrace_v3.data.local.preferences.BlocklistManager.EntryType.IP_ADDRESS)) return true
        if (domain != null && blocklistManager.isBlocked(domain, com.example.inteltrace_v3.data.local.preferences.BlocklistManager.EntryType.DOMAIN)) return true
        if (blocklistManager.isBlocked(pkgName, com.example.inteltrace_v3.data.local.preferences.BlocklistManager.EntryType.PACKAGE_NAME)) return true
        return false
    }
    
    companion object {
        private const val TAG = "ThreatDetectionEngine"
    }
}
