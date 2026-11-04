package com.chamchamcham.application.coaching.recordfeedback.generation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public record RecordFeedbackLiveWeather(
    @NonNull RecordFeedbackCurrentWeather current,
    @NonNull List<RecordFeedbackForecastDay> forecastDays,
    @NonNull String source
) {
    public RecordFeedbackLiveWeather {
        Objects.requireNonNull(current, "current");
        Objects.requireNonNull(forecastDays, "forecastDays");
        Objects.requireNonNull(source, "source");
    }
    @NonNull public RecordFeedbackCurrentWeather getCurrent() { return current; }
    @NonNull public List<RecordFeedbackForecastDay> getForecastDays() { return forecastDays; }
    @NonNull public String getSource() { return source; }
}
