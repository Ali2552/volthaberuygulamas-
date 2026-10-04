package com.example.data.model

enum class WeatherIconType {
    SUNNY,
    PARTLY_CLOUDY,
    CLOUDY,
    RAINY,
    THUNDERSTORM,
    SNOWY,
    WINDY
}

data class MgmLocation(
    val il: String,
    val ilce: String,
    val merkezId: Int,
    val istNo: Int,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val isFavorite: Boolean = false
) {
    val displayName: String
        get() = if (ilce.equals("Merkez", ignoreCase = true) || ilce.isBlank() || ilce.equals(il, ignoreCase = true)) {
            "$il, Merkez"
        } else {
            "$il, $ilce"
        }
}

data class HourlyForecast(
    val time: String, // "14:00"
    val temp: String, // "24°C"
    val feelsLike: String, // "25°C"
    val humidity: String, // "%48"
    val wind: String, // "12 km/sa"
    val condition: String,
    val iconType: WeatherIconType = WeatherIconType.PARTLY_CLOUDY
)

data class DailyForecast(
    val dayName: String,
    val dateStr: String,
    val highTemp: String,
    val lowTemp: String,
    val condition: String,
    val rainProb: String = "",
    val iconType: WeatherIconType = WeatherIconType.PARTLY_CLOUDY
)

data class WeatherInfo(
    val location: MgmLocation = MgmLocation("İzmir", "Merkez", 93500, 17220, 38.4192, 27.1287),
    val currentTemp: String = "--",
    val feelsLike: String = "--",
    val condition: String = "--",
    val humidity: String = "--",
    val wind: String = "--",
    val windDirection: String = "--",
    val pressure: String = "--",
    val iconType: WeatherIconType = WeatherIconType.PARTLY_CLOUDY,
    val hourlyForecasts: List<HourlyForecast> = emptyList(),
    val forecasts: List<DailyForecast> = emptyList(),
    val lastUpdated: String = "",
    val fetchMethod: FetchMethod = FetchMethod.API,
    val isOutOfDate: Boolean = false
)
