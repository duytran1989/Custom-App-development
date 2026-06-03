package com.marknguyen.customappdevelopment.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.marknguyen.customappdevelopment.databinding.ItemDailyForecastBinding
import com.marknguyen.customappdevelopment.model.DailyForecast
import com.marknguyen.customappdevelopment.util.WeatherUtils
import kotlin.math.roundToInt

class DailyForecastAdapter(
    private val unit: String
) : RecyclerView.Adapter<DailyForecastAdapter.DayViewHolder>() {

    private val items = mutableListOf<DailyForecast>()

    fun submitList(newItems: List<DailyForecast>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val binding = ItemDailyForecastBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return DayViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class DayViewHolder(private val binding: ItemDailyForecastBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DailyForecast) {
            val unitSymbol = if (unit == "metric") "°C" else "°F"
            val min = item.tempMin.roundToInt()
            val max = item.tempMax.roundToInt()

            binding.textDay.text = item.dayName
            binding.textDate.text = item.date
            binding.textCondition.text = item.condition
            binding.textTempMin.text = "$min°"
            binding.textTempMax.text = "$max°"
            binding.imageIcon.load(WeatherUtils.getIconUrl(item.iconCode)) { crossfade(true) }

            // Accessibility: announce full day forecast
            binding.root.contentDescription =
                "${item.dayName} ${item.date}, ${item.condition}, low $min$unitSymbol, high $max$unitSymbol"
        }
    }
}
