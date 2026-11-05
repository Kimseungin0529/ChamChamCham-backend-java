package com.chamchamcham.application.policy.support;

import com.chamchamcham.application.policy.source.nongupez.NongupEzPolicyDetail;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import java.util.*;
import org.junit.jupiter.api.DisplayName;

class NongupEzPolicyTagExtractorTest {
    private final NongupEzPolicyTagExtractor extractor = new NongupEzPolicyTagExtractor();

    @Test
    @DisplayName("extracts young farmer return farmer medicinal and national tags")
    void extracts_young_farmer_return_farmer_medicinal_and_national_tags() throws Exception {
        var detail = detail("청년농 귀농 약용작물 지원", "만 40세 미만 청년농업인 및 귀농인", "약용작물 재배시설 지원", "농림축산식품부");
        var tags = extractor.extract(detail);
        assertThat(tags.getTargetTags()).contains("YOUNG_FARMER", "RETURNING_FARMER");
        assertThat(tags.getCropTags()).contains("MEDICINAL_CROP");
        assertThat(tags.getRegionTags()).contains("전국");
    }

    @Test
    @DisplayName("extracts regional tag from agency name")
    void extracts_regional_tag_from_agency_name() throws Exception {
        var detail = detail("충청북도 특용작물 지원", "충청북도 농업인", "특용작물 생산 지원", "충청북도");
        var tags = extractor.extract(detail);
        assertThat(tags.getCropTags()).contains("SPECIAL_CROP");
        assertThat(tags.getRegionTags()).contains("충청북도");
    }
    private NongupEzPolicyDetail detail(String title, String eligibility, String benefit, String agencyName) {
        return new NongupEzPolicyDetail("AB", "2026", title, null, null, eligibility, benefit, null, null, null, null, null, agencyName, List.of(), List.of(), "{}");
    }
}
