package com.chamchamcham.domain.farming;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import java.util.UUID;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.function.Function;

@Repository
public class FarmingCycleReportSourceRepositoryImpl implements FarmingCycleReportSourceRepository {
    private final EntityManager entityManager;

    public FarmingCycleReportSourceRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public FarmingCycleReportSourceSnapshot load(UUID memberId, UUID farmId, UUID cropId) {
        var records = entityManager.createQuery("""
            select record
              from FarmingRecord record
             where record.member.id = :memberId
               and record.farm.id = :farmId
               and record.crop.id = :cropId
               and record.isDeleted = false
             order by record.workedAt asc, record.id asc
            """, FarmingRecord.class)
            .setParameter("memberId", memberId)
            .setParameter("farmId", farmId)
            .setParameter("cropId", cropId)
            .getResultList();
        if (records.isEmpty()) {
            return new FarmingCycleReportSourceSnapshot(List.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Set.of());
        }
        var recordIds = records.stream().map(r -> requireRecordId(r)).toList();
        return new FarmingCycleReportSourceSnapshot(
            records,
            byRecordId(loadDetails(PlantingRecord.class, recordIds), PlantingRecord::getRecord),
            byRecordId(loadDetails(WateringRecord.class, recordIds), WateringRecord::getRecord),
            byRecordId(loadDetails(FertilizingRecord.class, recordIds), FertilizingRecord::getRecord),
            byRecordId(loadDetails(PestControlRecord.class, recordIds), PestControlRecord::getRecord),
            byRecordId(loadDetails(WeedingRecord.class, recordIds), WeedingRecord::getRecord),
            byRecordId(loadDetails(HarvestRecord.class, recordIds), HarvestRecord::getRecord),
            new LinkedHashSet<>(entityManager.createQuery("""
                select distinct media.record.id
                  from FarmingRecordMedia media
                 where media.record.id in :recordIds
                """, UUID.class).setParameter("recordIds", recordIds).getResultList())
        );
    }

    private <T> List<T> loadDetails(Class<T> type, Collection<UUID> recordIds) {
        return entityManager.createQuery("select detail from " + type.getSimpleName() + " detail where detail.record.id in :recordIds", type)
            .setParameter("recordIds", recordIds).getResultList();
    }

    private <T> Map<UUID, T> byRecordId(List<T> details, Function<T, FarmingRecord> recordOf) {
        Map<UUID, T> result = new LinkedHashMap<>();
        for (T detail : details) result.put(requireRecordId(recordOf.apply(detail)), detail);
        return result;
    }

    private static UUID requireRecordId(FarmingRecord record) {
        if (record.getId() == null) throw new IllegalArgumentException("Persisted farming record id is required");
        return record.getId();
    }
}
