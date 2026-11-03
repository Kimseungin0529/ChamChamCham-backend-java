package com.chamchamcham.domain.policy;

import java.util.Objects;

public enum PolicyRecommendationSort {
    RECOMMENDED,
    LATEST;

    public static PolicyRecommendationSort fromKey(String key) {
        Objects.requireNonNull(key, "key");
        for (var sort : values()) {
            if (sort.name().equals(key)) {
                return sort;
            }
        }
        return null;
    }
}
