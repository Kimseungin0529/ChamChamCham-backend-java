package com.chamchamcham.api.policy.dto.program;

import com.chamchamcham.application.policy.recommendation.PolicyRecommendationResult;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public record PolicyProgramDetailResponse(
    @NonNull UUID id,
    @NonNull String programTitle,
    @NonNull String sourceYear,
    @NonNull String agencyName,
    @Nullable String departmentName,
    @NonNull String applicationPeriodLabel,
    boolean onlineApplyAvailable,
    @Nullable String sourceUrl,
    @Nullable String applicationUrl,
    @Nullable String purpose,
    @Nullable String summary,
    @Nullable String eligibility,
    @Nullable String benefit,
    @Nullable String applicationMethod,
    @Nullable String requiredDocuments,
    @Nullable String selectionCriteria,
    @NonNull List<PolicyContactResponse> contacts,
    @NonNull List<PolicyAttachmentResponse> attachments
) {
    public PolicyProgramDetailResponse {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(programTitle, "programTitle");
        Objects.requireNonNull(sourceYear, "sourceYear");
        Objects.requireNonNull(agencyName, "agencyName");
        Objects.requireNonNull(applicationPeriodLabel, "applicationPeriodLabel");
        Objects.requireNonNull(contacts, "contacts");
        Objects.requireNonNull(attachments, "attachments");
    }

    @NonNull public UUID getId() { return id; }
    @NonNull public String getProgramTitle() { return programTitle; }
    @NonNull public String getSourceYear() { return sourceYear; }
    @NonNull public String getAgencyName() { return agencyName; }
    @Nullable public String getDepartmentName() { return departmentName; }
    @NonNull public String getApplicationPeriodLabel() { return applicationPeriodLabel; }
    public boolean getOnlineApplyAvailable() { return onlineApplyAvailable; }
    @Nullable public String getSourceUrl() { return sourceUrl; }
    @Nullable public String getApplicationUrl() { return applicationUrl; }
    @Nullable public String getPurpose() { return purpose; }
    @Nullable public String getSummary() { return summary; }
    @Nullable public String getEligibility() { return eligibility; }
    @Nullable public String getBenefit() { return benefit; }
    @Nullable public String getApplicationMethod() { return applicationMethod; }
    @Nullable public String getRequiredDocuments() { return requiredDocuments; }
    @Nullable public String getSelectionCriteria() { return selectionCriteria; }
    @NonNull public List<PolicyContactResponse> getContacts() { return contacts; }
    @NonNull public List<PolicyAttachmentResponse> getAttachments() { return attachments; }

    public static PolicyProgramDetailResponse from(PolicyRecommendationResult.Detail result) {
        return new PolicyProgramDetailResponse(
            result.getId(),
            result.getProgramTitle(),
            result.getSourceYear(),
            result.getAgencyName(),
            result.getDepartmentName(),
            result.getApplicationPeriodLabel(),
            result.getOnlineApplyAvailable(),
            result.getSourceUrl(),
            result.getApplicationUrl(),
            result.getPurpose(),
            result.getSummary(),
            result.getEligibility(),
            result.getBenefit(),
            result.getApplicationMethod(),
            result.getRequiredDocuments(),
            result.getSelectionCriteria(),
            result.getContacts().stream().map(PolicyContactResponse::from).collect(Collectors.toCollection(ArrayList::new)),
            result.getAttachments().stream().map(PolicyAttachmentResponse::from).collect(Collectors.toCollection(ArrayList::new))
        );
    }
}
