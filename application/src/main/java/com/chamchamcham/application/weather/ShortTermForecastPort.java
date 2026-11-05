package com.chamchamcham.application.weather;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public interface ShortTermForecastPort {
    @Nullable
    ShortTermForecast fetchLatest(@NonNull WeatherLocation location);
    @Nullable
    DailyForecast fetchTodayRange(@NonNull WeatherLocation location);
}
