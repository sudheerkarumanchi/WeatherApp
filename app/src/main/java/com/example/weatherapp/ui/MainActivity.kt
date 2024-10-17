package com.example.weatherapp.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

import com.example.weatherapp.service.WeatherApiService
import com.example.weatherapp.repository.WeatherRepository
import com.example.weatherapp.ui.viewmodels.WeatherViewModel
import com.example.weatherapp.ui.viewmodels.WeatherViewModelFactory
import com.example.weatherapp.model.WeatherResponse
import com.example.weatherapp.R

class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"
    private lateinit var tvTemperature: TextView
    private lateinit var tvDescription: TextView
    private lateinit var ivWeatherIcon: ImageView

    private lateinit var weatherViewModel: WeatherViewModel
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val LOCATION_PERMISSION_REQUEST_CODE = 1000

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        tvTemperature = findViewById(R.id.textViewTemperature)
        tvDescription = findViewById(R.id.textViewDescription)
        ivWeatherIcon = findViewById(R.id.imageViewWeatherIcon)

        sharedPreferences = getSharedPreferences("WeatherApp", Context.MODE_PRIVATE)

        // Initialize Retrofit
        val weatherApiService = Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org/")
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build()
            .create(WeatherApiService::class.java)

        // Initialize Repository
        val repository = WeatherRepository(weatherApiService)

        // Use ViewModelFactory to create WeatherViewModel
        val viewModelFactory = WeatherViewModelFactory(repository)
        weatherViewModel = ViewModelProvider(this, viewModelFactory)[WeatherViewModel::class.java]

        // Observe LiveData
        weatherViewModel.weatherData.observe(this) { weatherResponse ->
            Log.d(TAG, "Weather data received: $weatherResponse")
            updateUI(weatherResponse)
            saveToPreferences(weatherResponse)  // Save response to shared preferences
        }

        weatherViewModel.errorMessage.observe(this) { errorMessage ->
            if (errorMessage != null) {
                tvDescription.text = errorMessage
                tvTemperature.text = ""
                ivWeatherIcon.setImageDrawable(null)
            }
        }

        loadSavedWeather()

        findViewById<Button>(R.id.buttonSearch).setOnClickListener {
            val city = findViewById<EditText>(R.id.editTextCity).text.toString()
            Log.d(TAG, "Fetching weather for city: $city")

            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                weatherViewModel.fetchWeather(city)
            } else {
                // Request location permission
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, fetch weather data
                val city = findViewById<EditText>(R.id.editTextCity).text.toString()
                weatherViewModel.fetchWeather(city)
            } else {
                Log.d(TAG, "Location permission denied")
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateUI(weatherResponse: WeatherResponse) {
        val temperature = weatherResponse.main.temp - 273.15
        val description = weatherResponse.weather[0].description
        val iconCode = weatherResponse.weather[0].icon

        val iconUrl = "https://openweathermap.org/img/wn/$iconCode@2x.png"
        Log.d(TAG, "Weather icon URL: $iconUrl")

        // Updating TextViews
        tvTemperature.text = "${"%.2f".format(temperature)} °C"
        tvDescription.text = description

        // Load the weather icon using Glide
        Glide.with(this)
            .load(iconUrl)
            .into(ivWeatherIcon)
    }

    private fun saveToPreferences(weatherResponse: WeatherResponse) {
        val editor = sharedPreferences.edit()
        val cityName = findViewById<EditText>(R.id.editTextCity).text.toString()
        editor.putString("city_name", cityName)
        editor.putString("weather_response", Gson().toJson(weatherResponse))
        editor.apply()
    }

    private fun loadSavedWeather() {
        val cityName = sharedPreferences.getString("city_name", null)
        val weatherResponseJson = sharedPreferences.getString("weather_response", null)

        if (cityName != null && weatherResponseJson != null) {
            val weatherResponse = Gson().fromJson(weatherResponseJson, WeatherResponse::class.java)
            updateUI(weatherResponse)
            findViewById<EditText>(R.id.editTextCity).setText(cityName)
        }
    }
}
