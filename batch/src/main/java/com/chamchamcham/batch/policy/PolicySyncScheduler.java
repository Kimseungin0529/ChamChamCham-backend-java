package com.chamchamcham.batch.policy;

import com.chamchamcham.application.policy.sync.PolicySyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PolicySyncScheduler {
    private static final Logger log = LoggerFactory.getLogger(PolicySyncScheduler.class);

    private final PolicySyncService policySyncService;

    public PolicySyncScheduler(PolicySyncService policySyncService) {
        this.policySyncService = policySyncService;
    }

    @Scheduled(
        cron = "${policy.sync.nongup-ez.cron:0 20 3 * * *}",
        zone = "${policy.sync.nongup-ez.zone:Asia/Seoul}"
    )
    public void syncNongupEzPolicies() {
        var result = policySyncService.runScheduledSync();
        log.info(
            "NongupEZ policy sync finished jobId={} status={} targetYear={}",
            result.getJobId(),
            result.getStatus(),
            result.getTargetYear()
        );
    }
}
