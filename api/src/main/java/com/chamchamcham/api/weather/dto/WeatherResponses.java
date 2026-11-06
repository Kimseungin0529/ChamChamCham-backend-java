package com.chamchamcham.api.weather.dto;

import com.chamchamcham.application.weather.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

public final class WeatherResponses {
    private WeatherResponses() {}

    public record ConditionResponse(
        String code,
        String text
    ) {
        public ConditionResponse {
            Objects.requireNonNull(code, "code");
            Objects.requireNonNull(text, "text");
        }
        public static ConditionResponse from(WeatherCondition condition) {
            return new ConditionResponse(condition.name(), condition.getText());
        }
        public String getCode() { return code; }
        public String getText() { return text; }
    }

    public record PartialResponse(
        boolean degraded,
        List<String> missing
    ) {
        public PartialResponse {
            Objects.requireNonNull(missing, "missing");
        }
        public static PartialResponse from(PartialFailure partial) {
            return new PartialResponse(partial.getDegraded(), partial.getMissing());
        }
        public boolean getDegraded() { return degraded; }
        public List<String> getMissing() { return missing; }
    }

    public record ForecastResponse(
        LocalDate date,
        String dayOfWeek,
        ConditionResponse condition,
        Integer minTemperature,
        Integer maxTemperature
    ) {
        public ForecastResponse {
            Objects.requireNonNull(date, "date");
            Objects.requireNonNull(dayOfWeek, "dayOfWeek");
            Objects.requireNonNull(condition, "condition");
        }
        public static ForecastResponse from(DailyForecast forecast) {
            return new ForecastResponse(forecast.getDate(), forecast.getDate().getDayOfWeek().name(), ConditionResponse.from(forecast.getCondition()), forecast.getMinTemperature(), forecast.getMaxTemperature());
        }
        public LocalDate getDate() { return date; }
        public String getDayOfWeek() { return dayOfWeek; }
        public ConditionResponse getCondition() { return condition; }
        public Integer getMinTemperature() { return minTemperature; }
        public Integer getMaxTemperature() { return maxTemperature; }
    }

    public record HomeResponse(
        UUID farmId,
        int temperature,
        ConditionResponse condition,
        Integer minTemperature,
        Integer maxTemperature,
        LocalDateTime observedAt,
        PartialResponse partial
    ) {
        public HomeResponse {
            Objects.requireNonNull(farmId, "farmId");
            Objects.requireNonNull(condition, "condition");
            Objects.requireNonNull(observedAt, "observedAt");
            Objects.requireNonNull(partial, "partial");
        }
        public static HomeResponse from(HomeWeather result) {
            return new HomeResponse(result.getFarmId(), result.getTemperature(), ConditionResponse.from(result.getCondition()), result.getMinTemperature(), result.getMaxTemperature(), result.getObservedAt(), PartialResponse.from(result.getPartial()));
        }
        public UUID getFarmId() { return farmId; }
        public int getTemperature() { return temperature; }
        public ConditionResponse getCondition() { return condition; }
        public Integer getMinTemperature() { return minTemperature; }
        public Integer getMaxTemperature() { return maxTemperature; }
        public LocalDateTime getObservedAt() { return observedAt; }
        public PartialResponse getPartial() { return partial; }
    }

    public record CurrentResponse(
        int temperature,
        Integer feelsLikeTemperature,
        ConditionResponse condition,
        Integer minTemperature,
        Integer maxTemperature,
        Integer humidity,
        Double windSpeed,
        Integer precipitationProbability,
        Integer uvIndex
    ) {
        public CurrentResponse {
            Objects.requireNonNull(condition, "condition");
        }
        public static CurrentResponse from(DetailWeather result) {
            return new CurrentResponse(result.getTemperature(), result.getFeelsLikeTemperature(), ConditionResponse.from(result.getCondition()), result.getMinTemperature(), result.getMaxTemperature(), result.getHumidity(), result.getWindSpeed(), result.getPrecipitationProbability(), result.getUvIndex());
        }
        public int getTemperature() { return temperature; }
        public Integer getFeelsLikeTemperature() { return feelsLikeTemperature; }
        public ConditionResponse getCondition() { return condition; }
        public Integer getMinTemperature() { return minTemperature; }
        public Integer getMaxTemperature() { return maxTemperature; }
        public Integer getHumidity() { return humidity; }
        public Double getWindSpeed() { return windSpeed; }
        public Integer getPrecipitationProbability() { return precipitationProbability; }
        public Integer getUvIndex() { return uvIndex; }
    }

    public record DetailResponse(
        UUID farmId,
        String address,
        LocalDateTime observedAt,
        CurrentResponse current,
        List<ForecastResponse> forecast,
        PartialResponse partial
    ) {
        public DetailResponse {
            Objects.requireNonNull(farmId, "farmId");
            Objects.requireNonNull(address, "address");
            Objects.requireNonNull(observedAt, "observedAt");
            Objects.requireNonNull(current, "current");
            Objects.requireNonNull(forecast, "forecast");
            Objects.requireNonNull(partial, "partial");
        }
        public static DetailResponse from(DetailWeather result) {
            return new DetailResponse(result.getFarmId(), result.getAddress(), result.getObservedAt(), CurrentResponse.from(result), result.getForecast().stream().map(ForecastResponse::from).collect(Collectors.toCollection(ArrayList::new)), PartialResponse.from(result.getPartial()));
        }
        public UUID getFarmId() { return farmId; }
        public String getAddress() { return address; }
        public LocalDateTime getObservedAt() { return observedAt; }
        public CurrentResponse getCurrent() { return current; }
        public List<ForecastResponse> getForecast() { return forecast; }
        public PartialResponse getPartial() { return partial; }
    }

    public record DailyResponse(
        LocalDate date,
        ConditionResponse condition,
        int minTemperature,
        int maxTemperature
    ) {
        public DailyResponse {
            Objects.requireNonNull(date, "date");
            Objects.requireNonNull(condition, "condition");
        }
        public static DailyResponse from(DailyWeather result) {
            return new DailyResponse(result.getDate(), ConditionResponse.from(result.getCondition()), result.getMinTemperature(), result.getMaxTemperature());
        }
        public LocalDate getDate() { return date; }
        public ConditionResponse getCondition() { return condition; }
        public int getMinTemperature() { return minTemperature; }
        public int getMaxTemperature() { return maxTemperature; }
    }
}
