package com.chamchamcham.domain.farming;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface FarmingRecordRepository extends JpaRepository<FarmingRecord, UUID> {
    FarmingRecord findByIdAndMember_Id(UUID id, UUID memberId);
    FarmingRecord findByIdAndIsDeletedFalse(UUID id);
    boolean existsByFarm_Id(UUID farmId);
    boolean existsByMember_IdAndFarm_IdAndCrop_Id(UUID memberId, UUID farmId, UUID cropId);
}
