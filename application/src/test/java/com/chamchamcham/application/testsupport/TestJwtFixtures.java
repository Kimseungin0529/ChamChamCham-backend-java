package com.chamchamcham.application.testsupport;

import com.chamchamcham.application.security.TokenProvider;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.util.Base64;
import java.util.Date;
import java.util.Objects;
import org.springframework.lang.Nullable;

public final class TestJwtFixtures {
    private TestJwtFixtures() {}
    public static final String TEST_JWT_SECRET = "t2oRk29vBQZWS8GEt4xr8AJznlPK0ipBKUwdyqe10SOGZB26vVBMjzqualdJsjcOY1wX9DOqJC9V1DFl58F0tQ==";
    public static TokenProvider testTokenProvider() { return new TokenProvider(TEST_JWT_SECRET); }
    public static String signedTestToken(String subject, String tokenType) { return signedTestToken(subject, tokenType, null); }
    public static String signedTestToken(String subject, String tokenType, @Nullable String role) {
        Objects.requireNonNull(subject, "subject"); Objects.requireNonNull(tokenType, "tokenType");
        var now = new Date();
        var builder = Jwts.builder().setSubject(subject).claim("tokenType", tokenType)
            .setIssuedAt(now).setExpiration(new Date(now.getTime() + 60_000L));
        if (role != null) builder.claim("role", role);
        return builder.signWith(Keys.hmacShaKeyFor(Base64.getDecoder().decode(TEST_JWT_SECRET)), SignatureAlgorithm.HS512).compact();
    }
}
