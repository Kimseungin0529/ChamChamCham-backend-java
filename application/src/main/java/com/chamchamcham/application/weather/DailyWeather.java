package com.chamchamcham.application.weather;

import java.time.LocalDate;
import java.util.Objects;
import org.springframework.lang.NonNull;

public record DailyWeather(
    @NonNull LocalDate date,
    @NonNull WeatherCondition condition,
    int minTemperature,
    int maxTemperature
) {
    public DailyWeather {
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(condition, "condition");
    }

    @NonNull
    public LocalDate getDate() { return date; }
    @NonNull
    public WeatherCondition getCondition() { return condition; }
    public int getMinTemperature() { return minTemperature; }
    public int getMaxTemperature() { return maxTemperature; }
}
