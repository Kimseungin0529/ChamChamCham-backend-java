package com.chamchamcham.application.coaching.recordfeedback.generation;

import com.chamchamcham.application.coaching.common.CoachingTextPolicy;
import org.springframework.stereotype.Component;
import java.util.*;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public record RecordFeedbackEvidence(
    @NonNull String id,
    @NonNull String title,
    @Nullable Integer page,
    @NonNull String content
) {
    public RecordFeedbackEvidence {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(content, "content");
    }
    @NonNull public String getId() { return id; }
    @NonNull public String getTitle() { return title; }
    @Nullable public Integer getPage() { return page; }
    @NonNull public String getContent() { return content; }
}
