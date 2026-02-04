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
