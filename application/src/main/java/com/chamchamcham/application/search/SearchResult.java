package com.chamchamcham.application.search;

import com.chamchamcham.application.community.CommunityPostResult;
import com.chamchamcham.application.farming.FarmingRecordResult;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;
import java.util.Objects;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public final class SearchResult {
    private SearchResult() {}

    public record RecordPage(
        @NonNull List<FarmingRecordResult.Summary> items,
        @Nullable String nextCursor,
        long totalCount
    ) {
        public RecordPage {
            Objects.requireNonNull(items, "items");
        }

        @NonNull
        public List<FarmingRecordResult.Summary> getItems() { return items; }
        @Nullable
        public String getNextCursor() { return nextCursor; }
        public long getTotalCount() { return totalCount; }
    }

    public record PostPage(
        @NonNull List<CommunityPostResult.PostSummary> items,
        @Nullable String nextCursor,
        long totalCount
    ) {
        public PostPage {
            Objects.requireNonNull(items, "items");
        }

        @NonNull
        public List<CommunityPostResult.PostSummary> getItems() { return items; }
        @Nullable
        public String getNextCursor() { return nextCursor; }
        public long getTotalCount() { return totalCount; }
    }

    public record PolicyItem(
        @NonNull UUID id,
        @NonNull String title,
        @NonNull String agencyName,
        @NonNull String eligibilitySummary,
        @NonNull String benefitSummary,
        @NonNull String applicationPeriodLabel,
        @Nullable LocalDate applyStartsOn,
        @Nullable LocalDate applyEndsOn,
        @Nullable String sourceUrl,
        @NonNull LocalDateTime createdAt
    ) {
        public PolicyItem {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(title, "title");
            Objects.requireNonNull(agencyName, "agencyName");
            Objects.requireNonNull(eligibilitySummary, "eligibilitySummary");
            Objects.requireNonNull(benefitSummary, "benefitSummary");
            Objects.requireNonNull(applicationPeriodLabel, "applicationPeriodLabel");
            Objects.requireNonNull(createdAt, "createdAt");
        }

        @NonNull
        public UUID getId() { return id; }
        @NonNull
        public String getTitle() { return title; }
        @NonNull
        public String getAgencyName() { return agencyName; }
        @NonNull
        public String getEligibilitySummary() { return eligibilitySummary; }
        @NonNull
        public String getBenefitSummary() { return benefitSummary; }
        @NonNull
        public String getApplicationPeriodLabel() { return applicationPeriodLabel; }
        @Nullable
        public LocalDate getApplyStartsOn() { return applyStartsOn; }
        @Nullable
        public LocalDate getApplyEndsOn() { return applyEndsOn; }
        @Nullable
        public String getSourceUrl() { return sourceUrl; }
        @NonNull
        public LocalDateTime getCreatedAt() { return createdAt; }
    }

    public record PolicyPage(
        @NonNull List<PolicyItem> items,
        @Nullable String nextCursor,
        long totalCount
    ) {
        public PolicyPage {
            Objects.requireNonNull(items, "items");
        }

        @NonNull
        public List<PolicyItem> getItems() { return items; }
        @Nullable
        public String getNextCursor() { return nextCursor; }
        public long getTotalCount() { return totalCount; }
    }

    public record All(
        @NonNull RecordPage records,
        @NonNull PolicyPage policies,
        @NonNull PostPage posts
    ) {
        public All {
            Objects.requireNonNull(records, "records");
            Objects.requireNonNull(policies, "policies");
            Objects.requireNonNull(posts, "posts");
        }

        @NonNull
        public RecordPage getRecords() { return records; }
        @NonNull
        public PolicyPage getPolicies() { return policies; }
        @NonNull
        public PostPage getPosts() { return posts; }
    }
}
