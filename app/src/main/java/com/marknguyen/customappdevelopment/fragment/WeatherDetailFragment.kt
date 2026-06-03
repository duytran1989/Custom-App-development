package com.marknguyen.customappdevelopment.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.marknguyen.customappdevelopment.R
import com.marknguyen.customappdevelopment.adapter.DailyForecastAdapter
import com.marknguyen.customappdevelopment.adapter.HourlyForecastAdapter
import com.marknguyen.customappdevelopment.databinding.FragmentWeatherDetailBinding
import com.marknguyen.customappdevelopment.model.CurrentWeatherResponse
import com.marknguyen.customappdevelopment.repository.WeatherResult
import com.marknguyen.customappdevelopment.util.WeatherUtils
import com.marknguyen.customappdevelopment.viewmodel.WeatherViewModel
import kotlin.math.roundToInt

class WeatherDetailFragment : Fragment() {

    private var _binding: FragmentWeatherDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WeatherViewModel by activityViewModels()
    private lateinit var hourlyAdapter: HourlyForecastAdapter
    private lateinit var dailyAdapter: DailyForecastAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWeatherDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val unit = viewModel.settingsManager.temperatureUnit

        // Horizontal layout manager must be set in code — XML orientation attr doesn't apply to RecyclerView
        hourlyAdapter = HourlyForecastAdapter(unit, 0)
        binding.recyclerHourly.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerHourly.adapter = hourlyAdapter

        dailyAdapter = DailyForecastAdapter(unit)
        binding.recyclerDaily.adapter = dailyAdapter

        setupSwipeRefresh()
        setupFab()
        observeCurrentWeather()
        observeForecast()
        observeSaveState()
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshCurrentWeather()
        }
    }

    private fun setupFab() {
        binding.fabSave.setOnClickListener {
            val cityName = (viewModel.currentWeather.value as? WeatherResult.Success)?.data?.name
                ?: viewModel.currentCityName
            if (cityName.isNotBlank()) {
                viewModel.toggleSave(cityName)
            }
        }
    }

    private fun observeCurrentWeather() {
        viewModel.currentWeather.observe(viewLifecycleOwner) { result ->
            binding.swipeRefresh.isRefreshing = false
            when (result) {
                is WeatherResult.Loading -> showLoading()
                is WeatherResult.Success -> showWeather(result.data)
                is WeatherResult.Error -> showError(result.message)
            }
        }
    }

    private fun observeForecast() {
        viewModel.forecast.observe(viewLifecycleOwner) { result ->
            if (result is WeatherResult.Success) {
                val data = result.data
                // Recreate hourly adapter with the correct city timezone
                hourlyAdapter = HourlyForecastAdapter(
                    viewModel.settingsManager.temperatureUnit,
                    data.city.timezone
                )
                binding.recyclerHourly.adapter = hourlyAdapter
                hourlyAdapter.submitList(WeatherUtils.getHourlyForecast(data.list))
                dailyAdapter.submitList(WeatherUtils.groupForecastByDay(data.list))
            }
        }
    }

    private fun observeSaveState() {
        viewModel.isSaved.observe(viewLifecycleOwner) { saved ->
            if (saved) {
                binding.fabSave.setImageResource(R.drawable.ic_bookmark)
                binding.fabSave.contentDescription = getString(R.string.action_unsave)
            } else {
                binding.fabSave.setImageResource(R.drawable.ic_bookmark_border)
                binding.fabSave.contentDescription = getString(R.string.action_save)
            }
        }
    }

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.swipeRefresh.visibility = View.GONE
        binding.errorState.visibility = View.GONE
    }

    private fun showWeather(weather: CurrentWeatherResponse) {
        binding.progressBar.visibility = View.GONE
        binding.swipeRefresh.visibility = View.VISIBLE
        binding.errorState.visibility = View.GONE

        val condition = weather.weather.firstOrNull()
        val unit = viewModel.settingsManager.temperatureUnit
        val unitSymbol = viewModel.settingsManager.getUnitSymbol()
        val temp = weather.main.temp.roundToInt()
        val feelsLike = weather.main.feelsLike.roundToInt()

        binding.textCityName.text = weather.name
        binding.textCountry.text = weather.sys.country
        binding.textDateTime.text = WeatherUtils.formatDateTime(weather.dt, weather.timezone)
        binding.textTemperature.text = "$temp°"
        binding.textCondition.text = condition?.description?.replaceFirstChar { it.uppercase() } ?: ""
        binding.textFeelsLike.text = getString(R.string.label_feels_like) + " $feelsLike$unitSymbol"

        // Accessibility: screen readers announce the full temperature with unit
        binding.textTemperature.contentDescription =
            getString(R.string.cd_temperature, "$temp$unitSymbol")

        condition?.icon?.let { iconCode ->
            binding.imageWeatherIcon.load(WeatherUtils.getIconUrl(iconCode)) { crossfade(true) }
            binding.imageWeatherIcon.contentDescription =
                getString(R.string.cd_weather_icon, condition.description)
        }

        binding.statHumidity.statLabel.text = getString(R.string.label_humidity)
        binding.statHumidity.statValue.text = "${weather.main.humidity}%"

        binding.statWind.statLabel.text = getString(R.string.label_wind)
        binding.statWind.statValue.text = WeatherUtils.formatWindSpeed(weather.wind.speed, unit)

        binding.statPressure.statLabel.text = getString(R.string.label_pressure)
        binding.statPressure.statValue.text = "${weather.main.pressure} hPa"

        binding.statVisibility.statLabel.text = getString(R.string.label_visibility)
        binding.statVisibility.statValue.text = WeatherUtils.formatVisibility(weather.visibility)
    }

    private fun showError(message: String) {
        binding.progressBar.visibility = View.GONE
        binding.swipeRefresh.visibility = View.GONE
        binding.errorState.visibility = View.VISIBLE
        binding.textError.text = message
        binding.btnRetry.setOnClickListener { viewModel.refreshCurrentWeather() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
