package com.marknguyen.customappdevelopment.fragment

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import coil.load
import com.marknguyen.customappdevelopment.R
import com.marknguyen.customappdevelopment.adapter.DailyForecastAdapter
import com.marknguyen.customappdevelopment.adapter.HourlyForecastAdapter
import com.marknguyen.customappdevelopment.databinding.FragmentWeatherDetailBinding
import com.marknguyen.customappdevelopment.model.AirPollutionResponse
import com.marknguyen.customappdevelopment.model.CurrentWeatherResponse
import com.marknguyen.customappdevelopment.model.UviResponse
import com.marknguyen.customappdevelopment.repository.WeatherResult
import com.marknguyen.customappdevelopment.util.WeatherAnimationMapper
import com.marknguyen.customappdevelopment.util.WeatherUtils
import com.marknguyen.customappdevelopment.viewmodel.WeatherViewModel
import com.marknguyen.customappdevelopment.worker.WeatherAlertWorker
import java.util.concurrent.TimeUnit
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

        hourlyAdapter = HourlyForecastAdapter(unit, 0)
        binding.recyclerHourly.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerHourly.adapter = hourlyAdapter

        dailyAdapter = DailyForecastAdapter(unit)
        binding.recyclerDaily.adapter = dailyAdapter

        binding.swipeRefresh.setColorSchemeColors(
            requireContext().getColor(R.color.blue_700),
            requireContext().getColor(R.color.amber_500)
        )

        setupSwipeRefresh()
        setupFab()
        observeCurrentWeather()
        observeForecast()
        observeSaveState()
        observeUvIndex()
        observeAirPollution()
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshCurrentWeather()
        }
    }

    private fun setupFab() {
        binding.fabSave.setOnClickListener {
            binding.fabSave.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            val cityName = when (val result = viewModel.currentWeather.value) {
                is WeatherResult.Success -> result.data.name
                is WeatherResult.CachedSuccess -> result.data.name
                else -> viewModel.currentCityName
            }
            if (cityName.isNotBlank()) viewModel.toggleSave(cityName)
        }
    }

    private fun observeCurrentWeather() {
        viewModel.currentWeather.observe(viewLifecycleOwner) { result ->
            binding.swipeRefresh.isRefreshing = false
            when (result) {
                is WeatherResult.Loading -> showLoading()
                is WeatherResult.Success -> {
                    stopShimmer()
                    binding.bannerOffline.visibility = View.GONE
                    showWeather(result.data)
                }
                is WeatherResult.CachedSuccess -> {
                    stopShimmer()
                    binding.bannerOffline.visibility = View.VISIBLE
                    showWeather(result.data)
                }
                is WeatherResult.Error -> {
                    stopShimmer()
                    showError(result.message)
                }
            }
        }
    }

    private fun observeForecast() {
        viewModel.forecast.observe(viewLifecycleOwner) { result ->
            val data = when (result) {
                is WeatherResult.Success -> result.data
                is WeatherResult.CachedSuccess -> result.data
                else -> null
            } ?: return@observe

            hourlyAdapter = HourlyForecastAdapter(
                viewModel.settingsManager.temperatureUnit,
                data.city.timezone
            )
            binding.recyclerHourly.adapter = hourlyAdapter
            hourlyAdapter.submitList(WeatherUtils.getHourlyForecast(data.list))
            dailyAdapter.submitList(WeatherUtils.groupForecastByDay(data.list))
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

    private fun observeUvIndex() {
        viewModel.uvIndex.observe(viewLifecycleOwner) { result ->
            when (result) {
                is WeatherResult.Success -> showUviData(result.data)
                is WeatherResult.CachedSuccess -> showUviData(result.data)
                else -> { /* stay hidden */ }
            }
        }
    }

    private fun showUviData(data: UviResponse) {
        val uvi = data.value
        val (category, color) = WeatherUtils.getUvCategory(uvi)
        binding.cardUvi.visibility = View.VISIBLE
        binding.textUviValue.text = uvi.roundToInt().toString()
        binding.textUviCategory.text = category
        binding.viewUviBarBg.setBackgroundColor(color)
        binding.textUviBar.text = uvi.roundToInt().toString()
        binding.textUviBar.setTextColor(requireContext().getColor(android.R.color.white))
    }

    private fun observeAirPollution() {
        viewModel.airPollution.observe(viewLifecycleOwner) { result ->
            when (result) {
                is WeatherResult.Success -> showAqiData(result.data)
                is WeatherResult.CachedSuccess -> showAqiData(result.data)
                else -> { /* stay hidden */ }
            }
        }
    }

    private fun showAqiData(data: AirPollutionResponse) {
        val item = data.list.firstOrNull() ?: return
        val aqi = item.main.aqi
        val (label, _) = WeatherUtils.getAqiLabel(aqi)
        binding.cardAqi.visibility = View.VISIBLE
        binding.textAqiValue.text = aqi.toString()
        binding.textAqiLabel.text = label
    }

    private fun showLoading() {
        binding.shimmerLayout.root.visibility = View.VISIBLE
        binding.shimmerLayout.shimmerContainer.startShimmer()
        binding.progressBar.visibility = View.GONE
        binding.swipeRefresh.visibility = View.GONE
        binding.errorState.visibility = View.GONE
        binding.bannerAlert.visibility = View.GONE
    }

    private fun stopShimmer() {
        binding.shimmerLayout.shimmerContainer.stopShimmer()
        binding.shimmerLayout.root.visibility = View.GONE
        binding.swipeRefresh.visibility = View.VISIBLE
        binding.progressBar.visibility = View.GONE
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

        binding.textFeelsReason.text = WeatherUtils.getFeelsLikeReason(
            weather.main.feelsLike, weather.main.temp,
            weather.main.humidity, weather.wind.speed, unit
        )

        binding.textTemperature.contentDescription =
            getString(R.string.cd_temperature, "$temp$unitSymbol")

        // Apply adaptive gradient to card background
        val isDay = WeatherUtils.isDay(weather.dt, weather.sys.sunrise, weather.sys.sunset)
        val colors = WeatherUtils.getConditionGradient(condition?.id ?: 800, isDay)
        val grad = GradientDrawable(GradientDrawable.Orientation.TL_BR, colors)
        grad.cornerRadius = resources.displayMetrics.density * 16f // 16dp matches card corner radius
        binding.weatherCardContent.background = grad

        // Load Lottie animation or fall back to Coil image
        val iconCode = condition?.icon ?: ""
        val animRes = WeatherAnimationMapper.getAnimationRes(iconCode)
        if (animRes != null) {
            binding.lottieWeatherIcon.visibility = View.VISIBLE
            binding.imageWeatherIcon.visibility = View.GONE
            binding.lottieWeatherIcon.setAnimation(animRes)
            binding.lottieWeatherIcon.playAnimation()
        } else {
            binding.lottieWeatherIcon.visibility = View.GONE
            binding.imageWeatherIcon.visibility = View.VISIBLE
            binding.imageWeatherIcon.load(WeatherUtils.getIconUrl(iconCode)) { crossfade(true) }
        }
        binding.imageWeatherIcon.contentDescription =
            getString(R.string.cd_weather_icon, condition?.description ?: "")

        // Sunrise / sunset arc
        binding.viewSunriseSunset.setTimes(
            weather.sys.sunrise,
            weather.sys.sunset,
            weather.dt,
            weather.timezone
        )

        // Stats
        binding.statHumidity.statLabel.text = getString(R.string.label_humidity)
        binding.statHumidity.statValue.text = "${weather.main.humidity}%"
        binding.statHumidity.root.contentDescription = "${getString(R.string.label_humidity)}: ${weather.main.humidity}%"

        binding.statWind.statLabel.text = getString(R.string.label_wind)
        binding.statWind.statValue.text = WeatherUtils.formatWindSpeed(weather.wind.speed, unit)
        binding.statWind.root.contentDescription = "${getString(R.string.label_wind)}: ${WeatherUtils.formatWindSpeed(weather.wind.speed, unit)}"

        binding.statPressure.statLabel.text = getString(R.string.label_pressure)
        binding.statPressure.statValue.text = "${weather.main.pressure} hPa"
        binding.statPressure.root.contentDescription = "${getString(R.string.label_pressure)}: ${weather.main.pressure} hPa"

        binding.statVisibility.statLabel.text = getString(R.string.label_visibility)
        binding.statVisibility.statValue.text = WeatherUtils.formatVisibility(weather.visibility)
        binding.statVisibility.root.contentDescription = "${getString(R.string.label_visibility)}: ${WeatherUtils.formatVisibility(weather.visibility)}"

        binding.statWindDir.statLabel.text = getString(R.string.label_wind_direction)
        binding.statWindDir.statValue.text = WeatherUtils.getWindDirection(weather.wind.deg)
        binding.statWindDir.root.contentDescription = "${getString(R.string.label_wind_direction)}: ${WeatherUtils.getWindDirection(weather.wind.deg)}"

        binding.statWindGust.statLabel.text = getString(R.string.label_wind_gust)
        val gustText = weather.wind.gust?.let { WeatherUtils.formatWindSpeed(it, unit) } ?: getString(R.string.label_na)
        binding.statWindGust.statValue.text = gustText
        binding.statWindGust.root.contentDescription = "${getString(R.string.label_wind_gust)}: $gustText"

        showAlertBanner(condition?.id, weather)
    }

    private fun showAlertBanner(conditionId: Int?, weather: CurrentWeatherResponse) {
        if (conditionId == null) {
            binding.bannerAlert.visibility = View.GONE
            return
        }
        val (message, colorRes) = getAlertInfo(conditionId) ?: run {
            binding.bannerAlert.visibility = View.GONE
            return
        }
        binding.bannerAlert.setBackgroundColor(requireContext().getColor(colorRes))
        binding.textAlert.text = message
        binding.bannerAlert.visibility = View.VISIBLE

        // Schedule WorkManager notification
        val cityName = weather.name
        if (cityName.isNotBlank() && message.isNotEmpty()) {
            val workData = workDataOf("city" to cityName, "message" to message.take(100))
            val request = OneTimeWorkRequestBuilder<WeatherAlertWorker>()
                .setInitialDelay(30, TimeUnit.MINUTES)
                .setInputData(workData)
                .addTag("alert_$cityName")
                .build()
            WorkManager.getInstance(requireContext())
                .enqueueUniqueWork("alert_$cityName", ExistingWorkPolicy.REPLACE, request)
        }
    }

    private fun getAlertInfo(conditionId: Int): Pair<String, Int>? = when (conditionId) {
        in 200..299 -> "Severe Thunderstorm Warning — Seek shelter. Lightning and damaging winds possible." to R.color.alert_warning
        502, 503, 504, 511, 522, 531 -> "Heavy Rain Warning — Flash flooding possible. Exercise caution on roads." to R.color.alert_warning
        in 600..622 -> "Snow Alert — Hazardous road conditions expected." to R.color.alert_info
        in 700..781 -> "Visibility Warning — Dense fog or hazardous atmosphere. Drive with care." to R.color.alert_info
        900, 901, 902, 905, 906 -> "Extreme Weather Alert — Dangerous conditions. Remain indoors." to R.color.alert_danger
        960, 961, 962 -> "Extreme Storm Warning — Life-threatening conditions. Follow official advice." to R.color.alert_danger
        else -> null
    }

    private fun showError(message: String) {
        binding.progressBar.visibility = View.GONE
        binding.swipeRefresh.visibility = View.GONE
        binding.errorState.visibility = View.VISIBLE
        binding.bannerAlert.visibility = View.GONE
        binding.textError.text = message
        binding.btnRetry.setOnClickListener { viewModel.refreshCurrentWeather() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
