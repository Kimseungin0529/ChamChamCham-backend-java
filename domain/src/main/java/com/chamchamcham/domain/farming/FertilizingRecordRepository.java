package com.chamchamcham.domain.farming;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface FertilizingRecordRepository extends JpaRepository<FertilizingRecord, UUID> {
    FertilizingRecord findByRecord_Id(UUID recordId);
    void deleteByRecord(FarmingRecord record);
}
