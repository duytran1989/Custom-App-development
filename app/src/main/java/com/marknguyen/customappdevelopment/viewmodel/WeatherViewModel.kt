package com.marknguyen.customappdevelopment.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.marknguyen.customappdevelopment.model.AirPollutionResponse
import com.marknguyen.customappdevelopment.model.CurrentWeatherResponse
import com.marknguyen.customappdevelopment.model.ForecastResponse
import com.marknguyen.customappdevelopment.model.UviResponse
import com.marknguyen.customappdevelopment.repository.WeatherRepository
import com.marknguyen.customappdevelopment.repository.WeatherResult
import com.marknguyen.customappdevelopment.util.FavouritesManager
import com.marknguyen.customappdevelopment.util.SettingsManager
import kotlinx.coroutines.launch

class WeatherViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WeatherRepository(application)
    val favouritesManager = FavouritesManager(application)
    val settingsManager = SettingsManager(application)

    private val _currentWeather = MutableLiveData<WeatherResult<CurrentWeatherResponse>>()
    val currentWeather: LiveData<WeatherResult<CurrentWeatherResponse>> = _currentWeather

    private val _forecast = MutableLiveData<WeatherResult<ForecastResponse>>()
    val forecast: LiveData<WeatherResult<ForecastResponse>> = _forecast

    private val _isSaved = MutableLiveData<Boolean>()
    val isSaved: LiveData<Boolean> = _isSaved

    private val _favouriteWeather = MutableLiveData<List<WeatherResult<CurrentWeatherResponse>>>()
    val favouriteWeather: LiveData<List<WeatherResult<CurrentWeatherResponse>>> = _favouriteWeather

    private val _uvIndex = MutableLiveData<WeatherResult<UviResponse>>()
    val uvIndex: LiveData<WeatherResult<UviResponse>> = _uvIndex

    private val _airPollution = MutableLiveData<WeatherResult<AirPollutionResponse>>()
    val airPollution: LiveData<WeatherResult<AirPollutionResponse>> = _airPollution

    var currentCityName: String = ""
        private set

    fun searchWeather(city: String) {
        currentCityName = city
        _currentWeather.value = WeatherResult.Loading
        viewModelScope.launch {
            val result = repository.getCurrentWeather(city, settingsManager.temperatureUnit)
            _currentWeather.value = result
            val data = when (result) {
                is WeatherResult.Success -> result.data
                is WeatherResult.CachedSuccess -> result.data
                else -> null
            }
            if (data != null) {
                _isSaved.value = favouritesManager.isFavourite(data.name)
                viewModelScope.launch { _uvIndex.value = repository.getUvIndex(data.coord.lat, data.coord.lon) }
                viewModelScope.launch { _airPollution.value = repository.getAirPollution(data.coord.lat, data.coord.lon) }
            }
        }
    }

    fun loadForecast(city: String) {
        _forecast.value = WeatherResult.Loading
        viewModelScope.launch {
            _forecast.value = repository.getForecast(city, settingsManager.temperatureUnit)
        }
    }

    fun loadFavouritesWeather() {
        val cities = favouritesManager.getFavourites()
        if (cities.isEmpty()) {
            _favouriteWeather.value = emptyList()
            return
        }
        viewModelScope.launch {
            val results = cities.map { city ->
                repository.getCurrentWeather(city, settingsManager.temperatureUnit)
            }
            _favouriteWeather.value = results
        }
    }

    fun toggleSave(cityName: String) {
        val isFav = favouritesManager.isFavourite(cityName)
        if (isFav) {
            favouritesManager.removeFavourite(cityName)
            _isSaved.value = false
        } else {
            favouritesManager.addFavourite(cityName)
            _isSaved.value = true
        }
    }

    fun refreshCurrentWeather() {
        if (currentCityName.isNotBlank()) {
            searchWeather(currentCityName)
            loadForecast(currentCityName)
        }
    }

    fun loadUviAndAqi(lat: Double, lon: Double) {
        viewModelScope.launch { _uvIndex.value = repository.getUvIndex(lat, lon) }
        viewModelScope.launch { _airPollution.value = repository.getAirPollution(lat, lon) }
    }
}
