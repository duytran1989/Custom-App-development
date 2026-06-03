package com.marknguyen.customappdevelopment.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.marknguyen.customappdevelopment.databinding.FragmentSettingsBinding
import com.marknguyen.customappdevelopment.util.SettingsManager
import com.marknguyen.customappdevelopment.viewmodel.WeatherViewModel

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WeatherViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadCurrentSettings()
        setupListeners()
    }

    private fun loadCurrentSettings() {
        val settings = viewModel.settingsManager
        if (settings.temperatureUnit == SettingsManager.UNIT_METRIC) {
            binding.radioCelsius.isChecked = true
        } else {
            binding.radioFahrenheit.isChecked = true
        }
        binding.switchDarkMode.isChecked = settings.isDarkMode
    }

    private fun setupListeners() {
        binding.radioGroupUnits.setOnCheckedChangeListener { _, checkedId ->
            val unit = when (checkedId) {
                binding.radioCelsius.id -> SettingsManager.UNIT_METRIC
                binding.radioFahrenheit.id -> SettingsManager.UNIT_IMPERIAL
                else -> SettingsManager.UNIT_METRIC
            }
            viewModel.settingsManager.temperatureUnit = unit
        }

        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            viewModel.settingsManager.isDarkMode = isChecked
            val mode = if (isChecked) {
                AppCompatDelegate.MODE_NIGHT_YES
            } else {
                AppCompatDelegate.MODE_NIGHT_NO
            }
            AppCompatDelegate.setDefaultNightMode(mode)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
