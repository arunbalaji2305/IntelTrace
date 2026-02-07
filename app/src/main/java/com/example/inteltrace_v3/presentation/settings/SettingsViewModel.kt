package com.example.inteltrace_v3.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.inteltrace_v3.data.local.preferences.SecurityPreferences
import com.example.inteltrace_v3.data.local.database.IntelTraceDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val threatThreshold: Int = 50,
    val isAutoBlockEnabled: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val criticalAlertsOnly: Boolean = false,
    val dataRetentionDays: Int = 7,
    val abuseIPDBApiKey: String = "",
    val virusTotalApiKey: String = "",
    val alienVaultApiKey: String = ""
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: SecurityPreferences,
    private val database: IntelTraceDatabase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    init {
        loadSettings()
    }
    
    private fun loadSettings() {
        _uiState.update {
            SettingsUiState(
                threatThreshold = prefs.threatThreshold,
                isAutoBlockEnabled = prefs.isAutoBlockEnabled,
                notificationsEnabled = prefs.notificationsEnabled,
                criticalAlertsOnly = prefs.criticalAlertsOnly,
                dataRetentionDays = prefs.dataRetentionDays,
                abuseIPDBApiKey = prefs.abuseIPDBApiKey,
                virusTotalApiKey = prefs.virusTotalApiKey,
                alienVaultApiKey = prefs.alienVaultApiKey
            )
        }
    }
    
    fun updateThreatThreshold(value: Int) {
        prefs.threatThreshold = value
        _uiState.update { it.copy(threatThreshold = value) }
    }
    
    fun toggleAutoBlock() {
        val newValue = !_uiState.value.isAutoBlockEnabled
        prefs.isAutoBlockEnabled = newValue
        _uiState.update { it.copy(isAutoBlockEnabled = newValue) }
    }
    
    fun toggleNotifications() {
        val newValue = !_uiState.value.notificationsEnabled
        prefs.notificationsEnabled = newValue
        _uiState.update { it.copy(notificationsEnabled = newValue) }
    }
    
    fun toggleCriticalAlertsOnly() {
        val newValue = !_uiState.value.criticalAlertsOnly
        prefs.criticalAlertsOnly = newValue
        _uiState.update { it.copy(criticalAlertsOnly = newValue) }
    }
    
    fun updateDataRetention(days: Int) {
        prefs.dataRetentionDays = days
        _uiState.update { it.copy(dataRetentionDays = days) }
    }
    
    fun updateAbuseIPDBApiKey(key: String) {
        prefs.abuseIPDBApiKey = key
        _uiState.update { it.copy(abuseIPDBApiKey = key) }
    }
    
    fun updateVirusTotalApiKey(key: String) {
        prefs.virusTotalApiKey = key
        _uiState.update { it.copy(virusTotalApiKey = key) }
    }
    
    fun updateAlienVaultApiKey(key: String) {
        prefs.alienVaultApiKey = key
        _uiState.update { it.copy(alienVaultApiKey = key) }
    }
    
    fun clearAllData() {
        viewModelScope.launch {
            database.connectionDao().deleteAll()
            database.alertDao().deleteAll()
            database.threatDao().deleteAll()
        }
    }
}
