package com.example.inteltrace_v3.data.repository

import android.util.Log
import com.example.inteltrace_v3.BuildConfig
import com.example.inteltrace_v3.data.local.cache.ThreatCache
import com.example.inteltrace_v3.data.local.database.dao.ThreatDao
import com.example.inteltrace_v3.data.local.database.entities.ThreatEntity
import com.example.inteltrace_v3.data.local.preferences.SecurityPreferences
import com.example.inteltrace_v3.data.remote.api.AbuseIPDBService
import com.example.inteltrace_v3.data.remote.api.VirusTotalService
import com.example.inteltrace_v3.domain.models.ThreatLevel
import com.example.inteltrace_v3.domain.models.ThreatResult
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThreatRepository @Inject constructor(
    private val abuseIPDBService: AbuseIPDBService,
    private val virusTotalService: VirusTotalService,
    private val alienVaultService: com.example.inteltrace_v3.data.remote.api.AlienVaultOTXService,
    private val threatFoxService: com.example.inteltrace_v3.data.remote.api.ThreatFoxService,
    private val phishTankService: com.example.inteltrace_v3.data.remote.api.PhishTankService,
    private val threatDao: ThreatDao,
    private val threatCache: ThreatCache,
    private val bloomFilter: com.example.inteltrace_v3.core.utils.BloomFilter,
    private val prefs: SecurityPreferences
) {
    
    suspend fun checkIPReputation(ipAddress: String): ThreatResult {
        // Skip private IP addresses
        if (isPrivateIP(ipAddress)) {
            return ThreatResult(
                ipAddress = ipAddress,
                threatLevel = ThreatLevel.SAFE,
                threatScore = 0,
                message = "Private IP address"
            )
        }
        
        // Check Bloom filter for quick lookup
        if (bloomFilter.mightContain(ipAddress)) {
            Log.d(TAG, "Bloom filter match for $ipAddress - checking full sources")
        }
        
        // Check cache first
        threatCache.get(ipAddress)?.let { cached ->
            return cached.toThreatResult()
        }
        
        // Check database
        threatDao.getThreatByIp(ipAddress)?.let { dbThreat ->
            if (!isStale(dbThreat.lastChecked)) {
                threatCache.put(ipAddress, dbThreat)
                return dbThreat.toThreatResult()
            }
        }
        
        // Query OSINT sources
        return try {
            val threat = queryOSINTSources(ipAddress)
            
            if (threat.isMalicious) {
                bloomFilter.add(ipAddress)
            }
            
            threatDao.insert(threat)
            threatCache.put(ipAddress, threat)
            threat.toThreatResult()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking IP reputation: ${e.message}")
            ThreatResult(
                ipAddress = ipAddress,
                threatLevel = ThreatLevel.UNKNOWN,
                threatScore = 0,
                message = "Unable to check reputation"
            )
        }
    }
    
    private suspend fun queryOSINTSources(ipAddress: String): ThreatEntity {
        var abuseScore = 0
        var totalReports = 0
        var country: String? = null
        var isp: String? = null
        var domain: String? = null
        var usageType: String? = null
        var vtDetections = 0
        var vtEngines = 0
        var otxPulseCount = 0
        var threatFoxMatches = 0
        val detectionSources = mutableListOf<String>()
        
        // Query AbuseIPDB
        try {
            val apiKey = prefs.abuseIPDBApiKey.ifEmpty { BuildConfig.ABUSEIPDB_API_KEY }
            if (apiKey.isNotEmpty() && apiKey != "YOUR_ABUSEIPDB_KEY") {
                val response = abuseIPDBService.checkIP(apiKey, ipAddress)
                response.data.let { data ->
                    abuseScore = data.abuseConfidenceScore
                    totalReports = data.totalReports
                    country = data.countryName
                    isp = data.isp
                    domain = data.domain
                    usageType = data.usageType
                    if (abuseScore > 0) detectionSources.add("AbuseIPDB")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "AbuseIPDB query failed: ${e.message}")
        }
        
        // Query VirusTotal
        try {
            val apiKey = prefs.virusTotalApiKey.ifEmpty { BuildConfig.VIRUSTOTAL_API_KEY }
            if (apiKey.isNotEmpty() && apiKey != "YOUR_VIRUSTOTAL_KEY") {
                val response = virusTotalService.getIPReport(ipAddress, apiKey)
                response.data.attributes.lastAnalysisStats?.let { stats ->
                    vtDetections = stats.malicious + stats.suspicious
                    vtEngines = stats.harmless + stats.malicious + stats.suspicious + stats.undetected
                    if (vtDetections > 0) detectionSources.add("VirusTotal")
                }
                if (country == null) {
                    country = response.data.attributes.country
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "VirusTotal query failed: ${e.message}")
        }
        
        // Query AlienVault OTX
        try {
            val apiKey = prefs.alienVaultApiKey.ifEmpty { "" }
            if (apiKey.isNotEmpty()) {
                val response = alienVaultService.getIPReputation(ipAddress, apiKey)
                if (response.isSuccessful) {
                    response.body()?.let { otxData ->
                        otxPulseCount = otxData.pulseInfo?.count ?: 0
                        if (otxPulseCount > 0) {
                            detectionSources.add("AlienVault OTX ($otxPulseCount pulses)")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "AlienVault OTX query failed: ${e.message}")
        }
        
        // Query ThreatFox
        try {
            val request = com.example.inteltrace_v3.data.remote.api.ThreatFoxService.createIPSearchRequest(ipAddress)
            val response = threatFoxService.searchIOC(request)
            if (response.isSuccessful) {
                response.body()?.let { threatFoxData ->
                    threatFoxMatches = threatFoxData.data?.size ?: 0
                    if (threatFoxMatches > 0) {
                        detectionSources.add("ThreatFox ($threatFoxMatches IOCs)")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "ThreatFox query failed: ${e.message}")
        }
        
        // Calculate overall threat score with all sources
        val threatScore = calculateThreatScore(
            abuseScore = abuseScore,
            vtDetections = vtDetections,
            vtEngines = vtEngines,
            otxPulseCount = otxPulseCount,
            threatFoxMatches = threatFoxMatches
        )
        
        Log.d(TAG, "IP $ipAddress threat score: $threatScore from ${detectionSources.size} sources: ${detectionSources.joinToString()}")
        
        return ThreatEntity(
            ipAddress = ipAddress,
            threatScore = threatScore,
            abuseConfidenceScore = abuseScore,
            totalReports = totalReports,
            country = country,
            isp = isp,
            domain = domain,
            usageType = usageType,
            isMalicious = threatScore >= 70,
            lastChecked = System.currentTimeMillis(),
            virusTotalDetections = vtDetections,
            virusTotalEngines = vtEngines
        )
    }
    
    private fun calculateThreatScore(
        abuseScore: Int,
        vtDetections: Int,
        vtEngines: Int,
        otxPulseCount: Int = 0,
        threatFoxMatches: Int = 0
    ): Int {
        val weights = mutableListOf<Pair<Int, Double>>()
        
        // AbuseIPDB (weight: 0.4)
        weights.add(Pair(abuseScore, 0.4))
        
        // VirusTotal (weight: 0.3)
        if (vtEngines > 0) {
            val vtPercentage = (vtDetections.toFloat() / vtEngines * 100).toInt()
            weights.add(Pair(vtPercentage, 0.3))
        }
        
        // AlienVault OTX (weight: 0.15)
        if (otxPulseCount > 0) {
            val otxScore = (otxPulseCount * 20).coerceAtMost(100)
            weights.add(Pair(otxScore, 0.15))
        }
        
        // ThreatFox (weight: 0.15)
        if (threatFoxMatches > 0) {
            val tfScore = (threatFoxMatches * 25).coerceAtMost(100)
            weights.add(Pair(tfScore, 0.15))
        }
        
        val totalWeight = weights.sumOf { it.second }
        val weightedScore = if (totalWeight > 0) {
            weights.sumOf { (score, weight) -> score * weight } / totalWeight
        } else {
            0.0
        }
        
        return weightedScore.toInt().coerceIn(0, 100)
    }
    
    private fun isPrivateIP(ip: String): Boolean {
        val parts = ip.split(".")
        if (parts.size != 4) return false
        
        val first = parts[0].toIntOrNull() ?: return false
        val second = parts[1].toIntOrNull() ?: return false
        
        return when {
            first == 10 -> true
            first == 172 && second in 16..31 -> true
            first == 192 && second == 168 -> true
            first == 127 -> true
            else -> false
        }
    }
    
    private fun isStale(lastChecked: Long): Boolean {
        val maxAge = 24 * 60 * 60 * 1000L // 24 hours
        return System.currentTimeMillis() - lastChecked > maxAge
    }
    
    fun getHighThreats(minScore: Int = 50): Flow<List<ThreatEntity>> {
        return threatDao.getHighThreats(minScore)
    }
    
    fun getMaliciousIps(): Flow<List<ThreatEntity>> {
        return threatDao.getMaliciousIps()
    }
    
    suspend fun deleteStaleThreats() {
        val staleTime = System.currentTimeMillis() - (48 * 60 * 60 * 1000L)
        threatDao.deleteStaleThreats(staleTime)
    }
    
    companion object {
        private const val TAG = "ThreatRepository"
    }
}

private fun ThreatEntity.toThreatResult(): ThreatResult {
    val level = when {
        threatScore >= 80 -> ThreatLevel.CRITICAL
        threatScore >= 60 -> ThreatLevel.HIGH
        threatScore >= 40 -> ThreatLevel.MEDIUM
        threatScore >= 20 -> ThreatLevel.LOW
        else -> ThreatLevel.SAFE
    }
    
    return ThreatResult(
        ipAddress = ipAddress,
        threatLevel = level,
        threatScore = threatScore,
        message = when {
            isMalicious -> "Known malicious IP"
            abuseConfidenceScore > 0 -> "$totalReports abuse reports"
            else -> "No threats detected"
        },
        country = country,
        isp = isp
    )
}
