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

public record PestControlReportSource(
    @NonNull UUID pesticideId,
    @NonNull String pesticideName,
    @Nullable BigDecimal pesticideAmount,
    @NonNull String pesticideAmountUnit,
    @Nullable BigDecimal totalSprayAmountLiters,
    @Nullable String pestName
) {
    public PestControlReportSource {
        Objects.requireNonNull(pesticideId, "pesticideId");
        Objects.requireNonNull(pesticideName, "pesticideName");
        Objects.requireNonNull(pesticideAmountUnit, "pesticideAmountUnit");
    }

    @NonNull
    public UUID getPesticideId() { return pesticideId; }
    @NonNull
    public String getPesticideName() { return pesticideName; }
    @Nullable
    public BigDecimal getPesticideAmount() { return pesticideAmount; }
    @NonNull
    public String getPesticideAmountUnit() { return pesticideAmountUnit; }
    @Nullable
    public BigDecimal getTotalSprayAmountLiters() { return totalSprayAmountLiters; }
    @Nullable
    public String getPestName() { return pestName; }
}
