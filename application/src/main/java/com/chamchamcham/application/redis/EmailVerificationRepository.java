package com.chamchamcham.application.redis;

import java.time.Duration;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public interface EmailVerificationRepository {
    void saveCode(@NonNull String email, @NonNull String code, @NonNull Duration ttl);
    @Nullable String getCode(@NonNull String email);
    void markVerified(@NonNull String email, @NonNull Duration ttl);
    boolean isVerified(@NonNull String email);
    void deleteCode(@NonNull String email);
    void deleteVerified(@NonNull String email);
}
