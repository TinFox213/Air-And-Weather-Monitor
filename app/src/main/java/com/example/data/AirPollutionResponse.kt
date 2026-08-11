package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AirPollutionResponse(
    val list: List<PollutionData>
)

@JsonClass(generateAdapter = true)
data class PollutionData(
    val main: AqiData,
    val components: PollutantComponents
)

@JsonClass(generateAdapter = true)
data class AqiData(
    val aqi: Int
)

@JsonClass(generateAdapter = true)
data class PollutantComponents(
    val co: Double,
    val no2: Double,
    val o3: Double,
    @Json(name = "pm10") val pm10: Double,
    @Json(name = "pm2_5") val pm25: Double
)
