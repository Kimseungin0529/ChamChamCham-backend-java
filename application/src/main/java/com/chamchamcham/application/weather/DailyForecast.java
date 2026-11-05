package com.chamchamcham.application.weather;

import java.time.LocalDate;
import java.util.Objects;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public record DailyForecast(
    @NonNull LocalDate date,
    @NonNull WeatherCondition condition,
    @Nullable Integer minTemperature,
    @Nullable Integer maxTemperature
) {
    public DailyForecast {
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(condition, "condition");
    }

    @NonNull
    public LocalDate getDate() { return date; }
    @NonNull
    public WeatherCondition getCondition() { return condition; }
    @Nullable
    public Integer getMinTemperature() { return minTemperature; }
    @Nullable
    public Integer getMaxTemperature() { return maxTemperature; }

    public boolean getHasTemperatureRange() { return minTemperature != null && maxTemperature != null; }
}
