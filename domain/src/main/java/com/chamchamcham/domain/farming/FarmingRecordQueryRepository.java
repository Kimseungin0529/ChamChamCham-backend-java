package com.chamchamcham.domain.farming;

import java.util.UUID;
import java.util.List;
import java.util.Objects;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.chamchamcham.domain.crop.CropUsePartCategory;

public interface FarmingRecordQueryRepository {
    public record SearchCondition(
        UUID memberId,
        List<UUID> cropIds,
        List<WorkType> workTypes,
        LocalDateTime workedAtFrom,
        LocalDateTime workedAtTo,
        String keyword,
        List<WorkType> matchedWorkTypes,
        List<CropUsePartCategory> matchedParts,
        Cursor cursor,
        int size
    ) {
        public SearchCondition {
            Objects.requireNonNull(memberId, "memberId");
            Objects.requireNonNull(cropIds, "cropIds");
            Objects.requireNonNull(workTypes, "workTypes");
            Objects.requireNonNull(matchedWorkTypes, "matchedWorkTypes");
            Objects.requireNonNull(matchedParts, "matchedParts");
        }

        public SearchCondition(UUID memberId, LocalDateTime workedAtFrom, LocalDateTime workedAtTo, Cursor cursor, int size) {
            this(memberId, List.of(), List.of(), workedAtFrom, workedAtTo, null, List.of(), List.of(), cursor, size);
        }
    }

    public record Cursor(LocalDateTime workedAt, UUID id) {
        public Cursor {
            Objects.requireNonNull(workedAt, "workedAt");
            Objects.requireNonNull(id, "id");
        }

    }
    public record Row(
        FarmingRecord record,
        String thumbnailUrl,
        IrrigationMethod irrigationMethod,
        BigDecimal harvestAmount,
        String pesticideName,
        WeedingMethod weedingMethod,
        PlantingMethod plantingMethod,
        String materialName
    ) {
        public Row {
            Objects.requireNonNull(record, "record");
        }

        public Row(FarmingRecord record, String thumbnailUrl) {
            this(record, thumbnailUrl, null, null, null, null, null, null);
        }
    }

    public record SearchResult(List<Row> rows) {
        public SearchResult {
            Objects.requireNonNull(rows, "rows");
        }

    }
    SearchResult search(SearchCondition condition);
    long count(SearchCondition condition);
}
