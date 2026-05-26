package io.github.inegru.chargebook.web.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.inegru.chargebook.shared.model.ChargingSnapshot
import io.github.inegru.chargebook.web.api.ChargebookApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardUiState(
    val snapshot: ChargingSnapshot? = null,
    val isLoading: Boolean = true,
    val needsAuth: Boolean = false,
    val errorMessage: String? = null,
    val liveStreamActive: Boolean = false,
)

class DashboardViewModel(
    private val api: ChargebookApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadInitial()
        subscribeLive()
    }

    private fun loadInitial() {
        viewModelScope.launch {
            when (val r = api.latestSnapshot()) {
                is ChargebookApi.SnapshotResult.Success -> _uiState.update {
                    it.copy(snapshot = r.snapshot, isLoading = false)
                }
                ChargebookApi.SnapshotResult.NotFound -> _uiState.update {
                    it.copy(isLoading = false)
                }
                ChargebookApi.SnapshotResult.Unauthorized -> _uiState.update {
                    it.copy(isLoading = false, needsAuth = true)
                }
                is ChargebookApi.SnapshotResult.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = r.message)
                }
            }
        }
    }

    private fun subscribeLive() {
        viewModelScope.launch {
            runCatching {
                api.liveSnapshots().collect { snapshot ->
                    _uiState.update {
                        it.copy(snapshot = snapshot, liveStreamActive = true, errorMessage = null)
                    }
                }
            }.onFailure { t ->
                _uiState.update { it.copy(liveStreamActive = false, errorMessage = t.message) }
            }
        }
    }
}
