package com.wather.weatherapp.controller;

import com.wather.weatherapp.model.WeatherSummary;
import com.wather.weatherapp.service.WeatherService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/weather")
public class WeatherController {

    private final WeatherService weatherService;

    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    // Fetch weather data for all cities (called periodically via service)
    @GetMapping("/get")
    public ResponseEntity<String> fetchWeatherData() {
        weatherService.fetchWeatherData();
        return ResponseEntity.ok("Weather data is being fetched");
    }

    // Add a new weather summary
    @PostMapping
    public ResponseEntity<WeatherSummary> createWeatherSummary(@RequestBody WeatherSummary weatherSummary) {
        WeatherSummary savedSummary = weatherService.saveWeatherSummary(weatherSummary);
        return ResponseEntity.created(URI.create("/weather/" + savedSummary.getId())).body(savedSummary);
    }

    // Get all weather summaries
    @GetMapping
    public ResponseEntity<List<WeatherSummary>> getAllWeatherSummaries() {
        List<WeatherSummary> weatherSummaries = weatherService.getAllWeatherSummaries();
        return ResponseEntity.ok(weatherSummaries);
    }

    // Get weather summary for a specific city and date
    @GetMapping("/summary/{city}/{date}")
    public ResponseEntity<WeatherSummary> getWeatherSummaryByCityAndDate(
            @PathVariable String city,
            @PathVariable String date) {
        WeatherSummary weatherSummary = weatherService.getWeatherSummaryByCityAndDate(city, date);
        return weatherSummary != null ? ResponseEntity.ok(weatherSummary) : ResponseEntity.notFound().build();
    }

    // Get weather summary for a specific city (today's weather)
    @GetMapping("/summary/{city}")
    public ResponseEntity<WeatherSummary> getWeatherSummaryByCity(@PathVariable String city) {
        WeatherSummary weatherSummary = weatherService.getWeatherSummaryByCity(city);
        return weatherSummary != null ? ResponseEntity.ok(weatherSummary) : ResponseEntity.notFound().build();
    }

    @GetMapping("/historical-summary/{city}")
    public ResponseEntity<List<WeatherSummary>> getHistoricalWeatherSummary(@PathVariable String city,
                                                                            @RequestParam String startDate,
                                                                            @RequestParam String endDate) {
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);

        List<WeatherSummary> weatherSummaries = weatherService.getWeatherSummariesByDateRange(city, start, end);

        return weatherSummaries.isEmpty() ? ResponseEntity.notFound().build() : ResponseEntity.ok(weatherSummaries);
    }
}
