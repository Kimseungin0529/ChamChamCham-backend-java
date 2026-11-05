package com.chamchamcham.api.farm.dto;

import com.chamchamcham.api.crop.dto.CropResponses;
import com.chamchamcham.application.farm.FarmResult;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.List;
import java.util.Objects;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import java.util.stream.Collectors;

public final class FarmResponses {
    private FarmResponses() {}

    public record FarmResponse(
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
        @NonNull List<BoundaryCoordinateResponse> boundaryCoordinates,
        @NonNull DataSourceResponse dataSource,
        @NonNull List<CropResponses.CropResponse> crops
    ) {
        public FarmResponse {
            Objects.requireNonNull(farmId, "farmId");
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(roadAddress, "roadAddress");
            Objects.requireNonNull(boundaryCoordinates, "boundaryCoordinates");
            Objects.requireNonNull(dataSource, "dataSource");
            Objects.requireNonNull(crops, "crops");
        }
        @NonNull public UUID getFarmId() { return farmId; }
        @NonNull public String getName() { return name; }
        @NonNull public String getRoadAddress() { return roadAddress; }
        @Nullable public String getJibunAddress() { return jibunAddress; }
        @Nullable public Double getLatitude() { return latitude; }
        @Nullable public Double getLongitude() { return longitude; }
        @Nullable public String getPnu() { return pnu; }
        @Nullable public String getLandCategory() { return landCategory; }
        @Nullable public BigDecimal getAreaSqm() { return areaSqm; }
        public boolean getAreaIsManualEntry() { return areaIsManualEntry; }
        @NonNull public List<BoundaryCoordinateResponse> getBoundaryCoordinates() { return boundaryCoordinates; }
        @NonNull public DataSourceResponse getDataSource() { return dataSource; }
        @NonNull public List<CropResponses.CropResponse> getCrops() { return crops; }
        public static FarmResponse from(FarmResult.Detail result) {
            return new FarmResponse(result.getFarmId(), result.getName(), result.getRoadAddress(), result.getJibunAddress(), result.getLatitude(), result.getLongitude(), result.getPnu(), result.getLandCategory(), result.getAreaSqm(), result.getAreaIsManualEntry(), result.getBoundaryCoordinates().stream().map(BoundaryCoordinateResponse::from).collect(Collectors.toList()), DataSourceResponse.from(result.getDataSource()), result.getCrops().stream().map(CropResponses.CropResponse::from).collect(Collectors.toList()));
        }
    }

    public record BoundaryCoordinateResponse(
        double latitude,
        double longitude
    ) {
        public double getLatitude() { return latitude; }
        public double getLongitude() { return longitude; }
        public static BoundaryCoordinateResponse from(FarmResult.BoundaryCoordinate result) {
            return new BoundaryCoordinateResponse(result.getLatitude(), result.getLongitude());
        }
    }

    public record DataSourceResponse(
        @Nullable String address,
        @Nullable String coordinate,
        @Nullable String parcel,
        @Nullable String landCharacteristic
    ) {
        @Nullable public String getAddress() { return address; }
        @Nullable public String getCoordinate() { return coordinate; }
        @Nullable public String getParcel() { return parcel; }
        @Nullable public String getLandCharacteristic() { return landCharacteristic; }
        public static DataSourceResponse from(FarmResult.DataSource result) {
            return new DataSourceResponse(result.getAddress(), result.getCoordinate(), result.getParcel(), result.getLandCharacteristic());
        }
    }
}
