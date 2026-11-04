package com.chamchamcham.application.report;

import java.util.UUID;
import java.util.Set;
import java.util.Objects;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public record FarmingCycleReportSearchCondition(
    @NonNull UUID memberId,
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = com.chamchamcham.application.farming.EmptyCollectionDefaultDeserializer.class)
    @NonNull Set<UUID> farmIds,
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = com.chamchamcham.application.farming.EmptyCollectionDefaultDeserializer.class)
    @NonNull Set<UUID> cropIds,
    @Nullable String cursor,
    int size
) {
    public FarmingCycleReportSearchCondition {
        Objects.requireNonNull(memberId, "memberId");
        Objects.requireNonNull(farmIds, "farmIds");
        Objects.requireNonNull(cropIds, "cropIds");
    }

    @NonNull
    public UUID getMemberId() { return memberId; }
    @NonNull
    public Set<UUID> getFarmIds() { return farmIds; }
    @NonNull
    public Set<UUID> getCropIds() { return cropIds; }
    @Nullable
    public String getCursor() { return cursor; }
    public int getSize() { return size; }
}
