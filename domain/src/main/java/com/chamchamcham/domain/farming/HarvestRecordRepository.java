package com.chamchamcham.domain.farming;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface HarvestRecordRepository extends JpaRepository<HarvestRecord, UUID> {
    HarvestRecord findByRecord_Id(UUID recordId);
    void deleteByRecord(FarmingRecord record);
}
