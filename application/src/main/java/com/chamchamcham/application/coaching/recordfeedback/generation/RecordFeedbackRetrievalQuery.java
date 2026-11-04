package com.chamchamcham.application.coaching.recordfeedback.generation;

import com.chamchamcham.domain.farming.WorkType;
import org.springframework.stereotype.Component;
import java.util.*;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public record RecordFeedbackRetrievalQuery(
    @NonNull String query,
    @NonNull String reason
) {
    public RecordFeedbackRetrievalQuery {
        Objects.requireNonNull(query, "query");
        Objects.requireNonNull(reason, "reason");
    }
    @NonNull public String getQuery() { return query; }
    @NonNull public String getReason() { return reason; }
}
