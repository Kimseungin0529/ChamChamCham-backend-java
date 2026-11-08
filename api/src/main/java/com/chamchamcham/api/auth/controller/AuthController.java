package com.chamchamcham.api.auth.controller;

import com.chamchamcham.api.common.ApiResponse;
import com.chamchamcham.api.auth.dto.AuthRequests;
import com.chamchamcham.api.auth.dto.AuthResponses;
import com.chamchamcham.application.auth.common.AuthCommand;
import com.chamchamcham.application.auth.common.OnboardingService;
import com.chamchamcham.application.auth.local.AuthService;
import com.chamchamcham.application.auth.social.AppleLoginService;
import com.chamchamcham.application.auth.social.KakaoLoginService;
import com.chamchamcham.application.auth.social.NaverLoginService;
import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.chamchamcham.application.security.TokenProvider;
import jakarta.validation.Valid;
import kotlin.Unit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;
    private final KakaoLoginService kakaoLoginService;
    private final AppleLoginService appleLoginService;
    private final NaverLoginService naverLoginService;
    private final OnboardingService onboardingService;
    private final TokenProvider tokenProvider;
    private final boolean refreshCookieSecure;

    public AuthController(AuthService authService, KakaoLoginService kakaoLoginService,
                          AppleLoginService appleLoginService, NaverLoginService naverLoginService,
                          OnboardingService onboardingService, TokenProvider tokenProvider,
                          @Value("${app.auth.refresh-cookie-secure:true}") boolean refreshCookieSecure) {
        this.authService = Objects.requireNonNull(authService, "authService");
        this.kakaoLoginService = Objects.requireNonNull(kakaoLoginService, "kakaoLoginService");
        this.appleLoginService = Objects.requireNonNull(appleLoginService, "appleLoginService");
        this.naverLoginService = Objects.requireNonNull(naverLoginService, "naverLoginService");
        this.onboardingService = Objects.requireNonNull(onboardingService, "onboardingService");
        this.tokenProvider = Objects.requireNonNull(tokenProvider, "tokenProvider");
        this.refreshCookieSecure = refreshCookieSecure;
    }

    @PostMapping("/email/send-code")
    public ResponseEntity<ApiResponse<Unit>> sendVerificationCode(@Valid @RequestBody AuthRequests.SendVerificationCodeRequest request) {
        authService.sendVerificationCode(new AuthCommand.SendVerificationCode(request.email()));
        return ResponseEntity.ok(ApiResponse.empty(Unit.INSTANCE));
    }

    @PostMapping("/email/verify-code")
    public ResponseEntity<ApiResponse<Unit>> verifyEmailCode(@Valid @RequestBody AuthRequests.VerifyEmailCodeRequest request) {
        authService.verifyEmailCode(new AuthCommand.VerifyEmailCode(request.email(), request.code()));
        return ResponseEntity.ok(ApiResponse.empty(Unit.INSTANCE));
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Unit>> signUp(@Valid @RequestBody AuthRequests.SignUpRequest request) {
        authService.signUp(new AuthCommand.SignUp(request.email(), request.password()));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.empty(Unit.INSTANCE));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponses.TokenResponse>> login(@Valid @RequestBody AuthRequests.LoginRequest request) {
        var result = authService.login(new AuthCommand.Login(request.email(), request.password()));
        return ResponseEntity.ok(ApiResponse.ok(AuthResponses.TokenResponse.from(result)));
    }

    @PostMapping("/kakao/login")
    public ResponseEntity<ApiResponse<AuthResponses.LoginResponse>> kakaoLogin(@Valid @RequestBody AuthRequests.KakaoLoginRequest request) {
        var result = kakaoLoginService.login(new AuthCommand.KakaoLogin(request.idToken(), request.nonce(), request.kakaoAccessToken()));
        return ResponseEntity.ok(ApiResponse.ok(AuthResponses.LoginResponse.from(result)));
    }

    @PostMapping("/apple/login")
    public ResponseEntity<ApiResponse<AuthResponses.LoginResponse>> appleLogin(@Valid @RequestBody AuthRequests.AppleLoginRequest request) {
        var result = appleLoginService.login(new AuthCommand.AppleLogin(request.identityToken(), request.nonce(),
            request.authorizationCode(), request.userIdentifier(), request.name()));
        return ResponseEntity.ok(ApiResponse.ok(AuthResponses.LoginResponse.from(result)));
    }

    @PostMapping("/naver/login")
    public ResponseEntity<ApiResponse<AuthResponses.LoginResponse>> naverLogin(@Valid @RequestBody AuthRequests.NaverLoginRequest request) {
        var result = naverLoginService.login(new AuthCommand.NaverLogin(request.accessToken()));
        return ResponseEntity.ok(ApiResponse.ok(AuthResponses.LoginResponse.from(result)));
    }

    @PostMapping("/onboarding/complete")
    public ResponseEntity<ApiResponse<AuthResponses.OnboardingCompleteResponse>> completeOnboarding(
        @AuthenticationPrincipal @Nullable String memberId,
        @Valid @RequestBody AuthRequests.CompleteOnboardingRequest request) {
        var result = onboardingService.complete(new AuthCommand.CompleteOnboarding(parseMemberId(memberId),
            request.name(), request.phone(), request.birthDate(), request.nickname(),
            requireNotNull(request.experienceLevel()), requireNotNull(request.managementType()),
            requireNotNull(request.farm()).toCommandDraft(), request.cropIds(), request.profileMediaId()));
        return ResponseEntity.ok(ApiResponse.ok(AuthResponses.OnboardingCompleteResponse.from(result)));
    }

    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<AuthResponses.TokenResponse>> reissue(
        @RequestBody(required = false) @Nullable AuthRequests.ReissueRequest request,
        @CookieValue(name = "refreshToken", required = false) @Nullable String refreshTokenCookie) {
        var refreshToken = request == null ? null : request.refreshToken();
        if (refreshToken == null) refreshToken = refreshTokenCookie;
        if (refreshToken == null) throw new BusinessException(ErrorCode.UNAUTHORIZED);
        var result = authService.reissue(new AuthCommand.Reissue(refreshToken));
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, refreshTokenCookie(result.getRefreshToken()).toString())
            .body(ApiResponse.ok(AuthResponses.TokenResponse.from(result)));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Unit>> logout(@AuthenticationPrincipal String memberId) {
        authService.logout(memberId);
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, clearRefreshTokenCookie().toString())
            .body(ApiResponse.empty(Unit.INSTANCE));
    }

    private static <T> T requireNotNull(T value) {
        if (value == null) throw new IllegalArgumentException("Required value was null.");
        return value;
    }

    private ResponseCookie refreshTokenCookie(String refreshToken) {
        return ResponseCookie.from("refreshToken", refreshToken).httpOnly(true).secure(refreshCookieSecure)
            .path("/").sameSite("Lax").maxAge(tokenProvider.getRefreshTokenValiditySeconds()).build();
    }

    private UUID parseMemberId(@Nullable String memberId) {
        if (memberId == null || memberId.chars().allMatch(c -> Character.isWhitespace(c) || Character.isSpaceChar(c))) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        try {
            return UUID.fromString(memberId);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }

    private ResponseCookie clearRefreshTokenCookie() {
        return ResponseCookie.from("refreshToken", "").httpOnly(true).secure(refreshCookieSecure)
            .path("/").sameSite("Lax").maxAge(0).build();
    }
}
