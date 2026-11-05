package com.chamchamcham.application.weather;

import java.time.LocalDate;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public interface HistoricalObservationPort {
    @Nullable
    DailyWeather fetch(@NonNull WeatherLocation location, @NonNull LocalDate date);
}
