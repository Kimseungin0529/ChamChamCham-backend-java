package com.chamchamcham.domain.coaching.reportfeedback;

import java.util.Objects;

public record ReportFeedbackItemDraft(ReportFeedbackItemSection section, String basis, String text) {
    public ReportFeedbackItemDraft {
        Objects.requireNonNull(section, "section");
        Objects.requireNonNull(basis, "basis");
        Objects.requireNonNull(text, "text");
    }
}
