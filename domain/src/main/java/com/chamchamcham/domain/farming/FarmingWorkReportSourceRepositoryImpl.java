package com.chamchamcham.domain.farming;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import java.util.UUID;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Comparator;

@Repository
public class FarmingWorkReportSourceRepositoryImpl implements FarmingWorkReportSourceRepository {
    private static final UUID EMPTY_RECORD_ID = new UUID(0, 0);
    private final EntityManager entityManager;

    public FarmingWorkReportSourceRepositoryImpl(EntityManager entityManager) { this.entityManager = entityManager; }

    @Override
    public FarmingWorkReportSourceSnapshot load(UUID memberId, Set<UUID> farmIds, Set<UUID> cropIds) {
        var records = findRecords(memberId, farmIds, cropIds);
        Set<UUID> recordIds = new LinkedHashSet<>();
        for (var record : records) {
            if (record.getId() == null) throw new IllegalArgumentException("Persisted farming record id is required");
            recordIds.add(record.getId());
        }
        var queryRecordIds = orEmptyRecordId(recordIds);
        return new FarmingWorkReportSourceSnapshot(records, findFinalHarvestRecordIds(queryRecordIds), findFirstImageUrls(queryRecordIds));
    }

    private List<FarmingRecord> findRecords(UUID memberId, Set<UUID> farmIds, Set<UUID> cropIds) {
        return entityManager.createQuery("""
            select distinct r
            from FarmingRecord r
            join fetch r.member
            join fetch r.farm f
            left join fetch f.boundaryCoordinates
            join fetch r.crop
            where r.member.id = :memberId
              and r.farm.id in :farmIds
              and r.crop.id in :cropIds
              and r.isDeleted = false
            """, FarmingRecord.class)
            .setParameter("memberId", memberId)
            .setParameter("farmIds", orEmptyRecordId(farmIds))
            .setParameter("cropIds", orEmptyRecordId(cropIds)).getResultList();
    }

    private Set<UUID> findFinalHarvestRecordIds(Set<UUID> recordIds) {
        return new LinkedHashSet<>(entityManager.createQuery("""
            select h.record.id
            from HarvestRecord h
            where h.record.id in :recordIds
              and h.isLastHarvest = true
            """, UUID.class).setParameter("recordIds", recordIds).getResultList());
    }

    private Map<UUID, String> findFirstImageUrls(Set<UUID> recordIds) {
        var rows = entityManager.createQuery("""
            select m.record.id, m.id, m.displayOrder, m.uploadedMedia.fileUrl
            from FarmingRecordMedia m
            where m.record.id in :recordIds
            """, Object[].class).setParameter("recordIds", recordIds).getResultList();
        Map<UUID, MediaProjection> firstMedia = new LinkedHashMap<>();
        var order = Comparator.comparingInt(MediaProjection::displayOrder).thenComparing(MediaProjection::mediaId);
        for (var row : rows) {
            var media = new MediaProjection((UUID) row[0], (UUID) row[1], (Integer) row[2], (String) row[3]);
            firstMedia.merge(media.recordId(), media, (a, b) -> order.compare(a, b) <= 0 ? a : b);
        }
        Map<UUID, String> result = new LinkedHashMap<>();
        firstMedia.forEach((id, media) -> result.put(id, media.fileUrl()));
        return result;
    }

    private static Set<UUID> orEmptyRecordId(Set<UUID> ids) { return ids.isEmpty() ? Set.of(EMPTY_RECORD_ID) : ids; }

    private record MediaProjection(UUID recordId, UUID mediaId, int displayOrder, String fileUrl) {}
}
