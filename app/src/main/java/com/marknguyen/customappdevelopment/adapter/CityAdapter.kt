package com.marknguyen.customappdevelopment.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.marknguyen.customappdevelopment.databinding.ItemCityBinding
import com.marknguyen.customappdevelopment.model.CurrentWeatherResponse
import com.marknguyen.customappdevelopment.util.WeatherUtils
import kotlin.math.roundToInt

class CityAdapter(
    private val unit: String,
    private val onCityClick: (String) -> Unit
) : ListAdapter<CurrentWeatherResponse, CityAdapter.CityViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CityViewHolder {
        val binding = ItemCityBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CityViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CityViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CityViewHolder(private val binding: ItemCityBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(weather: CurrentWeatherResponse) {
            val condition = weather.weather.firstOrNull()
            val iconCode = condition?.icon ?: "01d"
            val description = condition?.description?.replaceFirstChar { it.uppercase() } ?: ""
            val temp = weather.main.temp.roundToInt()
            val unitSymbol = if (unit == "metric") "°C" else "°F"
            val min = weather.main.tempMin.roundToInt()
            val max = weather.main.tempMax.roundToInt()

            binding.textCityName.text = weather.name
            binding.textCondition.text = description
            binding.textTemperature.text = "$temp°"
            binding.textMinMax.text = "↓$min° ↑$max°"

            binding.imageWeatherIcon.load(WeatherUtils.getIconUrl(iconCode)) {
                crossfade(true)
            }

            // Accessibility: announce full weather info when item is focused
            binding.root.contentDescription =
                "${weather.name}, $description, $temp$unitSymbol, min $min$unitSymbol, max $max$unitSymbol"

            binding.root.setOnClickListener { onCityClick(weather.name) }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<CurrentWeatherResponse>() {
            override fun areItemsTheSame(old: CurrentWeatherResponse, new: CurrentWeatherResponse) =
                old.name == new.name

            override fun areContentsTheSame(old: CurrentWeatherResponse, new: CurrentWeatherResponse) =
                old == new
        }
    }
}
