package com.chamchamcham.application.auth.social;

import com.chamchamcham.application.auth.common.AuthResult;
import com.chamchamcham.application.auth.common.OnboardingStatusResolver;
import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.chamchamcham.application.redis.RefreshTokenRepository;
import com.chamchamcham.application.security.TokenProvider;
import com.chamchamcham.domain.member.AuthProvider;
import com.chamchamcham.domain.member.ExternalIdentity;
import com.chamchamcham.domain.member.ExternalIdentityRepository;
import com.chamchamcham.domain.member.Member;
import com.chamchamcham.domain.member.MemberRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.Objects;

@Component
@Transactional
public class SocialLoginSupport {
    private final ExternalIdentityRepository externalIdentityRepository;
    private final MemberRepository memberRepository;
    private final TokenProvider tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OnboardingStatusResolver onboardingStatusResolver;

    public SocialLoginSupport(ExternalIdentityRepository externalIdentityRepository, MemberRepository memberRepository, TokenProvider tokenProvider, RefreshTokenRepository refreshTokenRepository, OnboardingStatusResolver onboardingStatusResolver) {
        this.externalIdentityRepository = Objects.requireNonNull(externalIdentityRepository, "externalIdentityRepository");
        this.memberRepository = Objects.requireNonNull(memberRepository, "memberRepository");
        this.tokenProvider = Objects.requireNonNull(tokenProvider, "tokenProvider");
        this.refreshTokenRepository = Objects.requireNonNull(refreshTokenRepository, "refreshTokenRepository");
        this.onboardingStatusResolver = Objects.requireNonNull(onboardingStatusResolver, "onboardingStatusResolver");
    }
    public AuthResult.Login login(AuthProvider provider, String providerSubject, @org.springframework.lang.Nullable String email, @org.springframework.lang.Nullable String name, @org.springframework.lang.Nullable String phone, @org.springframework.lang.Nullable LocalDate birthDate, Runnable beforeSideEffects) {
        Objects.requireNonNull(provider, "provider");
        Objects.requireNonNull(providerSubject, "providerSubject");
        Objects.requireNonNull(beforeSideEffects, "beforeSideEffects");
        var existing = externalIdentityRepository.findByProviderAndProviderSubject(provider, providerSubject);
        Member member;
        if (existing != null) {
            beforeSideEffects.run();
            member = existing.getMember();
        } else {
            beforeSideEffects.run();
            var created = new Member(availableEmailOrNull(email), null);
            created.setPhone(phone);
            created.setName(name);
            created.setBirthDate(birthDate);
            member = memberRepository.save(created);
            externalIdentityRepository.save(new ExternalIdentity(member, provider, providerSubject));
        }
        member.prefillProfile(name, phone, birthDate);
        var memberId = member.getId();
        if (memberId == null) throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        var pair = tokenProvider.generateToken(memberId, member.getRole());
        refreshTokenRepository.save(memberId, pair.refreshToken(), tokenProvider.getRefreshTokenValiditySeconds());
        return new AuthResult.Login(pair.accessToken(), pair.refreshToken(), AuthResult.MemberProfile.from(member), onboardingStatusResolver.resolve(member));
    }
    private String availableEmailOrNull(String email) {
        if (email == null || blank(email)) return null;
        return memberRepository.existsByEmail(email) ? null : email;
    }
    private static boolean blank(String value) {
        return value.chars().allMatch(c -> Character.isWhitespace(c) || Character.isSpaceChar(c));
    }

}
