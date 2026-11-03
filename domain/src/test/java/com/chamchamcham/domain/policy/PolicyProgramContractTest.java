package com.chamchamcham.domain.policy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class PolicyProgramContractTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 7, 7);

    @Test
    void listSyncUpdatesBodyFromSummaryOnlyBeforeDetailSync() {
        var program = program();
        assertThat(applyList(program, "첫 요약")).isTrue();
        assertThat(program.getBody()).isEqualTo("첫 요약");

        applyDetail(program, "자격 확인", "지원 확인", "접수기관문의");
        assertThat(applyList(program, "새 요약")).isTrue();
        assertThat(program.getBody()).isEqualTo("상세 본문");
        assertThat(program.getSummary()).isEqualTo("새 요약");
        assertThat(applyList(program, "새 요약")).isFalse();
    }

    @Test
    void nullListSummaryKeepsExistingBodyBeforeDetailSync() {
        var program = program();
        applyList(program, null);
        assertThat(program.getBody()).isEqualTo("목록 본문");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2})
    void oversizedDetailLabelsRejectBeforeChangingAnyField(int label) {
        var program = program();
        var tooLong = "가".repeat(20);

        assertThatIllegalArgumentException().isThrownBy(() -> applyDetail(
            program, label == 0 ? tooLong : "자격 확인", label == 1 ? tooLong : "지원 확인",
            label == 2 ? tooLong : "접수기관문의"
        ));

        assertThat(program.getBody()).isEqualTo("목록 본문");
        assertThat(program.getDetailSynced()).isFalse();
        assertThat(program.getRecommendable()).isFalse();
        assertThat(program.getRawPayload()).isEqualTo("{}");
    }

    @Test
    void nineteenCharacterLabelsAreAcceptedAndIdenticalDetailsReportNoChange() {
        var program = program();
        var label = "가".repeat(19);
        assertThat(applyDetail(program, label, label, label)).isTrue();
        assertThat(applyDetail(program, label, label, label)).isFalse();
        assertThat(program.getDetailSynced()).isTrue();
        assertThat(program.getRecommendable()).isTrue();
    }

    @Test
    void detailFailureDisablesRecommendationAndReportsOnlyActualChanges() {
        var program = program();
        applyList(program, "목록 요약");
        applyDetail(program, "자격 확인", "지원 확인", "접수기관문의");

        assertThat(program.markDetailSyncFailed("실패 응답")).isTrue();
        assertThat(program.getDetailSynced()).isFalse();
        assertThat(program.getRecommendable()).isFalse();
        assertThat(program.getTitle()).isEqualTo("정책");
        assertThat(program.getSummary()).isEqualTo("목록 요약");
        assertThat(program.getBody()).isEqualTo("상세 본문");
        assertThat(program.markDetailSyncFailed("실패 응답")).isFalse();
        assertThat(program.markDetailSyncFailed("다른 응답")).isTrue();
    }

    @Test
    void openDateIncludesDeadlineAndDoesNotFilterByFutureStartDate() {
        var program = program();
        program.setApplyStartsOn(TODAY.plusDays(10));
        assertThat(program.isOpenOn(TODAY)).isTrue();
        program.setApplyEndsOn(TODAY);
        assertThat(program.isOpenOn(TODAY)).isTrue();
        program.setApplyEndsOn(TODAY.minusDays(1));
        assertThat(program.isOpenOn(TODAY)).isFalse();
    }

    private boolean applyList(PolicyProgram program, String summary) {
        return program.applyListFields(PolicySource.NONGUP_EZ, "external", "2026", "정책",
            summary, "전국", null, "기관");
    }

    private boolean applyDetail(PolicyProgram program, String eligibility, String benefit, String period) {
        return program.applyDetailFields("상세 본문", "목적", "자격 원문", eligibility, "지원 원문", benefit,
            null, TODAY, period, null, null, null, null, null, false, null, "[]", "[]", "[]", "상세 응답", true);
    }

    private PolicyProgram program() {
        return new PolicyProgram(null, "정책", "목록 본문", "전국", null, null, null, null,
            PolicySource.NONGUP_EZ, "", "", null, "", null, false, null, "접수기관문의", null,
            null, "상세 자격 확인", null, "상세 지원 확인", null, null, null, null, false, false,
            "[]", "[]", "[]", "{}");
    }
}
