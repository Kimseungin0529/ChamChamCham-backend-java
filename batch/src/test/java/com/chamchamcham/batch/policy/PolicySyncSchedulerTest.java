package com.chamchamcham.batch.policy;

import com.chamchamcham.application.policy.sync.PolicySyncResult;
import com.chamchamcham.application.policy.sync.PolicySyncService;
import com.chamchamcham.domain.policy.PolicySyncJobStatus;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PolicySyncSchedulerTest {
    @Test
    void scheduledSyncDelegatesToApplicationServiceOnce() {
        PolicySyncService service = mock(PolicySyncService.class);
        when(service.runScheduledSync()).thenReturn(
            new PolicySyncResult.JobSummary(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                PolicySyncJobStatus.SUCCEEDED,
                "2026"
            )
        );

        new PolicySyncScheduler(service).syncNongupEzPolicies();

        verify(service, times(1)).runScheduledSync();
    }
}
