package com.chamchamcham.application.coaching.reportfeedback;

import java.util.Objects;
import org.springframework.lang.Nullable;

public class ReportFeedbackGenerationFailure extends RuntimeException {
    private final ReportFeedbackFailureCode code;
    public ReportFeedbackGenerationFailure(ReportFeedbackFailureCode code, @Nullable Throwable cause) { super(Objects.requireNonNull(code, "code").name(), cause); this.code = code; }
    public ReportFeedbackGenerationFailure(ReportFeedbackFailureCode code) { this(code, null); }
    public ReportFeedbackFailureCode getCode() { return code; }
}
