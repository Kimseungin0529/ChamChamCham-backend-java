package com.chamchamcham.application.redis;

import java.time.Duration;
import org.springframework.lang.NonNull;

public interface KakaoNonceReplayRepository {
    boolean reserve(@NonNull String nonce, @NonNull Duration ttl);
}
