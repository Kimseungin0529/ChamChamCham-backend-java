package com.chamchamcham.application.auth.social;

import com.chamchamcham.application.auth.common.AuthCommand;
import com.chamchamcham.application.auth.common.AuthResult;
import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.chamchamcham.application.redis.KakaoNonceReplayRepository;
import com.chamchamcham.application.security.KakaoOidcClaims;
import com.chamchamcham.application.security.KakaoOidcTokenVerifier;
import com.chamchamcham.domain.member.AuthProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

@Service
@Transactional
public class KakaoLoginService {
    private final KakaoOidcTokenVerifier kakaoOidcTokenVerifier;
    private final KakaoUserInfoClient kakaoUserInfoClient;
    private final KakaoNonceReplayRepository kakaoNonceReplayRepository;
    private final SocialLoginSupport socialLoginSupport;

    public KakaoLoginService(KakaoOidcTokenVerifier kakaoOidcTokenVerifier, KakaoUserInfoClient kakaoUserInfoClient, KakaoNonceReplayRepository kakaoNonceReplayRepository, SocialLoginSupport socialLoginSupport, @Value("${auth.kakao.oidc.nonce-replay-ttl-seconds:600}") long nonceReplayTtlSeconds, @Value("${auth.kakao.oidc.allowed-clock-skew-seconds:60}") long allowedClockSkewSeconds) {
        this.kakaoOidcTokenVerifier = Objects.requireNonNull(kakaoOidcTokenVerifier, "kakaoOidcTokenVerifier");
        this.kakaoUserInfoClient = Objects.requireNonNull(kakaoUserInfoClient, "kakaoUserInfoClient");
        this.kakaoNonceReplayRepository = Objects.requireNonNull(kakaoNonceReplayRepository, "kakaoNonceReplayRepository");
        this.socialLoginSupport = Objects.requireNonNull(socialLoginSupport, "socialLoginSupport");
        this.maxNonceReplayTtl = Duration.ofSeconds(nonceReplayTtlSeconds);
        this.allowedClockSkew = Duration.ofSeconds(allowedClockSkewSeconds);
    }
    private final Duration maxNonceReplayTtl;
    private final Duration allowedClockSkew;
    public AuthResult.Login login(AuthCommand.KakaoLogin command) {
        Objects.requireNonNull(command, "command");
        var claims = kakaoOidcTokenVerifier.verify(command.idToken(), command.nonce());
        var userInfo = command.kakaoAccessToken() != null && !blank(command.kakaoAccessToken()) ? kakaoUserInfoClient.fetch(command.kakaoAccessToken()) : null;
        if (userInfo != null && !userInfo.subject().equals(claims.subject())) throw new BusinessException(ErrorCode.INVALID_KAKAO_TOKEN);
        var email = userInfo != null && userInfo.email() != null ? userInfo.email() : claims.emailVerified() ? claims.email() : null;
        return socialLoginSupport.login(AuthProvider.KAKAO, claims.subject(), email,
            userInfo == null ? null : userInfo.name(), userInfo == null ? null : userInfo.phone(), userInfo == null ? null : userInfo.birthDate(), () -> reserveNonce(claims));
    }
    private void reserveNonce(KakaoOidcClaims claims) {
        var untilExpiry = Duration.between(Instant.now(), claims.expiresAt().plus(allowedClockSkew));
        if (untilExpiry.compareTo(Duration.ZERO) <= 0) throw new BusinessException(ErrorCode.INVALID_KAKAO_TOKEN);
        var ttl = untilExpiry.compareTo(maxNonceReplayTtl) < 0 ? untilExpiry : maxNonceReplayTtl;
        if (!kakaoNonceReplayRepository.reserve(claims.nonce(), ttl)) throw new BusinessException(ErrorCode.KAKAO_NONCE_REPLAY);
    }
    private static boolean blank(String value) {
        return value.chars().allMatch(c -> Character.isWhitespace(c) || Character.isSpaceChar(c));
    }

}
