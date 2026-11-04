package com.chamchamcham.application.farming;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Objects;
import org.springframework.lang.NonNull;

public record FarmingRecordCursorPayload(
    @NonNull LocalDateTime workedAt,
    @NonNull UUID id
) {
    public FarmingRecordCursorPayload {
        Objects.requireNonNull(workedAt, "workedAt");
        Objects.requireNonNull(id, "id");
    }

    @NonNull
    public LocalDateTime getWorkedAt() { return workedAt; }
    @NonNull
    public UUID getId() { return id; }
}
