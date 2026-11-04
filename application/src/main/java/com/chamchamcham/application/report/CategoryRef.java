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

public record CategoryRef(
    @NonNull String code,
    @NonNull String label
) {
    public CategoryRef {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(label, "label");
    }

    @NonNull
    public String getCode() { return code; }
    @NonNull
    public String getLabel() { return label; }
}
