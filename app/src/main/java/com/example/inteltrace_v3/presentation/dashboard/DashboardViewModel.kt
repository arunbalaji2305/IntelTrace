package com.example.inteltrace_v3.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.inteltrace_v3.data.local.preferences.SecurityPreferences
import com.example.inteltrace_v3.data.repository.AlertRepository
import com.example.inteltrace_v3.data.repository.ConnectionRepository
import com.example.inteltrace_v3.data.repository.ThreatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val connectionRepository: ConnectionRepository,
    private val threatRepository: ThreatRepository,
    private val alertRepository: AlertRepository,
    private val prefs: SecurityPreferences
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    
    init {
        loadDashboardData()
    }
    
    private fun loadDashboardData() {
        viewModelScope.launch {
            // Load VPN status
            _uiState.update { it.copy(isVpnActive = prefs.isVpnEnabled) }
            
            // Load connection stats
            val last24Hours = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
            val connectionCount = connectionRepository.getConnectionCountSince(last24Hours)
            
            // Load alerts
            combine(
                alertRepository.getUnreadCount(),
                connectionRepository.getSuspiciousConnections(50),
                threatRepository.getMaliciousIps()
            ) { unreadAlerts, suspiciousConnections, maliciousIps ->
                _uiState.update { state ->
                    state.copy(
                        totalConnections = connectionCount,
                        suspiciousConnections = suspiciousConnections.size,
                        unreadAlerts = unreadAlerts,
                        maliciousIpsDetected = maliciousIps.size,
                        isLoading = false
                    )
                }
            }.collect()
        }
    }
    
    fun toggleVpn() {
        viewModelScope.launch {
            val newState = !_uiState.value.isVpnActive
            prefs.isVpnEnabled = newState
            _uiState.update { it.copy(isVpnActive = newState) }
        }
    }
    
    fun refresh() {
        _uiState.update { it.copy(isLoading = true) }
        loadDashboardData()
    }
}

data class DashboardUiState(
    val isVpnActive: Boolean = false,
    val totalConnections: Int = 0,
    val suspiciousConnections: Int = 0,
    val unreadAlerts: Int = 0,
    val maliciousIpsDetected: Int = 0,
    val isLoading: Boolean = true
)
