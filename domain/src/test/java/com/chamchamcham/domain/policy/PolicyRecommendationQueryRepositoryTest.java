package com.chamchamcham.domain.policy;

import com.chamchamcham.domain.common.BaseTimeEntity;
import com.chamchamcham.domain.member.Member;
import com.chamchamcham.domain.member.MemberStatus;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@ActiveProfiles("test")
@ContextConfiguration(classes = PolicyJpaTestConfiguration.class)
class PolicyRecommendationQueryRepositoryTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 7, 9, 0);
    private static final PolicyRecommendationSort RECOMMENDED = PolicyRecommendationSort.RECOMMENDED;
    private static final PolicyRecommendationSort LATEST = PolicyRecommendationSort.LATEST;

    @Autowired TestEntityManager entityManager;
    @Autowired PolicyRecommendationQueryRepository queryRepository;
    private Member member;
    private Member otherMember;

    @BeforeEach
    void setUp() {
        member = member("policy@example.test");
        otherMember = member("other-policy@example.test");
    }

    @Test
    void pageDoesNotLoadPolicyProgramsWithOneSelectPerCard() {
        recommendation("정책 A", "0.9500", null, null);
        recommendation("정책 B", "0.9000", null, null);
        recommendation("정책 C", "0.8500", null, null);
        entityManager.clear();
        var statistics = entityManager.getEntityManager().getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        var result = queryRepository.findPage(condition(null, RECOMMENDED, null, 10));
        var cardFields = result.rows().stream().map(row -> List.of(row.getPolicyProgram().getTitle(),
            row.getPolicyProgram().getEligibilitySummary(), row.getPolicyProgram().getBenefitSummary(),
            row.getPolicyProgram().getAgencyName())).toList();

        assertThat(cardFields).hasSize(3);
        assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(2L);
    }

    @Test
    void recommendedPageFiltersMemberAndYearAndOrdersScoreThenDeadline() {
        recommendation("높은 점수", "0.9500", null, LocalDate.of(2026, 7, 20));
        recommendation("동점 빠른 마감", "0.9000", null, LocalDate.of(2026, 7, 10));
        recommendation("동점 늦은 마감", "0.9000", null, LocalDate.of(2026, 8, 1));
        recommendation("마감 없음", "0.8000", null, null);
        recommendation(otherMember, "2026", "다른 회원", "0.9900", null, null, "지원 확인");
        recommendation(member, "2025", "다른 연도", "0.9900", null, null, "지원 확인");
        entityManager.clear();

        var first = queryRepository.findPage(condition(null, RECOMMENDED, null, 2)).rows();
        var second = queryRepository.findPage(condition(null, RECOMMENDED, recommendedCursor(first.getLast()), 10)).rows();

        assertThat(first).extracting(row -> row.getPolicyProgram().getTitle()).containsExactly("높은 점수", "동점 빠른 마감");
        assertThat(second).extracting(row -> row.getPolicyProgram().getTitle()).containsExactly("동점 늦은 마감", "마감 없음");
    }

    @Test
    void maximumDeadlinePrecedesNullDeadlinesAcrossRecommendedPages() {
        recommendation("후보 A", "0.9000", null, null);
        recommendation("후보 B", "0.9000", null, null);
        recommendation("후보 C", "0.9000", null, null);
        var initial = queryRepository.findPage(condition(null, RECOMMENDED, null, 20)).rows();
        var dated = initial.getLast();
        dated.getPolicyProgram().setApplyEndsOn(LocalDate.of(9999, 12, 31));
        var expected = List.of(dated.getId(), initial.get(0).getId(), initial.get(1).getId());
        entityManager.flush();
        entityManager.clear();

        var first = queryRepository.findPage(condition(null, RECOMMENDED, null, 1)).rows();
        var second = queryRepository.findPage(condition(null, RECOMMENDED, recommendedCursor(first.getLast()), 1)).rows();
        var third = queryRepository.findPage(condition(null, RECOMMENDED, recommendedCursor(second.getLast()), 20)).rows();
        var ids = new ArrayList<UUID>();
        first.forEach(row -> ids.add(row.getId()));
        second.forEach(row -> ids.add(row.getId()));
        third.forEach(row -> ids.add(row.getId()));

        assertThat(ids).containsExactlyElementsOf(expected);
    }

    @Test
    void benefitSummaryFiltersRecommendations() {
        recommendation(member, "2026", "융자 지원", "0.9000", null, null, "융자/금융");
        recommendation(member, "2026", "직불 지원", "0.9500", null, null, "직불/수당");
        entityManager.clear();

        assertThat(queryRepository.findPage(condition("융자/금융", RECOMMENDED, null, 20)).rows())
            .extracting(row -> row.getPolicyProgram().getTitle()).containsExactly("융자 지원");
    }

    @Test
    void latestPageOrdersStartDateDescendingWithNullLastAndCursor() {
        recommendation("최근", "0.7000", LocalDate.of(2026, 7, 1), null);
        recommendation("중간", "0.9000", LocalDate.of(2026, 6, 1), null);
        recommendation("과거", "0.9500", LocalDate.of(2026, 5, 1), null);
        recommendation("시작일 없음", "0.9900", null, null);
        entityManager.clear();
        var first = queryRepository.findPage(condition(null, LATEST, null, 2)).rows();
        var second = queryRepository.findPage(condition(null, LATEST, latestCursor(first.getLast()), 20)).rows();

        assertThat(first).extracting(row -> row.getPolicyProgram().getTitle()).containsExactly("최근", "중간");
        assertThat(second).extracting(row -> row.getPolicyProgram().getTitle()).containsExactly("과거", "시작일 없음");
    }

    @Test
    void equalLatestDatesAreOrderedById() {
        recommendation("동일 A", "0.9000", LocalDate.of(2026, 6, 1), null);
        recommendation("동일 B", "0.9000", LocalDate.of(2026, 6, 1), null);
        entityManager.clear();
        assertThat(queryRepository.findPage(condition(null, LATEST, null, 20)).rows())
            .extracting(row -> row.getId().toString()).isSorted();
    }

    @Test
    void sameDateLatestRowsContinueAcrossPagesWithoutDuplicates() {
        var date = LocalDate.of(2026, 6, 1);
        recommendation("동일 A", "0.9000", date, null);
        recommendation("동일 B", "0.9000", date, null);
        recommendation("동일 C", "0.9000", date, null);
        recommendation("이전 날짜", "0.9000", date.minusDays(1), null);
        entityManager.clear();
        assertLatestPages(1);
    }

    @Test
    void nullStartLatestRowsContinueAcrossPagesWithoutDuplicates() {
        recommendation("날짜 있음", "0.9000", LocalDate.of(2026, 6, 1), null);
        recommendation("시작일 없음 A", "0.9000", null, null);
        recommendation("시작일 없음 B", "0.9000", null, null);
        recommendation("시작일 없음 C", "0.9000", null, null);
        entityManager.clear();
        assertLatestPages(2);
    }

    @Test
    void recommendedCursorRequiresScoreBeforeExecutingQuery() {
        var cursor = new PolicyRecommendationQueryRepository.Cursor(null, null, null, UUID.randomUUID());
        assertThatIllegalArgumentException().isThrownBy(() -> queryRepository.findPage(condition(null, RECOMMENDED, cursor, 20)));
    }

    @Test
    void equalRecommendedScoresAndDeadlinesContinueAcrossPagesWithoutDuplicates() {
        var deadline = LocalDate.of(2026, 8, 1);
        recommendation("동일 A", "0.9000", null, deadline);
        recommendation("동일 B", "0.9000", null, deadline);
        recommendation("동일 C", "0.9000", null, deadline);
        entityManager.clear();
        var full = queryRepository.findPage(condition(null, RECOMMENDED, null, 20)).rows();
        var first = queryRepository.findPage(condition(null, RECOMMENDED, null, 1)).rows();
        var second = queryRepository.findPage(condition(null, RECOMMENDED, recommendedCursor(first.getLast()), 20)).rows();
        var ids = new ArrayList<UUID>();
        first.forEach(row -> ids.add(row.getId()));
        second.forEach(row -> ids.add(row.getId()));
        assertThat(full).extracting(row -> row.getId().toString()).isSorted();
        assertThat(ids).containsExactlyElementsOf(full.stream().map(PolicyRecommendation::getId).toList());
        assertThat(ids).doesNotHaveDuplicates();
    }

    private void assertLatestPages(int size) {
        var full = queryRepository.findPage(condition(null, LATEST, null, 20)).rows();
        var first = queryRepository.findPage(condition(null, LATEST, null, size)).rows();
        var second = queryRepository.findPage(condition(null, LATEST, latestCursor(first.getLast()), 20)).rows();
        var ids = new ArrayList<UUID>();
        first.forEach(row -> ids.add(row.getId()));
        second.forEach(row -> ids.add(row.getId()));
        assertThat(ids).containsExactlyElementsOf(full.stream().map(PolicyRecommendation::getId).toList());
        assertThat(ids).doesNotHaveDuplicates();
    }

    private PolicyRecommendationQueryRepository.SearchCondition condition(String benefit, PolicyRecommendationSort sort,
        PolicyRecommendationQueryRepository.Cursor cursor, int size) {
        return new PolicyRecommendationQueryRepository.SearchCondition(member.getId(), PolicySource.NONGUP_EZ, "2026", benefit, sort, cursor, size);
    }

    private PolicyRecommendationQueryRepository.Cursor recommendedCursor(PolicyRecommendation row) {
        return new PolicyRecommendationQueryRepository.Cursor(row.getScore(), null, row.getPolicyProgram().getApplyEndsOn(), row.getId());
    }

    private PolicyRecommendationQueryRepository.Cursor latestCursor(PolicyRecommendation row) {
        return new PolicyRecommendationQueryRepository.Cursor(null, row.getPolicyProgram().getApplyStartsOn(), null, row.getId());
    }

    private PolicyRecommendation recommendation(String title, String score, LocalDate start, LocalDate end) {
        return recommendation(member, "2026", title, score, start, end, "지원 확인");
    }

    private PolicyRecommendation recommendation(Member owner, String year, String title, String score, LocalDate start, LocalDate end, String benefit) {
        var program = new PolicyProgram(title, "정책 상세", "전국", null);
        program.setExternalId(UUID.randomUUID().toString());
        program.setSourceYear(year);
        program.setSummary(title + " 요약");
        program.setAgencyName("농림축산식품부");
        program.setEligibilitySummary("자격 확인");
        program.setBenefitSummary(benefit);
        program.setDetailSynced(true);
        program.setRecommendable(true);
        program.setApplyStartsOn(start);
        program.setApplyEndsOn(end);
        persist(program, NOW.plusMinutes(10));
        return persist(new PolicyRecommendation(owner, program, new BigDecimal(score), "추천 사유"), NOW.plusMinutes(20));
    }

    private Member member(String email) {
        return persist(new Member(null, email, null, MemberStatus.ACTIVE, null, null, null,
            null, null, null, "ROLE_USER", null, null), NOW);
    }

    private <T extends BaseTimeEntity> T persist(T entity, LocalDateTime timestamp) {
        ReflectionTestUtils.setField(entity, "createdAt", timestamp);
        ReflectionTestUtils.setField(entity, "updatedAt", timestamp);
        return entityManager.persistAndFlush(entity);
    }
}
