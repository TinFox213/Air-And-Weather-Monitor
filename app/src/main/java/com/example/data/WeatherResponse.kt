package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WeatherResponse(
    val name: String,
    val main: MainData,
    val wind: WindData,
    val weather: List<WeatherDesc>
)

@JsonClass(generateAdapter = true)
data class MainData(
    val temp: Double,
    @Json(name = "feels_like") val feelsLike: Double,
    val pressure: Int,
    val humidity: Int
)

@JsonClass(generateAdapter = true)
data class WindData(
    val speed: Double
)

@JsonClass(generateAdapter = true)
data class WeatherDesc(
    val description: String,
    val icon: String
)
