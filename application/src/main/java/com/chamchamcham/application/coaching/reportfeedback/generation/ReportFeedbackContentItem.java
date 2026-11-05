package com.chamchamcham.application.coaching.reportfeedback.generation;

import com.chamchamcham.domain.coaching.reportfeedback.ReportFeedbackItemSection;
import java.util.*;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public record ReportFeedbackContentItem(
    @NonNull String basis,
    @NonNull String text,
    @NonNull List<String> evidenceRefs
) {
    public ReportFeedbackContentItem {
        Objects.requireNonNull(basis, "basis");
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(evidenceRefs, "evidenceRefs");
    }
    @NonNull public String getBasis() { return basis; }
    @NonNull public String getText() { return text; }
    @NonNull public List<String> getEvidenceRefs() { return evidenceRefs; }
    public ReportFeedbackContentItem normalizedParagraph() { return new ReportFeedbackContentItem(basis, ReportFeedbackContent.normalizeParagraph(text), evidenceRefs); }
}
