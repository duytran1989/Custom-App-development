package com.marknguyen.customappdevelopment.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.marknguyen.customappdevelopment.databinding.ItemHourlyForecastBinding
import com.marknguyen.customappdevelopment.model.ForecastItem
import com.marknguyen.customappdevelopment.util.WeatherUtils
import kotlin.math.roundToInt

class HourlyForecastAdapter(
    private val unit: String,
    private val timezoneOffsetSeconds: Int
) : RecyclerView.Adapter<HourlyForecastAdapter.HourViewHolder>() {

    private val items = mutableListOf<ForecastItem>()

    fun submitList(newItems: List<ForecastItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HourViewHolder {
        val binding = ItemHourlyForecastBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return HourViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HourViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class HourViewHolder(private val binding: ItemHourlyForecastBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ForecastItem) {
            val condition = item.weather.firstOrNull()
            val iconCode = condition?.icon ?: "01d"
            val description = condition?.description ?: ""
            val temp = item.main.temp.roundToInt()
            val unitSymbol = if (unit == "metric") "°C" else "°F"
            val timeLabel = WeatherUtils.formatHour(item.dt, timezoneOffsetSeconds)

            binding.textTime.text = timeLabel
            binding.textTemp.text = "$temp°"
            binding.imageIcon.load(WeatherUtils.getIconUrl(iconCode)) { crossfade(true) }

            // Accessibility: full description for screen readers
            binding.root.contentDescription = "$timeLabel, $description, $temp$unitSymbol"
        }
    }
}
