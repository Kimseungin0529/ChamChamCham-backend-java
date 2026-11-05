package com.chamchamcham.api.member.dto;

import com.chamchamcham.api.crop.dto.CropResponses;
import com.chamchamcham.application.member.MemberProfileResult;
import java.time.LocalDate;
import java.util.UUID;
import java.util.List;
import java.util.Objects;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import java.util.stream.Collectors;

public final class MemberResponses {
    private MemberResponses() {}

    public record MyProfileResponse(
        @NonNull UUID memberId,
        @Nullable String email,
        @Nullable String name,
        @Nullable String phone,
        @Nullable LocalDate birthDate,
        @Nullable String nickname,
        @Nullable Integer experienceLevel,
        @Nullable String managementType,
        @Nullable String profileImageUrl,
        @NonNull List<MyFarmResponse> farms,
        @NonNull List<CropProfileResponse> crops
    ) {
        public MyProfileResponse {
            Objects.requireNonNull(memberId, "memberId");
            Objects.requireNonNull(farms, "farms");
            Objects.requireNonNull(crops, "crops");
        }
        @NonNull public UUID getMemberId() { return memberId; }
        @Nullable public String getEmail() { return email; }
        @Nullable public String getName() { return name; }
        @Nullable public String getPhone() { return phone; }
        @Nullable public LocalDate getBirthDate() { return birthDate; }
        @Nullable public String getNickname() { return nickname; }
        @Nullable public Integer getExperienceLevel() { return experienceLevel; }
        @Nullable public String getManagementType() { return managementType; }
        @Nullable public String getProfileImageUrl() { return profileImageUrl; }
        @NonNull public List<MyFarmResponse> getFarms() { return farms; }
        @NonNull public List<CropProfileResponse> getCrops() { return crops; }
        public static MyProfileResponse from(MemberProfileResult.MyProfile result) {
            return new MyProfileResponse(result.getMemberId(), result.getEmail(), result.getName(), result.getPhone(), result.getBirthDate(), result.getNickname(), result.getExperienceLevel(), result.getManagementType(), result.getProfileImageUrl(), result.getFarms().stream().map(MyFarmResponse::from).collect(Collectors.toList()), result.getCrops().stream().map(CropProfileResponse::from).collect(Collectors.toList()));
        }
    }

    public record PublicProfileResponse(
        @NonNull UUID memberId,
        @Nullable String nickname,
        @Nullable Integer experienceLevel,
        @Nullable String managementType,
        @Nullable String profileImageUrl,
        @NonNull List<PublicFarmResponse> farms,
        @NonNull List<CropProfileResponse> crops
    ) {
        public PublicProfileResponse {
            Objects.requireNonNull(memberId, "memberId");
            Objects.requireNonNull(farms, "farms");
            Objects.requireNonNull(crops, "crops");
        }
        @NonNull public UUID getMemberId() { return memberId; }
        @Nullable public String getNickname() { return nickname; }
        @Nullable public Integer getExperienceLevel() { return experienceLevel; }
        @Nullable public String getManagementType() { return managementType; }
        @Nullable public String getProfileImageUrl() { return profileImageUrl; }
        @NonNull public List<PublicFarmResponse> getFarms() { return farms; }
        @NonNull public List<CropProfileResponse> getCrops() { return crops; }
        public static PublicProfileResponse from(MemberProfileResult.PublicProfile result) {
            return new PublicProfileResponse(result.getMemberId(), result.getNickname(), result.getExperienceLevel(), result.getManagementType(), result.getProfileImageUrl(), result.getFarms().stream().map(PublicFarmResponse::from).collect(Collectors.toList()), result.getCrops().stream().map(CropProfileResponse::from).collect(Collectors.toList()));
        }
    }

    public record MyFarmResponse(
        @NonNull UUID farmId,
        @NonNull String name,
        @NonNull String roadAddress,
        @Nullable String jibunAddress,
        @Nullable String displayRegion
    ) {
        public MyFarmResponse {
            Objects.requireNonNull(farmId, "farmId");
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(roadAddress, "roadAddress");
        }
        @NonNull public UUID getFarmId() { return farmId; }
        @NonNull public String getName() { return name; }
        @NonNull public String getRoadAddress() { return roadAddress; }
        @Nullable public String getJibunAddress() { return jibunAddress; }
        @Nullable public String getDisplayRegion() { return displayRegion; }
        public static MyFarmResponse from(MemberProfileResult.MyFarm result) {
            return new MyFarmResponse(result.getFarmId(), result.getName(), result.getRoadAddress(), result.getJibunAddress(), result.getDisplayRegion());
        }
    }

    public record PublicFarmResponse(
        @NonNull UUID farmId,
        @Nullable String displayRegion
    ) {
        public PublicFarmResponse {
            Objects.requireNonNull(farmId, "farmId");
        }
        @NonNull public UUID getFarmId() { return farmId; }
        @Nullable public String getDisplayRegion() { return displayRegion; }
        public static PublicFarmResponse from(MemberProfileResult.PublicFarm result) {
            return new PublicFarmResponse(result.getFarmId(), result.getDisplayRegion());
        }
    }

    public record CropProfileResponse(
        @NonNull UUID cropId,
        @NonNull String cropName
    ) {
        public CropProfileResponse {
            Objects.requireNonNull(cropId, "cropId");
            Objects.requireNonNull(cropName, "cropName");
        }
        @NonNull public UUID getCropId() { return cropId; }
        @NonNull public String getCropName() { return cropName; }
        public static CropProfileResponse from(MemberProfileResult.CropProfile result) {
            return new CropProfileResponse(result.getCropId(), result.getCropName());
        }
    }

    public record FarmCropsResponse(
        @NonNull UUID farmId,
        @NonNull String farmName,
        @NonNull List<CropResponses.CropResponse> crops
    ) {
        public FarmCropsResponse {
            Objects.requireNonNull(farmId, "farmId");
            Objects.requireNonNull(farmName, "farmName");
            Objects.requireNonNull(crops, "crops");
        }
        @NonNull public UUID getFarmId() { return farmId; }
        @NonNull public String getFarmName() { return farmName; }
        @NonNull public List<CropResponses.CropResponse> getCrops() { return crops; }
        public static FarmCropsResponse from(MemberProfileResult.FarmCrops result) {
            return new FarmCropsResponse(result.getFarmId(), result.getFarmName(), result.getCrops().stream().map(CropResponses.CropResponse::from).collect(Collectors.toList()));
        }
    }
}
