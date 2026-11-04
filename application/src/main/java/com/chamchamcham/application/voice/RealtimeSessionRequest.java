package com.chamchamcham.application.voice;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.lang.NonNull;

public record RealtimeSessionRequest(
    @NonNull String instructions,
    @NonNull List<Map<String, Object>> tools,
    int expiresAfterSeconds
) {
    public RealtimeSessionRequest {
        Objects.requireNonNull(instructions, "instructions");
        Objects.requireNonNull(tools, "tools");
    }

    @NonNull
    public String getInstructions() { return instructions; }
    @NonNull
    public List<Map<String, Object>> getTools() { return tools; }
    public int getExpiresAfterSeconds() { return expiresAfterSeconds; }
}
