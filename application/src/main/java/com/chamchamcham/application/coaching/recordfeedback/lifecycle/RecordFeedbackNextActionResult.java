package com.chamchamcham.application.coaching.recordfeedback.lifecycle;

import com.chamchamcham.domain.coaching.recordfeedback.RecordFeedbackActionCategory;
import com.chamchamcham.domain.coaching.recordfeedback.RecordFeedbackActionDue;
import com.chamchamcham.domain.coaching.recordfeedback.RecordFeedbackStatus;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.*;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public record RecordFeedbackNextActionResult(
    @NonNull String text,
    @NonNull RecordFeedbackActionDue due,
    @NonNull RecordFeedbackActionCategory category
) {
    public RecordFeedbackNextActionResult {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(due, "due");
        Objects.requireNonNull(category, "category");
    }
    @NonNull public String getText() { return text; }
    @NonNull public RecordFeedbackActionDue getDue() { return due; }
    @NonNull public RecordFeedbackActionCategory getCategory() { return category; }
}
