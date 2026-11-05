package com.chamchamcham.application.policy.recommendation;

import java.math.BigDecimal;
import java.util.Objects;
import org.springframework.lang.NonNull;

public record PolicyScore(
    boolean eligible,
    @NonNull BigDecimal score,
    @NonNull String reason
) {
    public PolicyScore {
        Objects.requireNonNull(score, "score");
        Objects.requireNonNull(reason, "reason");
    }

    public boolean getEligible() { return eligible; }
    @NonNull
    public BigDecimal getScore() { return score; }
    @NonNull
    public String getReason() { return reason; }
}
