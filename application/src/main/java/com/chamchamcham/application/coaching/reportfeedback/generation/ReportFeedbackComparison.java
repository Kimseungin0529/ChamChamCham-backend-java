package com.chamchamcham.application.coaching.reportfeedback.generation;

import com.chamchamcham.domain.farming.WorkType;
import com.chamchamcham.domain.report.CommonOnlyStatistics;
import com.chamchamcham.domain.report.Coverage;
import com.chamchamcham.domain.report.CycleReportStatistics;
import com.chamchamcham.domain.report.FertilizingStatistics;
import com.chamchamcham.domain.report.HarvestStatistics;
import com.chamchamcham.domain.report.PestControlStatistics;
import com.chamchamcham.domain.report.PlantingStatistics;
import com.chamchamcham.domain.report.WateringStatistics;
import com.chamchamcham.domain.report.WeedingStatistics;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public record ReportFeedbackComparison(
    @NonNull String metricKey,
    @NonNull String metricLabel,
    @NonNull BigDecimal currentValue,
    @NonNull BigDecimal previousValue,
    @NonNull BigDecimal difference,
    @Nullable BigDecimal relativeChangePct,
    @NonNull String unit,
    @Nullable Coverage currentCoverage,
    @Nullable Coverage previousCoverage
) {
    public ReportFeedbackComparison {
        Objects.requireNonNull(metricKey, "metricKey");
        Objects.requireNonNull(metricLabel, "metricLabel");
        Objects.requireNonNull(currentValue, "currentValue");
        Objects.requireNonNull(previousValue, "previousValue");
        Objects.requireNonNull(difference, "difference");
        Objects.requireNonNull(unit, "unit");
    }
    @NonNull public String getMetricKey() { return metricKey; }
    @NonNull public String getMetricLabel() { return metricLabel; }
    @NonNull public BigDecimal getCurrentValue() { return currentValue; }
    @NonNull public BigDecimal getPreviousValue() { return previousValue; }
    @NonNull public BigDecimal getDifference() { return difference; }
    @Nullable public BigDecimal getRelativeChangePct() { return relativeChangePct; }
    @NonNull public String getUnit() { return unit; }
    @Nullable public Coverage getCurrentCoverage() { return currentCoverage; }
    @Nullable public Coverage getPreviousCoverage() { return previousCoverage; }
}
