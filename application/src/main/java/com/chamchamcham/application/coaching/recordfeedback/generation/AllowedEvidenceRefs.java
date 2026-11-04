package com.chamchamcham.application.coaching.recordfeedback.generation;

import com.chamchamcham.application.coaching.common.CoachingTextPolicy;
import com.chamchamcham.domain.coaching.recordfeedback.RecordFeedbackActionCategory;
import java.util.*;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public record AllowedEvidenceRefs(
    @NonNull Set<String> ids,
    @NonNull Set<String> weatherIds,
    @NonNull Set<String> documentIds
) {
    public AllowedEvidenceRefs {
        Objects.requireNonNull(ids, "ids");
        Objects.requireNonNull(weatherIds, "weatherIds");
        Objects.requireNonNull(documentIds, "documentIds");
    }
    @NonNull public Set<String> getIds() { return ids; }
    @NonNull public Set<String> getWeatherIds() { return weatherIds; }
    @NonNull public Set<String> getDocumentIds() { return documentIds; }
}
