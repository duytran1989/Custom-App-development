package com.marknguyen.customappdevelopment.util

import android.content.Context

class SearchHistoryManager(context: Context) {
    private val prefs = context.getSharedPreferences("search_history", Context.MODE_PRIVATE)
    private val KEY = "history"
    private val MAX = 5

    fun addSearch(city: String) {
        val current = getHistory().toMutableList()
        current.remove(city)
        current.add(0, city)
        prefs.edit().putString(KEY, current.take(MAX).joinToString("|")).apply()
    }

    fun getHistory(): List<String> {
        val stored = prefs.getString(KEY, "") ?: ""
        return if (stored.isBlank()) emptyList() else stored.split("|").filter { it.isNotBlank() }
    }

    fun clear() = prefs.edit().remove(KEY).apply()
}
