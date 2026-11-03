package com.chamchamcham.domain.farming;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface WateringRecordRepository extends JpaRepository<WateringRecord, UUID> {
    WateringRecord findByRecord_Id(UUID recordId);
    void deleteByRecord(FarmingRecord record);
}
