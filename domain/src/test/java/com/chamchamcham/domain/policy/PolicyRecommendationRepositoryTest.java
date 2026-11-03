package com.chamchamcham.domain.policy;

import com.chamchamcham.domain.common.BaseTimeEntity;
import com.chamchamcham.domain.member.Member;
import com.chamchamcham.domain.member.MemberStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@ContextConfiguration(classes = PolicyJpaTestConfiguration.class)
class PolicyRecommendationRepositoryTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 7, 9, 0);

    @Autowired TestEntityManager entityManager;
    @Autowired PolicyRecommendationRepository repository;

    @Test
    void persistedRecommendationKeepsScoreReasonAndAssociatedEntities() {
        var member = member("member@example.test");
        var recommendation = recommendation(member, "2026", NOW);
        entityManager.clear();

        var loaded = repository.findById(recommendation.getId()).orElseThrow();

        assertThat(loaded.getId()).isNotNull();
        assertThat(loaded.getScore()).isEqualByComparingTo("0.9500");
        assertThat(loaded.getReason()).isEqualTo("농업인 지원");
        assertThat(loaded.getCreatedAt()).isEqualTo(NOW);
        assertThat(loaded.getUpdatedAt()).isEqualTo(NOW);
        assertThat(loaded.getMember().getEmail()).isEqualTo("member@example.test");
        assertThat(loaded.getPolicyProgram().getTitle()).isEqualTo("정책");
    }

    @Test
    void programIdsAndNewestTimestampAreScopedToMemberAndSourceYear() {
        var member = member("member@example.test");
        var older = recommendation(member, "2026", NOW);
        var newer = recommendation(member, "2026", NOW.plusHours(1));
        recommendation(member, "2025", NOW.plusDays(1));
        recommendation(member("other@example.test"), "2026", NOW.plusDays(2));
        entityManager.clear();

        assertThat(repository.findPolicyProgramIdsByMemberIdAndPolicyProgramSourceAndSourceYear(
            member.getId(), PolicySource.NONGUP_EZ, "2026"
        )).containsExactlyInAnyOrder(older.getPolicyProgram().getId(), newer.getPolicyProgram().getId());
        assertThat(repository.findNewestCreatedAtByMemberIdAndPolicyProgramSourceAndSourceYear(
            member.getId(), PolicySource.NONGUP_EZ, "2026"
        )).isEqualTo(NOW.plusHours(1));
    }

    @Test
    void absentRecommendationsReturnEmptyIdsAndNullNewestTimestamp() {
        var member = member("empty@example.test");
        assertThat(repository.findPolicyProgramIdsByMemberIdAndPolicyProgramSourceAndSourceYear(
            member.getId(), PolicySource.NONGUP_EZ, "2026"
        )).isEmpty();
        assertThat(repository.findNewestCreatedAtByMemberIdAndPolicyProgramSourceAndSourceYear(
            member.getId(), PolicySource.NONGUP_EZ, "2026"
        )).isNull();
    }

    @Test
    void bulkDeleteReturnsCountAndKeepsOtherMembersYearsAndPrograms() {
        var member = member("member@example.test");
        var removed = recommendation(member, "2026", NOW);
        recommendation(member, "2026", NOW);
        var otherYear = recommendation(member, "2025", NOW);
        var otherMember = recommendation(member("other@example.test"), "2026", NOW);

        int deleted = repository.deleteByMemberIdAndPolicyProgramSourceAndSourceYear(
            member.getId(), PolicySource.NONGUP_EZ, "2026"
        );
        entityManager.clear();

        assertThat(deleted).isEqualTo(2);
        assertThat(repository.findAll()).extracting(PolicyRecommendation::getId)
            .containsExactlyInAnyOrder(otherYear.getId(), otherMember.getId());
        assertThat(entityManager.find(PolicyProgram.class, removed.getPolicyProgram().getId())).isNotNull();
        assertThat(repository.deleteByMemberIdAndPolicyProgramSourceAndSourceYear(
            member.getId(), PolicySource.NONGUP_EZ, "2026"
        )).isZero();
    }

    private Member member(String email) {
        return persist(new Member(null, email, null, MemberStatus.ACTIVE, null, null, null,
            null, null, null, "ROLE_USER", null, null), NOW);
    }

    private PolicyRecommendation recommendation(Member member, String year, LocalDateTime createdAt) {
        var program = new PolicyProgram("정책", "상세", "전국", null);
        program.setSourceYear(year);
        program.setExternalId(java.util.UUID.randomUUID().toString());
        persist(program, NOW);
        return persist(new PolicyRecommendation(null, member, program, new BigDecimal("0.9500"), "농업인 지원"), createdAt);
    }

    private <T extends BaseTimeEntity> T persist(T entity, LocalDateTime timestamp) {
        ReflectionTestUtils.setField(entity, "createdAt", timestamp);
        ReflectionTestUtils.setField(entity, "updatedAt", timestamp);
        return entityManager.persistAndFlush(entity);
    }
}
