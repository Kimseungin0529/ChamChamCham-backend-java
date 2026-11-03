package com.chamchamcham.domain.farming;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface WeedingRecordRepository extends JpaRepository<WeedingRecord, UUID> {
    WeedingRecord findByRecord_Id(UUID recordId);
    void deleteByRecord(FarmingRecord record);
}
