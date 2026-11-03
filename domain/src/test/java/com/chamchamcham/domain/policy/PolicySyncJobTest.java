package com.chamchamcham.domain.policy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class PolicySyncJobTest {
    @Test
    void newJobUsesTheExistingDefaults() {
        var before = LocalDateTime.now();

        var job = new PolicySyncJob(PolicySource.NONGUP_EZ, "2026", PolicySyncTriggerType.SCHEDULED, null);

        assertThat(job.getId()).isNull();
        assertThat(job.getStatus()).isEqualTo(PolicySyncJobStatus.RUNNING);
        assertThat(job.getStartedAt()).isBetween(before, LocalDateTime.now());
        assertThat(job.getFinishedAt()).isNull();
        assertThat(job.getTotalCount()).isZero();
        assertThat(job.getSyncedCount()).isZero();
        assertThat(job.getDetailSuccessCount()).isZero();
        assertThat(job.getDetailFailureCount()).isZero();
        assertThat(job.getErrorMessage()).isNull();
        assertThat(job.getCreatedByMemberId()).isNull();
    }

    @Test
    void succeedRecordsAllCountersAndFinishedTimestamp() {
        var job = runningJob();
        var before = LocalDateTime.now();

        job.succeed(372, 371, 360, 11);

        assertThat(job.getStatus()).isEqualTo(PolicySyncJobStatus.SUCCEEDED);
        assertThat(job.getTotalCount()).isEqualTo(372);
        assertThat(job.getSyncedCount()).isEqualTo(371);
        assertThat(job.getDetailSuccessCount()).isEqualTo(360);
        assertThat(job.getDetailFailureCount()).isEqualTo(11);
        assertThat(job.getFinishedAt()).isBetween(before, LocalDateTime.now());
    }

    @Test
    void succeedClearsAnEarlierFailureMessage() {
        var job = runningJob();
        job.fail("previous failure");

        job.succeed(10, 9, 8, 1);

        assertThat(job.getStatus()).isEqualTo(PolicySyncJobStatus.SUCCEEDED);
        assertThat(job.getErrorMessage()).isNull();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 999, 1000, 1001, 1200})
    void failPreservesMessagesUpToOneThousandCharacters(int length) {
        var job = runningJob();
        var before = LocalDateTime.now();

        job.fail("x".repeat(length));

        assertThat(job.getStatus()).isEqualTo(PolicySyncJobStatus.FAILED);
        assertThat(job.getErrorMessage()).isEqualTo("x".repeat(Math.min(length, 1000)));
        assertThat(job.getFinishedAt()).isBetween(before, LocalDateTime.now());
    }

    @Test
    void failKeepsPreviouslyRecordedCounters() {
        var job = runningJob();
        job.succeed(10, 9, 8, 1);

        job.fail("retry failed");

        assertThat(job.getTotalCount()).isEqualTo(10);
        assertThat(job.getSyncedCount()).isEqualTo(9);
        assertThat(job.getDetailSuccessCount()).isEqualTo(8);
        assertThat(job.getDetailFailureCount()).isEqualTo(1);
    }

    @Test
    void nullFailureMessageIsRejected() {
        var job = runningJob();

        assertThatNullPointerException().isThrownBy(() -> job.fail(null));
        assertThat(job.getStatus()).isEqualTo(PolicySyncJobStatus.RUNNING);
        assertThat(job.getErrorMessage()).isNull();
        assertThat(job.getFinishedAt()).isNull();
    }

    private PolicySyncJob runningJob() {
        return new PolicySyncJob(
            null, PolicySource.NONGUP_EZ, "2026", PolicySyncTriggerType.SCHEDULED,
            PolicySyncJobStatus.RUNNING, LocalDateTime.now(), null, 0, 0, 0, 0, null, null
        );
    }
}
