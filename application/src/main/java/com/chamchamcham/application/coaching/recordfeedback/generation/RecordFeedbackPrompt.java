package com.chamchamcham.application.coaching.recordfeedback.generation;

import com.chamchamcham.application.coaching.common.CoachingTextPolicy;
import org.springframework.stereotype.Component;
import java.util.*;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public record RecordFeedbackPrompt(
    @NonNull String system,
    @NonNull String user
) {
    public RecordFeedbackPrompt {
        Objects.requireNonNull(system, "system");
        Objects.requireNonNull(user, "user");
    }
    @NonNull public String getSystem() { return system; }
    @NonNull public String getUser() { return user; }
}
