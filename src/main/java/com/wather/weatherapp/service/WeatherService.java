package com.wather.weatherapp.service;

import com.wather.weatherapp.model.WeatherSummary;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDate;
import java.util.List;

public interface WeatherService {
    void fetchWeatherData();

    @Scheduled(fixedRate = 86400000) // Run once a day
    void aggregateDailyWeatherData();

    List<WeatherSummary> getWeatherSummariesByDateRange(String city, LocalDate startDate, LocalDate endDate);

    WeatherSummary saveWeatherSummary(WeatherSummary weatherSummary);
    List<WeatherSummary> getAllWeatherSummaries();
    WeatherSummary getWeatherSummaryByCityAndDate(String city, String date);

    WeatherSummary getWeatherSummaryByCity(String city);

    List<WeatherSummary> getWeatherSummariesByDate(String date);
}
