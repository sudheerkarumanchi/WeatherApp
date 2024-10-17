package com.example.weatherapp.repository

import io.reactivex.Single

import com.example.weatherapp.service.WeatherApiService
import com.example.weatherapp.model.WeatherResponse

class WeatherRepository(private val weatherApiService: WeatherApiService) {
    fun getWeatherByCity(city: String, apiKey: String): Single<WeatherResponse> {
        return weatherApiService.getWeatherByCity(city, apiKey)
    }
}
