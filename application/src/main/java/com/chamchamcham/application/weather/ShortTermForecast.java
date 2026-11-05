package com.chamchamcham.application.weather;

import java.util.List;
import java.util.Objects;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public record ShortTermForecast(
    @NonNull WeatherCondition currentSky,
    @Nullable Integer precipitationProbability,
    @NonNull List<DailyForecast> dailyForecasts
) {
    public ShortTermForecast {
        Objects.requireNonNull(currentSky, "currentSky");
        Objects.requireNonNull(dailyForecasts, "dailyForecasts");
    }

    @NonNull
    public WeatherCondition getCurrentSky() { return currentSky; }
    @Nullable
    public Integer getPrecipitationProbability() { return precipitationProbability; }
    @NonNull
    public List<DailyForecast> getDailyForecasts() { return dailyForecasts; }
}
