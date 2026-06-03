package com.marknguyen.customappdevelopment.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import com.marknguyen.customappdevelopment.databinding.FragmentMapBinding
import com.marknguyen.customappdevelopment.util.Constants

class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    // Active OWM layer name — swapped by chips without recreating the WebView
    private var activeLayer = "precipitation_new"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupWebView()
        setupChips()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        binding.webviewMap.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    _binding?.mapLoading?.visibility = View.GONE
                }
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?) = false
            }
            loadDataWithBaseURL(
                "https://openweathermap.org",
                buildMapHtml(activeLayer),
                "text/html",
                "UTF-8",
                null
            )
        }
    }

    private fun setupChips() {
        binding.chipPrecipitation.setOnCheckedChangeListener { _, checked ->
            if (checked) switchLayer("precipitation_new")
        }
        binding.chipClouds.setOnCheckedChangeListener { _, checked ->
            if (checked) switchLayer("clouds_new")
        }
        binding.chipWind.setOnCheckedChangeListener { _, checked ->
            if (checked) switchLayer("wind_new")
        }
        binding.chipTemp.setOnCheckedChangeListener { _, checked ->
            if (checked) switchLayer("temp_new")
        }
    }

    private fun switchLayer(layer: String) {
        activeLayer = layer
        // Call into the already-loaded Leaflet map via JavaScript
        binding.webviewMap.evaluateJavascript("setLayer('$layer');", null)
    }

    private fun buildMapHtml(initialLayer: String): String {
        val key = Constants.API_KEY
        return """
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8"/>
  <meta name="viewport" content="width=device-width,initial-scale=1"/>
  <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"/>
  <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
  <style>html,body,#map{margin:0;padding:0;width:100%;height:100%;}</style>
</head>
<body>
<div id="map"></div>
<script>
  var map = L.map('map').setView([-25.2744, 133.7751], 4);

  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',{
    attribution:'&copy; OpenStreetMap contributors', maxZoom:18
  }).addTo(map);

  var owmLayer = L.tileLayer(
    'https://tile.openweathermap.org/map/$initialLayer/{z}/{x}/{y}.png?appid=$key',
    { opacity:0.65, attribution:'&copy; OpenWeatherMap', maxZoom:18 }
  ).addTo(map);

  function setLayer(name){
    map.removeLayer(owmLayer);
    owmLayer = L.tileLayer(
      'https://tile.openweathermap.org/map/'+name+'/{z}/{x}/{y}.png?appid=$key',
      { opacity:0.65, attribution:'&copy; OpenWeatherMap', maxZoom:18 }
    ).addTo(map);
  }
</script>
</body>
</html>
        """.trimIndent()
    }

    override fun onDestroyView() {
        binding.webviewMap.destroy()
        super.onDestroyView()
        _binding = null
    }
}
