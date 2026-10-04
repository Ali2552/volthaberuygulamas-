package com.example.ui.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.location.Location
import android.location.LocationManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.FavoriteLocationEntity
import com.example.data.model.MgmLocation
import com.example.data.model.WeatherInfo
import com.example.data.remote.TurkeyProvinces
import com.example.data.repository.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WeatherUiState(
    val weather: WeatherInfo = WeatherInfo(),
    val currentLocation: MgmLocation = MgmLocation("İzmir", "Merkez", 93500, 17220, 38.4192, 27.1287),
    val isFavorite: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isOfflineWarning: Boolean = false,
    val isLocationSheetOpen: Boolean = false
)

class WeatherViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppRepository(application)

    private val _uiState = MutableStateFlow(WeatherUiState(isLoading = true))
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    val favoriteLocations: StateFlow<List<FavoriteLocationEntity>> = repository.getFavoriteLocations()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            val savedLocation = repository.selectedLocationFlow.firstOrNull()
                ?: MgmLocation("İzmir", "Merkez", 93500, 17220, 38.4192, 27.1287)
            _uiState.value = _uiState.value.copy(currentLocation = savedLocation)
            loadWeather(savedLocation)
        }
    }

    fun loadWeather(location: MgmLocation = _uiState.value.currentLocation) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val isFav = repository.isLocationFavorite(location.merkezId).firstOrNull() ?: false
            val result = repository.fetchWeather(location)

            _uiState.value = _uiState.value.copy(
                weather = result.data,
                currentLocation = location,
                isFavorite = isFav,
                isLoading = false,
                isOfflineWarning = result.isOutOfDate,
                errorMessage = if (result.isOutOfDate) result.errorMessage else null
            )
        }
    }

    fun selectLocation(location: MgmLocation) {
        viewModelScope.launch {
            repository.saveSelectedLocation(location)
            _uiState.value = _uiState.value.copy(
                currentLocation = location,
                isLocationSheetOpen = false
            )
            loadWeather(location)
        }
    }

    fun resetToIzmir() {
        val izmir = MgmLocation("İzmir", "Merkez", 93500, 17220, 38.4192, 27.1287)
        selectLocation(izmir)
    }

    fun toggleFavoriteCurrentLocation() {
        viewModelScope.launch {
            val isNowFav = repository.toggleFavoriteLocation(_uiState.value.currentLocation)
            _uiState.value = _uiState.value.copy(isFavorite = isNowFav)
        }
    }

    fun removeFavoriteLocation(id: Long) {
        viewModelScope.launch {
            repository.removeFavoriteLocation(id)
        }
    }

    fun setLocationSheetOpen(open: Boolean) {
        _uiState.value = _uiState.value.copy(isLocationSheetOpen = open)
    }

    @SuppressLint("MissingPermission")
    fun useCurrentDeviceLocation() {
        val context = getApplication<Application>().applicationContext
        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            if (locationManager != null) {
                val providers = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER, LocationManager.PASSIVE_PROVIDER)
                var bestLocation: Location? = null

                for (provider in providers) {
                    if (locationManager.isProviderEnabled(provider)) {
                        val loc = locationManager.getLastKnownLocation(provider)
                        if (loc != null && (bestLocation == null || loc.accuracy < bestLocation.accuracy)) {
                            bestLocation = loc
                        }
                    }
                }

                if (bestLocation != null) {
                    val nearest = TurkeyProvinces.findNearestProvince(bestLocation.latitude, bestLocation.longitude)
                    selectLocation(nearest)
                    return
                }
            }
        } catch (_: Exception) {
            // Sessizce mevcut konumda kal
        }
    }
}
