package com.marknguyen.customappdevelopment.util

import android.content.Context

class FavouritesManager(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getFavourites(): List<String> =
        (prefs.getStringSet(KEY_FAVOURITES, emptySet()) ?: emptySet()).toList().sorted()

    fun addFavourite(city: String) {
        val current = prefs.getStringSet(KEY_FAVOURITES, emptySet())?.toMutableSet() ?: mutableSetOf()
        current.add(city)
        prefs.edit().putStringSet(KEY_FAVOURITES, current).apply()
    }

    fun removeFavourite(city: String) {
        val current = prefs.getStringSet(KEY_FAVOURITES, emptySet())?.toMutableSet() ?: mutableSetOf()
        current.remove(city)
        prefs.edit().putStringSet(KEY_FAVOURITES, current).apply()
    }

    fun isFavourite(city: String): Boolean =
        prefs.getStringSet(KEY_FAVOURITES, emptySet())?.contains(city) == true

    companion object {
        private const val PREFS_NAME = "weather_favourites"
        private const val KEY_FAVOURITES = "favourite_cities"
    }
}
