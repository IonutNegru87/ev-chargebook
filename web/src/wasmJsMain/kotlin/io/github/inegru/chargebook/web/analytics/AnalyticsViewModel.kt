package io.github.inegru.chargebook.web.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.inegru.chargebook.shared.analytics.MonthlyTotals
import io.github.inegru.chargebook.web.api.ChargebookApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AnalyticsUiState(
    val months: List<MonthlyTotals> = emptyList(),
    val isLoading: Boolean = true,
    val needsAuth: Boolean = false,
    val errorMessage: String? = null,
)

class AnalyticsViewModel(private val api: ChargebookApi) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val r = api.monthlyTotals()) {
                is ChargebookApi.ListResult.Success -> _uiState.update {
                    it.copy(months = r.data, isLoading = false, needsAuth = false)
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
}
