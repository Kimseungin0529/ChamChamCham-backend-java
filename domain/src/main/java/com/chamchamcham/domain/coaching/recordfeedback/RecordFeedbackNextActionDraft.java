package com.chamchamcham.domain.coaching.recordfeedback;

import java.util.Objects;

public record RecordFeedbackNextActionDraft(RecordFeedbackActionDue due, RecordFeedbackActionCategory category, String basis, String text) {
    public RecordFeedbackNextActionDraft {
        Objects.requireNonNull(due, "due");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(basis, "basis");
        Objects.requireNonNull(text, "text");
    }
}
