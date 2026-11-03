package com.chamchamcham.domain.farming;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.List;
import java.util.Collection;

public interface FarmingRecordMediaRepository extends JpaRepository<FarmingRecordMedia, UUID> {
    List<FarmingRecordMedia> findByRecord_Id(UUID recordId);
    List<FarmingRecordMedia> findByUploadedMediaIdIn(Collection<UUID> mediaIds);
    void deleteByRecord(FarmingRecord record);
}
