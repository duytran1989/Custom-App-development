package com.marknguyen.customappdevelopment.model

import com.google.gson.annotations.SerializedName

data class CurrentWeatherResponse(
    val name: String,
    val sys: Sys,
    val main: Main,
    val weather: List<Weather>,
    val wind: Wind,
    val visibility: Int,
    val dt: Long,
    val timezone: Int,
    val cod: Int
)

data class ForecastResponse(
    val cod: String,
    val cnt: Int,
    val list: List<ForecastItem>,
    val city: City
)

data class ForecastItem(
    val dt: Long,
    val main: Main,
    val weather: List<Weather>,
    val wind: Wind,
    @SerializedName("dt_txt") val dtTxt: String
)

data class Sys(
    val country: String,
    val sunrise: Long,
    val sunset: Long
)

data class Main(
    val temp: Double,
    @SerializedName("feels_like") val feelsLike: Double,
    @SerializedName("temp_min") val tempMin: Double,
    @SerializedName("temp_max") val tempMax: Double,
    val pressure: Int,
    val humidity: Int
)

data class Weather(
    val id: Int,
    val main: String,
    val description: String,
    val icon: String
)

data class Wind(
    val speed: Double,
    val deg: Int,
    val gust: Double? = null
)

data class City(
    val name: String,
    val country: String,
    val timezone: Int
)

data class DailyForecast(
    val dayName: String,
    val date: String,
    val iconCode: String,
    val condition: String,
    val tempMin: Double,
    val tempMax: Double
)
