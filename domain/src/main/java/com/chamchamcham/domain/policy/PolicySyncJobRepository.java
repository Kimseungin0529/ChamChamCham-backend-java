package com.chamchamcham.domain.policy;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.UUID;

public interface PolicySyncJobRepository extends JpaRepository<PolicySyncJob, UUID> {
    @Nullable
    PolicySyncJob findFirstBySourceAndStatusOrderByTargetYearDescFinishedAtDesc(
        @NonNull PolicySource source,
        @NonNull PolicySyncJobStatus status
    );
}
