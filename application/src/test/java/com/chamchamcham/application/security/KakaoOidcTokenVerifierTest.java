package com.chamchamcham.application.security;

import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.*;
import org.junit.jupiter.api.DisplayName;

class KakaoOidcTokenVerifierTest {
    private final KeyPair keyPair = rsaKeyPair();
    private final KakaoOidcTokenVerifier verifier = verifierFor(keyPair);

    @Test
    @DisplayName("verify accepts valid kakao oidc token")
    void verify_accepts_valid_kakao_oidc_token() throws Exception {
        var claims = verifier.verify(token(keyPair, ISSUER, AUDIENCE, "kakao-sub", "user@example.com", true, NONCE, Instant.now().plusSeconds(300), Instant.now().minusSeconds(10)), NONCE);
        assertEquals("kakao-sub", claims.getSubject());
        assertEquals("user@example.com", claims.getEmail());
        assertEquals(true, claims.getEmailVerified());
        assertEquals(NONCE, claims.getNonce());
    }

    @Test
    @DisplayName("verify rejects malformed jwt")
    void verify_rejects_malformed_jwt() throws Exception {
        var exception = assertThrows(BusinessException.class, () -> verifier.verify("not-a-jwt", NONCE));
        assertEquals(ErrorCode.MALFORMED_JWT, exception.getErrorCode());
    }

    @Test
    @DisplayName("verify rejects wrong issuer")
    void verify_rejects_wrong_issuer() throws Exception {
        var exception = assertThrows(BusinessException.class, () -> verifier.verify(token(keyPair, "https://wrong.example.com", AUDIENCE, "kakao-sub", "user@example.com", true, NONCE, Instant.now().plusSeconds(300), Instant.now().minusSeconds(10)), NONCE));
        assertEquals(ErrorCode.INVALID_KAKAO_TOKEN, exception.getErrorCode());
    }

    @Test
    @DisplayName("verify rejects wrong audience")
    void verify_rejects_wrong_audience() throws Exception {
        var exception = assertThrows(BusinessException.class, () -> verifier.verify(token(keyPair, ISSUER, "wrong-audience", "kakao-sub", "user@example.com", true, NONCE, Instant.now().plusSeconds(300), Instant.now().minusSeconds(10)), NONCE));
        assertEquals(ErrorCode.INVALID_KAKAO_TOKEN, exception.getErrorCode());
    }

    @Test
    @DisplayName("verify rejects expired token")
    void verify_rejects_expired_token() throws Exception {
        var exception = assertThrows(BusinessException.class, () -> {
        verifier.verify(token(keyPair, ISSUER, AUDIENCE, "kakao-sub", "user@example.com", true, NONCE, Instant.now().minusSeconds(120), Instant.now().minusSeconds(240)), NONCE);
        });
        assertEquals(ErrorCode.INVALID_KAKAO_TOKEN, exception.getErrorCode());
    }

    @Test
    @DisplayName("verify rejects bad signature")
    void verify_rejects_bad_signature() throws Exception {
        var otherKeyPair = rsaKeyPair();
        var exception = assertThrows(BusinessException.class, () -> verifier.verify(token(otherKeyPair, ISSUER, AUDIENCE, "kakao-sub", "user@example.com", true, NONCE, Instant.now().plusSeconds(300), Instant.now().minusSeconds(10)), NONCE));
        assertEquals(ErrorCode.INVALID_KAKAO_TOKEN, exception.getErrorCode());
    }

    @Test
    @DisplayName("verify maps decoder infrastructure failure to oidc unavailable")
    void verify_maps_decoder_infrastructure_failure_to_oidc_unavailable() throws Exception {
        var unavailableVerifier = new KakaoOidcTokenVerifier(ISSUER, AUDIENCE, Duration.ofSeconds(30), (JwtDecoder) token -> { throw new IllegalStateException("jwks unavailable"); });
        var exception = assertThrows(BusinessException.class, () -> unavailableVerifier.verify(token(keyPair, ISSUER, AUDIENCE, "kakao-sub", "user@example.com", true, NONCE, Instant.now().plusSeconds(300), Instant.now().minusSeconds(10)), NONCE));
        assertEquals(ErrorCode.KAKAO_OIDC_UNAVAILABLE, exception.getErrorCode());
    }

    @Test
    @DisplayName("verify rejects nonce mismatch")
    void verify_rejects_nonce_mismatch() throws Exception {
        var exception = assertThrows(BusinessException.class, () -> verifier.verify(token(keyPair, ISSUER, AUDIENCE, "kakao-sub", "user@example.com", true, "token-nonce", Instant.now().plusSeconds(300), Instant.now().minusSeconds(10)), NONCE));
        assertEquals(ErrorCode.KAKAO_NONCE_MISMATCH, exception.getErrorCode());
    }
    private KakaoOidcTokenVerifier verifierFor(KeyPair keyPair) {
        var decoder = NimbusJwtDecoder.withPublicKey(((RSAPublicKey) keyPair.getPublic())).build();
        decoder.setJwtValidator(it -> OAuth2TokenValidatorResult.success());
        return new KakaoOidcTokenVerifier(ISSUER, AUDIENCE, Duration.ofSeconds(30), decoder);
    }
    private String token(KeyPair signingKeyPair, String issuer, String audience, String subject, String email, Boolean emailVerified, String nonce, Instant expiresAt, Instant issuedAt) throws Exception {
        var builder = new JWTClaimsSet.Builder().issuer(issuer).audience(audience)
            .expirationTime(Date.from(expiresAt)).issueTime(Date.from(issuedAt));
        if (subject != null) builder.subject(subject);
        if (nonce != null) builder.claim("nonce", nonce);
        if (email != null) builder.claim("email", email);
        if (emailVerified != null) builder.claim("email_verified", emailVerified);
        var signedJwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), builder.build());
        signedJwt.sign(new RSASSASigner((RSAPrivateKey) signingKeyPair.getPrivate()));
        return signedJwt.serialize();
    }
    private static KeyPair rsaKeyPair() {
        try {
            var generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (java.security.GeneralSecurityException e) { throw new AssertionError(e); }
    }
    private static final String ISSUER = "https://kauth.kakao.com";
    private static final String AUDIENCE = "test-kakao-native-app-key";
    private static final String NONCE = "client-nonce";
}
