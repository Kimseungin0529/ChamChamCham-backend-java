package com.chamchamcham.application.community;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Objects;
import org.springframework.lang.NonNull;

public record CommunityCommentCursorPayload(
    @NonNull LocalDateTime createdAt,
    @NonNull UUID id
) {
    public CommunityCommentCursorPayload {
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(id, "id");
    }

    @NonNull
    public LocalDateTime getCreatedAt() { return createdAt; }
    @NonNull
    public UUID getId() { return id; }
}
