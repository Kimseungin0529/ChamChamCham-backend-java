package com.chamchamcham.application.weather;

import java.util.Objects;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public record DetailSources(
    @NonNull CurrentObservation current,
    @Nullable ShortTermForecast latest,
    @Nullable DailyForecast todayRange,
    @Nullable DailyForecast midTermD4,
    @Nullable Integer uvIndex,
    @NonNull PartialFailure partial
) {
    public DetailSources {
        Objects.requireNonNull(current, "current");
        Objects.requireNonNull(partial, "partial");
    }

    @NonNull
    public CurrentObservation getCurrent() { return current; }
    @Nullable
    public ShortTermForecast getLatest() { return latest; }
    @Nullable
    public DailyForecast getTodayRange() { return todayRange; }
    @Nullable
    public DailyForecast getMidTermD4() { return midTermD4; }
    @Nullable
    public Integer getUvIndex() { return uvIndex; }
    @NonNull
    public PartialFailure getPartial() { return partial; }
}
