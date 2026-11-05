package com.chamchamcham.application.security;

import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import static org.junit.jupiter.api.Assertions.*;

class OidcLazyInitializationTest {
    @Test void appleRetriesFailedLazyInitializationThenCachesDecoder() throws Exception {
        verifyRetry(AppleOidcTokenVerifier.class, ErrorCode.INVALID_APPLE_TOKEN);
    }
    @Test void kakaoRetriesFailedLazyInitializationThenCachesDecoder() throws Exception {
        verifyRetry(KakaoOidcTokenVerifier.class, ErrorCode.KAKAO_OIDC_UNAVAILABLE);
    }
    private void verifyRetry(Class<?> type, ErrorCode failureCode) throws Exception {
        var attempts = new AtomicInteger();
        var constructor = java.util.Arrays.stream(type.getDeclaredConstructors())
            .filter(c -> c.getParameterCount() == 4 && c.getParameterTypes()[2] == Duration.class
                && c.getParameterTypes()[3] != JwtDecoder.class).findFirst().orElseThrow();
        constructor.setAccessible(true);
        JwtDecoder decoder = token -> Jwt.withTokenValue(token).header("alg", "RS256")
            .issuer("https://issuer.example").audience(java.util.List.of("test-audience"))
            .subject("subject").issuedAt(Instant.now().minusSeconds(1))
            .expiresAt(Instant.now().plusSeconds(60)).claim("nonce", "wrong").build();
        var providerType = constructor.getParameterTypes()[3];
        var provider = Proxy.newProxyInstance(providerType.getClassLoader(), new Class<?>[]{providerType},
            (proxy, method, args) -> {
                if (attempts.incrementAndGet() == 1) throw new IllegalStateException("fixture unavailable");
                return decoder;
            });
        var verifier = constructor.newInstance("https://issuer.example", "test-audience", Duration.ZERO, provider);
        assertEquals(0, attempts.get());
        var verify = type.getMethod("verify", String.class, String.class);
        // Valid JWT shape; injected decoder never performs discovery or HTTP.
        String shape = "eyJhbGciOiJub25lIn0.eyJzdWIiOiJmaXh0dXJlIn0.";
        var first = assertThrows(java.lang.reflect.InvocationTargetException.class,
            () -> verify.invoke(verifier, shape, "expected"));
        assertEquals(failureCode, ((BusinessException) first.getCause()).getErrorCode());
        for (int i = 0; i < 2; i++) {
            var next = assertThrows(java.lang.reflect.InvocationTargetException.class,
                () -> verify.invoke(verifier, shape, "expected"));
            assertEquals(type == AppleOidcTokenVerifier.class ? ErrorCode.APPLE_NONCE_MISMATCH
                : ErrorCode.KAKAO_NONCE_MISMATCH, ((BusinessException) next.getCause()).getErrorCode());
        }
        assertEquals(2, attempts.get());
    }
}
