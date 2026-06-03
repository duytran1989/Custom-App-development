package com.marknguyen.customappdevelopment.api

import com.marknguyen.customappdevelopment.model.CurrentWeatherResponse
import com.marknguyen.customappdevelopment.model.ForecastResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApiService {

    @GET("weather")
    suspend fun getCurrentWeather(
        @Query("q") city: String,
        @Query("appid") apiKey: String,
        @Query("units") units: String
    ): Response<CurrentWeatherResponse>

    @GET("forecast")
    suspend fun getForecast(
        @Query("q") city: String,
        @Query("appid") apiKey: String,
        @Query("units") units: String
    ): Response<ForecastResponse>
}
