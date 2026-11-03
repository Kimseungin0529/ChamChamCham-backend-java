package com.chamchamcham.domain.policy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@ContextConfiguration(classes = PolicyEnumPersistenceTest.JpaConfiguration.class)
class PolicyEnumPersistenceTest {
    @Autowired
    private TestEntityManager entityManager;

    @ParameterizedTest
    @CsvSource({
        "SCHEDULED,RUNNING",
        "SCHEDULED,SUCCEEDED",
        "SCHEDULED,FAILED",
        "ADMIN,RUNNING",
        "ADMIN,SUCCEEDED",
        "ADMIN,FAILED"
    })
    void syncJobEnumsKeepTheirDatabaseStringsAndRoundTrip(String triggerKey, String statusKey) {
        var now = LocalDateTime.of(2026, 11, 3, 9, 0);
        var job = new PolicySyncJob(
            null, PolicySource.NONGUP_EZ, "2026",
            PolicySyncTriggerType.valueOf(triggerKey), PolicySyncJobStatus.valueOf(statusKey),
            now, null, 0, 0, 0, 0, null, null
        );
        ReflectionTestUtils.setField(job, "createdAt", now);
        ReflectionTestUtils.setField(job, "updatedAt", now);
        entityManager.persistAndFlush(job);
        entityManager.clear();

        var stored = (Object[]) entityManager.getEntityManager()
            .createNativeQuery("select source, trigger_type, status from policy_sync_job where id = :id")
            .setParameter("id", job.getId())
            .getSingleResult();
        assertThat(stored).containsExactly("NONGUP_EZ", triggerKey, statusKey);

        var loaded = entityManager.find(PolicySyncJob.class, job.getId());
        assertThat(loaded.getSource()).isEqualTo(PolicySource.NONGUP_EZ);
        assertThat(loaded.getTriggerType()).isEqualTo(PolicySyncTriggerType.valueOf(triggerKey));
        assertThat(loaded.getStatus()).isEqualTo(PolicySyncJobStatus.valueOf(statusKey));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EntityScan("com.chamchamcham.domain")
    static class JpaConfiguration {
    }
}
