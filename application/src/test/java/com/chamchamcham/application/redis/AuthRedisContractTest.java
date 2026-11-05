package com.chamchamcham.application.redis;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthRedisContractTest {
    @Test void nonceKeysUseHashAndClampPositiveSubMillisecondTtl() {
        var redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked") ValueOperations<String,String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        var kakao = new KakaoNonceReplayRedisRepository(redis);
        var apple = new AppleNonceReplayRedisRepository(redis);
        assertFalse(kakao.reserve("nonce", Duration.ZERO));
        assertFalse(kakao.reserve("nonce", Duration.ofNanos(-1)));
        verifyNoInteractions(values);
        assertFalse(kakao.reserve("nonce", Duration.ofNanos(1)));
        verify(values).setIfAbsent("auth:kakao:nonce:78377b525757b494427f89014f97d79928f3938d14eb51e20fb5dec9834eb304", "1", 1L, TimeUnit.MILLISECONDS);
        assertFalse(apple.reserve("already-hashed", Duration.ofNanos(1)));
        verify(values).setIfAbsent("auth:apple:nonce:already-hashed", "1", 1L, TimeUnit.MILLISECONDS);
    }
    @Test void emailCodeWriteClearsVerificationAndOnlyLiteralTrueIsVerified() {
        var redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked") ValueOperations<String,String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        var repository = new EmailVerificationRedisRepository(redis);
        repository.saveCode("fixture@example.test", "fixture-code", Duration.ofSeconds(3));
        var order = inOrder(values, redis);
        order.verify(values).set("auth-code:fixture@example.test", "fixture-code", Duration.ofSeconds(3));
        order.verify(redis).delete("verified-email:fixture@example.test");
        when(values.get("verified-email:fixture@example.test")).thenReturn(null, "TRUE", "true");
        assertFalse(repository.isVerified("fixture@example.test"));
        assertFalse(repository.isVerified("fixture@example.test"));
        assertTrue(repository.isVerified("fixture@example.test"));
        repository.markVerified("fixture@example.test", Duration.ofSeconds(5));
        verify(values).set("verified-email:fixture@example.test", "true", Duration.ofSeconds(5));
        repository.deleteCode("fixture@example.test");
        repository.deleteVerified("fixture@example.test");
        verify(redis).delete("auth-code:fixture@example.test");
        verify(redis, times(2)).delete("verified-email:fixture@example.test");
    }
    @Test void refreshUsesUuidKeyAndSeconds() {
        var redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked") ValueOperations<String,String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        var id = UUID.fromString("00000000-0000-0000-0000-000000000007");
        var repository = new RefreshTokenRedisRepository(redis);
        repository.save(id, "fixture", 42);
        verify(values).set("refresh:00000000-0000-0000-0000-000000000007", "fixture", 42L, TimeUnit.SECONDS);
        when(values.get("refresh:" + id)).thenReturn("fixture");
        assertEquals("fixture", repository.get(id));
        repository.delete(id);
        verify(redis).delete("refresh:" + id);
    }
}
