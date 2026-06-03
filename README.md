# WeatherNow

An Android weather application built for Australian users, providing real-time weather conditions, forecasts, rain radar, air quality, and severe weather alerts. Developed progressively across three implementation levels as part of a university assignment.

---

## Features

### Core (Level 1)
- Search weather for any city, defaulting to Australian locations
- Current conditions — temperature, feels like, humidity, pressure, visibility
- Wind speed, direction (compass bearing), and gust speed
- 48-hour hourly forecast (3-hour intervals)
- 5-day daily forecast with min/max temperatures
- Save/unsave favourite cities with instant access from the Saved tab
- Temperature unit toggle (°C / °F) in Settings
- Dark mode toggle

### Level 2 — Extended Functionality
- **Country code fix** — all queries append `,AU` so "Melbourne" resolves to Victoria, not Florida
- **Rain Radar map** — Leaflet.js WebView with OpenWeatherMap tile overlay; switchable layers (Rain, Clouds, Wind, Temperature)
- **Severe weather alert banner** — colour-coded banners (orange/red) for thunderstorms, heavy rain, extreme conditions
- **Offline mode** — 30-minute SharedPreferences cache; cached data served with an offline banner when the network is unavailable

### Level 3 — Professional Polish
- **Adaptive gradient backgrounds** — weather card gradient shifts with conditions (stormy charcoal, clear amber, rain blue, night navy)
- **Lottie animated icons** — six animations (sunny pulse, cloud drift, rain drops, storm flash, snow flakes, moon glow) with static fallback
- **Shimmer skeleton loading** — placeholder cards shown while data is fetching
- **Dark mode palette** — proper deep-navy overrides (`#1A1A2E` surface, `#16213E` card) instead of auto-invert
- **UV Index panel** — colour-coded Sunsmart categories (Low → Extreme) from OWM `/uvi`
- **Sunrise/Sunset arc** — custom canvas view with animated sun position and time labels
- **Feels-like explanation** — chip text explaining humidity warmth or wind chill
- **Air Quality Index (AQI)** — Good–Very Poor label from OWM `/air_pollution`
- **Location auto-search** — GPS button uses device location via `LocationManager` + `Geocoder`
- **Search history** — last 5 searches appear as chips when the field is focused
- **Haptic feedback** — save/unsave FAB tap triggers `HapticFeedbackConstants.VIRTUAL_KEY`
- **Severe alert notifications** — WorkManager schedules a notification 30 minutes after a severe banner is shown
- **Onboarding flow** — 3-page ViewPager2 intro with Lottie animations, shown once on first launch
- **App shortcuts** — long-pressing the launcher icon shows dynamic shortcuts for the top 3 saved cities
- **TalkBack accessibility** — stat rows have combined `contentDescription` for grouped announcements

---

## Screenshots

| Home | Weather Detail | Rain Radar |
|------|---------------|------------|
| *Search & favourites* | *Gradient card, Lottie icon, UV & AQI* | *Leaflet precipitation overlay* |

| Onboarding | Dark Mode | Alert Banner |
|------------|-----------|--------------|
| *3-page intro* | *Deep navy palette* | *Thunderstorm warning* |

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin |
| Architecture | MVVM + Repository pattern |
| UI | Fragments, Navigation Component, View Binding |
| Networking | Retrofit 2, OkHttp 4, Gson |
| Image loading | Coil 2 |
| Animations | Lottie 6 |
| Skeleton loading | Facebook Shimmer 0.5 |
| Background work | WorkManager 2.9 |
| Map | Leaflet.js (WebView) |
| Caching | SharedPreferences + Gson |
| Design | Material Components (MDC) |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 36 (Android 15) |

---

## Project Structure

```
app/src/main/java/com/marknguyen/customappdevelopment/
├── api/
│   └── WeatherApiService.kt        # Retrofit endpoints (weather, forecast, UVI, AQI)
├── fragment/
│   ├── HomeFragment.kt             # Search, favourites, location, history chips
│   ├── WeatherDetailFragment.kt    # Full detail view with all Level 3 panels
│   ├── SavedFragment.kt            # Saved cities list
│   ├── MapFragment.kt              # Rain radar WebView
│   ├── SettingsFragment.kt         # Units + dark mode
│   └── OnboardingFragment.kt       # First-launch 3-page intro
├── model/
│   └── WeatherModels.kt            # Data classes for all API responses
├── repository/
│   └── WeatherRepository.kt        # Network + cache fallback logic
├── viewmodel/
│   └── WeatherViewModel.kt         # LiveData, coroutines, UVI/AQI parallel fetch
├── adapter/
│   ├── CityAdapter.kt
│   ├── HourlyForecastAdapter.kt
│   └── DailyForecastAdapter.kt
├── ui/
│   └── SunriseSunsetView.kt        # Custom canvas arc view
├── util/
│   ├── Constants.kt
│   ├── WeatherUtils.kt             # Formatting, gradients, UVI/AQI categories
│   ├── WeatherCacheManager.kt      # SharedPreferences JSON cache
│   ├── WeatherAnimationMapper.kt   # Maps OWM icon codes to Lottie raw resources
│   ├── SearchHistoryManager.kt     # Last-5-searches persistence
│   ├── FavouritesManager.kt
│   └── SettingsManager.kt
└── worker/
    └── WeatherAlertWorker.kt       # WorkManager notification worker
```

---

## Getting Started

### Prerequisites
- Android Studio Hedgehog or newer
- Android device or emulator running API 26+
- A free [OpenWeatherMap API key](https://openweathermap.org/api)

### Setup

1. Clone the repository:
   ```bash
   git clone https://github.com/duytran1989/Custom-App-development.git
   cd Custom-App-development
   ```

2. Open the project in Android Studio.

3. Replace the API key in `app/src/main/java/com/marknguyen/customappdevelopment/util/Constants.kt`:
   ```kotlin
   const val API_KEY = "YOUR_API_KEY_HERE"
   ```

4. Sync Gradle and run on a device or emulator.

### API Endpoints Used

| Endpoint | Purpose |
|----------|---------|
| `GET /data/2.5/weather?q={city},AU` | Current weather |
| `GET /data/2.5/forecast?q={city},AU` | 5-day / 3-hour forecast |
| `GET /data/2.5/uvi?lat={lat}&lon={lon}` | UV Index |
| `GET /data/2.5/air_pollution?lat={lat}&lon={lon}` | Air Quality Index |
| `GET /map/precipitation_new/{z}/{x}/{y}.png` | Rain radar tiles |

All endpoints use the [OpenWeatherMap free tier](https://openweathermap.org/price) (60 calls/minute).

---

## Architecture

```
View (Fragment)
    │  observes LiveData
    ▼
ViewModel (AndroidViewModel)
    │  calls suspend functions
    ▼
Repository
    ├── Network (Retrofit → OWM API)
    └── Cache (WeatherCacheManager → SharedPreferences)
```

- **Sealed class `WeatherResult<T>`** — `Success`, `CachedSuccess`, `Error`, `Loading` — threads state cleanly from repository to UI without null checks.
- **Offline fallback** — on `IOException`, the repository checks the 30-minute SharedPreferences cache and returns `CachedSuccess` if valid data exists.
- **Parallel fetches** — UV Index and AQI are launched in parallel `viewModelScope` coroutines after the main weather response arrives.

---

## Permissions

| Permission | Reason |
|------------|--------|
| `INTERNET` | Fetch weather data and load radar map tiles |
| `ACCESS_COARSE_LOCATION` | Location auto-search feature |
| `POST_NOTIFICATIONS` | Severe weather WorkManager notifications (Android 13+) |

---

## Dependencies

```toml
# gradle/libs.versions.toml
lottie           = "6.4.0"
shimmer          = "0.5.0"
work             = "2.9.1"
retrofit         = "2.11.0"
okhttp           = "4.12.0"
coil             = "2.7.0"
material         = "1.12.0"
navigationFragment = "2.8.4"
coroutines       = "1.8.0"
```

---

## Branch History

| Branch | Description |
|--------|-------------|
| `master` | Stable merged codebase |
| `Extension-level2` | Level 2 features (radar, alerts, offline cache) |
| `Extension-level3` | Level 3 polish (gradients, Lottie, UV, AQI, onboarding) |

---

## Author

**Mark Nguyen** — [duytran1989](https://github.com/duytran1989)

---

## Acknowledgements

- [OpenWeatherMap](https://openweathermap.org/) — weather data and map tile APIs
- [LottieFiles](https://lottiefiles.com/) — animation format and tooling
- [Leaflet.js](https://leafletjs.com/) — interactive radar map
- [OpenStreetMap](https://www.openstreetmap.org/) — base map tiles
