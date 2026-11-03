package com.chamchamcham.domain.farming;

import java.util.UUID;
import java.util.Set;

public interface FarmingWorkReportSourceRepository {
    FarmingWorkReportSourceSnapshot load(UUID memberId, Set<UUID> farmIds, Set<UUID> cropIds);
}
