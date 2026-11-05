package com.chamchamcham.application.redis;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.Objects;

@Repository
public class RefreshTokenRedisRepository implements RefreshTokenRepository {
    private final StringRedisTemplate redis;

    public RefreshTokenRedisRepository(StringRedisTemplate redis) {
        this.redis = Objects.requireNonNull(redis, "redis");
    }
    @Override public void save(UUID memberId, String refreshToken, long expiresInSeconds) {
        Objects.requireNonNull(memberId, "memberId"); Objects.requireNonNull(refreshToken, "refreshToken");
        redis.opsForValue().set("refresh:" + memberId, refreshToken, expiresInSeconds, TimeUnit.SECONDS);
    }
    @Override @org.springframework.lang.Nullable public String get(UUID memberId) {
        Objects.requireNonNull(memberId, "memberId");
        return redis.opsForValue().get("refresh:" + memberId);
    }
    @Override public void delete(UUID memberId) {
        Objects.requireNonNull(memberId, "memberId");
        redis.delete("refresh:" + memberId);
    }

}
