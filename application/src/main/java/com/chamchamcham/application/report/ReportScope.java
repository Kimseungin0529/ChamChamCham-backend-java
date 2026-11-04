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

public record ReportScope(
    @NonNull UUID memberId,
    @NonNull UUID farmId,
    @NonNull UUID cropId
) {
    public ReportScope {
        Objects.requireNonNull(memberId, "memberId");
        Objects.requireNonNull(farmId, "farmId");
        Objects.requireNonNull(cropId, "cropId");
    }

    @NonNull
    public UUID getMemberId() { return memberId; }
    @NonNull
    public UUID getFarmId() { return farmId; }
    @NonNull
    public UUID getCropId() { return cropId; }
}
