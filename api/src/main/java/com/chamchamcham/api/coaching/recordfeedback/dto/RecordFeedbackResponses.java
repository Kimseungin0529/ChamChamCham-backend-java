package com.chamchamcham.api.coaching.recordfeedback.dto;


import com.chamchamcham.application.coaching.recordfeedback.lifecycle.RecordFeedbackDetailResult;
import com.chamchamcham.application.coaching.recordfeedback.lifecycle.RecordFeedbackResultContent;
import com.chamchamcham.domain.coaching.recordfeedback.RecordFeedbackActionCategory;
import com.chamchamcham.domain.coaching.recordfeedback.RecordFeedbackActionDue;
import com.chamchamcham.domain.coaching.recordfeedback.RecordFeedbackStatus;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.lang.Nullable;

public final class RecordFeedbackResponses {
    private RecordFeedbackResponses() { }

    public record StatusResponse(
            UUID feedbackId,
            UUID recordId,
            RecordFeedbackStatus status,
            long sourceRevision,
            boolean inputPrepared,
            @Nullable String failureCode,
            @Nullable FeedbackResponse feedback,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        public StatusResponse {
            Objects.requireNonNull(feedbackId, "feedbackId");
            Objects.requireNonNull(recordId, "recordId");
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(createdAt, "createdAt");
            Objects.requireNonNull(updatedAt, "updatedAt");
        }
        public UUID getFeedbackId() { return feedbackId; }
        public UUID getRecordId() { return recordId; }
        public RecordFeedbackStatus getStatus() { return status; }
        public long getSourceRevision() { return sourceRevision; }
        public boolean getInputPrepared() { return inputPrepared; }
        public String getFailureCode() { return failureCode; }
        public FeedbackResponse getFeedback() { return feedback; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }

        public static StatusResponse from(RecordFeedbackDetailResult source) {
            return new StatusResponse(source.getFeedbackId(), source.getRecordId(), source.getStatus(), source.getSourceRevision(),
                    source.getInputPrepared(), source.getFailureCode(), source.getContent() == null ? null : FeedbackResponse.from(source.getContent()),
                    source.getCreatedAt(), source.getUpdatedAt());
        }
    }

    public record FeedbackResponse(
            GoodPointResponse goodPoint,
            List<NextActionResponse> nextActions) {
        public FeedbackResponse {
            Objects.requireNonNull(goodPoint, "goodPoint");
            Objects.requireNonNull(nextActions, "nextActions");
        }
        public GoodPointResponse getGoodPoint() { return goodPoint; }
        public List<NextActionResponse> getNextActions() { return nextActions; }

        public static FeedbackResponse from(RecordFeedbackResultContent source) {
            var actions = new ArrayList<NextActionResponse>(source.getNextActions().size());
            for (var action : source.getNextActions()) actions.add(new NextActionResponse(action.getText(), action.getDue(), action.getCategory()));
            return new FeedbackResponse(new GoodPointResponse(source.getGoodPoint()), actions);
        }
    }

    public record GoodPointResponse(
            String text) {
        public GoodPointResponse {
            Objects.requireNonNull(text, "text");
        }
        public String getText() { return text; }
    }

    public record NextActionResponse(
            String text,
            RecordFeedbackActionDue due,
            RecordFeedbackActionCategory category) {
        public NextActionResponse {
            Objects.requireNonNull(text, "text");
            Objects.requireNonNull(due, "due");
            Objects.requireNonNull(category, "category");
        }
        public String getText() { return text; }
        public RecordFeedbackActionDue getDue() { return due; }
        public RecordFeedbackActionCategory getCategory() { return category; }
    }
}
