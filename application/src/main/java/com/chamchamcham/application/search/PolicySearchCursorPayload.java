package com.chamchamcham.application.search;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Objects;
import org.springframework.lang.NonNull;

public record PolicySearchCursorPayload(
    @NonNull LocalDateTime createdAt,
    @NonNull UUID id
) {
    public PolicySearchCursorPayload {
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(id, "id");
    }

    @NonNull
    public LocalDateTime getCreatedAt() { return createdAt; }
    @NonNull
    public UUID getId() { return id; }
}
