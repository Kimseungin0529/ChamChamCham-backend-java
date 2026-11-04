package com.chamchamcham.application.community;

import com.chamchamcham.domain.community.CommunityPostSort;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Objects;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public record CommunityPostCursorPayload(
    @NonNull CommunityPostSort sort,
    @Nullable Long score,
    @NonNull LocalDateTime createdAt,
    @NonNull UUID id
) {
    public CommunityPostCursorPayload {
        Objects.requireNonNull(sort, "sort");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(id, "id");
    }

    @NonNull
    public CommunityPostSort getSort() { return sort; }
    @Nullable
    public Long getScore() { return score; }
    @NonNull
    public LocalDateTime getCreatedAt() { return createdAt; }
    @NonNull
    public UUID getId() { return id; }
}
