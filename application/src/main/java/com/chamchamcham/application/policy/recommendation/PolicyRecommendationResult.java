package com.chamchamcham.application.policy.recommendation;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;
import java.util.Objects;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public final class PolicyRecommendationResult {
    private PolicyRecommendationResult() {}

    public record Page(
        @NonNull List<Card> items,
        @Nullable String nextCursor
    ) {
        public Page {
            Objects.requireNonNull(items, "items");
        }

        @NonNull
        public List<Card> getItems() { return items; }
        @Nullable
        public String getNextCursor() { return nextCursor; }
    }

    public record Card(
        @NonNull UUID recommendationId,
        @NonNull UUID policyProgramId,
        @NonNull String programTitle,
        @NonNull String eligibilitySummary,
        @NonNull String benefitSummary,
        @NonNull String applicationPeriodLabel,
        @NonNull String agencyName,
        @NonNull BigDecimal score,
        @NonNull String reason,
        @NonNull LocalDateTime createdAt
    ) {
        public Card {
            Objects.requireNonNull(recommendationId, "recommendationId");
            Objects.requireNonNull(policyProgramId, "policyProgramId");
            Objects.requireNonNull(programTitle, "programTitle");
            Objects.requireNonNull(eligibilitySummary, "eligibilitySummary");
            Objects.requireNonNull(benefitSummary, "benefitSummary");
            Objects.requireNonNull(applicationPeriodLabel, "applicationPeriodLabel");
            Objects.requireNonNull(agencyName, "agencyName");
            Objects.requireNonNull(score, "score");
            Objects.requireNonNull(reason, "reason");
            Objects.requireNonNull(createdAt, "createdAt");
        }

        @NonNull
        public UUID getRecommendationId() { return recommendationId; }
        @NonNull
        public UUID getPolicyProgramId() { return policyProgramId; }
        @NonNull
        public String getProgramTitle() { return programTitle; }
        @NonNull
        public String getEligibilitySummary() { return eligibilitySummary; }
        @NonNull
        public String getBenefitSummary() { return benefitSummary; }
        @NonNull
        public String getApplicationPeriodLabel() { return applicationPeriodLabel; }
        @NonNull
        public String getAgencyName() { return agencyName; }
        @NonNull
        public BigDecimal getScore() { return score; }
        @NonNull
        public String getReason() { return reason; }
        @NonNull
        public LocalDateTime getCreatedAt() { return createdAt; }
    }

    public record Detail(
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
        @NonNull List<Contact> contacts,
        @NonNull List<Attachment> attachments
    ) {
        public Detail {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(programTitle, "programTitle");
            Objects.requireNonNull(sourceYear, "sourceYear");
            Objects.requireNonNull(agencyName, "agencyName");
            Objects.requireNonNull(applicationPeriodLabel, "applicationPeriodLabel");
            Objects.requireNonNull(contacts, "contacts");
            Objects.requireNonNull(attachments, "attachments");
        }

        @NonNull
        public UUID getId() { return id; }
        @NonNull
        public String getProgramTitle() { return programTitle; }
        @NonNull
        public String getSourceYear() { return sourceYear; }
        @NonNull
        public String getAgencyName() { return agencyName; }
        @Nullable
        public String getDepartmentName() { return departmentName; }
        @NonNull
        public String getApplicationPeriodLabel() { return applicationPeriodLabel; }
        public boolean getOnlineApplyAvailable() { return onlineApplyAvailable; }
        @Nullable
        public String getSourceUrl() { return sourceUrl; }
        @Nullable
        public String getApplicationUrl() { return applicationUrl; }
        @Nullable
        public String getPurpose() { return purpose; }
        @Nullable
        public String getSummary() { return summary; }
        @Nullable
        public String getEligibility() { return eligibility; }
        @Nullable
        public String getBenefit() { return benefit; }
        @Nullable
        public String getApplicationMethod() { return applicationMethod; }
        @Nullable
        public String getRequiredDocuments() { return requiredDocuments; }
        @Nullable
        public String getSelectionCriteria() { return selectionCriteria; }
        @NonNull
        public List<Contact> getContacts() { return contacts; }
        @NonNull
        public List<Attachment> getAttachments() { return attachments; }
    }

    public record Contact(
        @Nullable String agencyName,
        @Nullable String departmentName,
        @Nullable String phoneNumber
    ) {
        @Nullable
        public String getAgencyName() { return agencyName; }
        @Nullable
        public String getDepartmentName() { return departmentName; }
        @Nullable
        public String getPhoneNumber() { return phoneNumber; }
    }

    public record Attachment(
        @Nullable String fileName,
        @Nullable String extension,
        @Nullable Long sizeBytes,
        @Nullable String url
    ) {
        @Nullable
        public String getFileName() { return fileName; }
        @Nullable
        public String getExtension() { return extension; }
        @Nullable
        public Long getSizeBytes() { return sizeBytes; }
        @Nullable
        public String getUrl() { return url; }
    }
}
