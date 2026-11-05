package com.chamchamcham.application.weather;

import org.springframework.lang.NonNull;

public interface CurrentObservationPort {
    @NonNull
    CurrentObservation fetch(@NonNull WeatherLocation location);
}
