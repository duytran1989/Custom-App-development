package com.marknguyen.customappdevelopment.util

import android.content.Context

class SettingsManager(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var temperatureUnit: String
        get() = prefs.getString(KEY_UNIT, UNIT_METRIC) ?: UNIT_METRIC
        set(value) { prefs.edit().putString(KEY_UNIT, value).apply() }

    var isDarkMode: Boolean
        get() = prefs.getBoolean(KEY_DARK_MODE, false)
        set(value) { prefs.edit().putBoolean(KEY_DARK_MODE, value).apply() }

    fun getUnitSymbol(): String = if (temperatureUnit == UNIT_METRIC) "°C" else "°F"

    fun getWindUnitSymbol(): String = if (temperatureUnit == UNIT_METRIC) "km/h" else "mph"

    companion object {
        private const val PREFS_NAME = "weather_settings"
        private const val KEY_UNIT = "temperature_unit"
        private const val KEY_DARK_MODE = "dark_mode"
        const val UNIT_METRIC = "metric"
        const val UNIT_IMPERIAL = "imperial"
    }
}
