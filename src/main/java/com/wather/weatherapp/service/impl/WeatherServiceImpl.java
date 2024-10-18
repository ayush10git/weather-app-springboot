package com.wather.weatherapp.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wather.weatherapp.model.WeatherSummary;
import com.wather.weatherapp.repository.WeatherRepository;
import com.wather.weatherapp.service.WeatherService;
import jakarta.annotation.PostConstruct;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class WeatherServiceImpl implements WeatherService {

    @Value("${openweathermap.api.key}")
    private String apiKey;

    private final WeatherRepository weatherRepository;
    private final RestTemplate restTemplate;
    private static final Logger logger = LoggerFactory.getLogger(WeatherServiceImpl.class);

    public WeatherServiceImpl(WeatherRepository weatherRepository, RestTemplate restTemplate) {
        this.weatherRepository = weatherRepository;
        this.restTemplate = restTemplate;
    }

    @PostConstruct
    public void init() {
        // Trigger the weather fetch on server startup
        logger.info("Weather service started. Fetching initial weather data...");
        fetchWeatherData();
    }

    @Override
    @Scheduled(fixedRate = 300000) // Update weather data every 5 minutes
    public void fetchWeatherData() {
        List<String> cities = List.of("Delhi", "Mumbai", "Chennai", "Bangalore", "Kolkata", "Hyderabad");

        // Log message when the scheduled task is triggered
        logger.info("Fetching weather data at {}", LocalDate.now());

        for (String city : cities) {
            logger.info("Fetching weather data for city: {}", city);
            String url = "https://api.openweathermap.org/data/2.5/weather?q=" + city + "&appid=" + apiKey;

            try {
                String response = restTemplate.getForObject(url, String.class);
                logger.info("Received response for city: {}: {}", city, response);

                JsonNode rootNode = new ObjectMapper().readTree(response);
                double tempKelvin = rootNode.path("main").path("temp").asDouble();
                double tempCelsius = convertToCelsius(tempKelvin);

                checkAndAlertThreshold(city, tempCelsius);

                LocalDate today = LocalDate.now();
                Optional<WeatherSummary> existingSummary = weatherRepository.findFirstByCityAndDate(city, today);

                if (existingSummary.isEmpty()) {
                    // Save new weather summary
                    WeatherSummary newSummary = new WeatherSummary();
                    newSummary.setCity(city);
                    newSummary.setDate(today);
                    newSummary.setAvgTemp(tempCelsius);
                    weatherRepository.save(newSummary);
                    logger.info("Saved new weather summary for city: {}", city);
                } else {
                    // Update existing weather summary
                    WeatherSummary existing = existingSummary.get();
                    existing.setAvgTemp(tempCelsius);
                    weatherRepository.save(existing);
                    logger.info("Updated weather summary for city: {}", city);
                }

            } catch (Exception e) {
                logger.error("Failed to fetch or process weather data for city: {}", city, e);
            }
        }
    }

    // Method to check and alert when the temperature exceeds threshold
    private void checkAndAlertThreshold(String cityName, double currentTemperature) {
        double threshold = 32;  // Example threshold
        if (currentTemperature > threshold) {
            String alertMessage = String.format("ALERT! Temperature in %s is now %.2f°C, exceeding the threshold of %.2f°C.", cityName, currentTemperature, threshold);
            logger.warn(alertMessage);
        }
    }

    @Scheduled(fixedRate = 86400000)
    @Override
    public void aggregateDailyWeatherData() {
        List<String> cities = List.of("Delhi", "Mumbai", "Chennai", "Bangalore", "Kolkata", "Hyderabad");
        LocalDate today = LocalDate.now();

        // Aggregate daily weather data for each city
        for (String city : cities) {
            List<WeatherSummary> dailyWeatherData = weatherRepository.findByCityAndDate(city, today);

            if (!dailyWeatherData.isEmpty()) {
                double avgTemp = dailyWeatherData.stream().mapToDouble(WeatherSummary::getAvgTemp).average().orElse(0);
                double maxTemp = dailyWeatherData.stream().mapToDouble(WeatherSummary::getMaxTemp).max().orElse(Double.MIN_VALUE);
                double minTemp = dailyWeatherData.stream().mapToDouble(WeatherSummary::getMinTemp).min().orElse(Double.MAX_VALUE);

                // Dominant weather condition (based on most frequent condition)
                Map<String, Long> conditionFrequency = dailyWeatherData.stream()
                        .collect(Collectors.groupingBy(WeatherSummary::getDominantCondition, Collectors.counting()));
                String dominantCondition = conditionFrequency.entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .map(Map.Entry::getKey)
                        .orElse("Unknown");

                // Save the aggregated daily data
                WeatherSummary dailySummary = new WeatherSummary();
                dailySummary.setCity(city);
                dailySummary.setDate(today);
                dailySummary.setAvgTemp(avgTemp);
                dailySummary.setMaxTemp(maxTemp);
                dailySummary.setMinTemp(minTemp);
                dailySummary.setDominantCondition(dominantCondition);

                weatherRepository.save(dailySummary);
                logger.info("Aggregated daily weather for city: {}", city);
            }
        }
    }

    @Override
    public List<WeatherSummary> getWeatherSummariesByDateRange(String city, LocalDate startDate, LocalDate endDate) {
        return weatherRepository.findByCityAndDateBetween(city, startDate, endDate);
    }

    @Override
    public WeatherSummary saveWeatherSummary(WeatherSummary weatherSummary) {
        return weatherRepository.save(weatherSummary);
    }

    @Override
    public List<WeatherSummary> getAllWeatherSummaries() {
        return weatherRepository.findAll();
    }

    @Override
    public WeatherSummary getWeatherSummaryByCityAndDate(String city, String date) {
        LocalDate localDate = LocalDate.parse(date);
        List<WeatherSummary> summaries = weatherRepository.findByCityAndDate(city, localDate);

        return summaries.isEmpty() ? null : summaries.get(0);
    }

    @Override
    public WeatherSummary getWeatherSummaryByCity(String city) {
        LocalDate today = LocalDate.now();
        List<WeatherSummary> summaries = weatherRepository.findByCityAndDate(city, today);
        return summaries.isEmpty() ? null : summaries.get(0);
    }

    @Override
    public List<WeatherSummary> getWeatherSummariesByDate(String date) {
        LocalDate localDate = LocalDate.parse(date);
        return weatherRepository.findByDate(localDate);
    }

    // Convert temperature from Kelvin to Celsius
    private double convertToCelsius(double kelvin) {
        return kelvin - 273.15;
    }
}
