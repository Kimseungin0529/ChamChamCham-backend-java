package com.chamchamcham.domain.policy;

import org.springframework.lang.Nullable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public interface PolicyRecommendationQueryRepository {
    SearchResult findPage(SearchCondition condition);

    record SearchCondition(UUID memberId, PolicySource source, String sourceYear, @Nullable String benefitSummary,
        PolicyRecommendationSort sort, @Nullable Cursor cursor, int size) {
        public SearchCondition {
            Objects.requireNonNull(memberId, "memberId");
            Objects.requireNonNull(source, "source");
            Objects.requireNonNull(sourceYear, "sourceYear");
            Objects.requireNonNull(sort, "sort");
        }
    }

    record Cursor(@Nullable BigDecimal score, @Nullable LocalDate applyStartsOn, @Nullable LocalDate applyEndsOn, UUID id) {
        public Cursor {
            Objects.requireNonNull(id, "id");
        }
    }

    record SearchResult(List<PolicyRecommendation> rows) {
        public SearchResult {
            Objects.requireNonNull(rows, "rows");
        }
    }
}
