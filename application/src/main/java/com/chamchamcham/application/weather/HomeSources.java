package com.chamchamcham.application.weather;

import java.util.Objects;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public record HomeSources(
    @NonNull CurrentObservation current,
    @Nullable ShortTermForecast latest,
    @Nullable DailyForecast todayRange,
    @NonNull PartialFailure partial
) {
    public HomeSources {
        Objects.requireNonNull(current, "current");
        Objects.requireNonNull(partial, "partial");
    }

    @NonNull
    public CurrentObservation getCurrent() { return current; }
    @Nullable
    public ShortTermForecast getLatest() { return latest; }
    @Nullable
    public DailyForecast getTodayRange() { return todayRange; }
    @NonNull
    public PartialFailure getPartial() { return partial; }
}
