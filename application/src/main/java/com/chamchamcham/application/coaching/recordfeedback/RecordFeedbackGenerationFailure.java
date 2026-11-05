package com.chamchamcham.application.coaching.recordfeedback;

import java.util.Objects;
import org.springframework.lang.Nullable;

public class RecordFeedbackGenerationFailure extends RuntimeException {
    private final RecordFeedbackFailureCode code;
    public RecordFeedbackGenerationFailure(RecordFeedbackFailureCode code, @Nullable Throwable cause) { super(Objects.requireNonNull(code, "code").name(), cause); this.code = code; }
    public RecordFeedbackGenerationFailure(RecordFeedbackFailureCode code) { this(code, null); }
    public RecordFeedbackFailureCode getCode() { return code; }
}
