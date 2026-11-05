package com.chamchamcham.application.redis;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.Objects;

@Repository
public class AppleNonceReplayRedisRepository implements AppleNonceReplayRepository {
    private final StringRedisTemplate redis;

    public AppleNonceReplayRedisRepository(StringRedisTemplate redis) {
        this.redis = Objects.requireNonNull(redis, "redis");
    }
    @Override public boolean reserve(String nonceHash, Duration ttl) {
        Objects.requireNonNull(nonceHash, "nonceHash");
        Objects.requireNonNull(ttl, "ttl");
        if (ttl.isZero() || ttl.isNegative()) return false;
        return Boolean.TRUE.equals(redis.opsForValue().setIfAbsent("auth:apple:nonce:" + nonceHash, "1", Math.max(ttl.toMillis(), 1L), TimeUnit.MILLISECONDS));
    }
}
