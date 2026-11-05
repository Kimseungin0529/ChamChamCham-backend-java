package com.chamchamcham.application.security;

import static com.chamchamcham.application.testsupport.TestJwtFixtures.signedTestToken;
import static com.chamchamcham.application.testsupport.TestJwtFixtures.testTokenProvider;
import io.jsonwebtoken.JwtException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import java.util.*;
import org.junit.jupiter.api.DisplayName;

class TokenProviderTest {
    private final UUID memberId = UUID.fromString("00000000-0000-0000-0000-000000000007");
    private final UUID adminMemberId = UUID.fromString("00000000-0000-0000-0000-000000000009");
    private final TokenProvider tokenProvider = testTokenProvider();

    @Test
    @DisplayName("generateToken embeds member id and role in access token")
    void generateToken_embeds_member_id_and_role_in_access_token() throws Exception {
        var tokenPair = tokenProvider.generateToken(memberId, "ROLE_USER");
        assertTrue(tokenProvider.validateToken(tokenPair.getAccessToken()));
        assertTrue(tokenProvider.isAccessToken(tokenPair.getAccessToken()));
        assertEquals(memberId, tokenProvider.getMemberId(tokenPair.getAccessToken()));
        assertEquals("ROLE_USER", tokenProvider.getRole(tokenPair.getAccessToken()));
    }

    @Test
    @DisplayName("refresh token is bound to the member id")
    void refresh_token_is_bound_to_the_member_id() throws Exception {
        var refreshToken = tokenProvider.createRefreshToken(adminMemberId);
        var authentication = tokenProvider.getAuthentication(tokenProvider.createAccessToken(adminMemberId, "ROLE_ADMIN"));
        assertTrue(tokenProvider.validateToken(refreshToken));
        assertTrue(tokenProvider.isRefreshToken(refreshToken));
        assertEquals(adminMemberId, tokenProvider.getMemberId(refreshToken));
        assertEquals(adminMemberId.toString(), authentication.getPrincipal());
        assertEquals("ROLE_ADMIN", authentication.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    @DisplayName("validateToken rejects signed access token with non uuid subject")
    void validateToken_rejects_signed_access_token_with_non_uuid_subject() throws Exception {
        var token = signedTestToken("42", "access", "ROLE_USER");
        assertFalse(tokenProvider.validateToken(token));
        assertThrows(JwtException.class, () -> tokenProvider.getMemberId(token));
    }

    @Test
    @DisplayName("validateToken rejects signed refresh token with non uuid subject")
    void validateToken_rejects_signed_refresh_token_with_non_uuid_subject() throws Exception {
        var token = signedTestToken("42", "refresh", null);
        assertFalse(tokenProvider.validateToken(token));
        assertThrows(JwtException.class, () -> tokenProvider.getMemberId(token));
    }
}
