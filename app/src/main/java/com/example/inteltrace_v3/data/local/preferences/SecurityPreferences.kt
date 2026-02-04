package com.example.inteltrace_v3.data.local.preferences

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityPreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "inteltrace_prefs",
        Context.MODE_PRIVATE
    )
    
    var isVpnEnabled: Boolean
        get() = prefs.getBoolean(KEY_VPN_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_VPN_ENABLED, value).apply()
    
    var isRealTimeMonitoringEnabled: Boolean
        get() = prefs.getBoolean(KEY_REAL_TIME_MONITORING, true)
        set(value) = prefs.edit().putBoolean(KEY_REAL_TIME_MONITORING, value).apply()
    
    var threatThreshold: Int
        get() = prefs.getInt(KEY_THREAT_THRESHOLD, 50)
        set(value) = prefs.edit().putInt(KEY_THREAT_THRESHOLD, value).apply()
    
    var isAutoBlockEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_BLOCK, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_BLOCK, value).apply()
    
    var notificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATIONS, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFICATIONS, value).apply()
    
    var criticalAlertsOnly: Boolean
        get() = prefs.getBoolean(KEY_CRITICAL_ALERTS, false)
        set(value) = prefs.edit().putBoolean(KEY_CRITICAL_ALERTS, value).apply()
    
    var dataRetentionDays: Int
        get() = prefs.getInt(KEY_DATA_RETENTION, 7)
        set(value) = prefs.edit().putInt(KEY_DATA_RETENTION, value).apply()
    
    var isFirstLaunch: Boolean
        get() = prefs.getBoolean(KEY_FIRST_LAUNCH, true)
        set(value) = prefs.edit().putBoolean(KEY_FIRST_LAUNCH, value).apply()
    
    var abuseIPDBApiKey: String
        get() = prefs.getString(KEY_ABUSEIPDB_API, "") ?: ""
        set(value) = prefs.edit().putString(KEY_ABUSEIPDB_API, value).apply()
    
    var virusTotalApiKey: String
        get() = prefs.getString(KEY_VIRUSTOTAL_API, "") ?: ""
        set(value) = prefs.edit().putString(KEY_VIRUSTOTAL_API, value).apply()
    
    companion object {
        private const val KEY_VPN_ENABLED = "vpn_enabled"
        private const val KEY_REAL_TIME_MONITORING = "real_time_monitoring"
        private const val KEY_THREAT_THRESHOLD = "threat_threshold"
        private const val KEY_AUTO_BLOCK = "auto_block"
        private const val KEY_NOTIFICATIONS = "notifications"
        private const val KEY_CRITICAL_ALERTS = "critical_alerts"
        private const val KEY_DATA_RETENTION = "data_retention"
        private const val KEY_FIRST_LAUNCH = "first_launch"
        private const val KEY_ABUSEIPDB_API = "abuseipdb_api_key"
        private const val KEY_VIRUSTOTAL_API = "virustotal_api_key"
    }
}
