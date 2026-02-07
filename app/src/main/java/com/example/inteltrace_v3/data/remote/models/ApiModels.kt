package com.example.inteltrace_v3.data.remote.models

import com.google.gson.annotations.SerializedName

// AbuseIPDB Response Models
data class AbuseIPDBResponse(
    @SerializedName("data")
    val data: AbuseIPData
)

data class AbuseIPData(
    @SerializedName("ipAddress")
    val ipAddress: String,
    @SerializedName("isPublic")
    val isPublic: Boolean,
    @SerializedName("ipVersion")
    val ipVersion: Int,
    @SerializedName("isWhitelisted")
    val isWhitelisted: Boolean,
    @SerializedName("abuseConfidenceScore")
    val abuseConfidenceScore: Int,
    @SerializedName("countryCode")
    val countryCode: String?,
    @SerializedName("countryName")
    val countryName: String?,
    @SerializedName("usageType")
    val usageType: String?,
    @SerializedName("isp")
    val isp: String?,
    @SerializedName("domain")
    val domain: String?,
    @SerializedName("hostnames")
    val hostnames: List<String>?,
    @SerializedName("totalReports")
    val totalReports: Int,
    @SerializedName("numDistinctUsers")
    val numDistinctUsers: Int,
    @SerializedName("lastReportedAt")
    val lastReportedAt: String?
)

// VirusTotal Response Models
data class VirusTotalIPResponse(
    @SerializedName("data")
    val data: VirusTotalIPData
)

data class VirusTotalIPData(
    @SerializedName("id")
    val id: String,
    @SerializedName("attributes")
    val attributes: VirusTotalIPAttributes
)

data class VirusTotalIPAttributes(
    @SerializedName("country")
    val country: String?,
    @SerializedName("as_owner")
    val asOwner: String?,
    @SerializedName("last_analysis_stats")
    val lastAnalysisStats: VirusTotalAnalysisStats?,
    @SerializedName("reputation")
    val reputation: Int?
)

data class VirusTotalAnalysisStats(
    @SerializedName("harmless")
    val harmless: Int,
    @SerializedName("malicious")
    val malicious: Int,
    @SerializedName("suspicious")
    val suspicious: Int,
    @SerializedName("undetected")
    val undetected: Int,
    @SerializedName("timeout")
    val timeout: Int
)

// URLhaus Response Models
data class URLhausResponse(
    @SerializedName("query_status")
    val queryStatus: String,
    @SerializedName("urlhaus_reference")
    val urlhausReference: String?,
    @SerializedName("url")
    val url: String?,
    @SerializedName("url_status")
    val urlStatus: String?,
    @SerializedName("threat")
    val threat: String?,
    @SerializedName("tags")
    val tags: List<String>?
)

// AlienVault OTX Response Models
data class AlienVaultOTXResponse(
    @SerializedName("pulse_info")
    val pulseInfo: PulseInfo?,
    @SerializedName("reputation")
    val reputation: Int?,
    @SerializedName("country_code")
    val countryCode: String?,
    @SerializedName("asn")
    val asn: String?
) {
    data class PulseInfo(
        @SerializedName("count")
        val count: Int,
        @SerializedName("pulses")
        val pulses: List<Pulse>?
    )
    
    data class Pulse(
        @SerializedName("id")
        val id: String,
        @SerializedName("name")
        val name: String,
        @SerializedName("description")
        val description: String?,
        @SerializedName("tags")
        val tags: List<String>?,
        @SerializedName("malware_families")
        val malwareFamilies: List<String>?
    )
    
    data class MalwareResponse(
        @SerializedName("data")
        val data: List<MalwareData>?
    )
    
    data class MalwareData(
        @SerializedName("hash")
        val hash: String,
        @SerializedName("detections")
        val detections: Map<String, String>?
    )
}

// ThreatFox Response Models
data class ThreatFoxResponse(
    @SerializedName("query_status")
    val queryStatus: String,
    @SerializedName("data")
    val data: List<ThreatFoxIOC>?
)

data class ThreatFoxIOC(
    @SerializedName("id")
    val id: String,
    @SerializedName("ioc")
    val ioc: String,
    @SerializedName("threat_type")
    val threatType: String,
    @SerializedName("threat_type_desc")
    val threatTypeDesc: String?,
    @SerializedName("malware")
    val malware: String?,
    @SerializedName("malware_printable")
    val malwarePrintable: String?,
    @SerializedName("malware_alias")
    val malwareAlias: String?,
    @SerializedName("confidence_level")
    val confidenceLevel: Int,
    @SerializedName("first_seen")
    val firstSeen: String?,
    @SerializedName("last_seen")
    val lastSeen: String?,
    @SerializedName("tags")
    val tags: List<String>?
)

// PhishTank Response Models
data class PhishTankResponse(
    @SerializedName("meta")
    val meta: PhishTankMeta,
    @SerializedName("results")
    val results: PhishTankResults
)

data class PhishTankMeta(
    @SerializedName("status")
    val status: String
)

data class PhishTankResults(
    @SerializedName("in_database")
    val inDatabase: Boolean,
    @SerializedName("phish_id")
    val phishId: String?,
    @SerializedName("phish_detail_url")
    val phishDetailUrl: String?,
    @SerializedName("verified")
    val verified: Boolean?,
    @SerializedName("verified_at")
    val verifiedAt: String?,
    @SerializedName("valid")
    val valid: Boolean?
)
