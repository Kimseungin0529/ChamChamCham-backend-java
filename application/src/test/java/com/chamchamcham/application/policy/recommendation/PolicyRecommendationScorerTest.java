package com.chamchamcham.application.policy.recommendation;

import com.chamchamcham.domain.member.ManagementType;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.DisplayName;

class PolicyRecommendationScorerTest {
    private final PolicyRecommendationScorer scorer = new PolicyRecommendationScorer(new PolicyRegionMatcher());

    @Test
    @DisplayName("scores young farmer medicinal crop and region matches above generic policy")
    void scores_young_farmer_medicinal_crop_and_region_matches_above_generic_policy() throws Exception {
        var profile = new PolicyMemberProfile(LocalDate.of(1995, 1, 1), 2, ManagementType.AGRICULTURAL_INDIVIDUAL, Set.of("참당귀"), Set.of("ROOT_BARK"), Set.of("충청북도", "제천시"));
        var scored = scorer.score(profile, Set.of("YOUNG_FARMER", "REGISTERED_FARMER"), Set.of("MEDICINAL_CROP"), Set.of("충청북도"), true, LocalDate.of(2026, 4, 1));
        assertThat(scored.getScore().doubleValue()).isGreaterThanOrEqualTo(70.0);
        assertThat(scored.getScore()).isEqualByComparingTo("96");
        assertThat(scored.getEligible()).isTrue();
        assertThat(scored.getReason()).isEqualTo("청년농 대상이고 재배 품목이 맞아요.");
    }

    @Test
    @DisplayName("returns zero score when a required target group does not match")
    void returns_zero_score_when_a_required_target_group_does_not_match() throws Exception {
        var profile = new PolicyMemberProfile(LocalDate.of(1975, 1, 1), 20, ManagementType.NON_REGISTERED_FARMER, Set.of("참당귀"), Set.of("ROOT_BARK"), Set.of("충청북도", "제천시"));
        var scored = scorer.score(profile, Set.of("YOUNG_FARMER"), Set.of("MEDICINAL_CROP"), Set.of("충청북도"), true, LocalDate.of(2026, 4, 1));
        assertThat(scored.getScore()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(scored.getEligible()).isFalse();
    }

    @Test
    @DisplayName("returns zero score when youth policy also matches registered farmer but member is not young")
    void returns_zero_score_when_youth_policy_also_matches_registered_farmer_but_member_is_not_young() throws Exception {
        var profile = new PolicyMemberProfile(LocalDate.of(1960, 10, 1), 20, ManagementType.AGRICULTURAL_INDIVIDUAL, Set.of("참당귀"), Set.of("ROOT_BARK"), Set.of("전북특별자치도", "전주시"));
        var scored = scorer.score(profile, Set.of("YOUNG_FARMER", "REGISTERED_FARMER"), Set.of("MEDICINAL_CROP"), Set.of("전북특별자치도"), true, LocalDate.of(2026, 4, 1));
        assertThat(scored.getScore()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(scored.getEligible()).isFalse();
    }

    @Test
    @DisplayName("returns zero score when a required region group does not match")
    void returns_zero_score_when_a_required_region_group_does_not_match() throws Exception {
        var profile = new PolicyMemberProfile(LocalDate.of(1995, 1, 1), 2, ManagementType.AGRICULTURAL_INDIVIDUAL, Set.of("참당귀"), Set.of("ROOT_BARK"), Set.of("충청북도", "제천시"));
        var scored = scorer.score(profile, Set.of("YOUNG_FARMER"), Set.of("MEDICINAL_CROP"), Set.of("전라남도"), true, LocalDate.of(2026, 4, 1));
        assertThat(scored.getScore()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(scored.getEligible()).isFalse();
    }

    @Test
    @DisplayName("returns zero score when no meaningful matching group exists")
    void returns_zero_score_when_no_meaningful_matching_group_exists() throws Exception {
        var profile = new PolicyMemberProfile(LocalDate.of(1995, 1, 1), 2, ManagementType.AGRICULTURAL_INDIVIDUAL, Set.of("참당귀"), Set.of("ROOT_BARK"), Set.of("충청북도", "제천시"));
        var scored = scorer.score(profile, Set.of(), Set.of(), Set.of("전국"), true, LocalDate.of(2026, 4, 1));
        assertThat(scored.getScore()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(scored.getEligible()).isFalse();
    }
}
