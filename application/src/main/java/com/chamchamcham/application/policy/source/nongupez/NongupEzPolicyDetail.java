package com.chamchamcham.application.policy.source.nongupez;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public record NongupEzPolicyDetail(
    @NonNull String externalId,
    @NonNull String sourceYear,
    @NonNull String title,
    @Nullable String purpose,
    @Nullable String summary,
    @Nullable String eligibility,
    @Nullable String benefit,
    @Nullable LocalDate applyStartsOn,
    @Nullable LocalDate applyEndsOn,
    @Nullable String applicationMethod,
    @Nullable String requiredDocuments,
    @Nullable String selectionCriteria,
    @NonNull String agencyName,
    @NonNull List<NongupEzPolicyContact> contacts,
    @NonNull List<NongupEzPolicyAttachment> attachments,
    @NonNull String rawJson
) {
    public NongupEzPolicyDetail {
        Objects.requireNonNull(externalId, "externalId");
        Objects.requireNonNull(sourceYear, "sourceYear");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(agencyName, "agencyName");
        Objects.requireNonNull(contacts, "contacts");
        Objects.requireNonNull(attachments, "attachments");
        Objects.requireNonNull(rawJson, "rawJson");
    }

    @NonNull
    public String getExternalId() { return externalId; }
    @NonNull
    public String getSourceYear() { return sourceYear; }
    @NonNull
    public String getTitle() { return title; }
    @Nullable
    public String getPurpose() { return purpose; }
    @Nullable
    public String getSummary() { return summary; }
    @Nullable
    public String getEligibility() { return eligibility; }
    @Nullable
    public String getBenefit() { return benefit; }
    @Nullable
    public LocalDate getApplyStartsOn() { return applyStartsOn; }
    @Nullable
    public LocalDate getApplyEndsOn() { return applyEndsOn; }
    @Nullable
    public String getApplicationMethod() { return applicationMethod; }
    @Nullable
    public String getRequiredDocuments() { return requiredDocuments; }
    @Nullable
    public String getSelectionCriteria() { return selectionCriteria; }
    @NonNull
    public String getAgencyName() { return agencyName; }
    @NonNull
    public List<NongupEzPolicyContact> getContacts() { return contacts; }
    @NonNull
    public List<NongupEzPolicyAttachment> getAttachments() { return attachments; }
    @NonNull
    public String getRawJson() { return rawJson; }
}
