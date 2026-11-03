package com.chamchamcham.domain.report;

import com.chamchamcham.domain.farming.WorkType;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Comparator;

@Repository
public class FarmingCycleReportQueryRepositoryImpl implements FarmingCycleReportQueryRepository {
    private final EntityManager entityManager;

    public FarmingCycleReportQueryRepositoryImpl(EntityManager entityManager) { this.entityManager = entityManager; }

    @Override
    public SearchResult searchCompleted(SearchCondition condition) {
        var scope = completedScope(condition.memberId(), condition.farmIds(), condition.cropIds());
        if (condition.cursor() != null) addCursor(scope, condition.cursor().endsAt(), condition.cursor().finalHarvestRecordId());
        return new SearchResult(findCompleted(scope, condition.size()));
    }

    @Override
    public WorkItemSearchResult searchWorkItems(WorkItemSearchCondition condition) {
        var scope = scope(condition.memberId(), condition.farmIds(), condition.cropIds(), false);
        var query = entityManager.createQuery("""
            select r.id, r.status, r.farm.id, r.farm.name, r.crop.id, r.crop.name,
                   r.startsAt, r.endsAt, r.finalHarvestRecord.id, r.statistics
            from FarmingCycleReport r
            where %s
            order by r.startsAt desc, r.id desc
            """.formatted(String.join(" and ", scope.where())), Object[].class);
        scope.params().forEach(query::setParameter);
        List<WorkType> workTypes = condition.workTypes().isEmpty() ? List.of(WorkType.values()) : condition.workTypes().stream().distinct().toList();
        var items = new ArrayList<WorkItem>();
        for (var row : query.getResultList()) {
            var statistics = (CycleReportStatistics) row[9];
            for (var workType : workTypes) {
                int count = statistics.recordCountFor(workType);
                if (count <= 0) continue;
                items.add(new WorkItem((UUID) row[0], (FarmingCycleReportStatus) row[1], (UUID) row[2], (String) row[3],
                    (UUID) row[4], (String) row[5], (LocalDateTime) row[6], (LocalDateTime) row[7], (UUID) row[8],
                    workType, count, statistics.lastWorkedOnFor(workType)));
            }
        }
        return new WorkItemSearchResult(items.stream().sorted(Comparator.comparing(FarmingCycleReportQueryRepositoryImpl::sortKey, SORT_KEY_COMPARATOR))
            .filter(item -> condition.cursor() == null || SORT_KEY_COMPARATOR.compare(sortKey(item), sortKey(condition.cursor())) > 0)
            .limit(condition.size()).toList());
    }

    @Override
    public FarmingCycleReport findPreviousCompleted(UUID memberId, UUID farmId, UUID cropId, LocalDateTime endsAt, UUID finalHarvestRecordId) {
        var scope = completedScope(memberId, Set.of(farmId), Set.of(cropId));
        addCursor(scope, endsAt, finalHarvestRecordId);
        return findCompleted(scope, 1).stream().findFirst().orElse(null);
    }

    private List<FarmingCycleReport> findCompleted(Scope scope, int maxResults) {
        var query = entityManager.createQuery("""
            select r
            from FarmingCycleReport r
            join fetch r.farm
            join fetch r.crop
            where %s
            order by r.endsAt desc, r.finalHarvestRecord.id desc
            """.formatted(String.join(" and ", scope.where())), FarmingCycleReport.class);
        scope.params().forEach(query::setParameter);
        query.setMaxResults(maxResults);
        return query.getResultList();
    }

    private Scope completedScope(UUID memberId, Set<UUID> farmIds, Set<UUID> cropIds) { return scope(memberId, farmIds, cropIds, true); }

    private Scope scope(UUID memberId, Set<UUID> farmIds, Set<UUID> cropIds, boolean completed) {
        var where = new ArrayList<>(List.of("r.member.id = :memberId", completed ? "r.status = :completed" : "r.status in :visibleStatuses"));
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("memberId", memberId);
        if (completed) params.put("completed", FarmingCycleReportStatus.COMPLETED);
        else params.put("visibleStatuses", Set.of(FarmingCycleReportStatus.ACTIVE, FarmingCycleReportStatus.COMPLETED));
        if (!farmIds.isEmpty()) { where.add("r.farm.id in :farmIds"); params.put("farmIds", farmIds); }
        if (!cropIds.isEmpty()) { where.add("r.crop.id in :cropIds"); params.put("cropIds", cropIds); }
        return new Scope(where, params);
    }

    private void addCursor(Scope scope, LocalDateTime endsAt, UUID finalHarvestRecordId) {
        scope.where().add("""
            (
              r.endsAt < :cursorEndsAt
              or (
                r.endsAt = :cursorEndsAt
                and r.finalHarvestRecord.id < :cursorFinalHarvestRecordId
              )
            )
            """);
        scope.params().put("cursorEndsAt", endsAt);
        scope.params().put("cursorFinalHarvestRecordId", finalHarvestRecordId);
    }

    private static int rank(FarmingCycleReportStatus status) {
        return switch (status) { case ACTIVE -> 0; case COMPLETED -> 1; case SUPERSEDED -> 2; };
    }

    private static WorkItemSortKey sortKey(WorkItem item) {
        return new WorkItemSortKey(item.lastWorkedOn(), rank(item.status()), item.getSortAt(), item.reportId(), item.workType().ordinal());
    }

    private static WorkItemSortKey sortKey(WorkItemCursor cursor) {
        return new WorkItemSortKey(cursor.lastWorkedOn(), rank(cursor.status()), cursor.sortAt(), cursor.reportId(), cursor.workType().ordinal());
    }

    private record Scope(List<String> where, Map<String, Object> params) {}
    private record WorkItemSortKey(LocalDate lastWorkedOn, int statusRank, LocalDateTime sortAt, UUID reportId, int workTypeOrdinal) {}

    private static final Comparator<WorkItemSortKey> SORT_KEY_COMPARATOR =
        Comparator.comparing((WorkItemSortKey key) -> key.lastWorkedOn() == null ? LocalDate.MIN : key.lastWorkedOn(), Comparator.reverseOrder())
            .thenComparingInt(WorkItemSortKey::statusRank)
            .thenComparing(WorkItemSortKey::sortAt, Comparator.reverseOrder())
            .thenComparing(WorkItemSortKey::reportId, Comparator.reverseOrder())
            .thenComparingInt(WorkItemSortKey::workTypeOrdinal);
}
