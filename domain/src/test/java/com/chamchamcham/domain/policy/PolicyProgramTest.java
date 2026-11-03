package com.chamchamcham.domain.policy;

import com.chamchamcham.domain.member.ManagementType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyProgramTest {
    @Test
    void applyingIdenticalPolicyFieldsReportsNoChange() {
        var program = new PolicyProgram("친환경농업 직불 지원", "친환경농업 지원", "전국", null);

        assertThat(applyList(program)).isTrue();
        assertThat(applyDetail(program)).isTrue();
        assertThat(applyList(program)).isFalse();
        assertThat(applyDetail(program)).isFalse();
    }

    @Test
    void detailSuccessMakesOpenDetailedPolicyRecommendable() {
        var program = new PolicyProgram("친환경농업직불", "목록 내용", "전국", ManagementType.AGRICULTURAL_INDIVIDUAL);
        applyList(program);
        applyDetail(program);

        assertThat(program.getDetailSynced()).isTrue();
        assertThat(program.getRecommendable()).isTrue();
        assertThat(program.isOpenOn(LocalDate.of(2026, 6, 1))).isTrue();
        assertThat(program.isOpenOn(LocalDate.of(2026, 7, 1))).isFalse();
        assertThat(program.getTargetManagementType()).isEqualTo(ManagementType.AGRICULTURAL_INDIVIDUAL);
        assertThat(program.getRawPayload()).contains("contacts", "attachments", "sourceTags");
    }

    @Test
    void detailFailureKeepsListFieldsButBlocksRecommendation() {
        var program = new PolicyProgram("청년농 지원", "목록 내용", "전국", null);
        program.markDetailSyncFailed("{\"listOnly\":true}");

        assertThat(program.getDetailSynced()).isFalse();
        assertThat(program.getRecommendable()).isFalse();
        assertThat(program.getRawPayload()).isEqualTo("{\"listOnly\":true}");
    }

    @Test
    void minimalConstructorPreservesListOnlyDefaults() {
        var program = new PolicyProgram("청년농 지원", "목록 내용", "전국", null);

        assertThat(program.getId()).isNull();
        assertThat(program.getSource()).isEqualTo(PolicySource.NONGUP_EZ);
        assertThat(program.getExternalId()).isEmpty();
        assertThat(program.getSourceYear()).isEmpty();
        assertThat(program.getAgencyName()).isEmpty();
        assertThat(program.getApplicationPeriodLabel()).isEqualTo("접수기관문의");
        assertThat(program.getEligibilitySummary()).isEqualTo("상세 자격 확인");
        assertThat(program.getBenefitSummary()).isEqualTo("상세 지원 확인");
        assertThat(program.getOnlineApplyAvailable()).isFalse();
        assertThat(program.getDetailSynced()).isFalse();
        assertThat(program.getRecommendable()).isFalse();
        assertThat(program.getTargetTagsJson()).isEqualTo("[]");
        assertThat(program.getCropTagsJson()).isEqualTo("[]");
        assertThat(program.getRegionTagsJson()).isEqualTo("[]");
        assertThat(program.getRawPayload()).isEqualTo("{}");
        assertThat(program.getApplyEndsOn()).isNull();
    }

    private boolean applyList(PolicyProgram program) {
        return program.applyListFields(PolicySource.NONGUP_EZ, "AB000009", "2026", "친환경농업 직불 지원",
            "친환경농업 지원", "전국", "https://example.test/policy", "농림축산식품부");
    }

    private boolean applyDetail(PolicyProgram program) {
        return program.applyDetailFields("상세 내용", "친환경농업 확산", "농업인", "농업인", "직불금 지원", "직불/수당",
            LocalDate.of(2026, 3, 25), LocalDate.of(2026, 6, 30), "2026.03.25~06.30", null,
            "방문 신청", "신청서", "자격 확인", "친환경농업과", false, null,
            "[\"REGISTERED_FARMER\"]", "[\"ECO_FRIENDLY\"]", "[\"전국\"]",
            "{\"contacts\":[],\"attachments\":[],\"sourceTags\":[]}", true);
    }
}
