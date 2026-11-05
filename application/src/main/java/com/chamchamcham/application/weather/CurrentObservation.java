package com.chamchamcham.application.weather;

import java.time.LocalDateTime;
import java.util.Objects;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public record CurrentObservation(
    int temperature,
    @Nullable WeatherCondition precipitationType,
    @NonNull LocalDateTime observedAt,
    @Nullable Integer humidity,
    @Nullable Double windSpeed,
    @Nullable Integer feelsLikeTemperature
) {
    public CurrentObservation {
        Objects.requireNonNull(observedAt, "observedAt");
    }

    public int getTemperature() { return temperature; }
    @Nullable
    public WeatherCondition getPrecipitationType() { return precipitationType; }
    @NonNull
    public LocalDateTime getObservedAt() { return observedAt; }
    @Nullable
    public Integer getHumidity() { return humidity; }
    @Nullable
    public Double getWindSpeed() { return windSpeed; }
    @Nullable
    public Integer getFeelsLikeTemperature() { return feelsLikeTemperature; }
}
