package com.chamchamcham.domain.policy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class PolicyRecommendationSortTest {
    @Test
    void exactKeysResolveToSort() {
        assertThat(PolicyRecommendationSort.fromKey("RECOMMENDED"))
            .isEqualTo(PolicyRecommendationSort.RECOMMENDED);
        assertThat(PolicyRecommendationSort.fromKey("LATEST"))
            .isEqualTo(PolicyRecommendationSort.LATEST);
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = {"BAD", "recommended", "latest", " RECOMMENDED", "LATEST "})
    void unknownOrNonExactKeysReturnNull(String key) {
        assertThat(PolicyRecommendationSort.fromKey(key)).isNull();
    }

    @Test
    void nullKeyIsRejected() {
        assertThatNullPointerException()
            .isThrownBy(() -> PolicyRecommendationSort.fromKey(null));
    }
}
