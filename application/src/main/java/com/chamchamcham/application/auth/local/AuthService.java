package com.chamchamcham.application.auth.local;

import com.chamchamcham.application.auth.common.AuthCommand;
import com.chamchamcham.application.auth.common.AuthResult;
import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.chamchamcham.application.redis.EmailVerificationRepository;
import com.chamchamcham.application.redis.RefreshTokenRepository;
import com.chamchamcham.application.security.TokenProvider;
import com.chamchamcham.domain.member.Member;
import com.chamchamcham.domain.member.MemberRepository;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.util.UUID;
import java.util.Objects;

@Service
@Transactional
public class AuthService {
    private final TokenProvider tokenProvider;
    private final EmailSender emailSender;
    private final VerificationCodeGenerator verificationCodeGenerator;
    private final EmailVerificationRepository emailVerificationRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final long codeTtlMillis;
    private final long verifiedTtlMillis;

    public AuthService(TokenProvider tokenProvider, EmailSender emailSender, VerificationCodeGenerator verificationCodeGenerator, EmailVerificationRepository emailVerificationRepository, RefreshTokenRepository refreshTokenRepository, MemberRepository memberRepository, PasswordEncoder passwordEncoder, @Value("${spring.mail.auth-code-expiration-millis}") long codeTtlMillis, @Value("${spring.mail.verified-state-expiration-millis:${spring.mail.auth-code-expiration-millis}}") long verifiedTtlMillis) {
        this.tokenProvider = Objects.requireNonNull(tokenProvider, "tokenProvider");
        this.emailSender = Objects.requireNonNull(emailSender, "emailSender");
        this.verificationCodeGenerator = Objects.requireNonNull(verificationCodeGenerator, "verificationCodeGenerator");
        this.emailVerificationRepository = Objects.requireNonNull(emailVerificationRepository, "emailVerificationRepository");
        this.refreshTokenRepository = Objects.requireNonNull(refreshTokenRepository, "refreshTokenRepository");
        this.memberRepository = Objects.requireNonNull(memberRepository, "memberRepository");
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder, "passwordEncoder");
        this.codeTtlMillis = codeTtlMillis;
        this.verifiedTtlMillis = verifiedTtlMillis;
    }
    private final org.slf4j.Logger logger = LoggerFactory.getLogger(AuthService.class);

    public void sendVerificationCode(AuthCommand.SendVerificationCode command) {
        Objects.requireNonNull(command, "command");
        if (memberRepository.existsByEmail(command.email())) throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        var code = verificationCodeGenerator.generate();
        emailSender.sendVerificationCode(command.email(), code);
        emailVerificationRepository.saveCode(command.email(), code, Duration.ofMillis(codeTtlMillis));
        logger.info("Verification code sent. emailHash={}", command.email().hashCode());
    }
    public void verifyEmailCode(AuthCommand.VerifyEmailCode command) {
        Objects.requireNonNull(command, "command");
        if (memberRepository.existsByEmail(command.email())) throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        var storedCode = emailVerificationRepository.getCode(command.email());
        if (storedCode == null) throw new BusinessException(ErrorCode.AUTH_CODE_NOT_FOUND);
        if (!storedCode.equals(command.code())) throw new BusinessException(ErrorCode.AUTH_CODE_MISMATCH);
        emailVerificationRepository.markVerified(command.email(), Duration.ofMillis(verifiedTtlMillis));
        emailVerificationRepository.deleteCode(command.email());
        logger.info("Email verified. emailHash={}", command.email().hashCode());
    }
    public void signUp(AuthCommand.SignUp command) {
        Objects.requireNonNull(command, "command");
        if (!emailVerificationRepository.isVerified(command.email())) throw new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED);
        if (memberRepository.existsByEmail(command.email())) throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        memberRepository.save(new Member(command.email(), passwordEncoder.encode(command.password())));
        emailVerificationRepository.deleteCode(command.email());
        emailVerificationRepository.deleteVerified(command.email());
        logger.info("Member signed up. emailHash={}", command.email().hashCode());
    }
    public AuthResult.TokenPair login(AuthCommand.Login command) {
        Objects.requireNonNull(command, "command");
        var member = memberRepository.findByEmail(command.email());
        if (member == null) throw new BusinessException(ErrorCode.UNAUTHORIZED);
        var passwordHash = member.getPasswordHash();
        if (passwordHash == null) throw new BusinessException(ErrorCode.SOCIAL_ONLY_MEMBER_LOCAL_LOGIN_FORBIDDEN);
        if (!passwordEncoder.matches(command.password(), passwordHash)) throw new BusinessException(ErrorCode.UNAUTHORIZED);
        return issueAndStoreTokens(member);
    }
    public AuthResult.TokenPair reissue(AuthCommand.Reissue command) {
        Objects.requireNonNull(command, "command");
        if (!tokenProvider.validateToken(command.refreshToken()) || !tokenProvider.isRefreshToken(command.refreshToken())) throw new BusinessException(ErrorCode.UNAUTHORIZED);
        var memberId = tokenProvider.getMemberId(command.refreshToken());
        var storedRefreshToken = refreshTokenRepository.get(memberId);
        if (storedRefreshToken == null || !storedRefreshToken.equals(command.refreshToken())) throw new BusinessException(ErrorCode.UNAUTHORIZED);
        var member = memberRepository.findById(memberId).orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        return issueAndStoreTokens(member);
    }
    public void logout(String memberId) {
        Objects.requireNonNull(memberId, "memberId");
        var parsedMemberId = parseMemberId(memberId);
        if (refreshTokenRepository.get(parsedMemberId) == null) throw new BusinessException(ErrorCode.ALREADY_LOGGED_OUT);
        refreshTokenRepository.delete(parsedMemberId);
        logger.info("Member logged out. memberHash={}", memberId.hashCode());
    }
    private AuthResult.TokenPair issueAndStoreTokens(Member member) {
        var memberId = member.getId();
        if (memberId == null) throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        var pair = tokenProvider.generateToken(memberId, member.getRole());
        refreshTokenRepository.save(memberId, pair.refreshToken(), tokenProvider.getRefreshTokenValiditySeconds());
        return pair;
    }
    private UUID parseMemberId(String memberId) {
        try { return UUID.fromString(memberId); }
        catch (IllegalArgumentException e) { throw new BusinessException(ErrorCode.UNAUTHORIZED); }
    }

}
