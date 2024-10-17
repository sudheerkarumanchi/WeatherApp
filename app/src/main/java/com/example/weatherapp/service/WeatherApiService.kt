package com.example.weatherapp.service

import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Query

import com.example.weatherapp.model.WeatherResponse
import com.example.weatherapp.utils.Constants

interface WeatherApiService {
    @GET("data/2.5/weather")
    fun getWeatherByCity(
        @Query("q") city: String,
        @Query("appid") apiKey: String = Constants.API_KEY
    ): Single<WeatherResponse>
}
