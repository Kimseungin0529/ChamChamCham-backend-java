package com.chamchamcham.application.redis;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.Objects;

@Repository
public class KakaoNonceReplayRedisRepository implements KakaoNonceReplayRepository {
    private final StringRedisTemplate redis;

    public KakaoNonceReplayRedisRepository(StringRedisTemplate redis) {
        this.redis = Objects.requireNonNull(redis, "redis");
    }
    @Override public boolean reserve(String nonce, Duration ttl) {
        Objects.requireNonNull(nonce, "nonce");
        Objects.requireNonNull(ttl, "ttl");
        if (ttl.isZero() || ttl.isNegative()) return false;
        return Boolean.TRUE.equals(redis.opsForValue().setIfAbsent("auth:kakao:nonce:" + sha256Hex(nonce), "1", Math.max(ttl.toMillis(), 1L), TimeUnit.MILLISECONDS));
    }
    private static String sha256Hex(String value) {
        try { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
