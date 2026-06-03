package com.marknguyen.customappdevelopment.util

import android.content.Context
import com.google.gson.Gson
import com.marknguyen.customappdevelopment.model.CurrentWeatherResponse
import com.marknguyen.customappdevelopment.model.ForecastResponse

class WeatherCacheManager(context: Context) {

    private val prefs = context.getSharedPreferences("weather_cache", Context.MODE_PRIVATE)
    private val gson = Gson()

    private fun currentKey(city: String) = "current_${normalise(city)}"
    private fun forecastKey(city: String) = "forecast_${normalise(city)}"
    private fun normalise(city: String) = city.trim().lowercase().removeSuffix(",au")

    fun saveCurrentWeather(city: String, data: CurrentWeatherResponse) {
        val k = currentKey(city)
        prefs.edit()
            .putString(k, gson.toJson(data))
            .putLong("${k}_ts", System.currentTimeMillis())
            .apply()
    }

    fun getCachedCurrentWeather(city: String): CurrentWeatherResponse? {
        val k = currentKey(city)
        if (!isFresh(k)) return null
        val json = prefs.getString(k, null) ?: return null
        return try { gson.fromJson(json, CurrentWeatherResponse::class.java) } catch (e: Exception) { null }
    }

    fun saveForecast(city: String, data: ForecastResponse) {
        val k = forecastKey(city)
        prefs.edit()
            .putString(k, gson.toJson(data))
            .putLong("${k}_ts", System.currentTimeMillis())
            .apply()
    }

    fun getCachedForecast(city: String): ForecastResponse? {
        val k = forecastKey(city)
        if (!isFresh(k)) return null
        val json = prefs.getString(k, null) ?: return null
        return try { gson.fromJson(json, ForecastResponse::class.java) } catch (e: Exception) { null }
    }

    private fun isFresh(key: String): Boolean {
        val ts = prefs.getLong("${key}_ts", 0L)
        return System.currentTimeMillis() - ts <= Constants.CACHE_TTL_MS
    }
}
