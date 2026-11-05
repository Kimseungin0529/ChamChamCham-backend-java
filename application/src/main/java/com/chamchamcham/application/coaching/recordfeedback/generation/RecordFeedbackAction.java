package com.chamchamcham.application.coaching.recordfeedback.generation;

import com.chamchamcham.domain.coaching.recordfeedback.RecordFeedbackActionCategory;
import com.chamchamcham.domain.coaching.recordfeedback.RecordFeedbackActionDue;
import java.util.*;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public record RecordFeedbackAction(
    @NonNull RecordFeedbackActionDue due,
    @NonNull RecordFeedbackActionCategory category,
    @NonNull String basis,
    @NonNull String text,
    @NonNull List<String> evidenceRefs
) {
    public RecordFeedbackAction {
        Objects.requireNonNull(due, "due");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(basis, "basis");
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(evidenceRefs, "evidenceRefs");
    }
    @NonNull public RecordFeedbackActionDue getDue() { return due; }
    @NonNull public RecordFeedbackActionCategory getCategory() { return category; }
    @NonNull public String getBasis() { return basis; }
    @NonNull public String getText() { return text; }
    @NonNull public List<String> getEvidenceRefs() { return evidenceRefs; }
}
