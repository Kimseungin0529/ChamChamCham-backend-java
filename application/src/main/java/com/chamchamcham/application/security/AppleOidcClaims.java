package com.chamchamcham.application.security;

import java.time.Instant;
import java.util.Objects;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public record AppleOidcClaims(
    @NonNull String subject,
    @Nullable String email,
    boolean emailVerified,
    @NonNull String nonce,
    @NonNull Instant expiresAt
) {
    public AppleOidcClaims {
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(nonce, "nonce");
        Objects.requireNonNull(expiresAt, "expiresAt");
    }

    @NonNull
    public String getSubject() { return subject; }
    @Nullable
    public String getEmail() { return email; }
    public boolean getEmailVerified() { return emailVerified; }
    @NonNull
    public String getNonce() { return nonce; }
    @NonNull
    public Instant getExpiresAt() { return expiresAt; }
}
