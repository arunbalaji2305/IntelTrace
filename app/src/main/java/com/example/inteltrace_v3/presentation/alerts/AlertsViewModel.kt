package com.example.inteltrace_v3.presentation.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.inteltrace_v3.data.local.database.entities.AlertEntity
import com.example.inteltrace_v3.data.repository.AlertRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AlertFilterType {
    ALL, UNREAD, CRITICAL, HIGH
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AlertsViewModel @Inject constructor(
    private val alertRepository: AlertRepository
) : ViewModel() {
    
    private val _filterType = MutableStateFlow(AlertFilterType.ALL)
    val filterType: StateFlow<AlertFilterType> = _filterType.asStateFlow()
    
    val alerts: StateFlow<List<AlertEntity>> = _filterType.flatMapLatest { filter ->
        when (filter) {
            AlertFilterType.ALL -> alertRepository.getAllAlerts()
            AlertFilterType.UNREAD -> alertRepository.getUnreadAlerts()
            AlertFilterType.CRITICAL -> alertRepository.getAlertsByType("CRITICAL")
            AlertFilterType.HIGH -> alertRepository.getAlertsByType("HIGH")
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    fun setFilter(filter: AlertFilterType) {
        _filterType.value = filter
    }
    
    fun markAsRead(id: Long) {
        viewModelScope.launch {
            alertRepository.markAsRead(id)
        }
    }
    
    fun dismiss(id: Long) {
        viewModelScope.launch {
            alertRepository.dismiss(id)
        }
    }
}
