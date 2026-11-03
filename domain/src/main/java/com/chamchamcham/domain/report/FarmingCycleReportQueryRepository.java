package com.chamchamcham.domain.report;

import java.util.UUID;
import java.util.List;
import java.util.Set;
import java.util.Objects;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.chamchamcham.domain.farming.WorkType;

public interface FarmingCycleReportQueryRepository {
    public record Cursor(LocalDateTime endsAt, UUID finalHarvestRecordId) {
        public Cursor {
            Objects.requireNonNull(endsAt, "endsAt");
            Objects.requireNonNull(finalHarvestRecordId, "finalHarvestRecordId");
        }

    }

    public record SearchCondition(UUID memberId, Set<UUID> farmIds, Set<UUID> cropIds, Cursor cursor, int size) {
        public SearchCondition {
            Objects.requireNonNull(memberId, "memberId");
            Objects.requireNonNull(farmIds, "farmIds");
            Objects.requireNonNull(cropIds, "cropIds");
        }

        public SearchCondition(UUID memberId, Cursor cursor, int size) {
            this(memberId, Set.of(), Set.of(), cursor, size);
        }
    }

    public record SearchResult(List<FarmingCycleReport> rows) {
        public SearchResult {
            Objects.requireNonNull(rows, "rows");
        }

    }
    public record WorkItemCursor(
        LocalDate lastWorkedOn,
        FarmingCycleReportStatus status,
        LocalDateTime sortAt,
        UUID reportId,
        WorkType workType
    ) {
        public WorkItemCursor {
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(sortAt, "sortAt");
            Objects.requireNonNull(reportId, "reportId");
            Objects.requireNonNull(workType, "workType");
        }

    }
    public record WorkItemSearchCondition(
        UUID memberId,
        Set<UUID> farmIds,
        Set<UUID> cropIds,
        List<WorkType> workTypes,
        WorkItemCursor cursor,
        int size
    ) {
        public WorkItemSearchCondition {
            Objects.requireNonNull(memberId, "memberId");
            Objects.requireNonNull(farmIds, "farmIds");
            Objects.requireNonNull(cropIds, "cropIds");
            Objects.requireNonNull(workTypes, "workTypes");
        }

        public WorkItemSearchCondition(UUID memberId, WorkItemCursor cursor, int size) {
            this(memberId, Set.of(), Set.of(), List.of(), cursor, size);
        }
    }
    public record WorkItem(
        UUID reportId,
        FarmingCycleReportStatus status,
        UUID farmId,
        String farmName,
        UUID cropId,
        String cropName,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        UUID finalHarvestRecordId,
        WorkType workType,
        int recordCount,
        LocalDate lastWorkedOn
    ) {
        public WorkItem {
            Objects.requireNonNull(reportId, "reportId");
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(farmId, "farmId");
            Objects.requireNonNull(farmName, "farmName");
            Objects.requireNonNull(cropId, "cropId");
            Objects.requireNonNull(cropName, "cropName");
            Objects.requireNonNull(startsAt, "startsAt");
            Objects.requireNonNull(workType, "workType");
        }

        public LocalDateTime getSortAt() { return endsAt != null ? endsAt : startsAt; }
    }

    public record WorkItemSearchResult(List<WorkItem> rows) {
        public WorkItemSearchResult {
            Objects.requireNonNull(rows, "rows");
        }

    }
    SearchResult searchCompleted(SearchCondition condition);
    WorkItemSearchResult searchWorkItems(WorkItemSearchCondition condition);
    FarmingCycleReport findPreviousCompleted(UUID memberId, UUID farmId, UUID cropId, LocalDateTime endsAt, UUID finalHarvestRecordId);
}
