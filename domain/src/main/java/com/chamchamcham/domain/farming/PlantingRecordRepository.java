package com.chamchamcham.domain.farming;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface PlantingRecordRepository extends JpaRepository<PlantingRecord, UUID> {
    PlantingRecord findByRecord_Id(UUID recordId);
    void deleteByRecord(FarmingRecord record);
}
