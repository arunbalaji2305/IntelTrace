package com.example.inteltrace_v3.presentation.connections

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.inteltrace_v3.data.repository.ConnectionRepository
import com.example.inteltrace_v3.domain.models.NetworkConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class ConnectionsViewModel @Inject constructor(
    private val connectionRepository: ConnectionRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val initialFilter = when (savedStateHandle.get<String>("filter")) {
        "suspicious" -> FilterType.SUSPICIOUS
        else -> FilterType.ALL
    }
    
    private val _filterType = MutableStateFlow(initialFilter)
    val filterType: StateFlow<FilterType> = _filterType.asStateFlow()
    
    val connections: StateFlow<List<NetworkConnection>> = _filterType.flatMapLatest { filter ->
        when (filter) {
            FilterType.ALL -> connectionRepository.getRecentConnections(500)
            FilterType.SUSPICIOUS -> connectionRepository.getSuspiciousConnections(40)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    fun setFilter(filter: FilterType) {
        _filterType.value = filter
    }
}

enum class FilterType {
    ALL, SUSPICIOUS
}
