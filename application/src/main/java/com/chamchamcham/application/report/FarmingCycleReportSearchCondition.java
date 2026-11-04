package com.chamchamcham.application.report;

import java.util.UUID;
import java.util.List;
import java.util.Set;
import java.util.Objects;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public record FarmingCycleReportSearchCondition(
    @NonNull UUID memberId,
    @NonNull Set<UUID> farmIds,
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
