package com.chamchamcham.application.weather;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public interface UvIndexPort {
    @Nullable
    Integer fetch(@NonNull WeatherLocation location);
}
