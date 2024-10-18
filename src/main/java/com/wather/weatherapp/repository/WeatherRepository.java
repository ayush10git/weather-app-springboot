package com.wather.weatherapp.repository;

import com.wather.weatherapp.model.WeatherSummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WeatherRepository extends JpaRepository<WeatherSummary, Long> {
    Optional<WeatherSummary> findFirstByCityAndDate(String city, LocalDate date);
    List<WeatherSummary> findByCityAndDate(String city, LocalDate date); // Method to handle multiple results
    List<WeatherSummary> findByDate(LocalDate date);
    List<WeatherSummary> findByCityAndDateBetween(String city, LocalDate startDate, LocalDate endDate); // Corrected method name
}
