package com.chamchamcham.application.search;

import java.util.UUID;
import java.util.Objects;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public record SearchQuery(
    @NonNull UUID memberId,
    @Nullable String keyword,
    @Nullable String cursor,
    int size
) {
    public SearchQuery {
        Objects.requireNonNull(memberId, "memberId");
    }

    @NonNull
    public UUID getMemberId() { return memberId; }
    @Nullable
    public String getKeyword() { return keyword; }
    @Nullable
    public String getCursor() { return cursor; }
    public int getSize() { return size; }
}
