package com.marknguyen.customappdevelopment.util

import android.graphics.Color
import com.marknguyen.customappdevelopment.model.DailyForecast
import com.marknguyen.customappdevelopment.model.ForecastItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

object WeatherUtils {

    fun formatTemperature(temp: Double, unit: String): String {
        val rounded = temp.roundToInt()
        return "$rounded°"
    }

    fun formatWindSpeed(speed: Double, unit: String): String {
        return if (unit == SettingsManager.UNIT_METRIC) {
            // OpenWeatherMap metric returns m/s; convert to km/h
            val kmh = (speed * 3.6).roundToInt()
            "$kmh km/h"
        } else {
            val mph = speed.roundToInt()
            "$mph mph"
        }
    }

    fun getWindDirection(deg: Int): String {
        val dirs = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        val idx = ((deg + 22.5) / 45.0).toInt() % 8
        return dirs[idx]
    }

    fun formatVisibility(visibilityMeters: Int): String {
        return if (visibilityMeters >= 1000) {
            "${visibilityMeters / 1000} km"
        } else {
            "$visibilityMeters m"
        }
    }

    fun getIconUrl(iconCode: String): String =
        "${Constants.ICON_BASE_URL}$iconCode${Constants.ICON_SUFFIX}"

    fun formatHour(dt: Long, timezoneOffsetSeconds: Int): String {
        val sdf = SimpleDateFormat("h a", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("GMT")
        val adjustedTime = (dt + timezoneOffsetSeconds) * 1000L
        return sdf.format(Date(adjustedTime))
    }

    fun formatDateTime(dt: Long, timezoneOffsetSeconds: Int): String {
        val sdf = SimpleDateFormat("EEE, MMM d · h:mm a", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("GMT")
        val adjustedTime = (dt + timezoneOffsetSeconds) * 1000L
        return sdf.format(Date(adjustedTime))
    }

    fun groupForecastByDay(items: List<ForecastItem>): List<DailyForecast> {
        val sdfIn = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val sdfDay = SimpleDateFormat("EEE", Locale.getDefault())
        val sdfDate = SimpleDateFormat("MMM d", Locale.getDefault())

        return items
            .groupBy { it.dtTxt.substring(0, 10) } // group by "yyyy-MM-dd"
            .entries
            .drop(1) // skip today (already shown in current weather)
            .take(5)
            .map { (_, dayItems) ->
                val midday = dayItems.minByOrNull {
                    val hour = it.dtTxt.substring(11, 13).toInt()
                    kotlin.math.abs(hour - 12)
                } ?: dayItems.first()

                val date = sdfIn.parse(midday.dtTxt) ?: Date()
                DailyForecast(
                    dayName = sdfDay.format(date),
                    date = sdfDate.format(date),
                    iconCode = midday.weather.firstOrNull()?.icon ?: "01d",
                    condition = midday.weather.firstOrNull()?.description?.replaceFirstChar { it.uppercase() } ?: "",
                    tempMin = dayItems.minOf { it.main.tempMin },
                    tempMax = dayItems.maxOf { it.main.tempMax }
                )
            }
    }

    // Returns up to 16 forecast items (48 hours in 3-hour steps)
    fun getHourlyForecast(items: List<ForecastItem>): List<ForecastItem> =
        items.take(16)

    fun getConditionGradient(conditionId: Int, isDay: Boolean): IntArray = when {
        !isDay -> intArrayOf(Color.parseColor("#0D1B2A"), Color.parseColor("#1B2A4A"))
        conditionId in 200..299 -> intArrayOf(Color.parseColor("#263238"), Color.parseColor("#546E7A"))
        conditionId in 300..531 -> intArrayOf(Color.parseColor("#1565C0"), Color.parseColor("#42A5F5"))
        conditionId in 600..622 -> intArrayOf(Color.parseColor("#78909C"), Color.parseColor("#CFD8DC"))
        conditionId in 700..781 -> intArrayOf(Color.parseColor("#5D4037"), Color.parseColor("#A1887F"))
        conditionId == 800 -> intArrayOf(Color.parseColor("#E65100"), Color.parseColor("#FFA726"))
        else -> intArrayOf(Color.parseColor("#1565C0"), Color.parseColor("#42A5F5"))
    }

    fun isDay(dt: Long, sunrise: Long, sunset: Long): Boolean = dt in sunrise..sunset

    fun getUvCategory(uvi: Double): Pair<String, Int> = when {
        uvi < 3  -> "Low"       to Color.parseColor("#4CAF50")
        uvi < 6  -> "Moderate"  to Color.parseColor("#FFC107")
        uvi < 8  -> "High"      to Color.parseColor("#FF9800")
        uvi < 11 -> "Very High" to Color.parseColor("#F44336")
        else     -> "Extreme"   to Color.parseColor("#9C27B0")
    }

    fun getAqiLabel(aqi: Int): Pair<String, Int> = when (aqi) {
        1 -> "Good"      to Color.parseColor("#4CAF50")
        2 -> "Fair"      to Color.parseColor("#8BC34A")
        3 -> "Moderate"  to Color.parseColor("#FFC107")
        4 -> "Poor"      to Color.parseColor("#FF5722")
        else -> "Very Poor" to Color.parseColor("#9C27B0")
    }

    fun getFeelsLikeReason(feelsLike: Double, actual: Double, humidity: Int, windSpeed: Double, unit: String): String {
        val humidEffect = feelsLike > actual + 1.0 && humidity > 70
        val windEffect = feelsLike < actual - 1.0 && windSpeed > 3.0
        return when {
            humidEffect -> "Humidity makes it feel warmer"
            windEffect  -> "Wind chill makes it feel cooler"
            feelsLike > actual + 2.0 -> "Heat index elevated"
            feelsLike < actual - 2.0 -> "Feels cooler than it is"
            else -> "Feels about right"
        }
    }

    fun formatSunTime(epochSeconds: Long, timezoneOffsetSeconds: Int): String {
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("GMT")
        return sdf.format(Date((epochSeconds + timezoneOffsetSeconds) * 1000L))
    }
}
