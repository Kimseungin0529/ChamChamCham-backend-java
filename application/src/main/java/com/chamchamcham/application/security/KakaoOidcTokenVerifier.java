package com.chamchamcham.application.security;

import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.nimbusds.jose.util.DefaultResourceRetriever;
import com.nimbusds.jose.util.JSONObjectUtils;
import com.nimbusds.jwt.JWTParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class KakaoOidcTokenVerifier {
    private final Logger logger = LoggerFactory.getLogger(KakaoOidcTokenVerifier.class);
    private final String issuer;
    private final String audience;
    private final Duration allowedClockSkew;
    private final Supplier<JwtDecoder> jwtDecoderProvider;
    private JwtDecoder jwtDecoder;

    private KakaoOidcTokenVerifier(String issuer, String audience, Duration allowedClockSkew, Supplier<JwtDecoder> jwtDecoderProvider) {
        this.issuer = Objects.requireNonNull(issuer, "issuer");
        this.audience = Objects.requireNonNull(audience, "audience");
        this.allowedClockSkew = Objects.requireNonNull(allowedClockSkew, "allowedClockSkew");
        this.jwtDecoderProvider = Objects.requireNonNull(jwtDecoderProvider, "jwtDecoderProvider");
    }
    @Autowired
    public KakaoOidcTokenVerifier(
        @Value("${auth.kakao.oidc.issuer:https://kauth.kakao.com}") String issuer,
        @Value("${auth.kakao.oidc.audience}") String audience,
        @Value("${auth.kakao.oidc.discovery-uri:https://kauth.kakao.com/.well-known/openid-configuration}") String discoveryUri,
        @Value("${auth.kakao.oidc.allowed-clock-skew-seconds:60}") long allowedClockSkewSeconds) {
        this(issuer, audience, Duration.ofSeconds(allowedClockSkewSeconds), () -> buildDecoderFromDiscovery(discoveryUri));
        Objects.requireNonNull(discoveryUri, "discoveryUri");
    }
    public KakaoOidcTokenVerifier(String issuer, String audience, Duration allowedClockSkew, JwtDecoder jwtDecoder) {
        this(issuer, audience, allowedClockSkew, () -> jwtDecoder);
        Objects.requireNonNull(jwtDecoder, "jwtDecoder");
    }
    private synchronized JwtDecoder decoder() {
        if (jwtDecoder == null) jwtDecoder = jwtDecoderProvider.get();
        return jwtDecoder;
    }
    public KakaoOidcClaims verify(String token, String nonce) {
        Objects.requireNonNull(token, "idToken");
        Objects.requireNonNull(nonce, "expectedNonce");
        try { JWTParser.parse(token); }
        catch (ParseException e) { throw new BusinessException(ErrorCode.MALFORMED_JWT); }
        Jwt jwt;
        try { jwt = decoder().decode(token); }
        catch (JwtException e) { throw new BusinessException(ErrorCode.INVALID_KAKAO_TOKEN); }
        catch (Exception e) {
            logger.error("Kakao OIDC decoder unavailable", e);
            throw new BusinessException(ErrorCode.KAKAO_OIDC_UNAVAILABLE);
        }
        if (!Objects.equals(jwt.getIssuer() == null ? null : jwt.getIssuer().toString(), issuer)) throw new BusinessException(ErrorCode.INVALID_KAKAO_TOKEN);
        if (!jwt.getAudience().contains(audience)) throw new BusinessException(ErrorCode.INVALID_KAKAO_TOKEN);
        var now = Instant.now();
        var expiresAt = jwt.getExpiresAt();
        if (expiresAt == null || expiresAt.plus(allowedClockSkew).isBefore(now)) throw new BusinessException(ErrorCode.INVALID_KAKAO_TOKEN);
        var issuedAt = jwt.getIssuedAt();
        if (issuedAt == null || issuedAt.minus(allowedClockSkew).isAfter(now)) throw new BusinessException(ErrorCode.INVALID_KAKAO_TOKEN);
        var actualNonce = jwt.getClaimAsString("nonce");
        if (actualNonce == null || !actualNonce.equals(nonce)) throw new BusinessException(ErrorCode.KAKAO_NONCE_MISMATCH);
        var subject = jwt.getSubject();
        if (subject == null || blank(subject)) throw new BusinessException(ErrorCode.INVALID_KAKAO_TOKEN);
        return new KakaoOidcClaims(subject, jwt.getClaimAsString("email"), Boolean.TRUE.equals(jwt.<Boolean>getClaim("email_verified")), actualNonce, expiresAt);
    }
    private static boolean blank(String value) {
        return value.chars().allMatch(c -> Character.isWhitespace(c) || Character.isSpaceChar(c));
    }
    private static JwtDecoder buildDecoderFromDiscovery(String discoveryUri) {
        try {
            var retriever = new DefaultResourceRetriever(2_000, 2_000);
            var resource = retriever.retrieveResource(URI.create(discoveryUri).toURL());
            var metadata = JSONObjectUtils.parse(resource.getContent());
            var jwksUri = JSONObjectUtils.getURI(metadata, "jwks_uri");
            if (jwksUri == null) jwksUri = URI.create("https://kauth.kakao.com/.well-known/jwks.json");
            return NimbusJwtDecoder.withJwkSetUri(jwksUri.toString()).build();
        } catch (java.io.IOException | ParseException e) { throw new IllegalStateException(e); }
    }
}
