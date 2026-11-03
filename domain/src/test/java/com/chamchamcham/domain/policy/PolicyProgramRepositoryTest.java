package com.chamchamcham.domain.policy;

import com.chamchamcham.domain.member.ManagementType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@ContextConfiguration(classes = PolicyJpaTestConfiguration.class)
class PolicyProgramRepositoryTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 7, 7);
    private static final LocalDateTime NOW = TODAY.atTime(9, 0);

    @Autowired TestEntityManager entityManager;
    @Autowired PolicyProgramRepository repository;

    @Test
    void titleSuggestionsIgnoreCaseAndOrderByTitle() {
        persist(program("YOUNG farmer B", "기관"));
        persist(program("Young farmer A", "기관"));
        persist(program("무관 정책", "기관"));
        entityManager.clear();

        assertThat(repository.findTitleSuggestions("young", PageRequest.of(0, 9)))
            .extracting(PolicyProgram::getTitle).containsExactly("YOUNG farmer B", "Young farmer A");
    }

    @Test
    void titleSuggestionsRespectPageSize() {
        for (int i = 0; i < 12; i++) persist(program("지원사업" + i, "기관"));
        entityManager.clear();
        assertThat(repository.findTitleSuggestions("지원사업", PageRequest.of(0, 9))).hasSize(9);
    }

    @Test
    void agencySuggestionsIgnoreCaseAndOrderByAgency() {
        persist(program("정책 A", "FARM B"));
        persist(program("정책 B", "Farm A"));
        persist(program("정책 C", "해양수산부"));
        entityManager.clear();

        assertThat(repository.findAgencySuggestions("farm", PageRequest.of(0, 9)))
            .extracting(PolicyProgram::getAgencyName).containsExactly("FARM B", "Farm A");
    }

    @Test
    void agencySuggestionsRespectPageSize() {
        for (int i = 0; i < 12; i++) persist(program("정책" + i, "공통기관"));
        entityManager.clear();
        assertThat(repository.findAgencySuggestions("공통기관", PageRequest.of(0, 9))).hasSize(9);
    }

    @Test
    void candidatesRequireSyncedRecommendableMatchingYearAndIncludeDeadlineOrNoDeadline() {
        var deadline = program("당일 마감", "기관");
        deadline.setApplyEndsOn(TODAY);
        var noDeadline = program("마감 없음", "기관");
        var expired = program("마감 지남", "기관");
        expired.setApplyEndsOn(TODAY.minusDays(1));
        var unsynced = program("미동기화", "기관");
        unsynced.setDetailSynced(false);
        var disabled = program("추천 제외", "기관");
        disabled.setRecommendable(false);
        var otherYear = program("다른 연도", "기관");
        otherYear.setSourceYear("2025");
        for (var row : new PolicyProgram[]{deadline, noDeadline, expired, unsynced, disabled, otherYear}) persist(row);
        entityManager.clear();

        assertThat(repository.findRecommendableCandidates(PolicySource.NONGUP_EZ, "2026", TODAY))
            .extracting(PolicyProgram::getTitle).containsExactlyInAnyOrder("당일 마감", "마감 없음");
        assertThat(repository.findBySourceAndSourceYear(PolicySource.NONGUP_EZ, "2025"))
            .extracting(PolicyProgram::getTitle).containsExactly("다른 연도");
        assertThat(repository.findByIdAndDetailSyncedTrueAndRecommendableTrue(deadline.getId())).isNotNull();
        assertThat(repository.findByIdAndDetailSyncedTrueAndRecommendableTrue(unsynced.getId())).isNull();
        assertThat(repository.findByIdAndDetailSyncedTrueAndRecommendableTrue(disabled.getId())).isNull();
        assertThat(repository.findByIdAndDetailSyncedTrueAndRecommendableTrue(UUID.randomUUID())).isNull();
    }

    @Test
    void allPolicyFieldsAndAuditTimestampsSurvivePersistenceRoundTrip() {
        var program = program("정책", "기관");
        program.setTargetManagementType(ManagementType.AGRICULTURAL_INDIVIDUAL);
        program.setApplyStartsOn(TODAY.minusDays(1));
        program.setApplyEndsOn(TODAY.plusDays(1));
        program.setSourceUrl("https://example.test/source");
        program.setSummary("요약");
        program.setDepartmentName("담당 부서");
        program.setOnlineApplyAvailable(true);
        program.setApplicationUrl("https://example.test/apply");
        program.setApplicationPeriodLabel("신청 기간");
        program.setApplicationPeriodNotice("기간 공지");
        program.setEligibilityOriginal("자격 원문");
        program.setEligibilitySummary("자격 요약");
        program.setBenefitOriginal("지원 원문");
        program.setBenefitSummary("지원 요약");
        program.setPurpose("사업 목적");
        program.setApplicationMethod("신청 방법");
        program.setRequiredDocuments("필수 서류");
        program.setSelectionCriteria("선정 기준");
        program.setTargetTagsJson("[\"농업인\"]");
        program.setCropTagsJson("[\"사과\"]");
        program.setRegionTagsJson("[\"충북\"]");
        program.setRawPayload("{\"body\":\"원문\"}");
        persist(program);
        entityManager.clear();

        var loaded = repository.findById(program.getId()).orElseThrow();
        assertThat(loaded.getId()).isNotNull();
        assertThat(loaded).usingRecursiveComparison().isEqualTo(program);
    }

    @Test
    void sameSourceExternalIdAndYearMustBeUnique() {
        var first = program("첫 정책", "기관");
        persist(first);
        var duplicate = program("중복 정책", "기관");
        duplicate.setExternalId(first.getExternalId());
        assertThatThrownBy(() -> persist(duplicate)).isInstanceOf(jakarta.persistence.PersistenceException.class);
    }

    private PolicyProgram program(String title, String agency) {
        var program = new PolicyProgram(title, "정책 상세", "전국", null);
        program.setExternalId(UUID.randomUUID().toString());
        program.setSourceYear("2026");
        program.setAgencyName(agency);
        program.setDetailSynced(true);
        program.setRecommendable(true);
        return program;
    }

    private PolicyProgram persist(PolicyProgram program) {
        ReflectionTestUtils.setField(program, "createdAt", NOW);
        ReflectionTestUtils.setField(program, "updatedAt", NOW);
        return entityManager.persistAndFlush(program);
    }
}
