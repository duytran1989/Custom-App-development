package com.marknguyen.customappdevelopment.util

import com.marknguyen.customappdevelopment.R

object WeatherAnimationMapper {
    fun getAnimationRes(iconCode: String): Int? = when (iconCode) {
        "01d" -> R.raw.lottie_sunny
        "01n" -> R.raw.lottie_night
        "02d", "03d", "04d" -> R.raw.lottie_cloudy
        "02n", "03n", "04n" -> R.raw.lottie_cloudy
        "09d", "09n", "10d", "10n", "11d", "11n" -> R.raw.lottie_rain
        "13d", "13n" -> R.raw.lottie_snow
        "50d", "50n" -> R.raw.lottie_cloudy
        else -> null
    }

    fun isThunderstorm(iconCode: String) = iconCode.startsWith("11")
}
