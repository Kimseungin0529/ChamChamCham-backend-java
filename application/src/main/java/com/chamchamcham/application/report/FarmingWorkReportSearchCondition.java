package com.chamchamcham.application.report;

import com.chamchamcham.domain.farming.WorkType;
import java.util.UUID;
import java.util.List;
import java.util.Set;
import java.util.Objects;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public record FarmingWorkReportSearchCondition(
    @NonNull UUID memberId,
    @NonNull Set<UUID> farmIds,
    @NonNull Set<UUID> cropIds,
    @NonNull List<WorkType> workTypes,
    @Nullable String cursor,
    int size
) {
    public FarmingWorkReportSearchCondition {
        Objects.requireNonNull(memberId, "memberId");
        Objects.requireNonNull(farmIds, "farmIds");
        Objects.requireNonNull(cropIds, "cropIds");
        Objects.requireNonNull(workTypes, "workTypes");
    }

    @NonNull
    public UUID getMemberId() { return memberId; }
    @NonNull
    public Set<UUID> getFarmIds() { return farmIds; }
    @NonNull
    public Set<UUID> getCropIds() { return cropIds; }
    @NonNull
    public List<WorkType> getWorkTypes() { return workTypes; }
    @Nullable
    public String getCursor() { return cursor; }
    public int getSize() { return size; }
}
