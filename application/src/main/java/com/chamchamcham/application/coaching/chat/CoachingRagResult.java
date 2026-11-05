package com.chamchamcham.application.coaching.chat;

import com.chamchamcham.application.coaching.common.RagAuditResult;
import com.chamchamcham.application.coaching.common.RagModelInfo;
import java.util.*;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public record CoachingRagResult(
    @NonNull CoachingStructuredResult result,
    @NonNull RagAuditResult audit,
    @NonNull RagModelInfo model
) {
    public CoachingRagResult {
        Objects.requireNonNull(result, "result");
        Objects.requireNonNull(audit, "audit");
        Objects.requireNonNull(model, "model");
    }
    @NonNull public CoachingStructuredResult getResult() { return result; }
    @NonNull public RagAuditResult getAudit() { return audit; }
    @NonNull public RagModelInfo getModel() { return model; }
    public String getAnswer() { return result.getSummary(); }
    public List<CoachingCitationRef> getCitations() { return result.getCitations(); }
}
