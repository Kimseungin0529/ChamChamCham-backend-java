package com.chamchamcham.application.redis;

import java.time.Duration;
import org.springframework.lang.NonNull;

public interface AppleNonceReplayRepository {
    boolean reserve(@NonNull String nonceHash, @NonNull Duration ttl);
}
