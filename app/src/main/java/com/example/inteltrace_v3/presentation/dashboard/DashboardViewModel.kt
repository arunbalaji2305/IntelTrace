package com.example.inteltrace_v3.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.inteltrace_v3.data.local.preferences.SecurityPreferences
import com.example.inteltrace_v3.data.repository.AlertRepository
import com.example.inteltrace_v3.data.repository.ConnectionRepository
import com.example.inteltrace_v3.data.repository.ThreatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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
    
    private var monitorJob: Job? = null
    
    init {
        startMonitoring()
    }
    
    private fun startMonitoring() {
        monitorJob?.cancel()
        monitorJob = viewModelScope.launch {
            // Load VPN status
            _uiState.update { it.copy(isVpnActive = prefs.isVpnEnabled) }
            
            // Continuously monitor all stats with real-time updates from Room
            combine(
                connectionRepository.getAllConnections(),
                alertRepository.getUnreadCount(),
                connectionRepository.getSuspiciousConnections(30),
                threatRepository.getMaliciousIps()
            ) { allConnections, unreadAlerts, suspiciousConnections, maliciousIps ->
                DashboardUiState(
                    isVpnActive = prefs.isVpnEnabled,
                    totalConnections = allConnections.size,
                    suspiciousConnections = suspiciousConnections.size,
                    unreadAlerts = unreadAlerts,
                    maliciousIpsDetected = maliciousIps.size,
                    isLoading = false
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }
    
    fun toggleVpn() {
        val newState = !_uiState.value.isVpnActive
        prefs.isVpnEnabled = newState
        _uiState.update { it.copy(isVpnActive = newState) }
    }
    
    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            kotlinx.coroutines.delay(300) // Brief visual feedback
            _uiState.update { it.copy(isRefreshing = false, isVpnActive = prefs.isVpnEnabled) }
        }
    }
}

data class DashboardUiState(
    val isVpnActive: Boolean = false,
    val totalConnections: Int = 0,
    val suspiciousConnections: Int = 0,
    val unreadAlerts: Int = 0,
    val maliciousIpsDetected: Int = 0,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false
)
