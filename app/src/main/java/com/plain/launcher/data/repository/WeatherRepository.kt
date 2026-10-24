package com.plain.launcher.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.roundToInt

class WeatherRepository(private val context: Context) {

    suspend fun fetchWeather(
        city: String = "Auto (GPS)",
        temperatureUnit: String = "celsius"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val coords = resolveCoordinates(city)
                ?: return@withContext Result.failure(Exception("Could not determine coordinates for: $city"))

            val tempUnitParam = if (temperatureUnit.equals("fahrenheit", ignoreCase = true)) {
                "fahrenheit"
            } else {
                "celsius"
            }

            val urlString = "https://api.open-meteo.com/v1/forecast?latitude=${coords.first}&longitude=${coords.second}&current=temperature_2m,weather_code&temperature_unit=$tempUnitParam"
            val jsonStr = httpGet(urlString) ?: return@withContext Result.failure(Exception("Failed to fetch weather data"))

            val root = JSONObject(jsonStr)
            if (!root.has("current")) {
                return@withContext Result.failure(Exception("Invalid weather response"))
            }

            val current = root.getJSONObject("current")
            val temp = current.getDouble("temperature_2m").roundToInt()
            val code = current.getInt("weather_code")
            val condition = mapWmoCodeToCondition(code)

            Result.success("${temp}° $condition")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun resolveCoordinates(city: String): Pair<Double, Double>? {
        if (city.startsWith("Auto", ignoreCase = true)) {
            val loc = getDeviceLocation()
            if (loc != null) {
                return Pair(loc.latitude, loc.longitude)
            }
            // Fallback: try IP-based location
            val ipCoords = getIpCoordinates()
            if (ipCoords != null) {
                return ipCoords
            }
            // Fallback: default to London coordinates (51.5074, -0.1278) if no GPS / IP available
            return Pair(51.5074, -0.1278)
        }

        // Geocode city name via Open-Meteo Geocoding API
        return geocodeCity(city)
    }

    @SuppressLint("MissingPermission")
    private fun getDeviceLocation(): Location? {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        val providers = listOf(
            LocationManager.NETWORK_PROVIDER,
            LocationManager.GPS_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )

        for (provider in providers) {
            try {
                if (locationManager.isProviderEnabled(provider)) {
                    val loc = locationManager.getLastKnownLocation(provider)
                    if (loc != null) return loc
                }
            } catch (e: SecurityException) {
                // Permission not granted or missing
            } catch (e: Exception) {
                // Provider unavailable
            }
        }
        return null
    }

    private fun getIpCoordinates(): Pair<Double, Double>? {
        return try {
            val json = httpGet("https://ipapi.co/json/") ?: return null
            val obj = JSONObject(json)
            val lat = obj.optDouble("latitude", Double.NaN)
            val lon = obj.optDouble("longitude", Double.NaN)
            if (!lat.isNaN() && !lon.isNaN()) {
                Pair(lat, lon)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun geocodeCity(cityName: String): Pair<Double, Double>? = withContext(Dispatchers.IO) {
        try {
            val encoded = URLEncoder.encode(cityName.trim(), "UTF-8")
            val urlString = "https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=1&language=en&format=json"
            val json = httpGet(urlString) ?: return@withContext null
            val obj = JSONObject(json)
            val results = obj.optJSONArray("results")
            if (results != null && results.length() > 0) {
                val first = results.getJSONObject(0)
                val lat = first.getDouble("latitude")
                val lon = first.getDouble("longitude")
                Pair(lat, lon)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun httpGet(urlString: String): String? {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL(urlString)
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("User-Agent", "PlainLauncher/1.0")
            }
            if (conn.responseCode in 200..299) {
                BufferedReader(InputStreamReader(conn.inputStream)).use { reader ->
                    reader.readText()
                }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun mapWmoCodeToCondition(code: Int): String {
        return when (code) {
            0 -> "clear"
            1, 2 -> "partly cloudy"
            3 -> "overcast"
            45, 48 -> "foggy"
            51, 53, 55 -> "drizzle"
            56, 57 -> "freezing drizzle"
            61, 63, 65 -> "rain"
            66, 67 -> "freezing rain"
            71, 73, 75, 77 -> "snow"
            80, 81, 82 -> "rain showers"
            85, 86 -> "snow showers"
            95 -> "thunderstorm"
            96, 99 -> "thunderstorm with hail"
            else -> "cloudy"
        }
    }
}
