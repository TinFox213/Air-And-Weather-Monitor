package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.api.RetrofitClient
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

sealed class UiState {
    object Loading : UiState()
    data class Success(
        val weather: WeatherResponse,
        val pollution: AirPollutionResponse,
        val isDemoMode: Boolean = false,
        val demoReason: String? = null
    ) : UiState()
    data class Error(val message: String) : UiState()
}

class WeatherViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    
    private var lastLat: Double? = null
    private var lastLon: Double? = null
    
    fun setLocation(lat: Double, lon: Double) {
        lastLat = lat
        lastLon = lon
        fetchData()
    }
    
    fun refresh() {
        _isRefreshing.value = true
        fetchData()
    }

    fun loadDemoData(reason: String? = null) {
        val sampleWeather = WeatherResponse(
            name = "San Francisco",
            main = MainData(temp = 19.0, feelsLike = 17.0, pressure = 1012, humidity = 64),
            wind = WindData(speed = 3.33),
            weather = listOf(WeatherDesc(description = "partly cloudy", icon = "02d"))
        )
        val samplePollution = AirPollutionResponse(
            list = listOf(
                PollutionData(
                    main = AqiData(aqi = 1),
                    components = PollutantComponents(
                        co = 210.3,
                        no2 = 8.5,
                        o3 = 45.2,
                        pm10 = 20.1,
                        pm25 = 12.4
                    )
                )
            )
        )
        _uiState.value = UiState.Success(sampleWeather, samplePollution, isDemoMode = true, demoReason = reason)
        _isRefreshing.value = false
    }

    fun fetchData() {
        val lat = lastLat ?: 51.5074
        val lon = lastLon ?: -0.1278
        
        viewModelScope.launch {
            if (!_isRefreshing.value) {
                _uiState.value = UiState.Loading
            }
            try {
                val apiKey = try {
                    BuildConfig.OPENWEATHER_API_KEY
                } catch (e: Exception) {
                    null
                }
                
                val isPlaceholderKey = apiKey.isNullOrEmpty() ||
                        apiKey == "MY_OPENWEATHER_API_KEY" ||
                        apiKey == "null"
                
                if (isPlaceholderKey) {
                    loadDemoData("Demo Mode • API key unconfigured")
                    return@launch
                }
                
                val weather = RetrofitClient.instance.getCurrentWeather(lat, lon, apiKey!!)
                val pollution = RetrofitClient.instance.getAirPollution(lat, lon, apiKey)
                
                _uiState.value = UiState.Success(weather, pollution, isDemoMode = false)
            } catch (e: HttpException) {
                if (e.code() == 401) {
                    loadDemoData("Demo Mode • API Key activating (401 Unauthorized)")
                } else {
                    loadDemoData("Demo Mode • API Error (${e.code()})")
                }
            } catch (e: IOException) {
                loadDemoData("Demo Mode • Network offline")
            } catch (e: Exception) {
                loadDemoData("Demo Mode • ${e.message}")
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}

