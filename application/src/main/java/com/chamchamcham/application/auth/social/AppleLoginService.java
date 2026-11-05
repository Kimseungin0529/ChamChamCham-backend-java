package com.chamchamcham.application.auth.social;

import com.chamchamcham.application.auth.common.AuthCommand;
import com.chamchamcham.application.auth.common.AuthResult;
import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.chamchamcham.application.redis.AppleNonceReplayRepository;
import com.chamchamcham.application.security.AppleOidcClaims;
import com.chamchamcham.application.security.AppleOidcTokenVerifier;
import com.chamchamcham.domain.member.AuthProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

@Service
@Transactional
public class AppleLoginService {
    private final AppleOidcTokenVerifier appleOidcTokenVerifier;
    private final AppleNonceReplayRepository appleNonceReplayRepository;
    private final SocialLoginSupport socialLoginSupport;

    public AppleLoginService(AppleOidcTokenVerifier appleOidcTokenVerifier, AppleNonceReplayRepository appleNonceReplayRepository, SocialLoginSupport socialLoginSupport, @Value("${auth.apple.oidc.allowed-clock-skew-seconds:60}") long allowedClockSkewSeconds) {
        this.appleOidcTokenVerifier = Objects.requireNonNull(appleOidcTokenVerifier, "appleOidcTokenVerifier");
        this.appleNonceReplayRepository = Objects.requireNonNull(appleNonceReplayRepository, "appleNonceReplayRepository");
        this.socialLoginSupport = Objects.requireNonNull(socialLoginSupport, "socialLoginSupport");
        this.allowedClockSkew = Duration.ofSeconds(allowedClockSkewSeconds);
    }
    private final Duration allowedClockSkew;
    public AuthResult.Login login(AuthCommand.AppleLogin command) {
        Objects.requireNonNull(command, "command");
        var claims = appleOidcTokenVerifier.verify(command.identityToken(), command.nonce());
        if (command.userIdentifier() != null && !blank(command.userIdentifier()) && !command.userIdentifier().equals(claims.subject())) throw new BusinessException(ErrorCode.INVALID_APPLE_TOKEN);
        return socialLoginSupport.login(AuthProvider.APPLE, claims.subject(), claims.emailVerified() ? claims.email() : null,
            command.name() != null && !blank(command.name()) ? command.name() : null, null, null, () -> reserveNonce(claims));
    }
    private void reserveNonce(AppleOidcClaims claims) {
        var ttl = Duration.between(Instant.now(), claims.expiresAt().plus(allowedClockSkew));
        if (ttl.compareTo(Duration.ZERO) <= 0) throw new BusinessException(ErrorCode.INVALID_APPLE_TOKEN);
        if (!appleNonceReplayRepository.reserve(claims.nonce(), ttl)) throw new BusinessException(ErrorCode.APPLE_NONCE_REPLAY);
    }
    private static boolean blank(String value) {
        return value.chars().allMatch(c -> Character.isWhitespace(c) || Character.isSpaceChar(c));
    }

}
