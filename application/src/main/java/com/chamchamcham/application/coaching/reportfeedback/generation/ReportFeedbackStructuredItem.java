package com.chamchamcham.application.coaching.reportfeedback.generation;

import com.chamchamcham.domain.coaching.reportfeedback.ReportFeedbackItemSection;
import java.util.*;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public record ReportFeedbackStructuredItem(
    @NonNull ReportFeedbackItemSection section,
    @NonNull ReportFeedbackContentItem item
) {
    public ReportFeedbackStructuredItem {
        Objects.requireNonNull(section, "section");
        Objects.requireNonNull(item, "item");
    }
    @NonNull public ReportFeedbackItemSection getSection() { return section; }
    @NonNull public ReportFeedbackContentItem getItem() { return item; }
}
