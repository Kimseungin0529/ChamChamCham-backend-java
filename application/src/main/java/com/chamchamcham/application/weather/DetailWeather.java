package com.chamchamcham.application.weather;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.Objects;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public record DetailWeather(
    @NonNull UUID farmId,
    @NonNull String address,
    @NonNull LocalDateTime observedAt,
    int temperature,
    @Nullable Integer feelsLikeTemperature,
    @NonNull WeatherCondition condition,
    @Nullable Integer minTemperature,
    @Nullable Integer maxTemperature,
    @Nullable Integer humidity,
    @Nullable Double windSpeed,
    @Nullable Integer precipitationProbability,
    @Nullable Integer uvIndex,
    @NonNull List<DailyForecast> forecast,
    @NonNull PartialFailure partial
) {
    public DetailWeather {
        Objects.requireNonNull(farmId, "farmId");
        Objects.requireNonNull(address, "address");
        Objects.requireNonNull(observedAt, "observedAt");
        Objects.requireNonNull(condition, "condition");
        Objects.requireNonNull(forecast, "forecast");
        Objects.requireNonNull(partial, "partial");
    }

    @NonNull
    public UUID getFarmId() { return farmId; }
    @NonNull
    public String getAddress() { return address; }
    @NonNull
    public LocalDateTime getObservedAt() { return observedAt; }
    public int getTemperature() { return temperature; }
    @Nullable
    public Integer getFeelsLikeTemperature() { return feelsLikeTemperature; }
    @NonNull
    public WeatherCondition getCondition() { return condition; }
    @Nullable
    public Integer getMinTemperature() { return minTemperature; }
    @Nullable
    public Integer getMaxTemperature() { return maxTemperature; }
    @Nullable
    public Integer getHumidity() { return humidity; }
    @Nullable
    public Double getWindSpeed() { return windSpeed; }
    @Nullable
    public Integer getPrecipitationProbability() { return precipitationProbability; }
    @Nullable
    public Integer getUvIndex() { return uvIndex; }
    @NonNull
    public List<DailyForecast> getForecast() { return forecast; }
    @NonNull
    public PartialFailure getPartial() { return partial; }
}
