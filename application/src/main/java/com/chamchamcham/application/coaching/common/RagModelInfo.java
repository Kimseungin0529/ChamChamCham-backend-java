package com.chamchamcham.application.coaching.common;

import java.util.*;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public record RagModelInfo(
    @NonNull String embedding,
    @NonNull String chat
) {
    public RagModelInfo {
        Objects.requireNonNull(embedding, "embedding");
        Objects.requireNonNull(chat, "chat");
    }
    @NonNull public String getEmbedding() { return embedding; }
    @NonNull public String getChat() { return chat; }
}
