package com.chamchamcham.application.policy.recommendation;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import java.util.*;
import org.junit.jupiter.api.DisplayName;

class PolicyRegionMatcherTest {
    private final PolicyRegionMatcher matcher = new PolicyRegionMatcher();

    @Test
    @DisplayName("extracts province and city tokens from farm addresses")
    void extracts_province_and_city_tokens_from_farm_addresses() throws Exception {
        var tokens = matcher.extractRegionTokens(java.util.Arrays.asList("충청북도 제천시 봉양읍 의암로 1", "충북 제천시 봉양읍 1"));
        assertThat(tokens).contains("충청북도", "제천시");
    }

    @Test
    @DisplayName("national policy matches any farm region")
    void national_policy_matches_any_farm_region() throws Exception {
        assertThat(matcher.matches(Set.of("충청북도", "제천시"), Set.of("전국"))).isTrue();
    }
}
