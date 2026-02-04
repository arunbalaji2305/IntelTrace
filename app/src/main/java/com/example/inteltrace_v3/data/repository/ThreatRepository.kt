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
    private val threatDao: ThreatDao,
    private val threatCache: ThreatCache,
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
                }
                if (country == null) {
                    country = response.data.attributes.country
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "VirusTotal query failed: ${e.message}")
        }
        
        // Calculate overall threat score
        val threatScore = calculateThreatScore(abuseScore, vtDetections, vtEngines)
        
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
        vtEngines: Int
    ): Int {
        var score = abuseScore
        
        // Add VirusTotal weight
        if (vtEngines > 0) {
            val vtPercentage = (vtDetections.toFloat() / vtEngines * 100).toInt()
            score = (score + vtPercentage) / 2
        }
        
        return score.coerceIn(0, 100)
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
