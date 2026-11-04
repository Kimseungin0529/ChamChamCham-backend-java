package com.chamchamcham.application.voice;

import java.time.LocalDateTime;
import java.util.Objects;
import org.springframework.lang.NonNull;

public record RealtimeSessionResult(
    @NonNull String clientSecret,
    @NonNull LocalDateTime expiresAt,
    @NonNull String model
) {
    public RealtimeSessionResult {
        Objects.requireNonNull(clientSecret, "clientSecret");
        Objects.requireNonNull(expiresAt, "expiresAt");
        Objects.requireNonNull(model, "model");
    }

    @NonNull
    public String getClientSecret() { return clientSecret; }
    @NonNull
    public LocalDateTime getExpiresAt() { return expiresAt; }
    @NonNull
    public String getModel() { return model; }
}
