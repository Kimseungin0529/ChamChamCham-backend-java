package com.chamchamcham.api.auth.dto;

import com.chamchamcham.application.auth.common.AuthResult;
import com.chamchamcham.application.crop.CropResult;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.List;
import java.util.Objects;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public final class AuthResponses {
    private AuthResponses() {}

    public record TokenResponse(
        @NonNull String accessToken,
        @NonNull String refreshToken
    ) {
        public TokenResponse {
            Objects.requireNonNull(accessToken, "accessToken");
            Objects.requireNonNull(refreshToken, "refreshToken");
        }
        @NonNull public String getAccessToken() { return accessToken; }
        @NonNull public String getRefreshToken() { return refreshToken; }
        public static TokenResponse from(AuthResult.TokenPair result) {
            return new TokenResponse(
                result.getAccessToken(),
                result.getRefreshToken()
            );
        }
    }

    public record LoginResponse(
        @NonNull String accessToken,
        @NonNull String refreshToken,
        @NonNull MemberProfileResponse member,
        @NonNull OnboardingResponse onboarding
    ) {
        public LoginResponse {
            Objects.requireNonNull(accessToken, "accessToken");
            Objects.requireNonNull(refreshToken, "refreshToken");
            Objects.requireNonNull(member, "member");
            Objects.requireNonNull(onboarding, "onboarding");
        }
        @NonNull public String getAccessToken() { return accessToken; }
        @NonNull public String getRefreshToken() { return refreshToken; }
        @NonNull public MemberProfileResponse getMember() { return member; }
        @NonNull public OnboardingResponse getOnboarding() { return onboarding; }
        public static LoginResponse from(AuthResult.Login result) {
            return new LoginResponse(
                result.getAccessToken(),
                result.getRefreshToken(),
                MemberProfileResponse.from(result.getMember()),
                OnboardingResponse.from(result.getOnboarding())
            );
        }
    }

    public record OnboardingCompleteResponse(
        @NonNull MemberProfileResponse member,
        @NonNull FarmResponse farm,
        @NonNull List<CropResponse> crops,
        @NonNull OnboardingResponse onboarding
    ) {
        public OnboardingCompleteResponse {
            Objects.requireNonNull(member, "member");
            Objects.requireNonNull(farm, "farm");
            Objects.requireNonNull(crops, "crops");
            Objects.requireNonNull(onboarding, "onboarding");
        }
        @NonNull public MemberProfileResponse getMember() { return member; }
        @NonNull public FarmResponse getFarm() { return farm; }
        @NonNull public List<CropResponse> getCrops() { return crops; }
        @NonNull public OnboardingResponse getOnboarding() { return onboarding; }
        public static OnboardingCompleteResponse from(AuthResult.OnboardingComplete result) {
            return new OnboardingCompleteResponse(
                MemberProfileResponse.from(result.getMember()),
                FarmResponse.from(result.getFarm()),
                result.getCrops().stream().map(CropResponse::from).collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new)),
                OnboardingResponse.from(result.getOnboarding())
            );
        }
    }

    public record MemberProfileResponse(
        @NonNull UUID id,
        @Nullable String email,
        @Nullable String name,
        @Nullable String phone,
        @Nullable LocalDate birthDate,
        @Nullable String nickname,
        @Nullable Integer experienceLevel,
        @Nullable String managementType,
        @Nullable String profileImageUrl
    ) {
        public MemberProfileResponse {
            Objects.requireNonNull(id, "id");
        }
        @NonNull public UUID getId() { return id; }
        @Nullable public String getEmail() { return email; }
        @Nullable public String getName() { return name; }
        @Nullable public String getPhone() { return phone; }
        @Nullable public LocalDate getBirthDate() { return birthDate; }
        @Nullable public String getNickname() { return nickname; }
        @Nullable public Integer getExperienceLevel() { return experienceLevel; }
        @Nullable public String getManagementType() { return managementType; }
        @Nullable public String getProfileImageUrl() { return profileImageUrl; }
        public static MemberProfileResponse from(AuthResult.MemberProfile result) {
            return new MemberProfileResponse(
                result.getId(),
                result.getEmail(),
                result.getName(),
                result.getPhone(),
                result.getBirthDate(),
                result.getNickname(),
                result.getExperienceLevel(),
                result.getManagementType(),
                result.getProfileImageUrl()
            );
        }
    }

    public record FarmResponse(
        @NonNull UUID id,
        @NonNull String name,
        @NonNull String roadAddress,
        @Nullable String jibunAddress,
        @Nullable Double latitude,
        @Nullable Double longitude,
        @Nullable String pnu,
        @Nullable String landCategory,
        @Nullable BigDecimal areaSqm,
        boolean areaIsManualEntry,
        @NonNull List<FarmBoundaryCoordinateResponse> boundaryCoordinates,
        @NonNull FarmDataSourceResponse dataSource
    ) {
        public FarmResponse {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(roadAddress, "roadAddress");
            Objects.requireNonNull(boundaryCoordinates, "boundaryCoordinates");
            Objects.requireNonNull(dataSource, "dataSource");
        }
        @NonNull public UUID getId() { return id; }
        @NonNull public String getName() { return name; }
        @NonNull public String getRoadAddress() { return roadAddress; }
        @Nullable public String getJibunAddress() { return jibunAddress; }
        @Nullable public Double getLatitude() { return latitude; }
        @Nullable public Double getLongitude() { return longitude; }
        @Nullable public String getPnu() { return pnu; }
        @Nullable public String getLandCategory() { return landCategory; }
        @Nullable public BigDecimal getAreaSqm() { return areaSqm; }
        public boolean getAreaIsManualEntry() { return areaIsManualEntry; }
        @NonNull public List<FarmBoundaryCoordinateResponse> getBoundaryCoordinates() { return boundaryCoordinates; }
        @NonNull public FarmDataSourceResponse getDataSource() { return dataSource; }
        public static FarmResponse from(AuthResult.FarmSummary result) {
            return new FarmResponse(
                result.getId(),
                result.getName(),
                result.getRoadAddress(),
                result.getJibunAddress(),
                result.getLatitude(),
                result.getLongitude(),
                result.getPnu(),
                result.getLandCategory(),
                result.getAreaSqm(),
                result.getAreaIsManualEntry(),
                result.getBoundaryCoordinates().stream().map(FarmBoundaryCoordinateResponse::from).collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new)),
                FarmDataSourceResponse.from(result.getDataSource())
            );
        }
    }

    public record FarmBoundaryCoordinateResponse(
        double latitude,
        double longitude
    ) {
        public double getLatitude() { return latitude; }
        public double getLongitude() { return longitude; }
        public static FarmBoundaryCoordinateResponse from(AuthResult.FarmBoundaryCoordinateSummary result) {
            return new FarmBoundaryCoordinateResponse(
                result.getLatitude(),
                result.getLongitude()
            );
        }
    }

    public record FarmDataSourceResponse(
        @Nullable String address,
        @Nullable String coordinate,
        @Nullable String parcel,
        @Nullable String landCharacteristic
    ) {
        @Nullable public String getAddress() { return address; }
        @Nullable public String getCoordinate() { return coordinate; }
        @Nullable public String getParcel() { return parcel; }
        @Nullable public String getLandCharacteristic() { return landCharacteristic; }
        public static FarmDataSourceResponse from(AuthResult.FarmDataSourceSummary result) {
            return new FarmDataSourceResponse(
                result.getAddress(),
                result.getCoordinate(),
                result.getParcel(),
                result.getLandCharacteristic()
            );
        }
    }

    public record CropResponse(
        @NonNull UUID id,
        int externalNo,
        @NonNull String name,
        @NonNull String usePartCategory,
        @NonNull String usePartCategoryLabel
    ) {
        public CropResponse {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(usePartCategory, "usePartCategory");
            Objects.requireNonNull(usePartCategoryLabel, "usePartCategoryLabel");
        }
        @NonNull public UUID getId() { return id; }
        public int getExternalNo() { return externalNo; }
        @NonNull public String getName() { return name; }
        @NonNull public String getUsePartCategory() { return usePartCategory; }
        @NonNull public String getUsePartCategoryLabel() { return usePartCategoryLabel; }
        public static CropResponse from(CropResult.CropSummary result) {
            return new CropResponse(
                result.getId(),
                result.getExternalNo(),
                result.getName(),
                result.getUsePartCategory(),
                result.getUsePartCategoryLabel()
            );
        }
    }

    public record OnboardingResponse(
        @NonNull AuthResult.OnboardingStatus status,
        @NonNull List<AuthResult.OnboardingField> missingFields
    ) {
        public OnboardingResponse {
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(missingFields, "missingFields");
        }
        @NonNull public AuthResult.OnboardingStatus getStatus() { return status; }
        @NonNull public List<AuthResult.OnboardingField> getMissingFields() { return missingFields; }
        public static OnboardingResponse from(AuthResult.Onboarding result) {
            return new OnboardingResponse(
                result.getStatus(),
                result.getMissingFields()
            );
        }
    }
}
