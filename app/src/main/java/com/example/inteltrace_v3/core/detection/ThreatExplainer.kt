package com.example.inteltrace_v3.core.detection

import com.example.inteltrace_v3.core.analysis.NetworkFlow
import com.example.inteltrace_v3.domain.models.ThreatLevel

object ThreatExplainer {
    
    data class ThreatExplanation(
        val threatLevel: ThreatLevel,
        val overallScore: Int,
        val primaryReason: String,
        val detailedFactors: List<Factor>,
        val recommendations: List<String>,
        val technicalDetails: Map<String, Any>
    )
    
    data class Factor(
        val name: String,
        val score: Int,
        val weight: Double,
        val description: String,
        val severity: String
    )
    
    fun explainThreat(
        threatScore: Int,
        threatLevel: ThreatLevel,
        osintScore: Int = 0,
        portScore: Int = 0,
        dgaAnalysis: DGADetector.DGAAnalysis? = null,
        heuristicResults: Map<String, AdvancedHeuristics.HeuristicResult> = emptyMap(),
        flow: NetworkFlow? = null,
        iocMatched: Boolean = false,
        iocDetails: String = ""
    ): ThreatExplanation {
        
        val factors = mutableListOf<Factor>()
        val recommendations = mutableListOf<String>()
        val technicalDetails = mutableMapOf<String, Any>()
        
        if (iocMatched) {
            factors.add(
                Factor(
                    name = "Known Malicious IOC",
                    score = 100,
                    weight = 1.0,
                    description = iocDetails,
                    severity = "CRITICAL"
                )
            )
            recommendations.add("🚨 IMMEDIATE ACTION: Block this connection immediately")
            recommendations.add("📋 Document this incident for security review")
            recommendations.add("🔍 Investigate the app that made this connection")
        }
        
        if (osintScore > 0) {
            val severity = when {
                osintScore >= 80 -> "CRITICAL"
                osintScore >= 60 -> "HIGH"
                osintScore >= 40 -> "MEDIUM"
                else -> "LOW"
            }
            
            factors.add(
                Factor(
                    name = "OSINT Threat Intelligence",
                    score = osintScore,
                    weight = 0.5,
                    description = "Multiple threat intelligence sources flagged this IP/domain with a score of $osintScore",
                    severity = severity
                )
            )
            
            technicalDetails["osint_score"] = osintScore
            
            if (osintScore > 50) {
                recommendations.add("⚠️ Review threat intelligence reports for this IP")
            }
        }
        
        if (portScore > 0) {
            factors.add(
                Factor(
                    name = "Suspicious Port Usage",
                    score = portScore,
                    weight = 0.2,
                    description = "Connection uses a port commonly associated with malware or unauthorized access",
                    severity = if (portScore > 20) "HIGH" else "MEDIUM"
                )
            )
            
            technicalDetails["port_score"] = portScore
            recommendations.add("🔌 Review why this app is using unusual ports")
        }
        
        dgaAnalysis?.let { dga ->
            if (dga.isDGA) {
                factors.add(
                    Factor(
                        name = "Domain Generation Algorithm (DGA)",
                        score = (dga.confidence * 100).toInt(),
                        weight = 0.3,
                        description = "Domain shows characteristics of being algorithmically generated (${dga.reason})",
                        severity = "HIGH"
                    )
                )
                
                technicalDetails["dga_entropy"] = dga.entropy
                technicalDetails["dga_confidence"] = dga.confidence
                technicalDetails["dga_characteristics"] = dga.domainCharacteristics
                
                recommendations.add("🧬 Possible DGA-based malware communication")
                recommendations.add("📊 Domain entropy: ${"%.2f".format(dga.entropy)} (normal < 3.5)")
            }
        }
        
        heuristicResults.forEach { (type, result) ->
            if (result.detected) {
                val severity = when {
                    result.confidence >= 0.8 -> "CRITICAL"
                    result.confidence >= 0.6 -> "HIGH"
                    result.confidence >= 0.4 -> "MEDIUM"
                    else -> "LOW"
                }
                
                factors.add(
                    Factor(
                        name = result.threatType,
                        score = (result.confidence * 100).toInt(),
                        weight = 0.25,
                        description = result.reason,
                        severity = severity
                    )
                )
                
                technicalDetails["${type}_indicators"] = result.indicators
                
                when (result.threatType) {
                    "Beaconing" -> {
                        recommendations.add("⏰ Regular callback pattern detected - possible C2 communication")
                        recommendations.add("🕵️ Monitor this app for data exfiltration")
                    }
                    "DNS Tunneling" -> {
                        recommendations.add("🚇 Possible data exfiltration via DNS")
                        recommendations.add("📡 Check DNS query patterns and sizes")
                    }
                    "Port Scan" -> {
                        recommendations.add("🔍 Network scanning activity detected")
                        recommendations.add("🛡️ This may indicate reconnaissance for an attack")
                    }
                    "Data Exfiltration" -> {
                        recommendations.add("📤 Large data upload detected")
                        recommendations.add("🔒 Verify if this data transfer is authorized")
                    }
                    "Cryptocurrency Mining" -> {
                        recommendations.add("⛏️ Possible unauthorized cryptocurrency mining")
                        recommendations.add("🔋 This may cause battery drain and performance issues")
                    }
                    "Fast Flux" -> {
                        recommendations.add("🌐 Domain resolves to rapidly changing IPs")
                        recommendations.add("🚩 Common technique used by botnets and malware")
                    }
                }
            }
        }
        
        flow?.let {
            val flowStats = mapOf(
                "duration_seconds" to (it.getDuration() / 1000.0),
                "packet_count" to it.packetCount,
                "bytes_sent" to it.bytesSent,
                "bytes_received" to it.bytesReceived,
                "packets_per_second" to it.getPacketsPerSecond(),
                "bytes_per_second" to it.getBytesPerSecond()
            )
            
            technicalDetails["flow_statistics"] = flowStats
        }
        
        val primaryReason = when {
            iocMatched -> "Known malicious infrastructure: $iocDetails"
            factors.isNotEmpty() -> factors.maxByOrNull { it.score * it.weight }?.description ?: "Multiple threat indicators detected"
            else -> "Threat score based on heuristic analysis"
        }
        
        if (recommendations.isEmpty()) {
            when (threatLevel) {
                ThreatLevel.CRITICAL, ThreatLevel.HIGH -> {
                    recommendations.add("⚠️ Block or monitor this connection closely")
                    recommendations.add("📝 Review the app's permissions and behavior")
                }
                ThreatLevel.MEDIUM -> {
                    recommendations.add("👀 Monitor this connection for suspicious patterns")
                }
                ThreatLevel.LOW -> {
                    recommendations.add("✅ Connection appears relatively safe but continue monitoring")
                }
                else -> {
                    recommendations.add("✅ No immediate action required")
                }
            }
        }
        
        if (factors.isEmpty()) {
            factors.add(
                Factor(
                    name = "Baseline Analysis",
                    score = threatScore,
                    weight = 1.0,
                    description = "General threat assessment based on connection metadata",
                    severity = threatLevel.name
                )
            )
        }
        
        return ThreatExplanation(
            threatLevel = threatLevel,
            overallScore = threatScore,
            primaryReason = primaryReason,
            detailedFactors = factors.sortedByDescending { it.score * it.weight },
            recommendations = recommendations,
            technicalDetails = technicalDetails
        )
    }
    
    fun generateUserFriendlyMessage(explanation: ThreatExplanation): String {
        return buildString {
            appendLine("🎯 Threat Level: ${explanation.threatLevel.name} (Score: ${explanation.overallScore}/100)")
            appendLine()
            appendLine("📊 Why this was flagged:")
            appendLine(explanation.primaryReason)
            appendLine()
            
            if (explanation.detailedFactors.isNotEmpty()) {
                appendLine("🔍 Contributing Factors:")
                explanation.detailedFactors.take(3).forEach { factor ->
                    appendLine("  • ${factor.name}: ${factor.description}")
                }
                appendLine()
            }
            
            if (explanation.recommendations.isNotEmpty()) {
                appendLine("💡 Recommendations:")
                explanation.recommendations.take(3).forEach { rec ->
                    appendLine("  $rec")
                }
            }
        }
    }
}
