package com.chamchamcham.api.auth.dto;

import com.chamchamcham.api.farm.dto.FarmRequests;
import com.chamchamcham.domain.member.ManagementType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.UniqueElements;
import java.time.LocalDate;
import java.time.Period;
import java.util.UUID;
import java.util.List;
import java.util.Objects;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public final class AuthRequests {
    private AuthRequests() {}

    public record SendVerificationCodeRequest(
        @NotBlank(message = "이메일을 입력해주세요") @Email(message = "이메일 형식이 올바르지 않습니다") @NonNull String email
    ) {
        public SendVerificationCodeRequest {
            Objects.requireNonNull(email, "email");
        }
        @NonNull public String getEmail() { return email; }
    }

    public record VerifyEmailCodeRequest(
        @NotBlank(message = "이메일을 입력해주세요") @Email(message = "이메일 형식이 올바르지 않습니다") @NonNull String email,
        @NotBlank(message = "인증번호를 입력해주세요") @Size(min = 6, max = 6, message = "인증번호는 6자리여야 합니다") @NonNull String code
    ) {
        public VerifyEmailCodeRequest {
            Objects.requireNonNull(email, "email");
            Objects.requireNonNull(code, "code");
        }
        @NonNull public String getEmail() { return email; }
        @NonNull public String getCode() { return code; }
    }

    public record SignUpRequest(
        @NotBlank(message = "이메일을 입력해주세요") @Email(message = "이메일 형식이 올바르지 않습니다") @NonNull String email,
        @NotBlank(message = "비밀번호를 입력해주세요") @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다") @NonNull String password
    ) {
        public SignUpRequest {
            Objects.requireNonNull(email, "email");
            Objects.requireNonNull(password, "password");
        }
        @NonNull public String getEmail() { return email; }
        @NonNull public String getPassword() { return password; }
    }

    public record LoginRequest(
        @NotBlank(message = "이메일을 입력해주세요") @Email(message = "이메일 형식이 올바르지 않습니다") @NonNull String email,
        @NotBlank(message = "비밀번호를 입력해주세요") @NonNull String password
    ) {
        public LoginRequest {
            Objects.requireNonNull(email, "email");
            Objects.requireNonNull(password, "password");
        }
        @NonNull public String getEmail() { return email; }
        @NonNull public String getPassword() { return password; }
    }

    public record KakaoLoginRequest(
        @NotBlank(message = "카카오 ID 토큰을 입력해주세요") @NonNull String idToken,
        @NotBlank(message = "nonce를 입력해주세요") @NonNull String nonce,
        @Nullable String kakaoAccessToken
    ) {
        public KakaoLoginRequest {
            Objects.requireNonNull(idToken, "idToken");
            Objects.requireNonNull(nonce, "nonce");
        }
        @NonNull public String getIdToken() { return idToken; }
        @NonNull public String getNonce() { return nonce; }
        @Nullable public String getKakaoAccessToken() { return kakaoAccessToken; }
    }

    public record AppleLoginRequest(
        @NotBlank(message = "애플 identity token을 입력해주세요") @NonNull String identityToken,
        @NotBlank(message = "nonce를 입력해주세요") @NonNull String nonce,
        @Nullable String authorizationCode,
        @Nullable String userIdentifier,
        @Nullable String name
    ) {
        public AppleLoginRequest {
            Objects.requireNonNull(identityToken, "identityToken");
            Objects.requireNonNull(nonce, "nonce");
        }
        @NonNull public String getIdentityToken() { return identityToken; }
        @NonNull public String getNonce() { return nonce; }
        @Nullable public String getAuthorizationCode() { return authorizationCode; }
        @Nullable public String getUserIdentifier() { return userIdentifier; }
        @Nullable public String getName() { return name; }
    }

    public record NaverLoginRequest(
        @NotBlank(message = "네이버 access token을 입력해주세요") @NonNull String accessToken
    ) {
        public NaverLoginRequest {
            Objects.requireNonNull(accessToken, "accessToken");
        }
        @NonNull public String getAccessToken() { return accessToken; }
    }

    public record CompleteOnboardingRequest(
        @Nullable String name,
        @Nullable String phone,
        @Nullable LocalDate birthDate,
        @Nullable String nickname,
        @NotNull(message = "경험 수준을 입력해주세요") @Min(value = 0, message = "경험 수준은 0 이상이어야 합니다") @Max(value = 100, message = "경험 수준은 100 이하여야 합니다") @Nullable Integer experienceLevel,
        @NotNull(message = "경영 형태를 입력해주세요") @Nullable ManagementType managementType,
        @Valid @NotNull(message = "농장 정보를 입력해주세요") @Nullable FarmRequests.FarmDraftRequest farm,
        @NotEmpty(message = "작물을 하나 이상 선택해주세요") @Size(max = 5, message = "작물은 최대 5개까지 선택할 수 있습니다") @UniqueElements(message = "작물은 중복해서 선택할 수 없습니다") @NonNull List<UUID> cropIds,
        @Nullable UUID profileMediaId
    ) {
        public CompleteOnboardingRequest {
            Objects.requireNonNull(cropIds, "cropIds");
        }
        @Nullable public String getName() { return name; }
        @Nullable public String getPhone() { return phone; }
        @Nullable public LocalDate getBirthDate() { return birthDate; }
        @Nullable public String getNickname() { return nickname; }
        @Nullable public Integer getExperienceLevel() { return experienceLevel; }
        @Nullable public ManagementType getManagementType() { return managementType; }
        @Nullable public FarmRequests.FarmDraftRequest getFarm() { return farm; }
        @NonNull public List<UUID> getCropIds() { return cropIds; }
        @Nullable public UUID getProfileMediaId() { return profileMediaId; }
        @AssertTrue(message = "귀농연차는 만 나이를 초과할 수 없습니다")
        public boolean isExperienceLevelWithinAge() {
            if (experienceLevel == null || experienceLevel < 0 || experienceLevel > 100) return true;
            return birthDate == null || experienceLevel <= Period.between(birthDate, LocalDate.now()).getYears();
        }
    }

    public record ReissueRequest(
        @Nullable String refreshToken
    ) {
        @Nullable public String getRefreshToken() { return refreshToken; }
        public ReissueRequest() { this(null); }
    }
}
