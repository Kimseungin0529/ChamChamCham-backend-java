package com.chamchamcham.application.coaching.recordfeedback.lifecycle;

import com.chamchamcham.domain.coaching.recordfeedback.RecordFeedbackActionCategory;
import com.chamchamcham.domain.coaching.recordfeedback.RecordFeedbackActionDue;
import com.chamchamcham.domain.coaching.recordfeedback.RecordFeedbackStatus;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.*;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public record RecordFeedbackResultContent(
    @NonNull String goodPoint,
    @NonNull List<RecordFeedbackNextActionResult> nextActions
) {
    public RecordFeedbackResultContent {
        Objects.requireNonNull(goodPoint, "goodPoint");
        Objects.requireNonNull(nextActions, "nextActions");
    }
    @NonNull public String getGoodPoint() { return goodPoint; }
    @NonNull public List<RecordFeedbackNextActionResult> getNextActions() { return nextActions; }
}
