package com.marknguyen.customappdevelopment.util

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

    // Returns the next 8 forecast items (24 hours in 3-hour steps)
    fun getHourlyForecast(items: List<ForecastItem>): List<ForecastItem> =
        items.take(8)
}
