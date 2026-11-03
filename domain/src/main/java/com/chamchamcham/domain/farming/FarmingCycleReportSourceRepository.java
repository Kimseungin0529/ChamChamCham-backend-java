package com.chamchamcham.domain.farming;

import java.util.UUID;

public interface FarmingCycleReportSourceRepository {
    FarmingCycleReportSourceSnapshot load(UUID memberId, UUID farmId, UUID cropId);
}
