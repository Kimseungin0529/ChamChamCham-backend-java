package com.chamchamcham.application.farm;

import com.chamchamcham.application.crop.CropResult;
import com.chamchamcham.domain.farm.Farm;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.List;
import java.util.Objects;
import org.springframework.lang.Nullable;
import org.springframework.lang.NonNull;

public final class FarmResult {
    private FarmResult() {}

    public record Detail(
        @NonNull UUID farmId,
        @NonNull String name,
        @NonNull String roadAddress,
        @Nullable String jibunAddress,
        @Nullable Double latitude,
        @Nullable Double longitude,
        @Nullable String pnu,
        @Nullable String landCategory,
        @Nullable BigDecimal areaSqm,
        boolean areaIsManualEntry,
        @NonNull List<BoundaryCoordinate> boundaryCoordinates,
        @NonNull DataSource dataSource,
        @NonNull List<CropResult.CropSummary> crops
    ) {
        public Detail {
            Objects.requireNonNull(farmId, "farmId");
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(roadAddress, "roadAddress");
            Objects.requireNonNull(boundaryCoordinates, "boundaryCoordinates");
            Objects.requireNonNull(dataSource, "dataSource");
            Objects.requireNonNull(crops, "crops");
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
        public List<BoundaryCoordinate> getBoundaryCoordinates() { return boundaryCoordinates; }
        @NonNull
        public DataSource getDataSource() { return dataSource; }
        @NonNull
        public List<CropResult.CropSummary> getCrops() { return crops; }

        public static Detail from(Farm farm, List<CropResult.CropSummary> crops) {
            Objects.requireNonNull(farm, "farm");
            Objects.requireNonNull(crops, "crops");
            if (farm.getId() == null) throw new IllegalArgumentException("Persisted farm id is required");
            return new Detail(farm.getId(), farm.getName(), farm.getRoadAddress(), farm.getJibunAddress(),
                farm.getLatitude(), farm.getLongitude(), farm.getPnu(), farm.getLandCategory(), farm.getAreaSqm(),
                farm.getAreaIsManualEntry(), farm.getBoundaryCoordinates().stream().map(c -> {
                    if (c.getLatitude() == null) throw new IllegalArgumentException("Boundary latitude is required");
                    if (c.getLongitude() == null) throw new IllegalArgumentException("Boundary longitude is required");
                    return new BoundaryCoordinate(c.getLatitude(), c.getLongitude());
                }).toList(), new DataSource(farm.getDataSource().getAddress(), farm.getDataSource().getCoordinate(),
                    farm.getDataSource().getParcel(), farm.getDataSource().getLandCharacteristic()), crops);
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

    }
}
