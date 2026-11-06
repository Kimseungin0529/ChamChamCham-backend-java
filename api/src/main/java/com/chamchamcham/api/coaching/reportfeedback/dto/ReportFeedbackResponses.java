package com.chamchamcham.api.coaching.reportfeedback.dto;


import com.chamchamcham.application.coaching.reportfeedback.lifecycle.*;
import com.chamchamcham.domain.coaching.reportfeedback.ReportFeedbackStatus;
import com.chamchamcham.domain.farming.WorkType;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.lang.Nullable;

public final class ReportFeedbackResponses {
    private ReportFeedbackResponses() { }

    public record ListResponse(
            UUID reportId,
            List<StatusResponse> feedbacks) {
        public ListResponse {
            Objects.requireNonNull(reportId, "reportId");
            Objects.requireNonNull(feedbacks, "feedbacks");
        }
        public UUID getReportId() { return reportId; }
        public List<StatusResponse> getFeedbacks() { return feedbacks; }

        public static ListResponse from(ReportFeedbackListResult source) {
            var feedbacks = new ArrayList<StatusResponse>(source.getFeedbacks().size());
            for (var feedback : source.getFeedbacks()) feedbacks.add(StatusResponse.from(feedback));
            return new ListResponse(source.getReportId(), feedbacks);
        }
    }

    public record StatusResponse(
            UUID feedbackId,
            WorkType workType,
            ReportFeedbackStatus status,
            boolean inputPrepared,
            @Nullable String failureCode,
            @Nullable FeedbackResponse feedback,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        public StatusResponse {
            Objects.requireNonNull(feedbackId, "feedbackId");
            Objects.requireNonNull(workType, "workType");
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(createdAt, "createdAt");
            Objects.requireNonNull(updatedAt, "updatedAt");
        }
        public UUID getFeedbackId() { return feedbackId; }
        public WorkType getWorkType() { return workType; }
        public ReportFeedbackStatus getStatus() { return status; }
        public boolean getInputPrepared() { return inputPrepared; }
        public String getFailureCode() { return failureCode; }
        public FeedbackResponse getFeedback() { return feedback; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }

        public static StatusResponse from(ReportFeedbackDetailResult source) {
            return new StatusResponse(source.getFeedbackId(), source.getWorkType(), source.getStatus(), source.getInputPrepared(),
                    source.getFailureCode(), source.getContent() == null ? null : FeedbackResponse.from(source.getContent()),
                    source.getCreatedAt(), source.getUpdatedAt());
        }
    }

    public record FeedbackResponse(
            String summary,
            List<ItemResponse> comparisons,
            List<ItemResponse> strengths,
            List<ItemResponse> improvements,
            List<ItemResponse> nextActions) {
        public FeedbackResponse {
            Objects.requireNonNull(summary, "summary");
            Objects.requireNonNull(comparisons, "comparisons");
            Objects.requireNonNull(strengths, "strengths");
            Objects.requireNonNull(improvements, "improvements");
            Objects.requireNonNull(nextActions, "nextActions");
        }
        public String getSummary() { return summary; }
        public List<ItemResponse> getComparisons() { return comparisons; }
        public List<ItemResponse> getStrengths() { return strengths; }
        public List<ItemResponse> getImprovements() { return improvements; }
        public List<ItemResponse> getNextActions() { return nextActions; }

        public static FeedbackResponse from(ReportFeedbackResultContent source) {
            return new FeedbackResponse(source.getSummary(), mapItems(source.getComparisons()), mapItems(source.getStrengths()),
                    mapItems(source.getImprovements()), mapItems(source.getNextActions()));
        }

        private static List<ItemResponse> mapItems(List<ReportFeedbackItemResult> source) {
            var items = new ArrayList<ItemResponse>(source.size());
            for (var item : source) items.add(ItemResponse.from(item));
            return items;
        }
    }

    public record ItemResponse(
            String text) {
        public ItemResponse {
            Objects.requireNonNull(text, "text");
        }
        public String getText() { return text; }

        public static ItemResponse from(ReportFeedbackItemResult source) { return new ItemResponse(source.getText()); }
    }
}
