package com.chamchamcham.application.report;

import com.chamchamcham.domain.farming.WorkType;
import com.chamchamcham.domain.report.FarmingCycleReportStatus;
import com.chamchamcham.domain.report.FarmingCycleStartBasis;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;
import java.util.Set;
import java.util.Objects;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public record WateringReportSource(
    @Nullable CategoryRef amount,
    @Nullable CategoryRef method
) {
    @Nullable
    public CategoryRef getAmount() { return amount; }
    @Nullable
    public CategoryRef getMethod() { return method; }
}
