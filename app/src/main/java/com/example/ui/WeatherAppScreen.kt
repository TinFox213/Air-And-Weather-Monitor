package com.example.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AirPollutionResponse
import com.example.data.WeatherResponse
import com.example.ui.theme.*
import com.example.viewmodel.UiState
import com.example.viewmodel.WeatherViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.delay
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun WeatherAppScreen() {
    val context = LocalContext.current
    val viewModel: WeatherViewModel = viewModel()
    
    val locationPermissionState = rememberPermissionState(
        Manifest.permission.ACCESS_FINE_LOCATION
    )
    
    LaunchedEffect(locationPermissionState.status.isGranted) {
        if (locationPermissionState.status.isGranted) {
            fetchLocationAndData(context, viewModel)
            while(true) {
                delay(5 * 60 * 1000)
                viewModel.refresh()
            }
        }
    }
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            when {
                locationPermissionState.status.isGranted -> {
                    WeatherContent(viewModel, onRefresh = {
                        fetchLocationAndData(context, viewModel)
                    })
                }
                locationPermissionState.status.shouldShowRationale || !locationPermissionState.status.isGranted -> {
                    PermissionRequestScreen(
                        onRequestPermission = { locationPermissionState.launchPermissionRequest() }
                    )
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
private fun fetchLocationAndData(context: Context, viewModel: WeatherViewModel) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
        if (location != null) {
            viewModel.setLocation(location.latitude, location.longitude)
        } else {
            viewModel.setLocation(51.5074, -0.1278) // fallback to London if location is completely null to avoid stuck UI
        }
    }.addOnFailureListener {
        viewModel.setLocation(51.5074, -0.1278) // fallback
    }
}

@Composable
fun PermissionRequestScreen(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Location Access Required",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
            text = "We need your location to show real-time weather and air quality data for your area.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        Button(
            onClick = onRequestPermission,
            modifier = Modifier.testTag("grant_permission_button")
        ) {
            Text("Grant Permission")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherContent(viewModel: WeatherViewModel, onRefresh: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    
    Box(modifier = Modifier.fillMaxSize()) {
        when (val state = uiState) {
            is UiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            is UiState.Error -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                    Button(onClick = onRefresh, modifier = Modifier.testTag("retry_button")) {
                        Text("Retry")
                    }
                }
            }
            is UiState.Success -> {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier.fillMaxSize()
                ) {
                    WeatherDashboard(
                        weather = state.weather,
                        pollution = state.pollution,
                        demoReason = if (state.isDemoMode) state.demoReason else null,
                        onRefresh = onRefresh
                    )
                }
            }
        }
    }
}

@Composable
fun WeatherDashboard(
    weather: WeatherResponse,
    pollution: AirPollutionResponse,
    demoReason: String? = null,
    onRefresh: () -> Unit
) {
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        val currentTime = SimpleDateFormat("EEEE, h:mm a", Locale.getDefault()).format(Date())
        
        if (demoReason != null) {
            Surface(
                color = SleekHighlight,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = demoReason,
                        style = MaterialTheme.typography.labelSmall,
                        color = SleekPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = weather.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = currentTime,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = onRefresh,
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(24.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        
        // Scrollable content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // First Card: Weather
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(32.dp)),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                text = "${weather.main.temp.toInt()}°",
                                style = MaterialTheme.typography.displayLarge.copy(fontSize = 60.sp),
                                fontWeight = FontWeight.Light,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = weather.weather.firstOrNull()?.description?.replaceFirstChar { it.uppercase() } ?: "",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            Text(
                                text = "FEELS LIKE ${weather.main.feelsLike.toInt()}°C",
                                style = MaterialTheme.typography.labelSmall,
                                color = SleekTertiaryText,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cloud,
                                contentDescription = "Weather Icon",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        WeatherStat(Modifier.weight(1f), "HUMIDITY", "${weather.main.humidity}%")
                        WeatherStat(Modifier.weight(1f), "WIND", "${weather.wind.speed.toInt()} m/s")
                        WeatherStat(Modifier.weight(1f), "PRESSURE", "${weather.main.pressure}")
                    }
                }
            }
            
            // Second Card: Air Quality
            val aqi = pollution.list.firstOrNull()?.main?.aqi ?: 1
            val components = pollution.list.firstOrNull()?.components
            
            val (aqiText, aqiColor) = when (aqi) {
                1 -> "Good" to Color(0xFF4CAF50)
                2 -> "Fair" to Color(0xFFFFEB3B)
                3 -> "Moderate" to Color(0xFFFF9800)
                4 -> "Poor" to Color(0xFFF44336)
                5 -> "Very Poor" to Color(0xFF8B0000)
                else -> "Unknown" to Color.Gray
            }
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(32.dp)),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Air Quality",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Box(
                            modifier = Modifier
                                .background(aqiColor, RoundedCornerShape(16.dp))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "INDEX: $aqi (${aqiText.uppercase()})",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (aqi == 2) Color.Black else Color.White,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    if (components != null) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                                PollutantStat(Modifier.weight(1f), "PM2.5", components.pm25)
                                PollutantStat(Modifier.weight(1f), "PM10", components.pm10)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                                PollutantStat(Modifier.weight(1f), "NO2", components.no2)
                                PollutantStat(Modifier.weight(1f), "O3", components.o3)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                                PollutantStat(Modifier.weight(1f), "CO", components.co)
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    val aqiMessage = when (aqi) {
                        1 -> "Air quality is satisfactory, and air pollution poses little or no risk."
                        2 -> "Air quality is acceptable. However, there may be a risk for some people."
                        3 -> "Members of sensitive groups may experience health effects."
                        4 -> "Everyone may begin to experience health effects."
                        else -> "Health warnings of emergency conditions. The entire population is more likely to be affected."
                    }
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .width(8.dp)
                                    .height(32.dp)
                                    .background(aqiColor, RoundedCornerShape(4.dp))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "\"$aqiMessage\"",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        }
                    }
                }
            }
        }
        
        // Footer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Updated just now",
                style = MaterialTheme.typography.labelMedium,
                color = SleekTertiaryText,
                fontWeight = FontWeight.Medium
            )
            Button(
                onClick = onRefresh,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = "Refresh Now",
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun WeatherStat(modifier: Modifier = Modifier, label: String, value: String) {
    Column(
        modifier = modifier
            .background(SleekWhite50, RoundedCornerShape(16.dp))
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = SleekTertiaryText,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun PollutantStat(modifier: Modifier = Modifier, label: String, value: Double) {
    Row(
        modifier = modifier
            .border(0.dp, Color.Transparent),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = String.format(Locale.US, "%.1f", value),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
    }
}
