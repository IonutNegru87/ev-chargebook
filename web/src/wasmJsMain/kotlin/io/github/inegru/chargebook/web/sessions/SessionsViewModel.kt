package io.github.inegru.chargebook.web.sessions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.inegru.chargebook.shared.model.ChargingSession
import io.github.inegru.chargebook.shared.model.SessionWithSnapshots
import io.github.inegru.chargebook.web.api.ChargebookApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SessionsUiState(
    val sessions: List<ChargingSession> = emptyList(),
    val isLoading: Boolean = true,
    val needsAuth: Boolean = false,
    val errorMessage: String? = null,
    // Drill-down: when non-null the detail screen is shown.
    val detail: SessionWithSnapshots? = null,
    val detailLoading: Boolean = false,
    val detailError: String? = null,
)

class SessionsViewModel(private val api: ChargebookApi) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionsUiState())
    val uiState: StateFlow<SessionsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val r = api.sessions()) {
                is ChargebookApi.ListResult.Success -> _uiState.update {
                    it.copy(sessions = r.data, isLoading = false, needsAuth = false)
                }
                ChargebookApi.ListResult.Unauthorized -> _uiState.update {
                    it.copy(isLoading = false, needsAuth = true)
                }
                is ChargebookApi.ListResult.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = r.message)
                }
            }
        }
    }

    fun openDetail(id: String) {
        _uiState.update { it.copy(detailLoading = true, detailError = null, detail = null) }
        viewModelScope.launch {
            when (val r = api.session(id)) {
                is ChargebookApi.ListResult.Success -> _uiState.update {
                    it.copy(detail = r.data, detailLoading = false)
                }
                ChargebookApi.ListResult.Unauthorized -> _uiState.update {
                    it.copy(detailLoading = false, detailError = "Unauthorized")
                }
                is ChargebookApi.ListResult.Error -> _uiState.update {
                    it.copy(detailLoading = false, detailError = r.message)
                }
            }
        }
    }

    fun closeDetail() {
        _uiState.update { it.copy(detail = null, detailLoading = false, detailError = null) }
    }
}
