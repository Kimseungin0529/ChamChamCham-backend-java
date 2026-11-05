package com.chamchamcham.application.auth.social;

import com.chamchamcham.application.auth.common.AuthCommand;
import com.chamchamcham.application.auth.common.AuthResult;
import com.chamchamcham.application.auth.common.OnboardingStatusResolver;
import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.chamchamcham.application.redis.AppleNonceReplayRepository;
import com.chamchamcham.application.redis.RefreshTokenRepository;
import com.chamchamcham.application.security.AppleOidcClaims;
import com.chamchamcham.application.security.AppleOidcTokenVerifier;
import com.chamchamcham.application.security.TokenProvider;
import com.chamchamcham.domain.member.AuthProvider;
import com.chamchamcham.domain.member.ExternalIdentity;
import com.chamchamcham.domain.member.ExternalIdentityRepository;
import com.chamchamcham.domain.member.Member;
import com.chamchamcham.domain.member.MemberStatus;
import com.chamchamcham.domain.member.MemberRepository;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.*;
import org.junit.jupiter.api.DisplayName;

@ExtendWith({MockitoExtension.class})
class AppleLoginServiceTest {
    private final UUID memberId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final UUID newMemberId = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private final UUID externalIdentityId = UUID.fromString("00000000-0000-0000-0000-000000000101");
    @Mock private AppleOidcTokenVerifier appleOidcTokenVerifier;
    @Mock private ExternalIdentityRepository externalIdentityRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private TokenProvider tokenProvider;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    private RecordingNonceRepository nonceRepository;
    private SocialLoginSupport socialLoginSupport;
    private AppleLoginService service;

    @BeforeEach
    void setUp() throws Exception {
        nonceRepository = new RecordingNonceRepository();
        socialLoginSupport = new SocialLoginSupport(externalIdentityRepository, memberRepository, tokenProvider, refreshTokenRepository, new OnboardingStatusResolver());
        service = new AppleLoginService(appleOidcTokenVerifier, nonceRepository, socialLoginSupport, 60L);
    }

    @Test
    @DisplayName("login reuses existing external identity and reserves nonce hash")
    void login_reuses_existing_external_identity_and_reserves_nonce_hash() throws Exception {
        var member = new Member(memberId, "user@example.com", null, MemberStatus.ACTIVE, null, null, null, null, null, null, "ROLE_USER", null, null);
        var claims = claims("user@example.com", true, Instant.now().plusSeconds(300));
        when(appleOidcTokenVerifier.verify("identity-token", "raw-nonce")).thenReturn(claims);
        when(externalIdentityRepository.findByProviderAndProviderSubject(AuthProvider.APPLE, "apple-sub")).thenReturn(existingIdentity(member));
        when(tokenProvider.generateToken(memberId, "ROLE_USER")).thenReturn(new AuthResult.TokenPair("access-token", "refresh-token"));
        when(tokenProvider.getRefreshTokenValiditySeconds()).thenReturn(120L);
        var result = service.login(command("apple-sub", null));
        assertLoginResult(result, memberId, "user@example.com");
        assertEquals("nonce-hash", nonceRepository.lastNonceHash);
        assertTrue(Objects.requireNonNull(nonceRepository.lastTtl).compareTo(Duration.ZERO) > 0);
        verify(refreshTokenRepository).save(memberId, "refresh-token", 120L);
    }

    @Test
    @DisplayName("login reuses existing external identity without requiring verified email")
    void login_reuses_existing_external_identity_without_requiring_verified_email() throws Exception {
        var member = new Member(memberId, "user@example.com", null, MemberStatus.ACTIVE, null, null, null, null, null, null, "ROLE_USER", null, null);
        when(appleOidcTokenVerifier.verify("identity-token", "raw-nonce")).thenReturn(claims(null, false, Instant.now().plusSeconds(300)));
        when(externalIdentityRepository.findByProviderAndProviderSubject(AuthProvider.APPLE, "apple-sub")).thenReturn(existingIdentity(member));
        when(tokenProvider.generateToken(memberId, "ROLE_USER")).thenReturn(new AuthResult.TokenPair("access-token", "refresh-token"));
        when(tokenProvider.getRefreshTokenValiditySeconds()).thenReturn(120L);
        var result = service.login(command("apple-sub", null));
        assertLoginResult(result, memberId, "user@example.com");
        verify(memberRepository, Mockito.never()).findByEmail(Mockito.anyString());
    }

    @Test
    @DisplayName("login rejects mismatched user identifier")
    void login_rejects_mismatched_user_identifier() throws Exception {
        when(appleOidcTokenVerifier.verify("identity-token", "raw-nonce")).thenReturn(claims("user@example.com", true, Instant.now().plusSeconds(300)));
        var exception = assertThrows(BusinessException.class, () -> service.login(command("different-sub", null)));
        assertEquals(ErrorCode.INVALID_APPLE_TOKEN, exception.getErrorCode());
        assertNull(nonceRepository.lastNonceHash);
        assertNull(nonceRepository.lastTtl);
    }

    @Test
    @DisplayName("login creates member without email for new identity")
    void login_creates_member_without_email_for_new_identity() throws Exception {
        var savedMember = new Member(newMemberId, null, null, MemberStatus.ACTIVE, null, null, null, null, null, null, "ROLE_USER", null, null);
        when(appleOidcTokenVerifier.verify("identity-token", "raw-nonce")).thenReturn(claims(null, false, Instant.now().plusSeconds(300)));
        when(externalIdentityRepository.findByProviderAndProviderSubject(AuthProvider.APPLE, "apple-sub")).thenReturn(null);
        when(memberRepository.save(Mockito.any(Member.class))).thenReturn(savedMember);
        when(externalIdentityRepository.save(Mockito.any(ExternalIdentity.class))).thenAnswer(it -> it.getArgument(0));
        when(tokenProvider.generateToken(newMemberId, "ROLE_USER")).thenReturn(new AuthResult.TokenPair("access-token", "refresh-token"));
        when(tokenProvider.getRefreshTokenValiditySeconds()).thenReturn(120L);
        var result = service.login(command("apple-sub", null));
        var memberCaptor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).save(memberCaptor.capture());
        assertNull(memberCaptor.getValue().getEmail());
        assertLoginResult(result, newMemberId, null);
        verify(memberRepository, Mockito.never()).findByEmail(Mockito.anyString());
    }

    @Test
    @DisplayName("login forwards client-supplied name when creating a new member")
    void login_forwards_client_supplied_name_when_creating_a_new_member() throws Exception {
        var savedMember = new Member(newMemberId, null, null, MemberStatus.ACTIVE, "홍길동", null, null, null, null, null, "ROLE_USER", null, null);
        when(appleOidcTokenVerifier.verify("identity-token", "raw-nonce")).thenReturn(claims(null, false, Instant.now().plusSeconds(300)));
        when(externalIdentityRepository.findByProviderAndProviderSubject(AuthProvider.APPLE, "apple-sub")).thenReturn(null);
        when(memberRepository.save(Mockito.any(Member.class))).thenReturn(savedMember);
        when(externalIdentityRepository.save(Mockito.any(ExternalIdentity.class))).thenAnswer(it -> it.getArgument(0));
        when(tokenProvider.generateToken(newMemberId, "ROLE_USER")).thenReturn(new AuthResult.TokenPair("access-token", "refresh-token"));
        when(tokenProvider.getRefreshTokenValiditySeconds()).thenReturn(120L);
        service.login(command("apple-sub", "홍길동"));
        var memberCaptor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).save(memberCaptor.capture());
        assertEquals("홍길동", memberCaptor.getValue().getName());
    }

    @Test
    @DisplayName("login rejects replayed nonce hash")
    void login_rejects_replayed_nonce_hash() throws Exception {
        nonceRepository.setReserveResult(false);
        when(appleOidcTokenVerifier.verify("identity-token", "raw-nonce")).thenReturn(claims("user@example.com", true, Instant.now().plusSeconds(300)));
        var exception = assertThrows(BusinessException.class, () -> service.login(command("apple-sub", null)));
        assertEquals(ErrorCode.APPLE_NONCE_REPLAY, exception.getErrorCode());
    }

    @Test
    @DisplayName("login rejects expired token beyond allowed skew")
    void login_rejects_expired_token_beyond_allowed_skew() throws Exception {
        when(appleOidcTokenVerifier.verify("identity-token", "raw-nonce")).thenReturn(claims("user@example.com", true, Instant.now().minusSeconds(61)));
        var exception = assertThrows(BusinessException.class, () -> service.login(command("apple-sub", null)));
        assertEquals(ErrorCode.INVALID_APPLE_TOKEN, exception.getErrorCode());
    }

    @Test
    @DisplayName("login creates member for new identity with verified email")
    void login_creates_member_for_new_identity_with_verified_email() throws Exception {
        var savedMember = new Member(newMemberId, "new@example.com", null, MemberStatus.ACTIVE, null, null, null, null, null, null, "ROLE_USER", null, null);
        when(appleOidcTokenVerifier.verify("identity-token", "raw-nonce")).thenReturn(claims("new@example.com", true, Instant.now().plusSeconds(300)));
        when(externalIdentityRepository.findByProviderAndProviderSubject(AuthProvider.APPLE, "apple-sub")).thenReturn(null);
        when(memberRepository.save(Mockito.any(Member.class))).thenReturn(savedMember);
        when(externalIdentityRepository.save(Mockito.any(ExternalIdentity.class))).thenAnswer(it -> it.getArgument(0));
        when(tokenProvider.generateToken(newMemberId, "ROLE_USER")).thenReturn(new AuthResult.TokenPair("access-token", "refresh-token"));
        when(tokenProvider.getRefreshTokenValiditySeconds()).thenReturn(120L);
        var result = service.login(command("", null));
        assertLoginResult(result, newMemberId, "new@example.com");
        verify(memberRepository, Mockito.never()).findByEmail(Mockito.anyString());
    }
    private void assertLoginResult(AuthResult.Login result, UUID expectedMemberId, String expectedEmail) {
        assertEquals("access-token", result.getAccessToken());
        assertEquals("refresh-token", result.getRefreshToken());
        assertEquals(expectedMemberId, result.getMember().getId());
        assertEquals(expectedEmail, result.getMember().getEmail());
        assertEquals(AuthResult.OnboardingStatus.REQUIRED, result.getOnboarding().getStatus());
    }
    private ExternalIdentity existingIdentity(Member member) {
        return new ExternalIdentity(externalIdentityId, member, AuthProvider.APPLE, "apple-sub");
    }
    private AuthCommand.AppleLogin command(String userIdentifier, String name) {
        return new AuthCommand.AppleLogin("identity-token", "raw-nonce", "authorization-code", userIdentifier, name);
    }
    private AppleOidcClaims claims(String email, boolean emailVerified, Instant expiresAt) {
        return new AppleOidcClaims("apple-sub", email, emailVerified, "nonce-hash", expiresAt);
    }
    private static class RecordingNonceRepository implements AppleNonceReplayRepository {
        boolean reserveResult = true;
        String lastNonceHash;
        Duration lastTtl;
        void setReserveResult(boolean result) { reserveResult = result; }
        @Override public boolean reserve(String nonce, Duration ttl) {
            lastNonceHash = nonce;
            lastTtl = ttl;
            return reserveResult;
        }
    }
}
