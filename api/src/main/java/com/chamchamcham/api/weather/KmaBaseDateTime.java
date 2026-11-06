package com.chamchamcham.api.weather;

import java.util.Objects;

public record KmaBaseDateTime(String baseDate, String baseTime) {
    public KmaBaseDateTime {
        Objects.requireNonNull(baseDate,"baseDate");
        Objects.requireNonNull(baseTime,"baseTime");
    }
    public String getBaseDate() { return baseDate; }
    public String getBaseTime() { return baseTime; }
}
