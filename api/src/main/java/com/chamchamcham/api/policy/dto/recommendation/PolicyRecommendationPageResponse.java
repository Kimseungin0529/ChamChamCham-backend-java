package com.chamchamcham.api.policy.dto.recommendation;

import com.chamchamcham.application.policy.recommendation.PolicyRecommendationResult;
import java.util.List;
import java.util.ArrayList;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public record PolicyRecommendationPageResponse(
    @NonNull List<PolicyRecommendationItemResponse> items,
    @Nullable String nextCursor
) {
    public PolicyRecommendationPageResponse {
        Objects.requireNonNull(items, "items");
    }

    @NonNull public List<PolicyRecommendationItemResponse> getItems() { return items; }
    @Nullable public String getNextCursor() { return nextCursor; }

    public static PolicyRecommendationPageResponse from(PolicyRecommendationResult.Page result) {
        return new PolicyRecommendationPageResponse(
            result.getItems().stream().map(PolicyRecommendationItemResponse::from).collect(Collectors.toCollection(ArrayList::new)),
            result.getNextCursor()
        );
    }
}
