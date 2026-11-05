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
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.*;
import org.junit.jupiter.api.DisplayName;

class AppleOidcTokenVerifierTest {
    private final KeyPair keyPair = rsaKeyPair();
    private final AppleOidcTokenVerifier verifier = verifierFor(keyPair);

    @Test
    @DisplayName("verify accepts valid apple oidc token")
    void verify_accepts_valid_apple_oidc_token() throws Exception {
        var claims = verifier.verify(token(keyPair, ISSUER, AUDIENCE, "apple-sub", "user@example.com", true, HASHED_NONCE, Instant.now().plusSeconds(300), Instant.now().minusSeconds(10)), RAW_NONCE);
        assertEquals("apple-sub", claims.getSubject());
        assertEquals("user@example.com", claims.getEmail());
        assertEquals(true, claims.getEmailVerified());
        assertEquals(HASHED_NONCE, claims.getNonce());
    }

    @Test
    @DisplayName("verify accepts string email verified claim")
    void verify_accepts_string_email_verified_claim() throws Exception {
        var claims = verifier.verify(token(keyPair, ISSUER, AUDIENCE, "apple-sub", "user@example.com", "true", HASHED_NONCE, Instant.now().plusSeconds(300), Instant.now().minusSeconds(10)), RAW_NONCE);
        assertEquals(true, claims.getEmailVerified());
    }

    @Test
    @DisplayName("verify rejects malformed jwt")
    void verify_rejects_malformed_jwt() throws Exception {
        var exception = assertThrows(BusinessException.class, () -> verifier.verify("not-a-jwt", RAW_NONCE));
        assertEquals(ErrorCode.MALFORMED_JWT, exception.getErrorCode());
    }

    @Test
    @DisplayName("verify rejects nonce mismatch")
    void verify_rejects_nonce_mismatch() throws Exception {
        var exception = assertThrows(BusinessException.class, () -> verifier.verify(token(keyPair, ISSUER, AUDIENCE, "apple-sub", "user@example.com", true, "wrong-nonce", Instant.now().plusSeconds(300), Instant.now().minusSeconds(10)), RAW_NONCE));
        assertEquals(ErrorCode.APPLE_NONCE_MISMATCH, exception.getErrorCode());
    }

    @Test
    @DisplayName("verify rejects wrong audience")
    void verify_rejects_wrong_audience() throws Exception {
        var exception = assertThrows(BusinessException.class, () -> verifier.verify(token(keyPair, ISSUER, "wrong-audience", "apple-sub", "user@example.com", true, HASHED_NONCE, Instant.now().plusSeconds(300), Instant.now().minusSeconds(10)), RAW_NONCE));
        assertEquals(ErrorCode.INVALID_APPLE_TOKEN, exception.getErrorCode());
    }

    @Test
    @DisplayName("verify rejects wrong issuer")
    void verify_rejects_wrong_issuer() throws Exception {
        var exception = assertThrows(BusinessException.class, () -> verifier.verify(token(keyPair, "https://invalid.example.com", AUDIENCE, "apple-sub", "user@example.com", true, HASHED_NONCE, Instant.now().plusSeconds(300), Instant.now().minusSeconds(10)), RAW_NONCE));
        assertEquals(ErrorCode.INVALID_APPLE_TOKEN, exception.getErrorCode());
    }

    @Test
    @DisplayName("verify rejects expired token")
    void verify_rejects_expired_token() throws Exception {
        var exception = assertThrows(BusinessException.class, () -> verifier.verify(token(keyPair, ISSUER, AUDIENCE, "apple-sub", "user@example.com", true, HASHED_NONCE, Instant.now().minusSeconds(60), Instant.now().minusSeconds(10)), RAW_NONCE));
        assertEquals(ErrorCode.INVALID_APPLE_TOKEN, exception.getErrorCode());
    }

    @Test
    @DisplayName("verify rejects future issued at")
    void verify_rejects_future_issued_at() throws Exception {
        var exception = assertThrows(BusinessException.class, () -> verifier.verify(token(keyPair, ISSUER, AUDIENCE, "apple-sub", "user@example.com", true, HASHED_NONCE, Instant.now().plusSeconds(300), Instant.now().plusSeconds(60)), RAW_NONCE));
        assertEquals(ErrorCode.INVALID_APPLE_TOKEN, exception.getErrorCode());
    }

    @Test
    @DisplayName("verify rejects missing subject")
    void verify_rejects_missing_subject() throws Exception {
        var exception = assertThrows(BusinessException.class, () -> verifier.verify(token(keyPair, ISSUER, AUDIENCE, null, "user@example.com", true, HASHED_NONCE, Instant.now().plusSeconds(300), Instant.now().minusSeconds(10)), RAW_NONCE));
        assertEquals(ErrorCode.INVALID_APPLE_TOKEN, exception.getErrorCode());
    }

    @Test
    @DisplayName("verify rejects blank subject")
    void verify_rejects_blank_subject() throws Exception {
        var exception = assertThrows(BusinessException.class, () -> verifier.verify(token(keyPair, ISSUER, AUDIENCE, "", "user@example.com", true, HASHED_NONCE, Instant.now().plusSeconds(300), Instant.now().minusSeconds(10)), RAW_NONCE));
        assertEquals(ErrorCode.INVALID_APPLE_TOKEN, exception.getErrorCode());
    }

    @Test
    @DisplayName("verify rejects missing nonce")
    void verify_rejects_missing_nonce() throws Exception {
        var exception = assertThrows(BusinessException.class, () -> verifier.verify(token(keyPair, ISSUER, AUDIENCE, "apple-sub", "user@example.com", true, null, Instant.now().plusSeconds(300), Instant.now().minusSeconds(10)), RAW_NONCE));
        assertEquals(ErrorCode.APPLE_NONCE_MISMATCH, exception.getErrorCode());
    }

    @Test
    @DisplayName("verify rejects bad signature")
    void verify_rejects_bad_signature() throws Exception {
        var otherKeyPair = rsaKeyPair();
        var exception = assertThrows(BusinessException.class, () -> verifier.verify(token(otherKeyPair, ISSUER, AUDIENCE, "apple-sub", "user@example.com", true, HASHED_NONCE, Instant.now().plusSeconds(300), Instant.now().minusSeconds(10)), RAW_NONCE));
        assertEquals(ErrorCode.INVALID_APPLE_TOKEN, exception.getErrorCode());
    }

    @Test
    @DisplayName("verify maps decoder invalid token to invalid apple token")
    void verify_maps_decoder_invalid_token_to_invalid_apple_token() throws Exception {
        var invalidVerifier = new AppleOidcTokenVerifier(ISSUER, AUDIENCE, Duration.ofSeconds(30), (JwtDecoder) token -> { throw new JwtException("invalid"); });
        var exception = assertThrows(BusinessException.class, () -> invalidVerifier.verify(token(keyPair, ISSUER, AUDIENCE, "apple-sub", "user@example.com", true, HASHED_NONCE, Instant.now().plusSeconds(300), Instant.now().minusSeconds(10)), RAW_NONCE));
        assertEquals(ErrorCode.INVALID_APPLE_TOKEN, exception.getErrorCode());
    }
    private AppleOidcTokenVerifier verifierFor(KeyPair keyPair) {
        var decoder = NimbusJwtDecoder.withPublicKey(((RSAPublicKey) keyPair.getPublic())).build();
        decoder.setJwtValidator(it -> OAuth2TokenValidatorResult.success());
        return new AppleOidcTokenVerifier(ISSUER, AUDIENCE, Duration.ofSeconds(30), decoder);
    }
    private String token(KeyPair signingKeyPair, String issuer, String audience, String subject, String email, Object emailVerified, String nonce, Instant expiresAt, Instant issuedAt) throws Exception {
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
    private static String sha256Hex(String value) {
        try { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8))); }
        catch (java.security.GeneralSecurityException e) { throw new AssertionError(e); }
    }
    private static final String ISSUER = "https://appleid.apple.com";
    private static final String AUDIENCE = "com.chamchamcham.app";
    private static final String RAW_NONCE = "client-nonce";
    private static final String HASHED_NONCE = sha256Hex(RAW_NONCE);
}
