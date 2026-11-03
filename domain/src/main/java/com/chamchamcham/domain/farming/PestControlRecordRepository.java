package com.chamchamcham.domain.farming;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface PestControlRecordRepository extends JpaRepository<PestControlRecord, UUID> {
    PestControlRecord findByRecord_Id(UUID recordId);
    void deleteByRecord(FarmingRecord record);
}
