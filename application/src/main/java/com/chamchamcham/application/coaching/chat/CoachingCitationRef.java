package com.chamchamcham.application.coaching.chat;

import com.chamchamcham.application.coaching.common.RagSourceType;
import java.util.*;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public record CoachingCitationRef(
    @NonNull String chunkId,
    @NonNull String label,
    @NonNull RagSourceType sourceType,
    @Nullable String documentTitle,
    @Nullable Integer page,
    @Nullable String source
) {
    public CoachingCitationRef {
        Objects.requireNonNull(chunkId, "chunkId");
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(sourceType, "sourceType");
    }
    @NonNull public String getChunkId() { return chunkId; }
    @NonNull public String getLabel() { return label; }
    @NonNull public RagSourceType getSourceType() { return sourceType; }
    @Nullable public String getDocumentTitle() { return documentTitle; }
    @Nullable public Integer getPage() { return page; }
    @Nullable public String getSource() { return source; }
}
