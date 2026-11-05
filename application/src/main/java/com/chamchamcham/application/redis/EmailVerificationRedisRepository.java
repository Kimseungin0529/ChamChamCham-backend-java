package com.chamchamcham.application.redis;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
import java.time.Duration;
import java.util.Objects;

@Repository
public class EmailVerificationRedisRepository implements EmailVerificationRepository {
    private final StringRedisTemplate redis;

    public EmailVerificationRedisRepository(StringRedisTemplate redis) {
        this.redis = Objects.requireNonNull(redis, "redis");
    }
    @Override public void saveCode(String email, String code, Duration ttl) {
        Objects.requireNonNull(email, "email"); Objects.requireNonNull(code, "code"); Objects.requireNonNull(ttl, "ttl");
        redis.opsForValue().set("auth-code:" + email, code, ttl);
        redis.delete("verified-email:" + email);
    }
    @Override @org.springframework.lang.Nullable public String getCode(String email) {
        Objects.requireNonNull(email, "email");
        return redis.opsForValue().get("auth-code:" + email);
    }
    @Override public void markVerified(String email, Duration ttl) {
        Objects.requireNonNull(email, "email"); Objects.requireNonNull(ttl, "ttl");
        redis.opsForValue().set("verified-email:" + email, "true", ttl);
    }
    @Override public boolean isVerified(String email) {
        Objects.requireNonNull(email, "email");
        return "true".equals(redis.opsForValue().get("verified-email:" + email));
    }
    @Override public void deleteCode(String email) {
        Objects.requireNonNull(email, "email");
        redis.delete("auth-code:" + email);
    }
    @Override public void deleteVerified(String email) {
        Objects.requireNonNull(email, "email");
        redis.delete("verified-email:" + email);
    }

}
