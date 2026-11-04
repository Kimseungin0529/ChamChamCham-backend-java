package com.chamchamcham.application.coaching.chat;

import com.chamchamcham.application.coaching.common.RagSourceType;
import java.util.*;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public record CoachingObservation(
    @NonNull String title,
    @NonNull String detail,
    @NonNull List<String> citationIds
) {
    public CoachingObservation {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(detail, "detail");
        Objects.requireNonNull(citationIds, "citationIds");
    }
    @NonNull public String getTitle() { return title; }
    @NonNull public String getDetail() { return detail; }
    @NonNull public List<String> getCitationIds() { return citationIds; }
}
