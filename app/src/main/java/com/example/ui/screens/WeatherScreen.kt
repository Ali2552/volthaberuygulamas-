package com.example.ui.screens

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.graphics.Color
import com.example.data.model.WeatherIconType
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.DailyForecast
import com.example.data.model.HourlyForecast
import com.example.data.model.MgmLocation
import com.example.data.remote.TurkeyProvinces
import com.example.ui.components.ErrorRetryView
import com.example.ui.components.LoadingView
import com.example.ui.components.OfflineBanner
import com.example.ui.viewmodel.WeatherViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(
    viewModel: WeatherViewModel,
    showDebugTag: Boolean
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val favorites by viewModel.favoriteLocations.collectAsStateWithLifecycle()
    val pullToRefreshState = rememberPullToRefreshState()
    val weather = uiState.weather

    // Konum İzni Başlatıcı
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.useCurrentDeviceLocation()
        } else {
            // İzin verilmezse sessizce kal
        }
    }

    if (uiState.isLocationSheetOpen) {
        LocationSelectionBottomSheet(
            currentLocation = uiState.currentLocation,
            onSelectLocation = { viewModel.selectLocation(it) },
            onUseDeviceLocation = {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            },
            onDismiss = { viewModel.setLocationSheetOpen(false) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { viewModel.setLocationSheetOpen(true) }
                            .padding(vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = uiState.currentLocation.displayName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Konum Değiştir"
                        )
                    }
                },
                actions = {
                    // Favori Yıldız Butonu
                    IconButton(onClick = { viewModel.toggleFavoriteCurrentLocation() }) {
                        Icon(
                            imageVector = if (uiState.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Favorilere Ekle",
                            tint = if (uiState.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }
                    IconButton(
                        onClick = { viewModel.loadWeather() },
                        enabled = !uiState.isLoading
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Yenile")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            OfflineBanner(visible = uiState.isOfflineWarning)

            // Favori Konumlar ve "İzmir'e Dön" Şeridi
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // İzmir'e Dön Kısayolu (Eğer İzmir'de değilse)
                if (uiState.currentLocation.il != "İzmir") {
                    InputChip(
                        selected = false,
                        onClick = { viewModel.resetToIzmir() },
                        label = { Text("İzmir'e Dön") },
                        leadingIcon = {
                            Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    )
                }

                // Favori Konum Çipleri
                favorites.forEach { fav ->
                    val isCurrent = fav.merkezId == uiState.currentLocation.merkezId
                    InputChip(
                        selected = isCurrent,
                        onClick = {
                            viewModel.selectLocation(
                                MgmLocation(fav.il, fav.ilce, fav.merkezId, fav.istNo, fav.latitude, fav.longitude, true)
                            )
                        },
                        label = { Text("${fav.il}, ${fav.ilce}") },
                        leadingIcon = {
                            Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = { viewModel.removeFavoriteLocation(fav.id) },
                                modifier = Modifier.size(16.dp)
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = "Favoriden Sil", modifier = Modifier.size(12.dp))
                            }
                        }
                    )
                }
            }

            PullToRefreshBox(
                isRefreshing = uiState.isLoading,
                onRefresh = { viewModel.loadWeather() },
                state = pullToRefreshState,
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    uiState.isLoading && weather.currentTemp == "--" -> {
                        LoadingView(message = "MGM verileri alınıyor...")
                    }
                    uiState.errorMessage != null && weather.currentTemp == "--" -> {
                        ErrorRetryView(
                            message = uiState.errorMessage ?: "Hava durumu yüklenemedi",
                            onRetry = { viewModel.loadWeather() }
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // BÜYÜK MGM ANLIK HAVA KARTI
                            item {
                                MgmCurrentWeatherCard(weather = weather)
                            }

                            // SAATLİK TAHMİN (Sonraki 24 saat)
                            if (weather.hourlyForecasts.isNotEmpty()) {
                                item {
                                    Column {
                                        Text(
                                            text = "Saatlik Tahmin (24 Saat)",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            items(weather.hourlyForecasts) { hourly ->
                                                HourlyForecastCard(hourly = hourly)
                                            }
                                        }
                                    }
                                }
                            }

                            // GÜNLÜK TAHMİN (MGM 5 Gün)
                            if (weather.forecasts.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "5 Günlük Tahmin",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                items(weather.forecasts) { daily ->
                                    DailyForecastCard(daily = daily)
                                }
                            }

                            // Kaynak Bilgisi Rozeti
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Kaynak: Meteoroloji Genel Müdürlüğü (MGM)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    Text(
                                        text = "Son Güncelleme: ${weather.lastUpdated}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MgmCurrentWeatherCard(weather: com.example.data.model.WeatherInfo) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = weather.location.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "İstasyon No: ${weather.location.istNo}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "MGM",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = weather.currentTemp,
                        fontSize = 54.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = weather.condition,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                }

                Icon(
                    imageVector = getWeatherIcon(weather.iconType),
                    contentDescription = weather.condition,
                    tint = getWeatherIconColor(weather.iconType),
                    modifier = Modifier.size(68.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(16.dp))

            // Metrikler: Nem, Rüzgar, Hissedilen, Basınç
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                WeatherMetricItem(icon = Icons.Default.DeviceThermostat, label = "Hissedilen", value = weather.feelsLike)
                WeatherMetricItem(icon = Icons.Default.WaterDrop, label = "Nem", value = weather.humidity)
                WeatherMetricItem(icon = Icons.Default.Air, label = "Rüzgâr", value = weather.wind)
                WeatherMetricItem(icon = Icons.Default.Compress, label = "Basınç", value = weather.pressure)
            }
        }
    }
}

@Composable
fun HourlyForecastCard(hourly: HourlyForecast) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.5.dp,
        modifier = Modifier.width(76.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = hourly.time, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Icon(
                imageVector = getWeatherIcon(hourly.iconType),
                contentDescription = hourly.condition,
                tint = getWeatherIconColor(hourly.iconType),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = hourly.temp, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = hourly.humidity, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
fun DailyForecastCard(daily: DailyForecast) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1.2f)) {
                Text(text = daily.dayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(text = daily.dateStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }

            Row(
                modifier = Modifier.weight(1.5f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = getWeatherIcon(daily.iconType),
                    contentDescription = daily.condition,
                    tint = getWeatherIconColor(daily.iconType),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = daily.condition, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Text(text = daily.highTemp, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = daily.lowTemp, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationSelectionBottomSheet(
    currentLocation: MgmLocation,
    onSelectLocation: (MgmLocation) -> Unit,
    onUseDeviceLocation: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }
    var selectedProvince by remember { mutableStateOf<TurkeyProvinces.ProvinceData?>(null) }

    val filteredProvinces = remember(searchQuery) {
        if (searchQuery.isBlank()) TurkeyProvinces.PROVINCES
        else TurkeyProvinces.PROVINCES.filter { it.il.contains(searchQuery.trim(), ignoreCase = true) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "MGM Konum Seçimi",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Konumumu Kullan Butonu
            Button(
                onClick = {
                    onUseDeviceLocation()
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Mevcut Konumumu Kullan (GPS / Ağ)")
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Arama Kutusu
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("81 İl Ara (örn. İzmir, Ankara, Bursa)...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedProvince != null) {
                // İlçe Seçim Ekranı
                val prov = selectedProvince!!
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "${prov.il} İlçeleri", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedButton(onClick = { selectedProvince = null }) {
                        Text("İllere Dön")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(modifier = Modifier.height(340.dp)) {
                    items(prov.districts) { district ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectLocation(
                                        MgmLocation(
                                            il = prov.il,
                                            ilce = district,
                                            merkezId = prov.merkezId,
                                            istNo = prov.istNo,
                                            latitude = prov.lat,
                                            longitude = prov.lon
                                        )
                                    )
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp)
                        ) {
                            Text(text = district, style = MaterialTheme.typography.bodyLarge)
                        }
                        HorizontalDivider()
                    }
                }
            } else {
                // 81 İl Listesi
                LazyColumn(modifier = Modifier.height(340.dp)) {
                    items(filteredProvinces) { prov ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedProvince = prov
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = prov.il, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                Text(text = "${prov.districts.size} ilçe ▾", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
fun WeatherMetricItem(
    icon: ImageVector,
    label: String,
    value: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

fun getWeatherIcon(type: WeatherIconType): ImageVector {
    return when (type) {
        WeatherIconType.SUNNY -> Icons.Default.WbSunny
        WeatherIconType.PARTLY_CLOUDY -> Icons.Default.CloudQueue
        WeatherIconType.CLOUDY -> Icons.Default.Cloud
        WeatherIconType.RAINY -> Icons.Default.WaterDrop
        WeatherIconType.THUNDERSTORM -> Icons.Default.Thunderstorm
        WeatherIconType.SNOWY -> Icons.Default.AcUnit
        WeatherIconType.WINDY -> Icons.Default.Air
    }
}

fun getWeatherIconColor(type: WeatherIconType): Color {
    return when (type) {
        WeatherIconType.SUNNY -> Color(0xFFF59E0B)
        WeatherIconType.PARTLY_CLOUDY -> Color(0xFF38BDF8)
        WeatherIconType.CLOUDY -> Color(0xFF94A3B8)
        WeatherIconType.RAINY -> Color(0xFF3B82F6)
        WeatherIconType.THUNDERSTORM -> Color(0xFF8B5CF6)
        WeatherIconType.SNOWY -> Color(0xFF06B6D4)
        WeatherIconType.WINDY -> Color(0xFF14B8A6)
    }
}
