package com.chamchamcham.application.coaching.reportfeedback.generation;

import com.chamchamcham.domain.coaching.reportfeedback.ReportFeedbackItemSection;
import java.util.*;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public record ReportFeedbackEvidence(
    @NonNull String id,
    @NonNull String title,
    @NonNull String content
) {
    public ReportFeedbackEvidence {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(content, "content");
    }
    @NonNull public String getId() { return id; }
    @NonNull public String getTitle() { return title; }
    @NonNull public String getContent() { return content; }
}
