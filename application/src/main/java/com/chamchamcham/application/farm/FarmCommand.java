package com.chamchamcham.application.farm;

import com.chamchamcham.domain.farm.Farm;
import com.chamchamcham.domain.farm.FarmBoundaryCoordinate;
import com.chamchamcham.domain.farm.FarmDataSource;
import com.chamchamcham.domain.member.Member;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.List;
import java.util.Objects;
import org.springframework.lang.Nullable;
import org.springframework.lang.NonNull;

public final class FarmCommand {
    private FarmCommand() {}

    public record Create(
        @NonNull UUID memberId,
        @NonNull Draft draft,
        @NonNull List<UUID> cropIds
    ) {
        public Create {
            Objects.requireNonNull(memberId, "memberId");
            Objects.requireNonNull(draft, "draft");
            Objects.requireNonNull(cropIds, "cropIds");
        }

        @NonNull
        public UUID getMemberId() { return memberId; }
        @NonNull
        public Draft getDraft() { return draft; }
        @NonNull
        public List<UUID> getCropIds() { return cropIds; }

    }

    public record Replace(
        @NonNull UUID memberId,
        @NonNull UUID farmId,
        @NonNull Draft draft,
        @NonNull List<UUID> cropIds
    ) {
        public Replace {
            Objects.requireNonNull(memberId, "memberId");
            Objects.requireNonNull(farmId, "farmId");
            Objects.requireNonNull(draft, "draft");
            Objects.requireNonNull(cropIds, "cropIds");
        }

        @NonNull
        public UUID getMemberId() { return memberId; }
        @NonNull
        public UUID getFarmId() { return farmId; }
        @NonNull
        public Draft getDraft() { return draft; }
        @NonNull
        public List<UUID> getCropIds() { return cropIds; }

    }

    public record Delete(
        @NonNull UUID memberId,
        @NonNull UUID farmId
    ) {
        public Delete {
            Objects.requireNonNull(memberId, "memberId");
            Objects.requireNonNull(farmId, "farmId");
        }

        @NonNull
        public UUID getMemberId() { return memberId; }
        @NonNull
        public UUID getFarmId() { return farmId; }

    }

    public record Draft(
        @NonNull String name,
        @NonNull String roadAddress,
        @Nullable String jibunAddress,
        double latitude,
        double longitude,
        @Nullable String pnu,
        @Nullable String landCategory,
        @Nullable BigDecimal areaSqm,
        boolean areaIsManualEntry,
        @NonNull List<BoundaryCoordinate> boundaryCoordinates,
        @NonNull DataSource dataSource
    ) {
        public Draft {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(roadAddress, "roadAddress");
            Objects.requireNonNull(boundaryCoordinates, "boundaryCoordinates");
            Objects.requireNonNull(dataSource, "dataSource");
        }

        @NonNull
        public String getName() { return name; }
        @NonNull
        public String getRoadAddress() { return roadAddress; }
        @Nullable
        public String getJibunAddress() { return jibunAddress; }
        public double getLatitude() { return latitude; }
        public double getLongitude() { return longitude; }
        @Nullable
        public String getPnu() { return pnu; }
        @Nullable
        public String getLandCategory() { return landCategory; }
        @Nullable
        public BigDecimal getAreaSqm() { return areaSqm; }
        public boolean getAreaIsManualEntry() { return areaIsManualEntry; }
        @NonNull
        public List<BoundaryCoordinate> getBoundaryCoordinates() { return boundaryCoordinates; }
        @NonNull
        public DataSource getDataSource() { return dataSource; }

        public Farm toFarm(Member owner) {
            Objects.requireNonNull(owner, "owner");
            return new Farm(null, owner, name, roadAddress, jibunAddress, latitude, longitude, pnu, landCategory,
                areaSqm, areaIsManualEntry, new java.util.ArrayList<>(boundaryCoordinates.stream()
                    .map(c -> new FarmBoundaryCoordinate(c.latitude(), c.longitude())).toList()), dataSource.toDomain());
        }

        public void applyTo(Farm farm) {
            Objects.requireNonNull(farm, "farm");
            farm.updateProfile(name, roadAddress, jibunAddress, latitude, longitude, pnu, landCategory,
                areaSqm, areaIsManualEntry, boundaryCoordinates.stream()
                    .map(c -> new FarmBoundaryCoordinate(c.latitude(), c.longitude())).toList(), dataSource.toDomain());
        }
    }

    public record BoundaryCoordinate(
        double latitude,
        double longitude
    ) {
        public double getLatitude() { return latitude; }
        public double getLongitude() { return longitude; }

    }

    public record DataSource(
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

        private FarmDataSource toDomain() {
            return new FarmDataSource(address, coordinate, parcel, landCharacteristic);
        }
    }
}
