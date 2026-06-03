package com.marknguyen.customappdevelopment.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.marknguyen.customappdevelopment.R
import com.marknguyen.customappdevelopment.adapter.CityAdapter
import com.marknguyen.customappdevelopment.databinding.FragmentHomeBinding
import com.marknguyen.customappdevelopment.model.CurrentWeatherResponse
import com.marknguyen.customappdevelopment.repository.WeatherResult
import com.marknguyen.customappdevelopment.viewmodel.WeatherViewModel

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WeatherViewModel by activityViewModels()
    private lateinit var cityAdapter: CityAdapter

    // Tracks whether the current weather observer should trigger navigation
    private var pendingNavigation = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupAdapter()
        setupSearch()
        observeViewModel()
        viewModel.loadFavouritesWeather()
    }

    private fun setupAdapter() {
        cityAdapter = CityAdapter(
            unit = viewModel.settingsManager.temperatureUnit,
            onCityClick = { cityName ->
                viewModel.searchWeather(cityName)
                viewModel.loadForecast(cityName)
                navigateToDetail(cityName)
            }
        )
        binding.recyclerCities.adapter = cityAdapter
    }

    private fun setupSearch() {
        binding.searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = binding.searchEditText.text?.toString()?.trim() ?: ""
                if (query.isNotEmpty()) {
                    pendingNavigation = true
                    hideKeyboard()
                    viewModel.searchWeather(query)
                    viewModel.loadForecast(query)
                }
                true
            } else false
        }
    }

    private fun observeViewModel() {
        viewModel.currentWeather.observe(viewLifecycleOwner) { result ->
            when (result) {
                is WeatherResult.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is WeatherResult.Success -> {
                    binding.progressBar.visibility = View.GONE
                    if (pendingNavigation) {
                        pendingNavigation = false
                        navigateToDetail(result.data.name)
                    }
                }
                is WeatherResult.Error -> {
                    binding.progressBar.visibility = View.GONE
                    if (pendingNavigation) {
                        pendingNavigation = false
                        Snackbar.make(binding.root, result.message, Snackbar.LENGTH_LONG).show()
                    }
                }
            }
        }

        viewModel.favouriteWeather.observe(viewLifecycleOwner) { results ->
            binding.progressBar.visibility = View.GONE
            val successItems = results
                .filterIsInstance<WeatherResult.Success<CurrentWeatherResponse>>()
                .map { it.data }

            cityAdapter.submitList(successItems)

            if (successItems.isEmpty() && binding.searchEditText.text.isNullOrBlank()) {
                binding.emptyState.visibility = View.VISIBLE
                binding.recyclerCities.visibility = View.GONE
            } else {
                binding.emptyState.visibility = View.GONE
                binding.recyclerCities.visibility = View.VISIBLE
            }
        }
    }

    private fun navigateToDetail(cityName: String) {
        val args = Bundle().apply { putString("cityName", cityName) }
        findNavController().navigate(R.id.action_home_to_detail, args)
    }

    private fun hideKeyboard() {
        val imm = getSystemService(requireContext(), InputMethodManager::class.java)
        imm?.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
