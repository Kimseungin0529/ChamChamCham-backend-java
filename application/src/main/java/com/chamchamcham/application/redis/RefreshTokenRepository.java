package com.chamchamcham.application.redis;

import java.util.UUID;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public interface RefreshTokenRepository {
    void save(@NonNull UUID memberId, @NonNull String refreshToken, long expiresInSeconds);
    @Nullable String get(@NonNull UUID memberId);
    void delete(@NonNull UUID memberId);
}
