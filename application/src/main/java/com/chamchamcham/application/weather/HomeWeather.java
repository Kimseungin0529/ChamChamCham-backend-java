package com.chamchamcham.application.weather;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Objects;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public record HomeWeather(
    @NonNull UUID farmId,
    int temperature,
    @NonNull WeatherCondition condition,
    @Nullable Integer minTemperature,
    @Nullable Integer maxTemperature,
    @NonNull LocalDateTime observedAt,
    @NonNull PartialFailure partial
) {
    public HomeWeather {
        Objects.requireNonNull(farmId, "farmId");
        Objects.requireNonNull(condition, "condition");
        Objects.requireNonNull(observedAt, "observedAt");
        Objects.requireNonNull(partial, "partial");
    }

    @NonNull
    public UUID getFarmId() { return farmId; }
    public int getTemperature() { return temperature; }
    @NonNull
    public WeatherCondition getCondition() { return condition; }
    @Nullable
    public Integer getMinTemperature() { return minTemperature; }
    @Nullable
    public Integer getMaxTemperature() { return maxTemperature; }
    @NonNull
    public LocalDateTime getObservedAt() { return observedAt; }
    @NonNull
    public PartialFailure getPartial() { return partial; }
}
