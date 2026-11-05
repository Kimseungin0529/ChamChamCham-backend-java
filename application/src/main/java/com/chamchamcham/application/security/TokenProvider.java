package com.chamchamcham.application.security;

import com.chamchamcham.application.auth.common.AuthResult;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import java.util.*;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.lang.Nullable;

@Component
public class TokenProvider {
    private static final long ACCESS_TOKEN_VALIDITY = 1000L * 60 * 60 * 24 * 7;
    private static final long REFRESH_TOKEN_VALIDITY = 1000L * 60 * 60 * 24 * 14;
    private static final String ROLE_CLAIM = "role";
    private static final String TOKEN_TYPE_CLAIM = "tokenType";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";
    private final SecretKey key;

    public TokenProvider(@Value("${jwt.secret}") String secretKey) {
        Objects.requireNonNull(secretKey, "secretKey");
        var decodedSecretKey = Base64.getDecoder().decode(secretKey);
        if (decodedSecretKey.length < 64) throw new IllegalArgumentException("jwt.secret is too short for HS512: " + decodedSecretKey.length * 8 + " bits. Set JWT secret to a Base64 value that decodes to at least 64 bytes.");
        key = Keys.hmacShaKeyFor(decodedSecretKey);
    }
    public String createAccessToken(UUID memberId, String role) {
        Objects.requireNonNull(memberId, "memberId"); Objects.requireNonNull(role, "role");
        var now = new Date();
        return Jwts.builder().setSubject(memberId.toString()).claim(ROLE_CLAIM, role)
            .claim(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE).setIssuedAt(now)
            .setExpiration(new Date(now.getTime() + ACCESS_TOKEN_VALIDITY))
            .signWith(key, SignatureAlgorithm.HS512).compact();
    }
    public String createRefreshToken(UUID memberId) {
        Objects.requireNonNull(memberId, "memberId");
        var now = new Date();
        return Jwts.builder().setSubject(memberId.toString()).claim(TOKEN_TYPE_CLAIM, REFRESH_TOKEN_TYPE)
            .setIssuedAt(now).setExpiration(new Date(now.getTime() + REFRESH_TOKEN_VALIDITY))
            .signWith(key, SignatureAlgorithm.HS512).compact();
    }
    public AuthResult.TokenPair generateToken(UUID memberId, String role) {
        Objects.requireNonNull(memberId, "memberId"); Objects.requireNonNull(role, "role");
        return new AuthResult.TokenPair(createAccessToken(memberId, role), createRefreshToken(memberId));
    }
    public long getRefreshTokenValiditySeconds() { return REFRESH_TOKEN_VALIDITY / 1000; }
    public boolean validateToken(String token) {
        Objects.requireNonNull(token, "token");
        try {
            var claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
            parseMemberId(claims.getSubject());
            return true;
        } catch (JwtException | IllegalArgumentException e) { return false; }
    }
    public Claims parseClaims(String token) {
        Objects.requireNonNull(token, "token");
        try { return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody(); }
        catch (ExpiredJwtException e) { return e.getClaims(); }
    }
    public UUID getMemberId(String token) { return parseMemberId(parseClaims(token).getSubject()); }
    @Nullable public String getRole(String token) {
        var role = parseClaims(token).get(ROLE_CLAIM);
        return role instanceof String value ? value : null;
    }
    public boolean isAccessToken(String token) { return ACCESS_TOKEN_TYPE.equals(parseClaims(token).get(TOKEN_TYPE_CLAIM)); }
    public boolean isRefreshToken(String token) { return REFRESH_TOKEN_TYPE.equals(parseClaims(token).get(TOKEN_TYPE_CLAIM)); }
    public Authentication getAuthentication(String token) {
        var memberId = getMemberId(token);
        var role = getRole(token);
        var authorities = role == null ? Collections.<SimpleGrantedAuthority>emptyList() : Collections.singletonList(new SimpleGrantedAuthority(role));
        return new UsernamePasswordAuthenticationToken(memberId.toString(), null, authorities);
    }
    private UUID parseMemberId(String subject) {
        if (subject == null) throw new MalformedJwtException("JWT subject must be a valid member UUID");
        try { return UUID.fromString(subject); }
        catch (IllegalArgumentException e) { throw new MalformedJwtException("JWT subject must be a valid member UUID", e); }
    }
}
