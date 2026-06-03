package com.marknguyen.customappdevelopment.repository

import com.marknguyen.customappdevelopment.api.RetrofitClient
import com.marknguyen.customappdevelopment.model.CurrentWeatherResponse
import com.marknguyen.customappdevelopment.model.ForecastResponse
import com.marknguyen.customappdevelopment.util.Constants
import java.io.IOException

sealed class WeatherResult<out T> {
    data class Success<T>(val data: T) : WeatherResult<T>()
    data class Error(val message: String) : WeatherResult<Nothing>()
    object Loading : WeatherResult<Nothing>()
}

class WeatherRepository {

    private val api = RetrofitClient.weatherApiService

    suspend fun getCurrentWeather(city: String, unit: String): WeatherResult<CurrentWeatherResponse> {
        return try {
            val response = api.getCurrentWeather(city.trim(), Constants.API_KEY, unit)
            when {
                response.isSuccessful -> {
                    val body = response.body()
                    if (body != null) WeatherResult.Success(body)
                    else WeatherResult.Error("No data received from server")
                }
                response.code() == 404 -> WeatherResult.Error("City \"$city\" not found. Check the spelling and try again.")
                response.code() == 401 -> WeatherResult.Error("Invalid API key. Please check your configuration.")
                response.code() == 429 -> WeatherResult.Error("Too many requests. Please wait a moment and try again.")
                else -> WeatherResult.Error("Server error (${response.code()}). Please try again later.")
            }
        } catch (e: IOException) {
            WeatherResult.Error("No internet connection. Please check your network and try again.")
        } catch (e: Exception) {
            WeatherResult.Error("An unexpected error occurred: ${e.localizedMessage}")
        }
    }

    suspend fun getForecast(city: String, unit: String): WeatherResult<ForecastResponse> {
        return try {
            val response = api.getForecast(city.trim(), Constants.API_KEY, unit)
            when {
                response.isSuccessful -> {
                    val body = response.body()
                    if (body != null) WeatherResult.Success(body)
                    else WeatherResult.Error("No forecast data received")
                }
                response.code() == 404 -> WeatherResult.Error("City not found")
                response.code() == 401 -> WeatherResult.Error("Invalid API key")
                else -> WeatherResult.Error("Error ${response.code()}")
            }
        } catch (e: IOException) {
            WeatherResult.Error("No internet connection")
        } catch (e: Exception) {
            WeatherResult.Error("Unexpected error: ${e.localizedMessage}")
        }
    }
}
