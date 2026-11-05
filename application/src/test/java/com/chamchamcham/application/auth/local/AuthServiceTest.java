package com.chamchamcham.application.auth.local;

import com.chamchamcham.application.auth.common.AuthCommand;
import com.chamchamcham.application.auth.common.AuthResult;
import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.chamchamcham.application.redis.EmailVerificationRepository;
import com.chamchamcham.application.redis.RefreshTokenRepository;
import com.chamchamcham.application.security.TokenProvider;
import static com.chamchamcham.application.testsupport.TestJwtFixtures.signedTestToken;
import static com.chamchamcham.application.testsupport.TestJwtFixtures.testTokenProvider;
import com.chamchamcham.domain.member.Member;
import com.chamchamcham.domain.member.MemberStatus;
import com.chamchamcham.domain.member.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.UUID;
import java.util.*;
import org.junit.jupiter.api.DisplayName;

@ExtendWith({MockitoExtension.class})
class AuthServiceTest {
    private final UUID memberId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    @Mock private TokenProvider tokenProvider;
    @Mock private EmailSender emailSender;
    @Mock private VerificationCodeGenerator verificationCodeGenerator;
    @Mock private EmailVerificationRepository emailVerificationRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private PasswordEncoder passwordEncoder;
    private AuthService authService;
    private final long codeTtlMillis = 180_000L;
    private final long verifiedTtlMillis = 300_000L;

    @BeforeEach
    void setUp() throws Exception {
        authService = new AuthService(tokenProvider, emailSender, verificationCodeGenerator, emailVerificationRepository, refreshTokenRepository, memberRepository, passwordEncoder, codeTtlMillis, verifiedTtlMillis);
    }

    @Test
    @DisplayName("sendVerificationCode stores generated code and sends email")
    void sendVerificationCode_stores_generated_code_and_sends_email() throws Exception {
        when(memberRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(verificationCodeGenerator.generate()).thenReturn("123456");
        authService.sendVerificationCode(new AuthCommand.SendVerificationCode("user@example.com"));
        verify(emailSender).sendVerificationCode("user@example.com", "123456");
        verify(emailVerificationRepository).saveCode("user@example.com", "123456", java.time.Duration.ofMillis(codeTtlMillis));
    }

    @Test
    @DisplayName("signUp rejects unverified email")
    void signUp_rejects_unverified_email() throws Exception {
        when(emailVerificationRepository.isVerified("user@example.com")).thenReturn(false);
        var exception = assertThrows(BusinessException.class, () -> authService.signUp(new AuthCommand.SignUp("user@example.com", "password123")));
        assertEquals(ErrorCode.EMAIL_NOT_VERIFIED, exception.getErrorCode());
    }

    @Test
    @DisplayName("login stores refresh token and returns tokens")
    void login_stores_refresh_token_and_returns_tokens() throws Exception {
        var member = new Member(memberId, "user@example.com", null, MemberStatus.ACTIVE, null, null, null, null, null, "hashed", "ROLE_USER", null, null);
        var tokenPair = new AuthResult.TokenPair("access-token", "refresh-token");
        when(memberRepository.findByEmail("user@example.com")).thenReturn(member);
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(tokenProvider.generateToken(memberId, "ROLE_USER")).thenReturn(tokenPair);
        when(tokenProvider.getRefreshTokenValiditySeconds()).thenReturn(120L);
        var result = authService.login(new AuthCommand.Login("user@example.com", "password123"));
        assertEquals(tokenPair, result);
        verify(refreshTokenRepository).save(memberId, "refresh-token", 120L);
    }

    @Test
    @DisplayName("login rejects social-only member without password hash")
    void login_rejects_social_only_member_without_password_hash() throws Exception {
        var member = new Member(memberId, "user@example.com", null, MemberStatus.ACTIVE, null, null, null, null, null, null, "ROLE_USER", null, null);
        when(memberRepository.findByEmail("user@example.com")).thenReturn(member);
        var exception = assertThrows(BusinessException.class, () -> authService.login(new AuthCommand.Login("user@example.com", "password123")));
        assertEquals(ErrorCode.SOCIAL_ONLY_MEMBER_LOCAL_LOGIN_FORBIDDEN, exception.getErrorCode());
    }

    @Test
    @DisplayName("reissue rejects refresh token mismatch")
    void reissue_rejects_refresh_token_mismatch() throws Exception {
        when(tokenProvider.validateToken("refresh-token")).thenReturn(true);
        when(tokenProvider.isRefreshToken("refresh-token")).thenReturn(true);
        when(tokenProvider.getMemberId("refresh-token")).thenReturn(memberId);
        when(refreshTokenRepository.get(memberId)).thenReturn("different-refresh-token");
        var exception = assertThrows(BusinessException.class, () -> authService.reissue(new AuthCommand.Reissue("refresh-token")));
        assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
    }

    @Test
    @DisplayName("reissue rejects signed refresh token with non uuid subject")
    void reissue_rejects_signed_refresh_token_with_non_uuid_subject() throws Exception {
        var service = new AuthService(testTokenProvider(), emailSender, verificationCodeGenerator, emailVerificationRepository, refreshTokenRepository, memberRepository, passwordEncoder, codeTtlMillis, verifiedTtlMillis);
        var legacyRefreshToken = signedTestToken("42", "refresh", null);
        var exception = assertThrows(BusinessException.class, () -> service.reissue(new AuthCommand.Reissue(legacyRefreshToken)));
        assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
    }

    @Test
    @DisplayName("logout deletes stored refresh token")
    void logout_deletes_stored_refresh_token() throws Exception {
        when(refreshTokenRepository.get(memberId)).thenReturn("refresh-token");
        authService.logout(memberId.toString());
        verify(refreshTokenRepository).delete(memberId);
    }
}
