package com.example.inteltrace_v3.core.detection

import com.example.inteltrace_v3.core.analysis.NetworkFlow
import kotlin.math.abs
import kotlin.math.sqrt

object AdvancedHeuristics {
    
    data class HeuristicResult(
        val detected: Boolean,
        val confidence: Double,
        val threatType: String,
        val reason: String,
        val indicators: Map<String, Any>
    )
    
    fun detectBeaconing(flow: NetworkFlow): HeuristicResult {
        if (flow.packetTimestamps.size < 5) {
            return HeuristicResult(
                detected = false,
                confidence = 0.0,
                threatType = "Beaconing",
                reason = "Insufficient packets for beaconing analysis",
                indicators = emptyMap()
            )
        }
        
        val intervals = flow.packetTimestamps.zipWithNext { a, b -> b - a }
        val avgInterval = intervals.average()
        val variance = intervals.map { (it - avgInterval) * (it - avgInterval) }.average()
        val stdDev = sqrt(variance)
        val coefficientOfVariation = if (avgInterval > 0) stdDev / avgInterval else 0.0
        
        val isRegularInterval = coefficientOfVariation < 0.2
        val intervalInSeconds = avgInterval / 1000.0
        val isSuspiciousInterval = intervalInSeconds in 5.0..3600.0
        
        val similarSizes = flow.packetSizes.groupingBy { it }.eachCount()
        val mostCommonSize = similarSizes.maxByOrNull { it.value }?.key ?: 0
        val sizeConsistency = similarSizes[mostCommonSize]?.toDouble() ?: 0.0 / flow.packetSizes.size
        
        val confidence = if (isRegularInterval && isSuspiciousInterval) {
            (0.4 + (sizeConsistency * 0.3) + (if (coefficientOfVariation < 0.1) 0.3 else 0.1))
                .coerceIn(0.0, 1.0)
        } else {
            0.0
        }
        
        return HeuristicResult(
            detected = confidence > 0.6,
            confidence = confidence,
            threatType = "Beaconing",
            reason = if (confidence > 0.6) 
                "Regular communication pattern detected: ${intervals.size} packets with ${"%.2f".format(intervalInSeconds)}s avg interval"
            else "No beaconing pattern detected",
            indicators = mapOf(
                "avgIntervalSeconds" to intervalInSeconds,
                "coefficientOfVariation" to coefficientOfVariation,
                "sizeConsistency" to sizeConsistency,
                "packetCount" to flow.packetTimestamps.size
            )
        )
    }
    
    fun detectDNSTunneling(flow: NetworkFlow): HeuristicResult {
        if (flow.destPort != 53 && flow.protocol != 17) {
            return HeuristicResult(
                detected = false,
                confidence = 0.0,
                threatType = "DNS Tunneling",
                reason = "Not a DNS connection",
                indicators = emptyMap()
            )
        }
        
        if (flow.dnsQueries.isEmpty()) {
            return HeuristicResult(
                detected = false,
                confidence = 0.0,
                threatType = "DNS Tunneling",
                reason = "No DNS queries captured",
                indicators = emptyMap()
            )
        }
        
        var suspicionScore = 0.0
        val reasons = mutableListOf<String>()
        
        val avgQueryLength = flow.dnsQueries.map { it.length }.average()
        if (avgQueryLength > 40) {
            suspicionScore += 0.3
            reasons.add("Unusually long DNS queries (avg: ${"%.1f".format(avgQueryLength)})")
        }
        
        val uniqueQueries = flow.dnsQueries.toSet().size
        val queryRate = uniqueQueries.toDouble() / flow.dnsQueries.size
        if (queryRate > 0.8) {
            suspicionScore += 0.2
            reasons.add("High unique query rate: ${"%.2f".format(queryRate)}")
        }
        
        val totalEntropy = flow.dnsQueries.map { query ->
            DGADetector.calculateShannonEntropy(query.split(".")[0])
        }.average()
        
        if (totalEntropy > 3.5) {
            suspicionScore += 0.3
            reasons.add("High entropy in queries: ${"%.2f".format(totalEntropy)}")
        }
        
        val packetsPerSecond = flow.getPacketsPerSecond()
        if (packetsPerSecond > 10) {
            suspicionScore += 0.2
            reasons.add("High DNS query rate: ${"%.1f".format(packetsPerSecond)} queries/sec")
        }
        
        return HeuristicResult(
            detected = suspicionScore > 0.5,
            confidence = suspicionScore.coerceIn(0.0, 1.0),
            threatType = "DNS Tunneling",
            reason = if (suspicionScore > 0.5) 
                "DNS tunneling indicators: ${reasons.joinToString(", ")}"
            else "Normal DNS traffic",
            indicators = mapOf(
                "avgQueryLength" to avgQueryLength,
                "uniqueQueryRate" to queryRate,
                "avgEntropy" to totalEntropy,
                "queriesPerSecond" to packetsPerSecond,
                "totalQueries" to flow.dnsQueries.size
            )
        )
    }
    
    fun detectFastFlux(destIp: String, previousIps: List<String>, domain: String?): HeuristicResult {
        if (domain == null || previousIps.size < 3) {
            return HeuristicResult(
                detected = false,
                confidence = 0.0,
                threatType = "Fast Flux",
                reason = "Insufficient data for Fast Flux detection",
                indicators = emptyMap()
            )
        }
        
        val uniqueIps = (previousIps + destIp).toSet()
        val ipChangeRate = uniqueIps.size.toDouble() / (previousIps.size + 1)
        
        val recentChanges = previousIps.takeLast(5).toSet().size
        val highRecentChanges = recentChanges >= 3
        
        val confidence = if (ipChangeRate > 0.5 && highRecentChanges) {
            0.7
        } else if (ipChangeRate > 0.3) {
            0.4
        } else {
            0.0
        }
        
        return HeuristicResult(
            detected = confidence > 0.6,
            confidence = confidence,
            threatType = "Fast Flux",
            reason = if (confidence > 0.6)
                "Domain $domain resolves to multiple IPs frequently ($uniqueIps distinct IPs)"
            else "No Fast Flux detected",
            indicators = mapOf(
                "uniqueIpCount" to uniqueIps.size,
                "totalObservations" to previousIps.size + 1,
                "ipChangeRate" to ipChangeRate,
                "recentChanges" to recentChanges,
                "domain" to domain
            )
        )
    }
    
    fun detectPortScan(sourceIp: String, destPorts: List<Int>, timeWindow: Long): HeuristicResult {
        if (destPorts.size < 5) {
            return HeuristicResult(
                detected = false,
                confidence = 0.0,
                threatType = "Port Scan",
                reason = "Insufficient port connection attempts",
                indicators = emptyMap()
            )
        }
        
        val uniquePorts = destPorts.toSet()
        val portsPerSecond = (destPorts.size.toDouble() / (timeWindow / 1000.0)).coerceAtMost(100.0)
        
        val sequentialPorts = destPorts.zipWithNext().count { (a, b) -> abs(a - b) == 1 }
        val sequentialRatio = sequentialPorts.toDouble() / (destPorts.size - 1).coerceAtLeast(1)
        
        val commonPorts = listOf(21, 22, 23, 25, 53, 80, 443, 445, 3389, 8080)
        val commonPortCount = destPorts.count { it in commonPorts }
        val targetingCommonPorts = commonPortCount >= 3
        
        val confidence = when {
            portsPerSecond > 10 && sequentialRatio > 0.5 -> 0.9
            portsPerSecond > 5 || targetingCommonPorts -> 0.7
            uniquePorts.size > 10 -> 0.5
            else -> 0.0
        }
        
        return HeuristicResult(
            detected = confidence > 0.6,
            confidence = confidence,
            threatType = "Port Scan",
            reason = if (confidence > 0.6)
                "Port scanning detected: ${uniquePorts.size} unique ports in ${timeWindow / 1000}s"
            else "Normal port access pattern",
            indicators = mapOf(
                "uniquePorts" to uniquePorts.size,
                "totalAttempts" to destPorts.size,
                "portsPerSecond" to portsPerSecond,
                "sequentialRatio" to sequentialRatio,
                "commonPortsTargeted" to commonPortCount
            )
        )
    }
    
    fun detectDataExfiltration(flow: NetworkFlow): HeuristicResult {
        val uploadRatio = flow.bytesSent.toDouble() / (flow.bytesSent + flow.bytesReceived).coerceAtLeast(1)
        val isHighUpload = uploadRatio > 0.8 && flow.bytesSent > 1_000_000
        
        val duration = flow.getDuration() / 1000.0
        val uploadRate = flow.bytesSent / duration.coerceAtLeast(1.0)
        
        val isSustainedUpload = duration > 30 && uploadRate > 50_000
        
        val isUnusualPort = flow.destPort !in listOf(80, 443, 8080, 8443)
        
        val confidence = when {
            isHighUpload && isSustainedUpload && isUnusualPort -> 0.8
            isHighUpload && isSustainedUpload -> 0.6
            isHighUpload || isSustainedUpload -> 0.4
            else -> 0.0
        }
        
        return HeuristicResult(
            detected = confidence > 0.6,
            confidence = confidence,
            threatType = "Data Exfiltration",
            reason = if (confidence > 0.6)
                "Suspicious data upload: ${"%.2f".format(flow.bytesSent / 1_048_576.0)}MB uploaded in ${"%.1f".format(duration)}s"
            else "Normal data transfer",
            indicators = mapOf(
                "bytesSent" to flow.bytesSent,
                "bytesReceived" to flow.bytesReceived,
                "uploadRatio" to uploadRatio,
                "durationSeconds" to duration,
                "uploadRateBytesPerSec" to uploadRate,
                "destPort" to flow.destPort
            )
        )
    }
    
    fun detectCryptoMining(flow: NetworkFlow): HeuristicResult {
        val miningPorts = listOf(3333, 4444, 5555, 7777, 8888, 9999, 14433, 14444, 45560)
        val miningDomains = listOf("pool.", "mining.", "stratum.", "xmr", "eth", "btc")
        
        val isMiningPort = flow.destPort in miningPorts
        val hasMiningDomain = flow.tlsSni?.let { sni ->
            miningDomains.any { sni.contains(it, ignoreCase = true) }
        } ?: false
        
        val isLongLived = flow.getDuration() > 300_000
        val hasRegularPattern = detectBeaconing(flow).confidence > 0.5
        
        val confidence = when {
            (isMiningPort || hasMiningDomain) && isLongLived && hasRegularPattern -> 0.9
            (isMiningPort || hasMiningDomain) && isLongLived -> 0.7
            isMiningPort || hasMiningDomain -> 0.5
            else -> 0.0
        }
        
        return HeuristicResult(
            detected = confidence > 0.6,
            confidence = confidence,
            threatType = "Cryptocurrency Mining",
            reason = if (confidence > 0.6)
                "Cryptocurrency mining indicators detected on port ${flow.destPort}"
            else "No mining activity detected",
            indicators = mapOf(
                "destPort" to flow.destPort,
                "isMiningPort" to isMiningPort,
                "hasMiningDomain" to hasMiningDomain,
                "durationMs" to flow.getDuration(),
                "sni" to (flow.tlsSni ?: "N/A")
            )
        )
    }
}
