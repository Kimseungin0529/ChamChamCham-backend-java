package com.chamchamcham.domain.policy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@ContextConfiguration(classes = PolicyJpaTestConfiguration.class)
class PolicyProgramQueryRepositoryTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 7, 9, 0);

    @Autowired TestEntityManager entityManager;
    @Autowired PolicyProgramQueryRepository queryRepository;

    @Test
    void searchMatchesTitleAgencyOrSummaryIgnoringCase() {
        persist(program("YOUNG farmer", "기관", "요약"), NOW);
        persist(program("귀농 정착", "Young department", "요약"), NOW);
        persist(program("스마트팜", "기관", "young support"), NOW);
        persist(program("무관 정책", "기관", "다른 내용"), NOW);
        entityManager.clear();

        assertThat(queryRepository.search(condition("yOuNg", null, null, 20)))
            .extracting(PolicyProgram::getTitle).containsExactlyInAnyOrder("YOUNG farmer", "귀농 정착", "스마트팜");
    }

    @Test
    void searchExcludesUnsyncedAndNonRecommendablePrograms() {
        persist(program("정상 정책"), NOW);
        var unsynced = program("미동기화");
        unsynced.setDetailSynced(false);
        persist(unsynced, NOW);
        var disabled = program("추천 제외");
        disabled.setRecommendable(false);
        persist(disabled, NOW);
        entityManager.clear();

        assertThat(queryRepository.search(condition(null, null, null, 20)))
            .extracting(PolicyProgram::getTitle).containsExactly("정상 정책");
    }

    @Test
    void searchWithoutKeywordOrdersByCreatedAtDescendingThenIdDescending() {
        var oldest = persist(program("과거"), NOW.minusDays(2));
        var middle = persist(program("중간"), NOW.minusDays(1));
        var newest = persist(program("최근"), NOW);
        entityManager.clear();

        assertThat(queryRepository.search(condition(null, null, null, 20)))
            .extracting(PolicyProgram::getId).containsExactly(newest.getId(), middle.getId(), oldest.getId());
    }

    @Test
    void sameCreatedAtRowsContinueAcrossCursorPagesWithoutDuplicates() {
        persist(program("과거"), NOW.minusDays(1));
        persist(program("동일 A"), NOW);
        persist(program("동일 B"), NOW);
        persist(program("동일 C"), NOW);
        entityManager.clear();
        var full = queryRepository.search(condition(null, null, null, 20));
        var first = queryRepository.search(condition(null, null, null, 1));
        var cursor = first.getFirst();
        var second = queryRepository.search(condition(null, cursor.getCreatedAt(), cursor.getId(), 20));
        var ids = new ArrayList<UUID>();
        first.forEach(row -> ids.add(row.getId()));
        second.forEach(row -> ids.add(row.getId()));

        assertThat(ids).containsExactlyElementsOf(full.stream().map(PolicyProgram::getId).toList());
        assertThat(ids).doesNotHaveDuplicates();
        assertThat(full.subList(0, 3)).extracting(row -> row.getId().toString())
            .isSortedAccordingTo(java.util.Comparator.reverseOrder());
    }

    @Test
    void searchCombinesKeywordAndCursor() {
        persist(program("청년 과거"), NOW.minusDays(1));
        persist(program("무관 정책"), NOW.minusHours(1));
        persist(program("청년 최근"), NOW);
        entityManager.clear();
        var first = queryRepository.search(condition("청년", null, null, 1));
        var cursor = first.getFirst();
        var second = queryRepository.search(condition("청년", cursor.getCreatedAt(), cursor.getId(), 20));

        assertThat(first).extracting(PolicyProgram::getTitle).containsExactly("청년 최근");
        assertThat(second).extracting(PolicyProgram::getTitle).containsExactly("청년 과거");
    }

    @Test
    void countUsesKeywordAndEligibilityFiltersIndependentlyOfPageSize() {
        persist(program("청년 농업인"), NOW);
        persist(program("귀농 정착", "기관", "청년 지원"), NOW);
        persist(program("무관 정책"), NOW);
        var unsynced = program("청년 미동기화");
        unsynced.setDetailSynced(false);
        persist(unsynced, NOW);
        entityManager.clear();

        assertThat(queryRepository.search(condition("청년", null, null, 1))).hasSize(1);
        assertThat(queryRepository.count("청년")).isEqualTo(2);
    }

    @Test
    void countWithoutKeywordReturnsOnlyEligiblePrograms() {
        persist(program("정책 1"), NOW);
        persist(program("정책 2"), NOW);
        var disabled = program("추천 제외");
        disabled.setRecommendable(false);
        persist(disabled, NOW);
        entityManager.clear();

        assertThat(queryRepository.count(null)).isEqualTo(2);
    }

    @Test
    void partialCursorIsIgnoredUnlessTimestampAndIdAreBothPresent() {
        persist(program("정책"), NOW);
        entityManager.clear();
        assertThat(queryRepository.search(condition(null, NOW.minusDays(1), null, 20))).hasSize(1);
        assertThat(queryRepository.search(condition(null, null, UUID.randomUUID(), 20))).hasSize(1);
    }

    @Test
    void unicodeWhitespaceIsTrimmedAndKeywordCaseDoesNotDependOnDefaultLocale() {
        persist(program("income support"), NOW);
        entityManager.clear();
        var previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            assertThat(queryRepository.search(condition("\u00a0 I \u2007", null, null, 20)))
                .extracting(PolicyProgram::getTitle).containsExactly("income support");
            assertThat(queryRepository.count("\u00a0 I \u2007")).isEqualTo(1);
            assertThat(queryRepository.search(condition("\u00a0\u2007", null, null, 20))).hasSize(1);
        } finally {
            Locale.setDefault(previous);
        }
    }

    private PolicyProgramQueryRepository.SearchCondition condition(String keyword, LocalDateTime createdAt, UUID id, int size) {
        return new PolicyProgramQueryRepository.SearchCondition(keyword, createdAt, id, size);
    }

    private PolicyProgram program(String title) {
        return program(title, "기관", title + " 요약");
    }

    private PolicyProgram program(String title, String agency, String summary) {
        var program = new PolicyProgram(title, "정책 상세", "전국", null);
        program.setExternalId(UUID.randomUUID().toString());
        program.setSourceYear("2026");
        program.setAgencyName(agency);
        program.setSummary(summary);
        program.setDetailSynced(true);
        program.setRecommendable(true);
        return program;
    }

    private PolicyProgram persist(PolicyProgram program, LocalDateTime timestamp) {
        ReflectionTestUtils.setField(program, "createdAt", timestamp);
        ReflectionTestUtils.setField(program, "updatedAt", timestamp);
        return entityManager.persistAndFlush(program);
    }
}
