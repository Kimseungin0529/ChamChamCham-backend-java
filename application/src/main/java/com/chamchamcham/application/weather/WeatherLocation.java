package com.chamchamcham.application.weather;

import java.util.Objects;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public record WeatherLocation(
    double latitude,
    double longitude,
    @NonNull String roadAddress,
    @Nullable String pnu
) {
    public WeatherLocation {
        Objects.requireNonNull(roadAddress, "roadAddress");
    }

    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    @NonNull
    public String getRoadAddress() { return roadAddress; }
    @Nullable
    public String getPnu() { return pnu; }
}
