package com.chamchamcham.application.coaching.recordfeedback.generation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public record RecordFeedbackForecastDay(
    @NonNull LocalDate date,
    @Nullable BigDecimal rainfallMm,
    @Nullable Integer rainProbabilityPct,
    @Nullable BigDecimal maxTemperatureC,
    @Nullable BigDecimal minTemperatureC,
    @Nullable BigDecimal humidityPct,
    @Nullable BigDecimal windSpeedMs,
    @NonNull List<String> riskFlags
) {
    public RecordFeedbackForecastDay {
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(riskFlags, "riskFlags");
    }
    @NonNull public LocalDate getDate() { return date; }
    @Nullable public BigDecimal getRainfallMm() { return rainfallMm; }
    @Nullable public Integer getRainProbabilityPct() { return rainProbabilityPct; }
    @Nullable public BigDecimal getMaxTemperatureC() { return maxTemperatureC; }
    @Nullable public BigDecimal getMinTemperatureC() { return minTemperatureC; }
    @Nullable public BigDecimal getHumidityPct() { return humidityPct; }
    @Nullable public BigDecimal getWindSpeedMs() { return windSpeedMs; }
    @NonNull public List<String> getRiskFlags() { return riskFlags; }
}
