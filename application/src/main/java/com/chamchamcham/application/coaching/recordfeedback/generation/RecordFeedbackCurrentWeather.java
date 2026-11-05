package com.chamchamcham.application.coaching.recordfeedback.generation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public record RecordFeedbackCurrentWeather(
     int temperatureC,
    @NonNull String skyCondition,
    @NonNull LocalDateTime observedAt
) {
    public RecordFeedbackCurrentWeather {
        Objects.requireNonNull(skyCondition, "skyCondition");
        Objects.requireNonNull(observedAt, "observedAt");
    }
    public int getTemperatureC() { return temperatureC; }
    @NonNull public String getSkyCondition() { return skyCondition; }
    @NonNull public LocalDateTime getObservedAt() { return observedAt; }
}
