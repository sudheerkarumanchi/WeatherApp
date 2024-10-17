package com.example.weatherapp.ui.viewmodels

import android.annotation.SuppressLint
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import retrofit2.HttpException

import com.example.weatherapp.model.WeatherResponse
import com.example.weatherapp.repository.WeatherRepository
import com.example.weatherapp.utils.Constants

class WeatherViewModel(private val repository: WeatherRepository) : ViewModel() {
    private val _weatherData = MutableLiveData<WeatherResponse>()
    val weatherData: LiveData<WeatherResponse> get() = _weatherData

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> get() = _errorMessage
    private val TAG = "WeatherViewModel"

    @SuppressLint("CheckResult")
    fun fetchWeather(city: String) {
        repository.getWeatherByCity(city, Constants.API_KEY)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ response ->
                Log.d(TAG, "Weather data received: $response")
                _weatherData.value = response
                _errorMessage.value = null
            }, { error ->
                Log.e(TAG, "Error fetching weather data: ${error.message}", error)
                if (error is HttpException) {
                    val errorResponse = error.response()?.errorBody()?.string()
                    Log.e(TAG, "HTTP Error Response: $errorResponse")
                    _errorMessage.value = "Error ${error.code()}: ${error.message()}"
                } else {
                    _errorMessage.value = error.message ?: "An unknown error occurred"
                }
            })
    }
}
