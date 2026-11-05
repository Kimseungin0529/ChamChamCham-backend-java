package com.chamchamcham.application.redis;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.*;
import org.junit.jupiter.api.DisplayName;

class AppleNonceReplayRedisRepositoryTest {

    @Test
    @DisplayName("reserve stores nonce hash with millisecond ttl")
    void reserve_stores_nonce_hash_with_millisecond_ttl() throws Exception {
        var redis = org.mockito.Mockito.mock(StringRedisTemplate.class);
        var valueOperations = ((ValueOperations<String, String>) org.mockito.Mockito.mock(ValueOperations.class));
        var repository = new AppleNonceReplayRedisRepository(redis);
        when(redis.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(org.mockito.Mockito.eq("auth:apple:nonce:nonce-hash"), org.mockito.Mockito.eq("1"), org.mockito.Mockito.eq(500L), org.mockito.Mockito.eq(TimeUnit.MILLISECONDS))).thenReturn(true);
        var reserved = repository.reserve("nonce-hash", Duration.ofMillis(500));
        assertTrue(reserved);
        verify(valueOperations).setIfAbsent(org.mockito.Mockito.eq("auth:apple:nonce:nonce-hash"), org.mockito.Mockito.eq("1"), org.mockito.Mockito.eq(500L), org.mockito.Mockito.eq(TimeUnit.MILLISECONDS));
    }

    @Test
    @DisplayName("reserve rejects non-positive ttl")
    void reserve_rejects_non_positive_ttl() throws Exception {
        var redis = org.mockito.Mockito.mock(StringRedisTemplate.class);
        var repository = new AppleNonceReplayRedisRepository(redis);
        assertFalse(repository.reserve("nonce-hash", Duration.ZERO));
        assertFalse(repository.reserve("nonce-hash", Duration.ofMillis(-1)));
        verify(redis, never()).opsForValue();
    }
}
