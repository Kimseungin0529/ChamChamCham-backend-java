package com.chamchamcham.application.farming;

import com.chamchamcham.domain.farming.WorkType;
import java.time.LocalDate;
import java.util.UUID;
import java.util.List;
import java.util.Objects;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public record FarmingRecordSearchCondition(
    @NonNull UUID memberId,
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = com.chamchamcham.application.farming.EmptyCollectionDefaultDeserializer.class)
    @NonNull List<UUID> cropIds,
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = com.chamchamcham.application.farming.EmptyCollectionDefaultDeserializer.class)
    @NonNull List<WorkType> workTypes,
    @Nullable LocalDate startDate,
    @Nullable LocalDate endDate,
    @Nullable String keyword,
    @Nullable String cursor,
    int size
) {
    public FarmingRecordSearchCondition {
        Objects.requireNonNull(memberId, "memberId");
        Objects.requireNonNull(cropIds, "cropIds");
        Objects.requireNonNull(workTypes, "workTypes");
    }

    @NonNull
    public UUID getMemberId() { return memberId; }
    @NonNull
    public List<UUID> getCropIds() { return cropIds; }
    @NonNull
    public List<WorkType> getWorkTypes() { return workTypes; }
    @Nullable
    public LocalDate getStartDate() { return startDate; }
    @Nullable
    public LocalDate getEndDate() { return endDate; }
    @Nullable
    public String getKeyword() { return keyword; }
    @Nullable
    public String getCursor() { return cursor; }
    public int getSize() { return size; }
}
