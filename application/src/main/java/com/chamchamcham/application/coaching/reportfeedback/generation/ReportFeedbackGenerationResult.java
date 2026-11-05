package com.chamchamcham.application.coaching.reportfeedback.generation;

import com.chamchamcham.application.coaching.common.RagModelInfo;
import java.util.*;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public record ReportFeedbackGenerationResult(
    @NonNull ReportFeedbackContent content,
    @NonNull List<Map<String, Object>> citations,
    @NonNull List<String> auditWarnings,
    @NonNull RagModelInfo modelInfo
) {
    public ReportFeedbackGenerationResult {
        Objects.requireNonNull(content, "content");
        Objects.requireNonNull(citations, "citations");
        Objects.requireNonNull(auditWarnings, "auditWarnings");
        Objects.requireNonNull(modelInfo, "modelInfo");
    }
    @NonNull public ReportFeedbackContent getContent() { return content; }
    @NonNull public List<Map<String, Object>> getCitations() { return citations; }
    @NonNull public List<String> getAuditWarnings() { return auditWarnings; }
    @NonNull public RagModelInfo getModelInfo() { return modelInfo; }
}
