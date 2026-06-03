package com.marknguyen.customappdevelopment.fragment

import android.Manifest
import android.location.Geocoder
import android.location.LocationManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.marknguyen.customappdevelopment.R
import com.marknguyen.customappdevelopment.adapter.CityAdapter
import com.marknguyen.customappdevelopment.databinding.FragmentHomeBinding
import com.marknguyen.customappdevelopment.model.CurrentWeatherResponse
import com.marknguyen.customappdevelopment.repository.WeatherResult
import com.marknguyen.customappdevelopment.util.SearchHistoryManager
import com.marknguyen.customappdevelopment.viewmodel.WeatherViewModel
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WeatherViewModel by activityViewModels()
    private lateinit var cityAdapter: CityAdapter
    private lateinit var searchHistoryManager: SearchHistoryManager

    // Tracks whether the current weather observer should trigger navigation
    private var pendingNavigation = false

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            searchByLocation()
        } else {
            Snackbar.make(binding.root, getString(R.string.label_locate_me) + " permission denied", Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        searchHistoryManager = SearchHistoryManager(requireContext())
        setupAdapter()
        setupSearch()
        setupLocationButton()
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
        binding.searchEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                loadHistoryChips()
            }
        }

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

    private fun setupLocationButton() {
        binding.searchInputLayout.setEndIconOnClickListener {
            val permission = Manifest.permission.ACCESS_COARSE_LOCATION
            if (ContextCompat.checkSelfPermission(requireContext(), permission) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                searchByLocation()
            } else {
                locationPermissionLauncher.launch(permission)
            }
        }
    }

    private fun searchByLocation() {
        val locationManager = requireContext().getSystemService(android.content.Context.LOCATION_SERVICE) as LocationManager
        try {
            val location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if (location != null) {
                val geocoder = Geocoder(requireContext(), Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                val cityName = addresses?.firstOrNull()?.locality
                    ?: addresses?.firstOrNull()?.subAdminArea
                if (!cityName.isNullOrBlank()) {
                    binding.searchEditText.setText(cityName)
                    pendingNavigation = true
                    hideKeyboard()
                    viewModel.searchWeather(cityName)
                    viewModel.loadForecast(cityName)
                } else {
                    Snackbar.make(binding.root, "Could not determine city name", Snackbar.LENGTH_SHORT).show()
                }
            } else {
                Snackbar.make(binding.root, "Location not available", Snackbar.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Snackbar.make(binding.root, "Location error: ${e.localizedMessage}", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun loadHistoryChips() {
        val history = searchHistoryManager.getHistory()
        if (history.isEmpty()) {
            binding.scrollHistory.visibility = View.GONE
            return
        }
        binding.chipGroupHistory.removeAllViews()
        history.forEach { city ->
            val chip = Chip(requireContext()).apply {
                text = city
                isCheckable = false
                setOnClickListener {
                    binding.searchEditText.setText(city)
                    pendingNavigation = true
                    hideKeyboard()
                    viewModel.searchWeather(city)
                    viewModel.loadForecast(city)
                    binding.scrollHistory.visibility = View.GONE
                }
            }
            binding.chipGroupHistory.addView(chip)
        }
        binding.scrollHistory.visibility = View.VISIBLE
    }

    private fun observeViewModel() {
        viewModel.currentWeather.observe(viewLifecycleOwner) { result ->
            when (result) {
                is WeatherResult.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is WeatherResult.Success -> {
                    binding.progressBar.visibility = View.GONE
                    searchHistoryManager.addSearch(result.data.name)
                    if (pendingNavigation) {
                        pendingNavigation = false
                        navigateToDetail(result.data.name)
                    }
                }
                is WeatherResult.CachedSuccess -> {
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
                .mapNotNull { result ->
                    when (result) {
                        is WeatherResult.Success -> result.data
                        is WeatherResult.CachedSuccess -> result.data
                        else -> null
                    }
                }

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
        binding.scrollHistory.visibility = View.GONE
        val imm = getSystemService(requireContext(), InputMethodManager::class.java)
        imm?.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
