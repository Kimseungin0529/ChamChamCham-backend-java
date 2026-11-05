package com.chamchamcham.application.auth.social;

import com.chamchamcham.application.auth.common.AuthCommand;
import com.chamchamcham.application.auth.common.AuthResult;
import com.chamchamcham.application.auth.common.OnboardingStatusResolver;
import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.chamchamcham.application.redis.KakaoNonceReplayRepository;
import com.chamchamcham.application.redis.RefreshTokenRepository;
import com.chamchamcham.application.security.KakaoOidcClaims;
import com.chamchamcham.application.security.KakaoOidcTokenVerifier;
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
import java.time.LocalDate;
import java.util.UUID;
import java.util.*;
import org.junit.jupiter.api.DisplayName;

@ExtendWith({MockitoExtension.class})
class KakaoLoginServiceTest {
    private final UUID memberId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final UUID newMemberId = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private final UUID externalIdentityId = UUID.fromString("00000000-0000-0000-0000-000000000101");
    @Mock private KakaoOidcTokenVerifier kakaoOidcTokenVerifier;
    @Mock private KakaoUserInfoClient kakaoUserInfoClient;
    @Mock private ExternalIdentityRepository externalIdentityRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private TokenProvider tokenProvider;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    private RecordingNonceRepository nonceRepository;
    private SocialLoginSupport socialLoginSupport;
    private KakaoLoginService service;

    @BeforeEach
    void setUp() throws Exception {
        nonceRepository = new RecordingNonceRepository();
        socialLoginSupport = new SocialLoginSupport(externalIdentityRepository, memberRepository, tokenProvider, refreshTokenRepository, new OnboardingStatusResolver());
        service = new KakaoLoginService(kakaoOidcTokenVerifier, kakaoUserInfoClient, nonceRepository, socialLoginSupport, 600L, 60L);
    }

    @Test
    @DisplayName("login reuses existing external identity without requiring email")
    void login_reuses_existing_external_identity_without_requiring_email() throws Exception {
        var member = new Member(memberId, "user@example.com", null, MemberStatus.ACTIVE, null, null, null, null, null, null, "ROLE_USER", null, null);
        var claims = claims(null, false, Instant.now().plusSeconds(300));
        when(kakaoOidcTokenVerifier.verify("id-token", "nonce")).thenReturn(claims);
        when(externalIdentityRepository.findByProviderAndProviderSubject(AuthProvider.KAKAO, "kakao-sub")).thenReturn(new ExternalIdentity(externalIdentityId, member, AuthProvider.KAKAO, "kakao-sub"));
        when(tokenProvider.generateToken(memberId, "ROLE_USER")).thenReturn(new AuthResult.TokenPair("access-token", "refresh-token"));
        when(tokenProvider.getRefreshTokenValiditySeconds()).thenReturn(120L);
        var result = service.login(new AuthCommand.KakaoLogin("id-token", "nonce", null));
        assertLoginResult(result, memberId, "user@example.com");
        assertEquals("nonce", nonceRepository.lastNonce);
        verify(memberRepository, Mockito.never()).findByEmail(Mockito.anyString());
        verify(refreshTokenRepository).save(memberId, "refresh-token", 120L);
    }

    @Test
    @DisplayName("login creates new member by provider subject without linking by email")
    void login_creates_new_member_by_provider_subject_without_linking_by_email() throws Exception {
        var savedMember = new Member(newMemberId, "user@example.com", null, MemberStatus.ACTIVE, null, null, null, null, null, null, "ROLE_USER", null, null);
        var claims = claims("user@example.com", true, Instant.now().plusSeconds(300));
        when(kakaoOidcTokenVerifier.verify("id-token", "nonce")).thenReturn(claims);
        when(externalIdentityRepository.findByProviderAndProviderSubject(AuthProvider.KAKAO, "kakao-sub")).thenReturn(null);
        when(memberRepository.save(Mockito.any(Member.class))).thenReturn(savedMember);
        when(externalIdentityRepository.save(Mockito.any(ExternalIdentity.class))).thenAnswer(it -> it.getArgument(0));
        when(tokenProvider.generateToken(newMemberId, "ROLE_USER")).thenReturn(new AuthResult.TokenPair("access-token", "refresh-token"));
        when(tokenProvider.getRefreshTokenValiditySeconds()).thenReturn(120L);
        var result = service.login(new AuthCommand.KakaoLogin("id-token", "nonce", null));
        var memberCaptor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).save(memberCaptor.capture());
        assertEquals("user@example.com", memberCaptor.getValue().getEmail());
        var identityCaptor = ArgumentCaptor.forClass(ExternalIdentity.class);
        verify(externalIdentityRepository).save(identityCaptor.capture());
        assertEquals(savedMember, identityCaptor.getValue().getMember());
        assertEquals(AuthProvider.KAKAO, identityCaptor.getValue().getProvider());
        assertEquals("kakao-sub", identityCaptor.getValue().getProviderSubject());
        assertLoginResult(result, newMemberId, "user@example.com");
        verify(memberRepository, Mockito.never()).findByEmail(Mockito.anyString());
    }

    @Test
    @DisplayName("login creates new member with null password hash")
    void login_creates_new_member_with_null_password_hash() throws Exception {
        var savedMember = new Member(newMemberId, "new@example.com", null, MemberStatus.ACTIVE, null, null, null, null, null, null, "ROLE_USER", null, null);
        when(kakaoOidcTokenVerifier.verify("id-token", "nonce")).thenReturn(claims("new@example.com", true, Instant.now().plusSeconds(300)));
        when(externalIdentityRepository.findByProviderAndProviderSubject(AuthProvider.KAKAO, "kakao-sub")).thenReturn(null);
        when(memberRepository.save(Mockito.any(Member.class))).thenReturn(savedMember);
        when(externalIdentityRepository.save(Mockito.any(ExternalIdentity.class))).thenAnswer(it -> it.getArgument(0));
        when(tokenProvider.generateToken(newMemberId, "ROLE_USER")).thenReturn(new AuthResult.TokenPair("access-token", "refresh-token"));
        when(tokenProvider.getRefreshTokenValiditySeconds()).thenReturn(120L);
        var result = service.login(new AuthCommand.KakaoLogin("id-token", "nonce", null));
        var memberCaptor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).save(memberCaptor.capture());
        assertEquals("new@example.com", memberCaptor.getValue().getEmail());
        assertNull(memberCaptor.getValue().getPasswordHash());
        assertLoginResult(result, newMemberId, "new@example.com");
        verify(memberRepository, Mockito.never()).findByEmail(Mockito.anyString());
    }

    @Test
    @DisplayName("login creates new member without email")
    void login_creates_new_member_without_email() throws Exception {
        var savedMember = new Member(newMemberId, null, null, MemberStatus.ACTIVE, null, null, null, null, null, null, "ROLE_USER", null, null);
        when(kakaoOidcTokenVerifier.verify("id-token", "nonce")).thenReturn(claims(null, false, Instant.now().plusSeconds(300)));
        when(externalIdentityRepository.findByProviderAndProviderSubject(AuthProvider.KAKAO, "kakao-sub")).thenReturn(null);
        when(memberRepository.save(Mockito.any(Member.class))).thenReturn(savedMember);
        when(externalIdentityRepository.save(Mockito.any(ExternalIdentity.class))).thenAnswer(it -> it.getArgument(0));
        when(tokenProvider.generateToken(newMemberId, "ROLE_USER")).thenReturn(new AuthResult.TokenPair("access-token", "refresh-token"));
        when(tokenProvider.getRefreshTokenValiditySeconds()).thenReturn(120L);
        var result = service.login(new AuthCommand.KakaoLogin("id-token", "nonce", null));
        var memberCaptor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).save(memberCaptor.capture());
        assertNull(memberCaptor.getValue().getEmail());
        assertLoginResult(result, newMemberId, null);
        verify(memberRepository, Mockito.never()).findByEmail(Mockito.anyString());
    }

    @Test
    @DisplayName("login rejects replayed nonce")
    void login_rejects_replayed_nonce() throws Exception {
        nonceRepository.setReserveResult(false);
        when(kakaoOidcTokenVerifier.verify("id-token", "nonce")).thenReturn(claims("user@example.com", true, Instant.now().plusSeconds(300)));
        var exception = assertThrows(BusinessException.class, () -> service.login(new AuthCommand.KakaoLogin("id-token", "nonce", null)));
        assertEquals(ErrorCode.KAKAO_NONCE_REPLAY, exception.getErrorCode());
    }

    @Test
    @DisplayName("login reserves nonce for token accepted within clock skew")
    void login_reserves_nonce_for_token_accepted_within_clock_skew() throws Exception {
        var member = new Member(memberId, "user@example.com", null, MemberStatus.ACTIVE, null, null, null, null, null, null, "ROLE_USER", null, null);
        when(kakaoOidcTokenVerifier.verify("id-token", "nonce")).thenReturn(claims("user@example.com", true, Instant.now().minusSeconds(10)));
        when(externalIdentityRepository.findByProviderAndProviderSubject(AuthProvider.KAKAO, "kakao-sub")).thenReturn(new ExternalIdentity(externalIdentityId, member, AuthProvider.KAKAO, "kakao-sub"));
        when(tokenProvider.generateToken(memberId, "ROLE_USER")).thenReturn(new AuthResult.TokenPair("access-token", "refresh-token"));
        when(tokenProvider.getRefreshTokenValiditySeconds()).thenReturn(120L);
        var result = service.login(new AuthCommand.KakaoLogin("id-token", "nonce", null));
        var ttl = Objects.requireNonNull(nonceRepository.lastTtl);
        assertTrue(ttl.compareTo(Duration.ZERO) > 0);
        assertTrue(ttl.compareTo(Duration.ofSeconds(60)) <= 0);
        assertLoginResult(result, memberId, "user@example.com");
    }

    @Test
    @DisplayName("login creates new member without email when access token is absent and id token email is unavailable")
    void login_creates_new_member_without_email_when_access_token_is_absent_and_id_token_email_is_unavailable() throws Exception {
        var savedMember = new Member(newMemberId, null, null, MemberStatus.ACTIVE, null, null, null, null, null, null, "ROLE_USER", null, null);
        when(kakaoOidcTokenVerifier.verify("id-token", "nonce")).thenReturn(claims("user@example.com", false, Instant.now().plusSeconds(300)));
        when(externalIdentityRepository.findByProviderAndProviderSubject(AuthProvider.KAKAO, "kakao-sub")).thenReturn(null);
        when(memberRepository.save(Mockito.any(Member.class))).thenReturn(savedMember);
        when(externalIdentityRepository.save(Mockito.any(ExternalIdentity.class))).thenAnswer(it -> it.getArgument(0));
        when(tokenProvider.generateToken(newMemberId, "ROLE_USER")).thenReturn(new AuthResult.TokenPair("access-token", "refresh-token"));
        when(tokenProvider.getRefreshTokenValiditySeconds()).thenReturn(120L);
        var result = service.login(new AuthCommand.KakaoLogin("id-token", "nonce", null));
        var memberCaptor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).save(memberCaptor.capture());
        assertNull(memberCaptor.getValue().getEmail());
        assertLoginResult(result, newMemberId, null);
        Mockito.verifyNoInteractions(kakaoUserInfoClient);
    }

    @Test
    @DisplayName("login prefills profile from userinfo when access token is present")
    void login_prefills_profile_from_userinfo_when_access_token_is_present() throws Exception {
        var savedMember = new Member(newMemberId, "userinfo@example.com", null, MemberStatus.ACTIVE, null, null, null, null, null, null, "ROLE_USER", null, null);
        when(kakaoOidcTokenVerifier.verify("id-token", "nonce")).thenReturn(claims("claims@example.com", true, Instant.now().plusSeconds(300)));
        when(kakaoUserInfoClient.fetch("access-token")).thenReturn(new KakaoUserInfo("kakao-sub", "userinfo@example.com", "카카오회원", "010-1234-5678", LocalDate.of(1990, 5, 12)));
        when(externalIdentityRepository.findByProviderAndProviderSubject(AuthProvider.KAKAO, "kakao-sub")).thenReturn(null);
        when(memberRepository.save(Mockito.any(Member.class))).thenReturn(savedMember);
        when(externalIdentityRepository.save(Mockito.any(ExternalIdentity.class))).thenAnswer(it -> it.getArgument(0));
        when(tokenProvider.generateToken(newMemberId, "ROLE_USER")).thenReturn(new AuthResult.TokenPair("access-token", "refresh-token"));
        when(tokenProvider.getRefreshTokenValiditySeconds()).thenReturn(120L);
        var result = service.login(new AuthCommand.KakaoLogin("id-token", "nonce", "access-token"));
        var memberCaptor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).save(memberCaptor.capture());
        assertEquals("userinfo@example.com", memberCaptor.getValue().getEmail());
        assertEquals("카카오회원", memberCaptor.getValue().getName());
        assertEquals("010-1234-5678", memberCaptor.getValue().getPhone());
        assertEquals(LocalDate.of(1990, 5, 12), memberCaptor.getValue().getBirthDate());
        assertLoginResult(result, newMemberId, "userinfo@example.com");
        assertEquals("nonce", nonceRepository.lastNonce);
    }

    @Test
    @DisplayName("login rejects userinfo subject mismatch before nonce reservation and member writes")
    void login_rejects_userinfo_subject_mismatch_before_nonce_reservation_and_member_writes() throws Exception {
        when(kakaoOidcTokenVerifier.verify("id-token", "nonce")).thenReturn(claims("user@example.com", true, Instant.now().plusSeconds(300)));
        when(kakaoUserInfoClient.fetch("access-token")).thenReturn(new KakaoUserInfo("other-sub", "userinfo@example.com", null, null, null));
        var exception = assertThrows(BusinessException.class, () -> service.login(new AuthCommand.KakaoLogin("id-token", "nonce", "access-token")));
        assertEquals(ErrorCode.INVALID_KAKAO_TOKEN, exception.getErrorCode());
        assertNull(nonceRepository.lastNonce);
        verify(memberRepository, Mockito.never()).save(Mockito.any(Member.class));
        verify(externalIdentityRepository, Mockito.never()).save(Mockito.any(ExternalIdentity.class));
    }
    private void assertLoginResult(AuthResult.Login result, UUID expectedMemberId, String expectedEmail) {
        assertEquals("access-token", result.getAccessToken());
        assertEquals("refresh-token", result.getRefreshToken());
        assertEquals(expectedMemberId, result.getMember().getId());
        assertEquals(expectedEmail, result.getMember().getEmail());
        assertEquals(AuthResult.OnboardingStatus.REQUIRED, result.getOnboarding().getStatus());
    }
    private KakaoOidcClaims claims(String email, boolean emailVerified, Instant expiresAt) {
        return new KakaoOidcClaims("kakao-sub", email, emailVerified, "nonce", expiresAt);
    }
    private static class RecordingNonceRepository implements KakaoNonceReplayRepository {
        boolean reserveResult = true;
        String lastNonce;
        Duration lastTtl;
        void setReserveResult(boolean result) { reserveResult = result; }
        @Override public boolean reserve(String nonce, Duration ttl) {
            lastNonce = nonce;
            lastTtl = ttl;
            return reserveResult;
        }
    }
}
