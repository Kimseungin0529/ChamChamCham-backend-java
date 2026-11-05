package com.chamchamcham.api.member.dto;

import com.chamchamcham.api.farm.dto.FarmRequests;
import com.chamchamcham.domain.member.ManagementType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.UUID;
import java.util.List;
import java.util.Objects;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public final class MemberRequests {
    private MemberRequests() {}

    public record UpdateMyProfileRequest(
        @Nullable String name,
        @Nullable String phone,
        @Nullable LocalDate birthDate,
        @Nullable String nickname,
        @NotNull(message = "경험 수준을 입력해주세요")
        @Min(value = 0, message = "경험 수준은 0 이상이어야 합니다")
        @Max(value = 100, message = "경험 수준은 100 이하여야 합니다")
        @Nullable Integer experienceLevel,
        @NotNull(message = "경영 형태를 입력해주세요")
        @Nullable ManagementType managementType,
        @Nullable UUID profileMediaId,
        @Valid
        @NotEmpty(message = "농장 정보를 하나 이상 입력해주세요")
        @NonNull List<FarmRequest> farms
    ) {
        public UpdateMyProfileRequest {
            Objects.requireNonNull(farms, "farms");
        }
        @Nullable public String getName() { return name; }
        @Nullable public String getPhone() { return phone; }
        @Nullable public LocalDate getBirthDate() { return birthDate; }
        @Nullable public String getNickname() { return nickname; }
        @Nullable public Integer getExperienceLevel() { return experienceLevel; }
        @Nullable public ManagementType getManagementType() { return managementType; }
        @Nullable public UUID getProfileMediaId() { return profileMediaId; }
        @NonNull public List<FarmRequest> getFarms() { return farms; }
        @AssertTrue(message = "귀농연차는 만 나이를 초과할 수 없습니다")
        public boolean isExperienceLevelWithinAge() {
            if (experienceLevel == null || experienceLevel < 0 || experienceLevel > 100) return true;
            Integer age = birthDate == null ? null : Period.between(birthDate, LocalDate.now()).getYears();
            return age == null || experienceLevel <= age;
        }
    }

    public record FarmRequest(
        @Nullable UUID farmId,
        @NotBlank(message = "농장 이름을 입력해주세요")
        @NonNull String name,
        @NotBlank(message = "도로명 주소를 입력해주세요")
        @NonNull String roadAddress,
        @Nullable String jibunAddress,
        @NotNull(message = "위도를 입력해주세요")
        @Nullable Double latitude,
        @NotNull(message = "경도를 입력해주세요")
        @Nullable Double longitude,
        @Nullable String pnu,
        @Nullable String landCategory,
        @DecimalMin(value = "0.0", inclusive = false, message = "면적은 0보다 커야 합니다")
        @Nullable BigDecimal areaSqm,
        boolean areaIsManualEntry,
        @Valid
        @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = FarmRequests.BoundaryCoordinatesDefault.class)
        @NonNull List<FarmRequests.BoundaryCoordinateRequest> boundaryCoordinates,
        @Valid
        @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = FarmRequests.DataSourceDefault.class)
        @NonNull FarmRequests.DataSourceRequest dataSource,
        @NotEmpty(message = "작물을 하나 이상 선택해주세요")
        @NonNull List<UUID> cropIds
    ) {
        public FarmRequest {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(roadAddress, "roadAddress");
            Objects.requireNonNull(boundaryCoordinates, "boundaryCoordinates");
            Objects.requireNonNull(dataSource, "dataSource");
            Objects.requireNonNull(cropIds, "cropIds");
        }
        @Nullable public UUID getFarmId() { return farmId; }
        @NonNull public String getName() { return name; }
        @NonNull public String getRoadAddress() { return roadAddress; }
        @Nullable public String getJibunAddress() { return jibunAddress; }
        @Nullable public Double getLatitude() { return latitude; }
        @Nullable public Double getLongitude() { return longitude; }
        @Nullable public String getPnu() { return pnu; }
        @Nullable public String getLandCategory() { return landCategory; }
        @Nullable public BigDecimal getAreaSqm() { return areaSqm; }
        public boolean getAreaIsManualEntry() { return areaIsManualEntry; }
        @NonNull public List<FarmRequests.BoundaryCoordinateRequest> getBoundaryCoordinates() { return boundaryCoordinates; }
        @NonNull public FarmRequests.DataSourceRequest getDataSource() { return dataSource; }
        @NonNull public List<UUID> getCropIds() { return cropIds; }
    }
}
