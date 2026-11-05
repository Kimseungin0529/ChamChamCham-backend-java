package com.chamchamcham.application.coaching.common;

import java.util.*;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public record RagAuditResult(
    @NonNull RagAuditStatus status,
    @NonNull List<String> warnings,
    @NonNull List<String> citations
) {
    public RagAuditResult {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(warnings, "warnings");
        Objects.requireNonNull(citations, "citations");
    }
    @NonNull public RagAuditStatus getStatus() { return status; }
    @NonNull public List<String> getWarnings() { return warnings; }
    @NonNull public List<String> getCitations() { return citations; }
}
