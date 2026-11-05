package com.chamchamcham.application.redis;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.*;
import org.junit.jupiter.api.DisplayName;

class KakaoNonceReplayRedisRepositoryTest {

    @Test
    @DisplayName("reserve uses millisecond ttl for sub-second accepted window")
    void reserve_uses_millisecond_ttl_for_sub_second_accepted_window() throws Exception {
        var redis = org.mockito.Mockito.mock(StringRedisTemplate.class);
        var valueOperations = ((ValueOperations<String, String>) org.mockito.Mockito.mock(ValueOperations.class));
        var repository = new KakaoNonceReplayRedisRepository(redis);
        when(redis.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(org.mockito.Mockito.anyString(), org.mockito.Mockito.eq("1"), org.mockito.Mockito.eq(500L), org.mockito.Mockito.eq(TimeUnit.MILLISECONDS))).thenReturn(true);
        var reserved = repository.reserve("nonce", Duration.ofMillis(500));
        assertTrue(reserved);
        verify(valueOperations).setIfAbsent(org.mockito.Mockito.anyString(), org.mockito.Mockito.eq("1"), org.mockito.Mockito.eq(500L), org.mockito.Mockito.eq(TimeUnit.MILLISECONDS));
    }
}
