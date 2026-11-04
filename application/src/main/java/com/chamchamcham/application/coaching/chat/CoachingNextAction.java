package com.chamchamcham.application.coaching.chat;

import com.chamchamcham.application.coaching.common.RagSourceType;
import java.util.*;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public record CoachingNextAction(
    @NonNull CoachingActionDue due,
    @NonNull String action,
    @NonNull List<String> citationIds
) {
    public CoachingNextAction {
        Objects.requireNonNull(due, "due");
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(citationIds, "citationIds");
    }
    @NonNull public CoachingActionDue getDue() { return due; }
    @NonNull public String getAction() { return action; }
    @NonNull public List<String> getCitationIds() { return citationIds; }
}
