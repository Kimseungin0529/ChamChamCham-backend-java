package com.chamchamcham.api.policy.dto.program;

import com.chamchamcham.application.policy.recommendation.PolicyRecommendationResult;
import org.springframework.lang.Nullable;

public record PolicyContactResponse(
    @Nullable String agencyName,
    @Nullable String departmentName,
    @Nullable String phoneNumber
) {
    @Nullable public String getAgencyName() { return agencyName; }
    @Nullable public String getDepartmentName() { return departmentName; }
    @Nullable public String getPhoneNumber() { return phoneNumber; }

    public static PolicyContactResponse from(PolicyRecommendationResult.Contact result) {
        return new PolicyContactResponse(
            result.getAgencyName(),
            result.getDepartmentName(),
            result.getPhoneNumber()
        );
    }
}
