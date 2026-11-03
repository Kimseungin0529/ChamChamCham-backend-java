package com.chamchamcham.application.member;

import com.chamchamcham.application.crop.CropResult;
import java.time.LocalDate;
import java.util.UUID;
import java.util.List;
import java.util.Objects;
import org.springframework.lang.Nullable;
import org.springframework.lang.NonNull;

public final class MemberProfileResult {
    private MemberProfileResult() {}

    public record MyProfile(
        @NonNull UUID memberId,
        @Nullable String email,
        @Nullable String name,
        @Nullable String phone,
        @Nullable LocalDate birthDate,
        @Nullable String nickname,
        @Nullable Integer experienceLevel,
        @Nullable String managementType,
        @Nullable String profileImageUrl,
        @NonNull List<MyFarm> farms,
        @NonNull List<CropProfile> crops
    ) {
        public MyProfile {
            Objects.requireNonNull(memberId, "memberId");
            Objects.requireNonNull(farms, "farms");
            Objects.requireNonNull(crops, "crops");
        }

        @NonNull
        public UUID getMemberId() { return memberId; }
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
        @NonNull
        public List<MyFarm> getFarms() { return farms; }
        @NonNull
        public List<CropProfile> getCrops() { return crops; }

    }

    public record PublicProfile(
        @NonNull UUID memberId,
        @Nullable String nickname,
        @Nullable Integer experienceLevel,
        @Nullable String managementType,
        @Nullable String profileImageUrl,
        @NonNull List<PublicFarm> farms,
        @NonNull List<CropProfile> crops
    ) {
        public PublicProfile {
            Objects.requireNonNull(memberId, "memberId");
            Objects.requireNonNull(farms, "farms");
            Objects.requireNonNull(crops, "crops");
        }

        @NonNull
        public UUID getMemberId() { return memberId; }
        @Nullable
        public String getNickname() { return nickname; }
        @Nullable
        public Integer getExperienceLevel() { return experienceLevel; }
        @Nullable
        public String getManagementType() { return managementType; }
        @Nullable
        public String getProfileImageUrl() { return profileImageUrl; }
        @NonNull
        public List<PublicFarm> getFarms() { return farms; }
        @NonNull
        public List<CropProfile> getCrops() { return crops; }

    }

    public record MyFarm(
        @NonNull UUID farmId,
        @NonNull String name,
        @NonNull String roadAddress,
        @Nullable String jibunAddress,
        @Nullable String displayRegion
    ) {
        public MyFarm {
            Objects.requireNonNull(farmId, "farmId");
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(roadAddress, "roadAddress");
        }

        @NonNull
        public UUID getFarmId() { return farmId; }
        @NonNull
        public String getName() { return name; }
        @NonNull
        public String getRoadAddress() { return roadAddress; }
        @Nullable
        public String getJibunAddress() { return jibunAddress; }
        @Nullable
        public String getDisplayRegion() { return displayRegion; }

    }

    public record PublicFarm(
        @NonNull UUID farmId,
        @Nullable String displayRegion
    ) {
        public PublicFarm {
            Objects.requireNonNull(farmId, "farmId");
        }

        @NonNull
        public UUID getFarmId() { return farmId; }
        @Nullable
        public String getDisplayRegion() { return displayRegion; }

    }

    public record CropProfile(
        @NonNull UUID cropId,
        @NonNull String cropName
    ) {
        public CropProfile {
            Objects.requireNonNull(cropId, "cropId");
            Objects.requireNonNull(cropName, "cropName");
        }

        @NonNull
        public UUID getCropId() { return cropId; }
        @NonNull
        public String getCropName() { return cropName; }

    }

    public record FarmCrops(
        @NonNull UUID farmId,
        @NonNull String farmName,
        @NonNull List<CropResult.CropSummary> crops
    ) {
        public FarmCrops {
            Objects.requireNonNull(farmId, "farmId");
            Objects.requireNonNull(farmName, "farmName");
            Objects.requireNonNull(crops, "crops");
        }

        @NonNull
        public UUID getFarmId() { return farmId; }
        @NonNull
        public String getFarmName() { return farmName; }
        @NonNull
        public List<CropResult.CropSummary> getCrops() { return crops; }

    }
}
