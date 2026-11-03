package com.chamchamcham.domain.policy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@ContextConfiguration(classes = PolicySyncJobRepositoryTest.JpaConfiguration.class)
class PolicySyncJobRepositoryTest {
    private static final LocalDateTime STARTED_AT = LocalDateTime.of(2026, 11, 3, 9, 0);
    private static final UUID MEMBER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PolicySyncJobRepository repository;

    @Test
    void generatedIdAndAllJobFieldsSurvivePersistenceAndReferenceLoading() {
        var job = savedJob("2026", PolicySyncJobStatus.FAILED, STARTED_AT.plusMinutes(1));
        var id = job.getId();
        assertThat(id).isNotNull();
        entityManager.clear();

        var loaded = entityManager.getEntityManager().getReference(PolicySyncJob.class, id);

        assertThat(loaded.getId()).isEqualTo(id);
        assertThat(loaded.getSource()).isEqualTo(PolicySource.NONGUP_EZ);
        assertThat(loaded.getTargetYear()).isEqualTo("2026");
        assertThat(loaded.getTriggerType()).isEqualTo(PolicySyncTriggerType.ADMIN);
        assertThat(loaded.getStatus()).isEqualTo(PolicySyncJobStatus.FAILED);
        assertThat(loaded.getStartedAt()).isEqualTo(STARTED_AT);
        assertThat(loaded.getFinishedAt()).isEqualTo(STARTED_AT.plusMinutes(1));
        assertThat(loaded.getTotalCount()).isEqualTo(12);
        assertThat(loaded.getSyncedCount()).isEqualTo(10);
        assertThat(loaded.getDetailSuccessCount()).isEqualTo(8);
        assertThat(loaded.getDetailFailureCount()).isEqualTo(2);
        assertThat(loaded.getErrorMessage()).isEqualTo("partial failure");
        assertThat(loaded.getCreatedByMemberId()).isEqualTo(MEMBER_ID);
        assertThat(loaded.getCreatedAt()).isEqualTo(STARTED_AT);
        assertThat(loaded.getUpdatedAt()).isEqualTo(STARTED_AT);
    }

    @Test
    void nullableFieldsAndZeroCountersSurvivePersistence() {
        var job = persist(new PolicySyncJob(
            null, PolicySource.NONGUP_EZ, "2026", PolicySyncTriggerType.SCHEDULED,
            PolicySyncJobStatus.RUNNING, STARTED_AT, null, 0, 0, 0, 0, null, null
        ));
        entityManager.clear();

        var loaded = repository.findById(job.getId()).orElseThrow();

        assertThat(loaded.getFinishedAt()).isNull();
        assertThat(loaded.getErrorMessage()).isNull();
        assertThat(loaded.getCreatedByMemberId()).isNull();
        assertThat(loaded.getTotalCount()).isZero();
        assertThat(loaded.getSyncedCount()).isZero();
        assertThat(loaded.getDetailSuccessCount()).isZero();
        assertThat(loaded.getDetailFailureCount()).isZero();
    }

    @Test
    void latestSucceededJobOrdersByTargetYearThenFinishedAtAndExcludesOtherStatuses() {
        savedJob("2025", PolicySyncJobStatus.SUCCEEDED, STARTED_AT.plusDays(10));
        savedJob("2026", PolicySyncJobStatus.SUCCEEDED, STARTED_AT.plusMinutes(1));
        var expected = savedJob("2026", PolicySyncJobStatus.SUCCEEDED, STARTED_AT.plusMinutes(2));
        savedJob("2027", PolicySyncJobStatus.FAILED, STARTED_AT.plusDays(1));
        savedJob("2028", PolicySyncJobStatus.RUNNING, null);
        entityManager.clear();

        var loaded = repository.findFirstBySourceAndStatusOrderByTargetYearDescFinishedAtDesc(
            PolicySource.NONGUP_EZ, PolicySyncJobStatus.SUCCEEDED
        );

        assertThat(loaded).isNotNull();
        assertThat(loaded.getId()).isEqualTo(expected.getId());
    }

    @Test
    void latestJobReturnsNullWhenRepositoryIsEmpty() {
        assertThat(repository.findFirstBySourceAndStatusOrderByTargetYearDescFinishedAtDesc(
            PolicySource.NONGUP_EZ, PolicySyncJobStatus.SUCCEEDED
        )).isNull();
    }

    @Test
    void latestJobReturnsNullWhenNoStatusMatches() {
        savedJob("2026", PolicySyncJobStatus.RUNNING, null);
        savedJob("2026", PolicySyncJobStatus.FAILED, STARTED_AT.plusMinutes(1));
        entityManager.clear();

        assertThat(repository.findFirstBySourceAndStatusOrderByTargetYearDescFinishedAtDesc(
            PolicySource.NONGUP_EZ, PolicySyncJobStatus.SUCCEEDED
        )).isNull();
    }

    private PolicySyncJob savedJob(String year, PolicySyncJobStatus status, LocalDateTime finishedAt) {
        return persist(new PolicySyncJob(
            null, PolicySource.NONGUP_EZ, year, PolicySyncTriggerType.ADMIN, status,
            STARTED_AT, finishedAt, 12, 10, 8, 2, "partial failure", MEMBER_ID
        ));
    }

    private PolicySyncJob persist(PolicySyncJob job) {
        ReflectionTestUtils.setField(job, "createdAt", STARTED_AT);
        ReflectionTestUtils.setField(job, "updatedAt", STARTED_AT);
        return entityManager.persistAndFlush(job);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EntityScan("com.chamchamcham.domain")
    @EnableJpaRepositories(basePackageClasses = PolicySyncJobRepository.class)
    static class JpaConfiguration {
    }
}
