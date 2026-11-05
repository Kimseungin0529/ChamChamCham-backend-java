package com.chamchamcham.application.policy.support;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.DisplayName;

class PolicyCardTextGeneratorTest {
    private final PolicyCardTextGenerator generator = new PolicyCardTextGenerator();

    @Test
    @DisplayName("benefit categories expose canonical keys and labels")
    void benefit_categories_expose_canonical_keys_and_labels() throws Exception {
        assertThat(Arrays.asList(PolicyBenefitCategory.values()).stream().map(it -> new AbstractMap.SimpleEntry<>(it.name(), it.getLabel())).toList()).containsExactly(new AbstractMap.SimpleEntry<>("GRANT", "지원금"), new AbstractMap.SimpleEntry<>("FINANCE", "융자·금융"), new AbstractMap.SimpleEntry<>("FACILITY_EQUIPMENT", "시설·장비"), new AbstractMap.SimpleEntry<>("EDUCATION", "교육"), new AbstractMap.SimpleEntry<>("WELFARE", "복지"), new AbstractMap.SimpleEntry<>("CERTIFICATION", "인증"), new AbstractMap.SimpleEntry<>("MARKET", "판로"), new AbstractMap.SimpleEntry<>("STARTUP", "창업"), new AbstractMap.SimpleEntry<>("ENVIRONMENT_INFRA", "환경·인프라"), new AbstractMap.SimpleEntry<>("ETC", "기타"));
        assertThat(PolicyBenefitCategory.Companion.fromKey("FINANCE")).isEqualTo(PolicyBenefitCategory.FINANCE);
        assertThat(PolicyBenefitCategory.Companion.fromKey("bad")).isNull();
    }

    @Test
    @DisplayName("eligibility category exposes stable enum key and card label")
    void eligibility_category_exposes_stable_enum_key_and_card_label() throws Exception {
        var category = generator.eligibilityCategory("청년후계농업경영인");
        assertThat(category).isEqualTo(PolicyEligibilityCategory.YOUNG_FARMER);
        assertThat(category.getLabel()).isEqualTo("청년 농업인");
        assertThat(generator.eligibilitySummary("청년후계농업경영인")).isEqualTo("청년 농업인");
    }

    @Test
    @DisplayName("eligibility summary maps source text to target category labels")
    void eligibility_summary_maps_source_text_to_target_category_labels() throws Exception {
        var cases = map(new AbstractMap.SimpleEntry<>("만 40세 미만 청년후계농", "청년 농업인"), new AbstractMap.SimpleEntry<>("귀농 예정자 및 귀촌 희망자", "귀농·귀촌인"), new AbstractMap.SimpleEntry<>("여성농업인 행복바우처 지원 대상", "여성 농업인"), new AbstractMap.SimpleEntry<>("65세 이상 고령농 농지이양 대상", "고령·은퇴 농업인"), new AbstractMap.SimpleEntry<>("차상위 취약계층 농가", "특수 대상자"), new AbstractMap.SimpleEntry<>("재해 피해 농가 복구 지원", "피해 농가"), new AbstractMap.SimpleEntry<>("친환경인증을 받은 농업인", "인증 보유 농업인"), new AbstractMap.SimpleEntry<>("축산법에 따라 허가받은 한우 농가", "축산 농가"), new AbstractMap.SimpleEntry<>("수출농가 및 수출업체", "수출 농가·기업"), new AbstractMap.SimpleEntry<>("농식품 벤처기업", "농식품 기업"), new AbstractMap.SimpleEntry<>("농업법인 및 영농조합법인", "농업법인·단체"), new AbstractMap.SimpleEntry<>("농업경영정보를 등록한 농업인", "경영체 등록 농업인"), new AbstractMap.SimpleEntry<>("농업인 및 임업인", "농업인"));
        cases.forEach((source, expected) -> {
        var summary = generator.eligibilitySummary(source);
        assertThat(summary).isEqualTo(expected);
        assertThat(summary.length()).isLessThanOrEqualTo(19);
        });
    }

    @Test
    @DisplayName("eligibility summary uses fallback for blank or uncategorized source text")
    void eligibility_summary_uses_fallback_for_blank_or_uncategorized_source_text() throws Exception {
        assertThat(generator.eligibilitySummary("   ")).isEqualTo("상세 자격 확인");
        assertThat(generator.eligibilitySummary("세부 공고문 참조")).isEqualTo("상세 자격 확인");
    }

    @Test
    @DisplayName("benefit summary maps source text to canonical support categories")
    void benefit_summary_maps_source_text_to_canonical_support_categories() throws Exception {
        var cases = map(new AbstractMap.SimpleEntry<>("청년농 영농정착지원금 지급", "지원금"), new AbstractMap.SimpleEntry<>("정책자금 융자 및 이자 지원", "융자·금융"), new AbstractMap.SimpleEntry<>("농기계 장비 설치 및 개보수", "시설·장비"), new AbstractMap.SimpleEntry<>("농업인 교육과 컨설팅", "교육"), new AbstractMap.SimpleEntry<>("농작물재해보험 보험료 지원", "복지"), new AbstractMap.SimpleEntry<>("GAP 인증 및 품질 검정", "인증"), new AbstractMap.SimpleEntry<>("수출 박람회 참가와 판로 홍보", "판로"), new AbstractMap.SimpleEntry<>("청년 농업인 창업 사업화", "창업"), new AbstractMap.SimpleEntry<>("농업용수 환경 인프라 개선", "환경·인프라"), new AbstractMap.SimpleEntry<>("세부 내용은 공고문 참조", "기타"));
        cases.forEach((source, expected) -> {
        assertThat(generator.benefitSummary(source)).isEqualTo(expected);
        });
        assertThat(generator.benefitCategory("농업 데이터 제공")).isEqualTo(PolicyBenefitCategory.ETC);
    }

    @Test
    @DisplayName("benefit summary uses fallback for blank source text")
    void benefit_summary_uses_fallback_for_blank_source_text() throws Exception {
        var summary = generator.benefitSummary("   ");
        assertThat(summary).isEqualTo("기타");
        assertThat(summary.length()).isLessThanOrEqualTo(19);
    }

    @Test
    @DisplayName("period label is nineteen characters or fewer")
    void period_label_is_nineteen_characters_or_fewer() throws Exception {
        var label = generator.periodLabel(LocalDate.of(2026, 3, 25), LocalDate.of(2026, 6, 30), null);
        assertThat(label).isEqualTo("2026.03.25~06.30");
        assertThat(label.length()).isLessThanOrEqualTo(19);
    }

    @Test
    @DisplayName("period label preserves agency inquiry notice")
    void period_label_preserves_agency_inquiry_notice() throws Exception {
        var label = generator.periodLabel(null, null, "접수기관문의");
        assertThat(label).isEqualTo("접수기관문의");
    }
    @SafeVarargs
    private static <K,V> Map<K,V> map(Map.Entry<K,V>... entries) {
        var result = new LinkedHashMap<K,V>();
        for (var entry : entries) result.put(entry.getKey(), entry.getValue());
        return result;
    }
}
