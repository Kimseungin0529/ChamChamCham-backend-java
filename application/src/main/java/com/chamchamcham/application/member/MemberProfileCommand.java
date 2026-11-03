package com.chamchamcham.application.member;

import com.chamchamcham.domain.member.ManagementType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.List;
import java.util.Objects;
import org.springframework.lang.Nullable;
import org.springframework.lang.NonNull;

public final class MemberProfileCommand {
    private MemberProfileCommand() {}

    public record UpdateMyProfile(
        @NonNull UUID memberId,
        @Nullable String name,
        @Nullable String phone,
        @Nullable LocalDate birthDate,
        @Nullable String nickname,
        int experienceLevel,
        @NonNull ManagementType managementType,
        @Nullable UUID profileMediaId,
        @NonNull List<Farm> farms
    ) {
        public UpdateMyProfile {
            Objects.requireNonNull(memberId, "memberId");
            Objects.requireNonNull(managementType, "managementType");
            Objects.requireNonNull(farms, "farms");
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
        @Nullable
        public UUID getProfileMediaId() { return profileMediaId; }
        @NonNull
        public List<Farm> getFarms() { return farms; }

    }

    public record Farm(
        @Nullable UUID farmId,
        @NonNull String name,
        @NonNull String roadAddress,
        @Nullable String jibunAddress,
        @Nullable Double latitude,
        @Nullable Double longitude,
        @Nullable String pnu,
        @Nullable String landCategory,
        @Nullable BigDecimal areaSqm,
        boolean areaIsManualEntry,
        @NonNull List<FarmBoundaryCoordinate> boundaryCoordinates,
        @NonNull FarmDataSource dataSource,
        @NonNull List<UUID> cropIds
    ) {
        public Farm {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(roadAddress, "roadAddress");
            Objects.requireNonNull(boundaryCoordinates, "boundaryCoordinates");
            Objects.requireNonNull(dataSource, "dataSource");
            Objects.requireNonNull(cropIds, "cropIds");
        }

        @Nullable
        public UUID getFarmId() { return farmId; }
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
        public List<FarmBoundaryCoordinate> getBoundaryCoordinates() { return boundaryCoordinates; }
        @NonNull
        public FarmDataSource getDataSource() { return dataSource; }
        @NonNull
        public List<UUID> getCropIds() { return cropIds; }

    }

    public record FarmBoundaryCoordinate(
        double latitude,
        double longitude
    ) {
        public double getLatitude() { return latitude; }
        public double getLongitude() { return longitude; }

    }

    public record FarmDataSource(
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

    }
}
