package com.chamchamcham.application.security;

import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.nimbusds.jwt.JWTParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;
import java.security.MessageDigest;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class AppleOidcTokenVerifier {
    private final Logger logger = LoggerFactory.getLogger(AppleOidcTokenVerifier.class);
    private final String issuer;
    private final String audience;
    private final Duration allowedClockSkew;
    private final Supplier<JwtDecoder> jwtDecoderProvider;
    private JwtDecoder jwtDecoder;

    private AppleOidcTokenVerifier(String issuer, String audience, Duration allowedClockSkew, Supplier<JwtDecoder> jwtDecoderProvider) {
        this.issuer = Objects.requireNonNull(issuer, "issuer");
        this.audience = Objects.requireNonNull(audience, "audience");
        this.allowedClockSkew = Objects.requireNonNull(allowedClockSkew, "allowedClockSkew");
        this.jwtDecoderProvider = Objects.requireNonNull(jwtDecoderProvider, "jwtDecoderProvider");
    }
    @Autowired
    public AppleOidcTokenVerifier(
        @Value("${auth.apple.oidc.issuer:https://appleid.apple.com}") String issuer,
        @Value("${auth.apple.oidc.audience}") String audience,
        @Value("${auth.apple.oidc.jwks-uri:https://appleid.apple.com/auth/keys}") String jwksUri,
        @Value("${auth.apple.oidc.allowed-clock-skew-seconds:60}") long allowedClockSkewSeconds) {
        this(issuer, audience, Duration.ofSeconds(allowedClockSkewSeconds), () -> NimbusJwtDecoder.withJwkSetUri(jwksUri).build());
        Objects.requireNonNull(jwksUri, "jwksUri");
    }
    public AppleOidcTokenVerifier(String issuer, String audience, Duration allowedClockSkew, JwtDecoder jwtDecoder) {
        this(issuer, audience, allowedClockSkew, () -> jwtDecoder);
        Objects.requireNonNull(jwtDecoder, "jwtDecoder");
    }
    private synchronized JwtDecoder decoder() {
        if (jwtDecoder == null) jwtDecoder = jwtDecoderProvider.get();
        return jwtDecoder;
    }
    public AppleOidcClaims verify(String token, String nonce) {
        Objects.requireNonNull(token, "identityToken");
        Objects.requireNonNull(nonce, "rawNonce");
        try { JWTParser.parse(token); }
        catch (ParseException e) { throw new BusinessException(ErrorCode.MALFORMED_JWT); }
        Jwt jwt;
        try { jwt = decoder().decode(token); }
        catch (JwtException e) { throw new BusinessException(ErrorCode.INVALID_APPLE_TOKEN); }
        catch (Exception e) {
            logger.error("Apple OIDC decoder failed", e);
            throw new BusinessException(ErrorCode.INVALID_APPLE_TOKEN);
        }
        if (!Objects.equals(jwt.getIssuer() == null ? null : jwt.getIssuer().toString(), issuer)) throw new BusinessException(ErrorCode.INVALID_APPLE_TOKEN);
        if (!jwt.getAudience().contains(audience)) throw new BusinessException(ErrorCode.INVALID_APPLE_TOKEN);
        var now = Instant.now();
        var expiresAt = jwt.getExpiresAt();
        if (expiresAt == null || expiresAt.plus(allowedClockSkew).isBefore(now)) throw new BusinessException(ErrorCode.INVALID_APPLE_TOKEN);
        var issuedAt = jwt.getIssuedAt();
        if (issuedAt == null || issuedAt.minus(allowedClockSkew).isAfter(now)) throw new BusinessException(ErrorCode.INVALID_APPLE_TOKEN);
        var actualNonce = jwt.getClaimAsString("nonce");
        if (actualNonce == null || !actualNonce.equals(sha256Hex(nonce))) throw new BusinessException(ErrorCode.APPLE_NONCE_MISMATCH);
        var subject = jwt.getSubject();
        if (subject == null || blank(subject)) throw new BusinessException(ErrorCode.INVALID_APPLE_TOKEN);
        return new AppleOidcClaims(subject, jwt.getClaimAsString("email"), emailVerified(jwt.getClaim("email_verified")), actualNonce, expiresAt);
    }
    private static boolean blank(String value) {
        return value.chars().allMatch(c -> Character.isWhitespace(c) || Character.isSpaceChar(c));
    }
    private static boolean emailVerified(Object value) { return Boolean.TRUE.equals(value) || "true".equals(value); }
    private static String sha256Hex(String value) {
        try { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
