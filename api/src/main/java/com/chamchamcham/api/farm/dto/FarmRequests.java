package com.chamchamcham.api.farm.dto;

import com.chamchamcham.application.farm.FarmCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.UniqueElements;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.List;
import java.util.Objects;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import java.util.stream.Collectors;

public final class FarmRequests {
    private FarmRequests() {}

    public record FarmDraftRequest(
        @NotBlank(message = "농장 이름을 입력해주세요")
        @NonNull String name,
        @NotBlank(message = "도로명 주소를 입력해주세요")
        @NonNull String roadAddress,
        @Nullable String jibunAddress,
        @NotNull(message = "위도를 입력해주세요")
        @DecimalMin(value = "-90.0", message = "위도 범위가 올바르지 않습니다")
        @DecimalMax(value = "90.0", message = "위도 범위가 올바르지 않습니다")
        @Nullable Double latitude,
        @NotNull(message = "경도를 입력해주세요")
        @DecimalMin(value = "-180.0", message = "경도 범위가 올바르지 않습니다")
        @DecimalMax(value = "180.0", message = "경도 범위가 올바르지 않습니다")
        @Nullable Double longitude,
        @Nullable String pnu,
        @Nullable String landCategory,
        @DecimalMin(value = "0.0", inclusive = false, message = "면적은 0보다 커야 합니다")
        @Nullable BigDecimal areaSqm,
        boolean areaIsManualEntry,
        @Valid
        @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = BoundaryCoordinatesDefault.class)
        @NonNull List<BoundaryCoordinateRequest> boundaryCoordinates,
        @Valid
        @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = DataSourceDefault.class)
        @NonNull DataSourceRequest dataSource
    ) {
        public FarmDraftRequest {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(roadAddress, "roadAddress");
            Objects.requireNonNull(boundaryCoordinates, "boundaryCoordinates");
            Objects.requireNonNull(dataSource, "dataSource");
        }
        @NonNull public String getName() { return name; }
        @NonNull public String getRoadAddress() { return roadAddress; }
        @Nullable public String getJibunAddress() { return jibunAddress; }
        @Nullable public Double getLatitude() { return latitude; }
        @Nullable public Double getLongitude() { return longitude; }
        @Nullable public String getPnu() { return pnu; }
        @Nullable public String getLandCategory() { return landCategory; }
        @Nullable public BigDecimal getAreaSqm() { return areaSqm; }
        public boolean getAreaIsManualEntry() { return areaIsManualEntry; }
        @NonNull public List<BoundaryCoordinateRequest> getBoundaryCoordinates() { return boundaryCoordinates; }
        @NonNull public DataSourceRequest getDataSource() { return dataSource; }
        public FarmCommand.Draft toCommandDraft() {
            return new FarmCommand.Draft(name, roadAddress, jibunAddress, Objects.requireNonNull(latitude), Objects.requireNonNull(longitude),
                pnu, landCategory, areaSqm, areaIsManualEntry, boundaryCoordinates.stream().map(c -> new FarmCommand.BoundaryCoordinate(
                    Objects.requireNonNull(c.latitude()), Objects.requireNonNull(c.longitude()))).collect(Collectors.toList()),
                new FarmCommand.DataSource(dataSource.address(), dataSource.coordinate(), dataSource.parcel(), dataSource.landCharacteristic()));
        }
    }

    public record SaveFarmRequest(
        @NotBlank(message = "농장 이름을 입력해주세요")
        @NonNull String name,
        @NotBlank(message = "도로명 주소를 입력해주세요")
        @NonNull String roadAddress,
        @Nullable String jibunAddress,
        @NotNull(message = "위도를 입력해주세요")
        @DecimalMin(value = "-90.0", message = "위도 범위가 올바르지 않습니다")
        @DecimalMax(value = "90.0", message = "위도 범위가 올바르지 않습니다")
        @Nullable Double latitude,
        @NotNull(message = "경도를 입력해주세요")
        @DecimalMin(value = "-180.0", message = "경도 범위가 올바르지 않습니다")
        @DecimalMax(value = "180.0", message = "경도 범위가 올바르지 않습니다")
        @Nullable Double longitude,
        @Nullable String pnu,
        @Nullable String landCategory,
        @DecimalMin(value = "0.0", inclusive = false, message = "면적은 0보다 커야 합니다")
        @Nullable BigDecimal areaSqm,
        boolean areaIsManualEntry,
        @Valid
        @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = BoundaryCoordinatesDefault.class)
        @NonNull List<BoundaryCoordinateRequest> boundaryCoordinates,
        @Valid
        @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = DataSourceDefault.class)
        @NonNull DataSourceRequest dataSource,
        @NotEmpty(message = "작물을 하나 이상 선택해주세요")
        @Size(max = 5, message = "작물은 최대 5개까지 선택할 수 있습니다")
        @UniqueElements(message = "작물은 중복해서 선택할 수 없습니다")
        @NonNull List<UUID> cropIds
    ) {
        public SaveFarmRequest {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(roadAddress, "roadAddress");
            Objects.requireNonNull(boundaryCoordinates, "boundaryCoordinates");
            Objects.requireNonNull(dataSource, "dataSource");
            Objects.requireNonNull(cropIds, "cropIds");
        }
        @NonNull public String getName() { return name; }
        @NonNull public String getRoadAddress() { return roadAddress; }
        @Nullable public String getJibunAddress() { return jibunAddress; }
        @Nullable public Double getLatitude() { return latitude; }
        @Nullable public Double getLongitude() { return longitude; }
        @Nullable public String getPnu() { return pnu; }
        @Nullable public String getLandCategory() { return landCategory; }
        @Nullable public BigDecimal getAreaSqm() { return areaSqm; }
        public boolean getAreaIsManualEntry() { return areaIsManualEntry; }
        @NonNull public List<BoundaryCoordinateRequest> getBoundaryCoordinates() { return boundaryCoordinates; }
        @NonNull public DataSourceRequest getDataSource() { return dataSource; }
        @NonNull public List<UUID> getCropIds() { return cropIds; }
        public FarmCommand.Draft toCommandDraft() {
            return new FarmCommand.Draft(name, roadAddress, jibunAddress, Objects.requireNonNull(latitude), Objects.requireNonNull(longitude),
                pnu, landCategory, areaSqm, areaIsManualEntry, boundaryCoordinates.stream().map(c -> new FarmCommand.BoundaryCoordinate(
                    Objects.requireNonNull(c.latitude()), Objects.requireNonNull(c.longitude()))).collect(Collectors.toList()),
                new FarmCommand.DataSource(dataSource.address(), dataSource.coordinate(), dataSource.parcel(), dataSource.landCharacteristic()));
        }
    }

    public record BoundaryCoordinateRequest(
        @NotNull(message = "경계 위도를 입력해주세요")
        @DecimalMin(value = "-90.0", message = "경계 위도 범위가 올바르지 않습니다")
        @DecimalMax(value = "90.0", message = "경계 위도 범위가 올바르지 않습니다")
        @Nullable Double latitude,
        @NotNull(message = "경계 경도를 입력해주세요")
        @DecimalMin(value = "-180.0", message = "경계 경도 범위가 올바르지 않습니다")
        @DecimalMax(value = "180.0", message = "경계 경도 범위가 올바르지 않습니다")
        @Nullable Double longitude
    ) {
        @Nullable public Double getLatitude() { return latitude; }
        @Nullable public Double getLongitude() { return longitude; }
    }

    public record DataSourceRequest(
        @Nullable String address,
        @Nullable String coordinate,
        @Nullable String parcel,
        @Nullable String landCharacteristic
    ) {
        @Nullable public String getAddress() { return address; }
        @Nullable public String getCoordinate() { return coordinate; }
        @Nullable public String getParcel() { return parcel; }
        @Nullable public String getLandCharacteristic() { return landCharacteristic; }
        public DataSourceRequest() { this(null, null, null, null); }
    }
    public static final class BoundaryCoordinatesDefault extends com.fasterxml.jackson.databind.JsonDeserializer<List<BoundaryCoordinateRequest>> {
        @Override
        public List<BoundaryCoordinateRequest> deserialize(com.fasterxml.jackson.core.JsonParser parser,
                com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            return context.readValue(parser, context.getTypeFactory().constructCollectionType(List.class, BoundaryCoordinateRequest.class));
        }
        @Override
        public List<BoundaryCoordinateRequest> getAbsentValue(com.fasterxml.jackson.databind.DeserializationContext context) {
            return java.util.Collections.emptyList();
        }
    }
    public static final class DataSourceDefault extends com.fasterxml.jackson.databind.JsonDeserializer<DataSourceRequest> {
        @Override
        public DataSourceRequest deserialize(com.fasterxml.jackson.core.JsonParser parser,
                com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            return context.readValue(parser, DataSourceRequest.class);
        }
        @Override
        public DataSourceRequest getAbsentValue(com.fasterxml.jackson.databind.DeserializationContext context) { return new DataSourceRequest(); }
    }
}
