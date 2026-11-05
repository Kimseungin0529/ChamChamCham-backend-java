package com.chamchamcham.application.auth.common;

import com.chamchamcham.application.farm.FarmCommand;
import com.chamchamcham.domain.member.ManagementType;
import java.time.LocalDate;
import java.util.UUID;
import java.util.List;
import java.util.Objects;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public final class AuthCommand {
    private AuthCommand() {}

    public record SendVerificationCode(
        @NonNull String email
    ) {
        public SendVerificationCode {
            Objects.requireNonNull(email, "email");
        }

        @NonNull
        public String getEmail() { return email; }
    }
    public record VerifyEmailCode(
        @NonNull String email,
        @NonNull String code
    ) {
        public VerifyEmailCode {
            Objects.requireNonNull(email, "email");
            Objects.requireNonNull(code, "code");
        }

        @NonNull
        public String getEmail() { return email; }
        @NonNull
        public String getCode() { return code; }
    }
    public record SignUp(
        @NonNull String email,
        @NonNull String password
    ) {
        public SignUp {
            Objects.requireNonNull(email, "email");
            Objects.requireNonNull(password, "password");
        }

        @NonNull
        public String getEmail() { return email; }
        @NonNull
        public String getPassword() { return password; }
    }
    public record Login(
        @NonNull String email,
        @NonNull String password
    ) {
        public Login {
            Objects.requireNonNull(email, "email");
            Objects.requireNonNull(password, "password");
        }

        @NonNull
        public String getEmail() { return email; }
        @NonNull
        public String getPassword() { return password; }
    }
    public record KakaoLogin(
        @NonNull String idToken,
        @NonNull String nonce,
        @Nullable String kakaoAccessToken
    ) {
        public KakaoLogin {
            Objects.requireNonNull(idToken, "idToken");
            Objects.requireNonNull(nonce, "nonce");
        }

        @NonNull
        public String getIdToken() { return idToken; }
        @NonNull
        public String getNonce() { return nonce; }
        @Nullable
        public String getKakaoAccessToken() { return kakaoAccessToken; }
        public KakaoLogin(String idToken, String nonce) { this(idToken, nonce, null); }
    }
    public record AppleLogin(
        @NonNull String identityToken,
        @NonNull String nonce,
        @Nullable String authorizationCode,
        @Nullable String userIdentifier,
        @Nullable String name
    ) {
        public AppleLogin {
            Objects.requireNonNull(identityToken, "identityToken");
            Objects.requireNonNull(nonce, "nonce");
        }

        @NonNull
        public String getIdentityToken() { return identityToken; }
        @NonNull
        public String getNonce() { return nonce; }
        @Nullable
        public String getAuthorizationCode() { return authorizationCode; }
        @Nullable
        public String getUserIdentifier() { return userIdentifier; }
        @Nullable
        public String getName() { return name; }
        public AppleLogin(String identityToken, String nonce, @Nullable String authorizationCode, @Nullable String userIdentifier) { this(identityToken, nonce, authorizationCode, userIdentifier, null); }
    }
    public record NaverLogin(
        @NonNull String accessToken
    ) {
        public NaverLogin {
            Objects.requireNonNull(accessToken, "accessToken");
        }

        @NonNull
        public String getAccessToken() { return accessToken; }
    }
    public record CompleteOnboarding(
        @NonNull UUID memberId,
        @Nullable String name,
        @Nullable String phone,
        @Nullable LocalDate birthDate,
        @Nullable String nickname,
        int experienceLevel,
        @NonNull ManagementType managementType,
        @NonNull FarmCommand.Draft farm,
        @NonNull List<UUID> cropIds,
        @Nullable UUID profileMediaId
    ) {
        public CompleteOnboarding {
            Objects.requireNonNull(memberId, "memberId");
            Objects.requireNonNull(managementType, "managementType");
            Objects.requireNonNull(farm, "farm");
            Objects.requireNonNull(cropIds, "cropIds");
        }

        @NonNull
        public UUID getMemberId() { return memberId; }
        @Nullable
        public String getName() { return name; }
        @Nullable
        public String getPhone() { return phone; }
        @Nullable
        public LocalDate getBirthDate() { return birthDate; }
        @Nullable
        public String getNickname() { return nickname; }
        public int getExperienceLevel() { return experienceLevel; }
        @NonNull
        public ManagementType getManagementType() { return managementType; }
        @NonNull
        public FarmCommand.Draft getFarm() { return farm; }
        @NonNull
        public List<UUID> getCropIds() { return cropIds; }
        @Nullable
        public UUID getProfileMediaId() { return profileMediaId; }
    }
    public record Reissue(
        @NonNull String refreshToken
    ) {
        public Reissue {
            Objects.requireNonNull(refreshToken, "refreshToken");
        }

        @NonNull
        public String getRefreshToken() { return refreshToken; }
    }
}
