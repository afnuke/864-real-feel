package com.afnuke.realfeel.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afnuke.realfeel.data.WeatherData
import com.afnuke.realfeel.data.WeatherRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface RealFeelUiState {
    data object Loading : RealFeelUiState

    data class Success(
        val weather: WeatherData,
        val isRefreshing: Boolean = false,
        val isStale: Boolean = false,
        val staleNotice: String = ""
    ) : RealFeelUiState

    data class Error(
        val message: String
    ) : RealFeelUiState
}

class RealFeelViewModel(
    private val repository: WeatherRepository = WeatherRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<RealFeelUiState>(RealFeelUiState.Loading)
    val uiState: StateFlow<RealFeelUiState> = _uiState.asStateFlow()

    private var tickerJob: Job? = null

    init {
        loadWeather(isRefresh = false)
        startStalenessTicker()
    }

    fun refresh() {
        loadWeather(isRefresh = true)
    }

    private fun loadWeather(isRefresh: Boolean) {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is RealFeelUiState.Success && isRefresh) {
                _uiState.value = currentState.copy(isRefreshing = true)
            } else if (!isRefresh) {
                _uiState.value = RealFeelUiState.Loading
            }

            val result = repository.fetchWeather()
            result.onSuccess { weather ->
                val (stale, notice) = computeStaleness(weather.fetchedAtEpochMs)
                _uiState.value = RealFeelUiState.Success(
                    weather = weather,
                    isRefreshing = false,
                    isStale = stale,
                    staleNotice = notice
                )
            }.onFailure { error ->
                if (currentState is RealFeelUiState.Success) {
                    // Keep existing data, stop refreshing indicator
                    _uiState.value = currentState.copy(isRefreshing = false)
                } else {
                    _uiState.value = RealFeelUiState.Error(
                        error.localizedMessage ?: "Couldn't reach the weather — tap to retry"
                    )
                }
            }
        }
    }

    private fun startStalenessTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (true) {
                delay(60_000L) // Check staleness every minute
                val state = _uiState.value
                if (state is RealFeelUiState.Success) {
                    val (stale, notice) = computeStaleness(state.weather.fetchedAtEpochMs)
                    if (stale != state.isStale || notice != state.staleNotice) {
                        _uiState.value = state.copy(isStale = stale, staleNotice = notice)
                    }
                }
            }
        }
    }

    private fun computeStaleness(fetchedAtEpochMs: Long): Pair<Boolean, String> {
        val ageMs = System.currentTimeMillis() - fetchedAtEpochMs
        val oneHourMs = 3600_000L
        if (ageMs > oneHourMs) {
            val hours = (ageMs / oneHourMs).toInt()
            val notice = if (hours <= 1) {
                "Last updated 1 hour ago — tap to refresh."
            } else {
                "Last updated $hours hours ago — tap to refresh."
            }
            return Pair(true, notice)
        }
        return Pair(false, "")
    }

    override fun onCleared() {
        super.onCleared()
        tickerJob?.cancel()
    }
}
