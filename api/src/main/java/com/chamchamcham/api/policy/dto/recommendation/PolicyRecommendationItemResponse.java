package com.chamchamcham.api.policy.dto.recommendation;

import com.chamchamcham.application.policy.recommendation.PolicyRecommendationResult;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.Objects;
import org.springframework.lang.NonNull;

public record PolicyRecommendationItemResponse(
    @NonNull UUID recommendationId,
    @NonNull UUID policyProgramId,
    @NonNull String programTitle,
    @NonNull String eligibilitySummary,
    @NonNull String benefitSummary,
    @NonNull String applicationPeriodLabel,
    @NonNull String agencyName,
    @NonNull BigDecimal score,
    @NonNull String reason
) {
    public PolicyRecommendationItemResponse {
        Objects.requireNonNull(recommendationId, "recommendationId");
        Objects.requireNonNull(policyProgramId, "policyProgramId");
        Objects.requireNonNull(programTitle, "programTitle");
        Objects.requireNonNull(eligibilitySummary, "eligibilitySummary");
        Objects.requireNonNull(benefitSummary, "benefitSummary");
        Objects.requireNonNull(applicationPeriodLabel, "applicationPeriodLabel");
        Objects.requireNonNull(agencyName, "agencyName");
        Objects.requireNonNull(score, "score");
        Objects.requireNonNull(reason, "reason");
    }

    @NonNull public UUID getRecommendationId() { return recommendationId; }
    @NonNull public UUID getPolicyProgramId() { return policyProgramId; }
    @NonNull public String getProgramTitle() { return programTitle; }
    @NonNull public String getEligibilitySummary() { return eligibilitySummary; }
    @NonNull public String getBenefitSummary() { return benefitSummary; }
    @NonNull public String getApplicationPeriodLabel() { return applicationPeriodLabel; }
    @NonNull public String getAgencyName() { return agencyName; }
    @NonNull public BigDecimal getScore() { return score; }
    @NonNull public String getReason() { return reason; }

    public static PolicyRecommendationItemResponse from(PolicyRecommendationResult.Card result) {
        return new PolicyRecommendationItemResponse(
            result.getRecommendationId(),
            result.getPolicyProgramId(),
            result.getProgramTitle(),
            result.getEligibilitySummary(),
            result.getBenefitSummary(),
            result.getApplicationPeriodLabel(),
            result.getAgencyName(),
            result.getScore(),
            result.getReason()
        );
    }
}
