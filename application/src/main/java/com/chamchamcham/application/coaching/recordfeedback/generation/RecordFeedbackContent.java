package com.chamchamcham.application.coaching.recordfeedback.generation;

import com.chamchamcham.domain.coaching.recordfeedback.RecordFeedbackActionCategory;
import com.chamchamcham.domain.coaching.recordfeedback.RecordFeedbackActionDue;
import java.util.*;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public record RecordFeedbackContent(
    @NonNull RecordFeedbackGoodPoint goodPoint,
    @NonNull List<RecordFeedbackAction> nextActions
) {
    public RecordFeedbackContent {
        Objects.requireNonNull(goodPoint, "goodPoint");
        Objects.requireNonNull(nextActions, "nextActions");
    }
    @NonNull public RecordFeedbackGoodPoint getGoodPoint() { return goodPoint; }
    @NonNull public List<RecordFeedbackAction> getNextActions() { return nextActions; }
}
