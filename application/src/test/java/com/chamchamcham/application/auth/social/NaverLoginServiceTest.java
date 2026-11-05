package com.chamchamcham.application.auth.social;

import com.chamchamcham.application.auth.common.AuthCommand;
import com.chamchamcham.application.auth.common.AuthResult;
import com.chamchamcham.application.auth.common.OnboardingStatusResolver;
import com.chamchamcham.application.redis.RefreshTokenRepository;
import com.chamchamcham.application.security.TokenProvider;
import com.chamchamcham.domain.member.AuthProvider;
import com.chamchamcham.domain.member.ExternalIdentity;
import com.chamchamcham.domain.member.ExternalIdentityRepository;
import com.chamchamcham.domain.member.Member;
import com.chamchamcham.domain.member.MemberStatus;
import com.chamchamcham.domain.member.MemberRepository;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDate;
import java.util.UUID;
import java.util.*;
import org.junit.jupiter.api.DisplayName;

@ExtendWith({MockitoExtension.class})
class NaverLoginServiceTest {
    private final UUID memberId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final UUID existingMemberId = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private final UUID externalIdentityId = UUID.fromString("00000000-0000-0000-0000-000000000101");
    @Mock private ExternalIdentityRepository externalIdentityRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private TokenProvider tokenProvider;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    private StubNaverProfileClient profileClient;
    private NaverLoginService service;

    @BeforeEach
    void setUp() throws Exception {
        profileClient = new StubNaverProfileClient();
        var socialLoginSupport = new SocialLoginSupport(externalIdentityRepository, memberRepository, tokenProvider, refreshTokenRepository, new OnboardingStatusResolver());
        service = new NaverLoginService(profileClient, socialLoginSupport);
    }

    @Test
    @DisplayName("login creates member and pre-fills profile from Naver")
    void login_creates_member_and_pre_fills_profile_from_Naver() throws Exception {
        var birthDate = LocalDate.of(1990, 5, 12);
        var savedMember = new Member(memberId, "new@example.com", null, MemberStatus.ACTIVE, null, null, null, null, null, null, "ROLE_USER", null, null);
        profileClient.setProfile(profile("new@example.com", "네이버회원", "010-1234-5678", birthDate));
        when(externalIdentityRepository.findByProviderAndProviderSubject(AuthProvider.NAVER, "naver-sub")).thenReturn(null);
        when(memberRepository.save(Mockito.any(Member.class))).thenReturn(savedMember);
        when(externalIdentityRepository.save(Mockito.any(ExternalIdentity.class))).thenAnswer(it -> it.getArgument(0));
        when(tokenProvider.generateToken(memberId, "ROLE_USER")).thenReturn(new AuthResult.TokenPair("access-token", "refresh-token"));
        when(tokenProvider.getRefreshTokenValiditySeconds()).thenReturn(120L);
        var result = service.login(new AuthCommand.NaverLogin("access-token"));
        var memberCaptor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).save(memberCaptor.capture());
        assertEquals("new@example.com", memberCaptor.getValue().getEmail());
        assertEquals("네이버회원", memberCaptor.getValue().getName());
        assertEquals("010-1234-5678", memberCaptor.getValue().getPhone());
        assertEquals(birthDate, memberCaptor.getValue().getBirthDate());
        var identityCaptor = ArgumentCaptor.forClass(ExternalIdentity.class);
        verify(externalIdentityRepository).save(identityCaptor.capture());
        assertEquals(AuthProvider.NAVER, identityCaptor.getValue().getProvider());
        assertEquals("naver-sub", identityCaptor.getValue().getProviderSubject());
        assertEquals("access-token", result.getAccessToken());
        assertEquals("refresh-token", result.getRefreshToken());
        assertEquals(memberId, result.getMember().getId());
        assertEquals("네이버회원", result.getMember().getName());
        assertEquals("010-1234-5678", result.getMember().getPhone());
        assertEquals(birthDate, result.getMember().getBirthDate());
        verify(memberRepository, Mockito.never()).findByEmail(Mockito.anyString());
        verify(refreshTokenRepository).save(memberId, "refresh-token", 120L);
    }

    @Test
    @DisplayName("login creates member without email for new identity")
    void login_creates_member_without_email_for_new_identity() throws Exception {
        var savedMember = new Member(memberId, null, null, MemberStatus.ACTIVE, null, null, null, null, null, null, "ROLE_USER", null, null);
        profileClient.setProfile(profile(null, "네이버회원", null, null));
        when(externalIdentityRepository.findByProviderAndProviderSubject(AuthProvider.NAVER, "naver-sub")).thenReturn(null);
        when(memberRepository.save(Mockito.any(Member.class))).thenReturn(savedMember);
        when(externalIdentityRepository.save(Mockito.any(ExternalIdentity.class))).thenAnswer(it -> it.getArgument(0));
        when(tokenProvider.generateToken(memberId, "ROLE_USER")).thenReturn(new AuthResult.TokenPair("access-token", "refresh-token"));
        when(tokenProvider.getRefreshTokenValiditySeconds()).thenReturn(120L);
        var result = service.login(new AuthCommand.NaverLogin("access-token"));
        var memberCaptor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).save(memberCaptor.capture());
        assertNull(memberCaptor.getValue().getEmail());
        assertEquals("네이버회원", memberCaptor.getValue().getName());
        assertEquals(memberId, result.getMember().getId());
        assertNull(result.getMember().getEmail());
        assertEquals("네이버회원", result.getMember().getName());
        verify(memberRepository, Mockito.never()).findByEmail(Mockito.anyString());
    }

    @Test
    @DisplayName("login creates member without email when provider email already belongs to another member")
    void login_creates_member_without_email_when_provider_email_already_belongs_to_another_member() throws Exception {
        var savedMember = new Member(memberId, null, null, MemberStatus.ACTIVE, null, null, null, null, null, null, "ROLE_USER", null, null);
        profileClient.setProfile(profile("existing@example.com", "네이버회원", null, null));
        when(externalIdentityRepository.findByProviderAndProviderSubject(AuthProvider.NAVER, "naver-sub")).thenReturn(null);
        when(memberRepository.existsByEmail("existing@example.com")).thenReturn(true);
        when(memberRepository.save(Mockito.any(Member.class))).thenReturn(savedMember);
        when(externalIdentityRepository.save(Mockito.any(ExternalIdentity.class))).thenAnswer(it -> it.getArgument(0));
        when(tokenProvider.generateToken(memberId, "ROLE_USER")).thenReturn(new AuthResult.TokenPair("access-token", "refresh-token"));
        when(tokenProvider.getRefreshTokenValiditySeconds()).thenReturn(120L);
        var result = service.login(new AuthCommand.NaverLogin("access-token"));
        var memberCaptor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).save(memberCaptor.capture());
        assertNull(memberCaptor.getValue().getEmail());
        assertEquals("네이버회원", memberCaptor.getValue().getName());
        assertEquals(memberId, result.getMember().getId());
        assertNull(result.getMember().getEmail());
        assertEquals("네이버회원", result.getMember().getName());
        verify(memberRepository, Mockito.never()).findByEmail(Mockito.anyString());
    }

    @Test
    @DisplayName("login reuses existing identity without requiring email")
    void login_reuses_existing_identity_without_requiring_email() throws Exception {
        var member = new Member(existingMemberId, "existing@example.com", null, MemberStatus.ACTIVE, null, null, null, null, null, null, "ROLE_USER", null, null);
        profileClient.setProfile(profile(null, "네이버회원", null, null));
        when(externalIdentityRepository.findByProviderAndProviderSubject(AuthProvider.NAVER, "naver-sub")).thenReturn(new ExternalIdentity(externalIdentityId, member, AuthProvider.NAVER, "naver-sub"));
        when(tokenProvider.generateToken(existingMemberId, "ROLE_USER")).thenReturn(new AuthResult.TokenPair("access-token", "refresh-token"));
        when(tokenProvider.getRefreshTokenValiditySeconds()).thenReturn(120L);
        var result = service.login(new AuthCommand.NaverLogin("access-token"));
        assertEquals(existingMemberId, result.getMember().getId());
        assertEquals("existing@example.com", result.getMember().getEmail());
        assertEquals("네이버회원", result.getMember().getName());
        verify(memberRepository, Mockito.never()).findByEmail(Mockito.anyString());
    }
    private NaverProfile profile(String email, String name, String phone, LocalDate birthDate) {
        return new NaverProfile("naver-sub", email, name, phone, birthDate);
    }
    private static class StubNaverProfileClient implements NaverProfileClient {
        NaverProfile profile;
        String lastAccessToken;
        void setProfile(NaverProfile value) { profile = value; }
        @Override public NaverProfile fetch(String accessToken) {
            lastAccessToken = accessToken;
            return profile;
        }
    }
}
