package com.marknguyen.customappdevelopment.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.marknguyen.customappdevelopment.R
import com.marknguyen.customappdevelopment.adapter.CityAdapter
import com.marknguyen.customappdevelopment.databinding.FragmentSavedBinding
import com.marknguyen.customappdevelopment.model.CurrentWeatherResponse
import com.marknguyen.customappdevelopment.repository.WeatherResult
import com.marknguyen.customappdevelopment.viewmodel.WeatherViewModel

class SavedFragment : Fragment() {

    private var _binding: FragmentSavedBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WeatherViewModel by activityViewModels()
    private lateinit var cityAdapter: CityAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSavedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupAdapter()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadFavouritesWeather()
    }

    private fun setupAdapter() {
        cityAdapter = CityAdapter(
            unit = viewModel.settingsManager.temperatureUnit,
            onCityClick = { cityName ->
                viewModel.searchWeather(cityName)
                viewModel.loadForecast(cityName)
                val args = Bundle().apply { putString("cityName", cityName) }
                findNavController().navigate(R.id.action_saved_to_detail, args)
            }
        )
        binding.recyclerSaved.adapter = cityAdapter
    }

    private fun observeViewModel() {
        viewModel.favouriteWeather.observe(viewLifecycleOwner) { results ->
            binding.progressBar.visibility = View.GONE

            val successItems = results
                .filterIsInstance<WeatherResult.Success<CurrentWeatherResponse>>()
                .map { it.data }

            cityAdapter.submitList(successItems)

            if (successItems.isEmpty()) {
                binding.emptyState.visibility = View.VISIBLE
                binding.recyclerSaved.visibility = View.GONE
            } else {
                binding.emptyState.visibility = View.GONE
                binding.recyclerSaved.visibility = View.VISIBLE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
