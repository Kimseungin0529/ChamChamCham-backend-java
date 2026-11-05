package com.chamchamcham.application.policy.source.nongupez;

import java.time.LocalDate;
import java.util.Objects;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public record NongupEzPolicyListItem(
    @NonNull String externalId,
    @NonNull String sourceYear,
    @NonNull String title,
    @Nullable String summary,
    @NonNull String agencyName,
    @Nullable LocalDate applyStartsOn,
    @Nullable LocalDate applyEndsOn,
    @NonNull String rawJson
) {
    public NongupEzPolicyListItem {
        Objects.requireNonNull(externalId, "externalId");
        Objects.requireNonNull(sourceYear, "sourceYear");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(agencyName, "agencyName");
        Objects.requireNonNull(rawJson, "rawJson");
    }

    @NonNull
    public String getExternalId() { return externalId; }
    @NonNull
    public String getSourceYear() { return sourceYear; }
    @NonNull
    public String getTitle() { return title; }
    @Nullable
    public String getSummary() { return summary; }
    @NonNull
    public String getAgencyName() { return agencyName; }
    @Nullable
    public LocalDate getApplyStartsOn() { return applyStartsOn; }
    @Nullable
    public LocalDate getApplyEndsOn() { return applyEndsOn; }
    @NonNull
    public String getRawJson() { return rawJson; }
}
