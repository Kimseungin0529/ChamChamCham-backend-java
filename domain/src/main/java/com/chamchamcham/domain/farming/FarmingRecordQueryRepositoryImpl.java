package com.chamchamcham.domain.farming;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;

@Repository
public class FarmingRecordQueryRepositoryImpl implements FarmingRecordQueryRepository {
    private final EntityManager entityManager;

    public FarmingRecordQueryRepositoryImpl(EntityManager entityManager) { this.entityManager = entityManager; }

    @Override
    public SearchResult search(SearchCondition condition) {
        var records = findRecords(condition);
        if (records.isEmpty()) return new SearchResult(List.of());
        var recordIds = records.stream().map(row -> requireRecordId((FarmingRecord) row[0])).toList();
        var thumbnails = findThumbnails(recordIds);
        return new SearchResult(records.stream().map(row -> {
            var record = (FarmingRecord) row[0];
            return new Row(record, thumbnails.get(record.getId()), (IrrigationMethod) row[1], (BigDecimal) row[2],
                (String) row[3], (WeedingMethod) row[4], (PlantingMethod) row[5], (String) row[6]);
        }).toList());
    }

    @Override
    public long count(SearchCondition condition) {
        var filter = buildFilterPredicates(condition);
        var query = entityManager.createQuery("select count(r) from FarmingRecord r where " + String.join(" and ", filter.where()), Long.class);
        filter.params().forEach(query::setParameter);
        return query.getSingleResult();
    }

    private Filter buildFilterPredicates(SearchCondition condition) {
        var where = new ArrayList<>(List.of("r.isDeleted = false", "r.member.id = :memberId"));
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("memberId", condition.memberId());
        if (!condition.cropIds().isEmpty()) { where.add("r.crop.id in :cropIds"); params.put("cropIds", condition.cropIds()); }
        if (!condition.workTypes().isEmpty()) { where.add("r.workType in :workTypes"); params.put("workTypes", condition.workTypes()); }
        if (condition.workedAtFrom() != null) { where.add("r.workedAt >= :workedAtFrom"); params.put("workedAtFrom", condition.workedAtFrom()); }
        if (condition.workedAtTo() != null) { where.add("r.workedAt < :workedAtTo"); params.put("workedAtTo", condition.workedAtTo()); }
        var keywordPredicates = new ArrayList<String>();
        String keyword = condition.keyword() == null ? null : condition.keyword().replaceAll("^[\\p{javaWhitespace}\\p{javaSpaceChar}]+|[\\p{javaWhitespace}\\p{javaSpaceChar}]+$", "").toLowerCase(Locale.ROOT);
        if (keyword != null && !keyword.isEmpty()) {
            params.put("keyword", "%" + keyword + "%");
            keywordPredicates.add("lower(r.crop.name) like :keyword");
            keywordPredicates.add("lower(r.memo) like :keyword");
            keywordPredicates.add("exists (select 1 from FertilizingRecord f where f.record.id = r.id and lower(f.materialName) like :keyword)");
            keywordPredicates.add("exists (select 1 from PestControlRecord p where p.record.id = r.id and (lower(p.pesticide.brandName) like :keyword or lower(p.pesticide.itemName) like :keyword))");
        }
        if (!condition.matchedWorkTypes().isEmpty()) { keywordPredicates.add("r.workType in :matchedWorkTypes"); params.put("matchedWorkTypes", condition.matchedWorkTypes()); }
        if (!condition.matchedParts().isEmpty()) { keywordPredicates.add("exists (select 1 from HarvestRecord h where h.record.id = r.id and h.medicinalPart in :matchedParts)"); params.put("matchedParts", condition.matchedParts()); }
        if (!keywordPredicates.isEmpty()) where.add("(" + String.join(" or ", keywordPredicates) + ")");
        return new Filter(where, params);
    }

    private List<Object[]> findRecords(SearchCondition condition) {
        var filter = buildFilterPredicates(condition);
        if (condition.cursor() != null) {
            filter.where().add("(r.workedAt < :cursorWorkedAt or (r.workedAt = :cursorWorkedAt and r.id < :cursorId))");
            filter.params().put("cursorWorkedAt", condition.cursor().workedAt());
            filter.params().put("cursorId", condition.cursor().id());
        }
        var query = entityManager.createQuery("""
            select r, w.irrigationMethod, h.harvestAmount, pest.itemName, wd.weedingMethod, pl.plantingMethod, fz.materialName
            from FarmingRecord r
            left join WateringRecord w on w.record = r
            left join HarvestRecord h on h.record = r
            left join PestControlRecord p on p.record = r
            left join p.pesticide pest
            left join WeedingRecord wd on wd.record = r
            left join PlantingRecord pl on pl.record = r
            left join FertilizingRecord fz on fz.record = r
            where %s
            order by r.workedAt desc, r.id desc
            """.formatted(String.join(" and ", filter.where())), Object[].class);
        filter.params().forEach(query::setParameter);
        query.setMaxResults(condition.size());
        return query.getResultList();
    }

    private Map<UUID, String> findThumbnails(List<UUID> recordIds) {
        var media = entityManager.createQuery("""
            select m from FarmingRecordMedia m
            where m.record.id in :recordIds
            order by m.displayOrder asc
            """, FarmingRecordMedia.class).setParameter("recordIds", recordIds).getResultList();
        Map<UUID, String> result = new LinkedHashMap<>();
        for (var item : media) result.putIfAbsent(requireRecordId(item.getRecord()), item.getUploadedMedia().getFileUrl());
        return result;
    }

    private static UUID requireRecordId(FarmingRecord record) {
        if (record.getId() == null) throw new IllegalArgumentException("Persisted farming record id is required");
        return record.getId();
    }

    private record Filter(List<String> where, Map<String, Object> params) {}
}
