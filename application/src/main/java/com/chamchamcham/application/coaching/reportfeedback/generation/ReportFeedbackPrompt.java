package com.chamchamcham.application.coaching.reportfeedback.generation;

import com.chamchamcham.domain.coaching.reportfeedback.ReportFeedbackItemSection;
import java.util.*;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public record ReportFeedbackPrompt(
    @NonNull String system,
    @NonNull String user
) {
    public ReportFeedbackPrompt {
        Objects.requireNonNull(system, "system");
        Objects.requireNonNull(user, "user");
    }
    @NonNull public String getSystem() { return system; }
    @NonNull public String getUser() { return user; }
}
