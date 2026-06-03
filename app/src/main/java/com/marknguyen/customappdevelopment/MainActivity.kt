package com.marknguyen.customappdevelopment

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.marknguyen.customappdevelopment.databinding.ActivityMainBinding
import com.marknguyen.customappdevelopment.model.CurrentWeatherResponse
import com.marknguyen.customappdevelopment.repository.WeatherResult
import com.marknguyen.customappdevelopment.util.SettingsManager
import com.marknguyen.customappdevelopment.viewmodel.WeatherViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private val viewModel: WeatherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply saved dark mode preference before super.onCreate
        val settings = SettingsManager(applicationContext)
        if (settings.isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Top-level destinations don't show the Up button
        val appBarConfig = AppBarConfiguration(
            setOf(
                R.id.homeFragment,
                R.id.savedFragment,
                R.id.mapFragment,
                R.id.settingsFragment,
                R.id.onboardingFragment
            )
        )
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfig)
        binding.bottomNavigation.setupWithNavController(navController)

        // Hide bottom nav on detail and onboarding screens
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.weatherDetailFragment, R.id.onboardingFragment -> {
                    binding.bottomNavigation.visibility = android.view.View.GONE
                }
                else -> {
                    binding.bottomNavigation.visibility = android.view.View.VISIBLE
                }
            }
        }

        // Check onboarding status
        val prefs = getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        if (!prefs.getBoolean("onboarding_done", false)) {
            navController.navigate(R.id.onboardingFragment)
        }

        // Handle shortcut intents
        intent?.getStringExtra("shortcut_city")?.let { city ->
            viewModel.searchWeather(city)
            viewModel.loadForecast(city)
            navController.navigate(R.id.action_home_to_detail, Bundle().apply {
                putString("cityName", city)
            })
        }

        // Load favourites and update app shortcuts
        viewModel.loadFavouritesWeather()
        viewModel.favouriteWeather.observe(this) { results ->
            updateAppShortcuts(results)
        }
    }

    private fun updateAppShortcuts(results: List<WeatherResult<CurrentWeatherResponse>>) {
        val shortcuts = results.take(3).mapIndexedNotNull { idx, result ->
            val data = when (result) {
                is WeatherResult.Success -> result.data
                is WeatherResult.CachedSuccess -> result.data
                else -> null
            } ?: return@mapIndexedNotNull null
            ShortcutInfoCompat.Builder(this, "city_$idx")
                .setShortLabel(data.name)
                .setLongLabel(data.name)
                .setIcon(IconCompat.createWithResource(this, R.drawable.ic_location))
                .setIntent(Intent(this, MainActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    putExtra("shortcut_city", data.name)
                })
                .build()
        }
        ShortcutManagerCompat.setDynamicShortcuts(this, shortcuts)
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
