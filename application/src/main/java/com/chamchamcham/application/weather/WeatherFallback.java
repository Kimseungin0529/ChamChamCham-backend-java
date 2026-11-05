package com.chamchamcham.application.weather;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.lang.NonNull;

/** 외부 날씨 제공자 장애 때 사용하는 고정 폴백 값. */
public final class WeatherFallback {
    public static final WeatherFallback INSTANCE = new WeatherFallback();
    public static final WeatherCondition CONDITION = WeatherCondition.CLOUDY;
    public static final int TEMPERATURE = 20;
    public static final int FEELS_LIKE_TEMPERATURE = 20;
    public static final int HUMIDITY = 50;
    public static final double WIND_SPEED = 0.0;
    public static final int PRECIPITATION_PROBABILITY = 0;
    public static final int UV_INDEX = 0;
    public static final int MIN_TEMPERATURE = 20;
    public static final int MAX_TEMPERATURE = 20;

    private WeatherFallback() {}

    @NonNull
    public WeatherCondition getCONDITION() { return CONDITION; }

    @NonNull
    public static CurrentObservation currentObservation(@NonNull Clock clock) {
        Objects.requireNonNull(clock, "clock");
        return new CurrentObservation(TEMPERATURE, null, LocalDateTime.now(clock), HUMIDITY, WIND_SPEED, FEELS_LIKE_TEMPERATURE);
    }

    @NonNull
    public static ShortTermForecast shortTermForecast(@NonNull LocalDate today) {
        Objects.requireNonNull(today, "today");
        List<DailyForecast> days = new ArrayList<>();
        for (int offset = 0; offset <= 4; offset++) days.add(dailyForecast(today.plusDays(offset)));
        return new ShortTermForecast(CONDITION, PRECIPITATION_PROBABILITY, days);
    }

    @NonNull
    public static DailyForecast dailyForecast(@NonNull LocalDate date) {
        return new DailyForecast(Objects.requireNonNull(date, "date"), CONDITION, MIN_TEMPERATURE, MAX_TEMPERATURE);
    }

    @NonNull
    public static DailyWeather dailyWeather(@NonNull LocalDate date) {
        return new DailyWeather(Objects.requireNonNull(date, "date"), CONDITION, MIN_TEMPERATURE, MAX_TEMPERATURE);
    }
}
