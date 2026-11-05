package com.chamchamcham.application.policy.recommendation;

import com.chamchamcham.domain.policy.PolicyRecommendationSort;
import com.chamchamcham.domain.policy.PolicySource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.Objects;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public record PolicyRecommendationCursorPayload(
    @NonNull PolicySource source,
    @NonNull String sourceYear,
    @Nullable String benefitCategory,
    @NonNull PolicyRecommendationSort sort,
    @Nullable BigDecimal score,
    @Nullable LocalDate applyStartsOn,
    @Nullable LocalDate applyEndsOn,
    @NonNull UUID id
) {
    public PolicyRecommendationCursorPayload {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(sourceYear, "sourceYear");
        Objects.requireNonNull(sort, "sort");
        Objects.requireNonNull(id, "id");
    }

    @NonNull
    public PolicySource getSource() { return source; }
    @NonNull
    public String getSourceYear() { return sourceYear; }
    @Nullable
    public String getBenefitCategory() { return benefitCategory; }
    @NonNull
    public PolicyRecommendationSort getSort() { return sort; }
    @Nullable
    public BigDecimal getScore() { return score; }
    @Nullable
    public LocalDate getApplyStartsOn() { return applyStartsOn; }
    @Nullable
    public LocalDate getApplyEndsOn() { return applyEndsOn; }
    @NonNull
    public UUID getId() { return id; }
}
