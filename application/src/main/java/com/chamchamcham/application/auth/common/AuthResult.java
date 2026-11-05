package com.chamchamcham.application.auth.common;

import com.chamchamcham.application.crop.CropResult;
import com.chamchamcham.domain.farm.Farm;
import com.chamchamcham.domain.farm.FarmBoundaryCoordinate;
import com.chamchamcham.domain.farm.FarmDataSource;
import com.chamchamcham.domain.member.Member;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.List;
import java.util.Objects;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public final class AuthResult {
    private AuthResult() {}

    public record TokenPair(
        @NonNull String accessToken,
        @NonNull String refreshToken
    ) {
        public TokenPair {
            Objects.requireNonNull(accessToken, "accessToken");
            Objects.requireNonNull(refreshToken, "refreshToken");
        }

        @NonNull
        public String getAccessToken() { return accessToken; }
        @NonNull
        public String getRefreshToken() { return refreshToken; }
    }
    public record Login(
        @NonNull String accessToken,
        @NonNull String refreshToken,
        @NonNull MemberProfile member,
        @NonNull Onboarding onboarding
    ) {
        public Login {
            Objects.requireNonNull(accessToken, "accessToken");
            Objects.requireNonNull(refreshToken, "refreshToken");
            Objects.requireNonNull(member, "member");
            Objects.requireNonNull(onboarding, "onboarding");
        }

        @NonNull
        public String getAccessToken() { return accessToken; }
        @NonNull
        public String getRefreshToken() { return refreshToken; }
        @NonNull
        public MemberProfile getMember() { return member; }
        @NonNull
        public Onboarding getOnboarding() { return onboarding; }
    }
    public record OnboardingComplete(
        @NonNull MemberProfile member,
        @NonNull FarmSummary farm,
        @NonNull List<CropResult.CropSummary> crops,
        @NonNull Onboarding onboarding
    ) {
        public OnboardingComplete {
            Objects.requireNonNull(member, "member");
            Objects.requireNonNull(farm, "farm");
            Objects.requireNonNull(crops, "crops");
            Objects.requireNonNull(onboarding, "onboarding");
        }

        @NonNull
        public MemberProfile getMember() { return member; }
        @NonNull
        public FarmSummary getFarm() { return farm; }
        @NonNull
        public List<CropResult.CropSummary> getCrops() { return crops; }
        @NonNull
        public Onboarding getOnboarding() { return onboarding; }
    }
    public record MemberProfile(
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
        public MemberProfile {
            Objects.requireNonNull(id, "id");
        }

        @NonNull
        public UUID getId() { return id; }
        @Nullable
        public String getEmail() { return email; }
        @Nullable
        public String getName() { return name; }
        @Nullable
        public String getPhone() { return phone; }
        @Nullable
        public LocalDate getBirthDate() { return birthDate; }
        @Nullable
        public String getNickname() { return nickname; }
        @Nullable
        public Integer getExperienceLevel() { return experienceLevel; }
        @Nullable
        public String getManagementType() { return managementType; }
        @Nullable
        public String getProfileImageUrl() { return profileImageUrl; }
        public static MemberProfile from(Member member) {
            Objects.requireNonNull(member, "member");
            if (member.getId() == null) throw new IllegalArgumentException("Persisted member id is required");
            return new MemberProfile(member.getId(), member.getEmail(), member.getName(), member.getPhone(), member.getBirthDate(), member.getNickname(), member.getExperienceLevel(), member.getManagementType() == null ? null : member.getManagementType().name(), member.getProfileMedia() == null ? null : member.getProfileMedia().getFileUrl());
        }
    }
    public record FarmSummary(
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
        @NonNull List<FarmBoundaryCoordinateSummary> boundaryCoordinates,
        @NonNull FarmDataSourceSummary dataSource
    ) {
        public FarmSummary {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(roadAddress, "roadAddress");
            Objects.requireNonNull(boundaryCoordinates, "boundaryCoordinates");
            Objects.requireNonNull(dataSource, "dataSource");
        }

        @NonNull
        public UUID getId() { return id; }
        @NonNull
        public String getName() { return name; }
        @NonNull
        public String getRoadAddress() { return roadAddress; }
        @Nullable
        public String getJibunAddress() { return jibunAddress; }
        @Nullable
        public Double getLatitude() { return latitude; }
        @Nullable
        public Double getLongitude() { return longitude; }
        @Nullable
        public String getPnu() { return pnu; }
        @Nullable
        public String getLandCategory() { return landCategory; }
        @Nullable
        public BigDecimal getAreaSqm() { return areaSqm; }
        public boolean getAreaIsManualEntry() { return areaIsManualEntry; }
        @NonNull
        public List<FarmBoundaryCoordinateSummary> getBoundaryCoordinates() { return boundaryCoordinates; }
        @NonNull
        public FarmDataSourceSummary getDataSource() { return dataSource; }
        public static FarmSummary from(Farm farm) {
            Objects.requireNonNull(farm, "farm");
            if (farm.getId() == null) throw new IllegalArgumentException("Persisted farm id is required");
            var coordinates = new java.util.ArrayList<FarmBoundaryCoordinateSummary>();
            for (var coordinate : farm.getBoundaryCoordinates()) coordinates.add(FarmBoundaryCoordinateSummary.from(coordinate));
            return new FarmSummary(farm.getId(), farm.getName(), farm.getRoadAddress(), farm.getJibunAddress(), farm.getLatitude(), farm.getLongitude(), farm.getPnu(), farm.getLandCategory(), farm.getAreaSqm(), farm.getAreaIsManualEntry(), coordinates, FarmDataSourceSummary.from(farm.getDataSource()));
        }
    }
    public record FarmBoundaryCoordinateSummary(
        double latitude,
        double longitude
    ) {
        public double getLatitude() { return latitude; }
        public double getLongitude() { return longitude; }
        public static FarmBoundaryCoordinateSummary from(FarmBoundaryCoordinate coordinate) {
            Objects.requireNonNull(coordinate, "coordinate");
            if (coordinate.getLatitude() == null) throw new IllegalArgumentException("Boundary latitude is required");
            if (coordinate.getLongitude() == null) throw new IllegalArgumentException("Boundary longitude is required");
            return new FarmBoundaryCoordinateSummary(coordinate.getLatitude(), coordinate.getLongitude());
        }
    }
    public record FarmDataSourceSummary(
        @Nullable String address,
        @Nullable String coordinate,
        @Nullable String parcel,
        @Nullable String landCharacteristic
    ) {
        @Nullable
        public String getAddress() { return address; }
        @Nullable
        public String getCoordinate() { return coordinate; }
        @Nullable
        public String getParcel() { return parcel; }
        @Nullable
        public String getLandCharacteristic() { return landCharacteristic; }
        public static FarmDataSourceSummary from(FarmDataSource dataSource) {
            Objects.requireNonNull(dataSource, "dataSource");
            return new FarmDataSourceSummary(dataSource.getAddress(), dataSource.getCoordinate(), dataSource.getParcel(), dataSource.getLandCharacteristic());
        }
    }
    public record Onboarding(
        @NonNull OnboardingStatus status,
        @NonNull List<OnboardingField> missingFields
    ) {
        public Onboarding {
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(missingFields, "missingFields");
        }

        @NonNull
        public OnboardingStatus getStatus() { return status; }
        @NonNull
        public List<OnboardingField> getMissingFields() { return missingFields; }
    }
    public enum OnboardingStatus { REQUIRED, COMPLETE }
    public enum OnboardingField { NAME, PHONE, BIRTH_DATE, NICKNAME, EXPERIENCE_LEVEL, MANAGEMENT_TYPE }
}
