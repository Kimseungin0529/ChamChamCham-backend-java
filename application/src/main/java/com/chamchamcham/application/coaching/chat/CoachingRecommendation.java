package com.chamchamcham.application.coaching.chat;

import com.chamchamcham.application.coaching.common.RagSourceType;
import java.util.*;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public record CoachingRecommendation(
    @NonNull CoachingPriority priority,
    @NonNull String action,
    @NonNull String reason,
    @Nullable String caution,
    @NonNull List<String> citationIds
) {
    public CoachingRecommendation {
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(citationIds, "citationIds");
    }
    @NonNull public CoachingPriority getPriority() { return priority; }
    @NonNull public String getAction() { return action; }
    @NonNull public String getReason() { return reason; }
    @Nullable public String getCaution() { return caution; }
    @NonNull public List<String> getCitationIds() { return citationIds; }
}
