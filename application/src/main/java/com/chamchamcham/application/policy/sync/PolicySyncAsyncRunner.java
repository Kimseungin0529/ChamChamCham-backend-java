package com.chamchamcham.application.policy.sync;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import java.util.Objects;
import java.util.UUID;

@Component
public class PolicySyncAsyncRunner {
    private final PolicySyncService policySyncService;
    public PolicySyncAsyncRunner(PolicySyncService policySyncService) { this.policySyncService = Objects.requireNonNull(policySyncService, "policySyncService"); }
    @Async
    public void run(UUID jobId) {
        Objects.requireNonNull(jobId, "jobId");
        policySyncService.runExistingJob(jobId);
    }
}
