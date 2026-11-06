package com.chamchamcham.api.report.dto;

import com.chamchamcham.domain.report.AmountByUnit;
import com.chamchamcham.domain.report.CategoryAmountByUnit;
import com.chamchamcham.domain.report.CategoryMethodStatistics;
import com.chamchamcham.domain.report.CommonOnlyStatistics;
import com.chamchamcham.domain.report.CountDistribution;
import com.chamchamcham.domain.report.Coverage;
import com.chamchamcham.domain.report.CycleReportStatistics;
import com.chamchamcham.domain.report.FertilizingStatistics;
import com.chamchamcham.domain.report.GrowthPeriodRange;
import com.chamchamcham.domain.report.HarvestPartStatistics;
import com.chamchamcham.domain.report.HarvestStatistics;
import com.chamchamcham.domain.report.MaterialCategoryStatistics;
import com.chamchamcham.domain.report.PestControlStatistics;
import com.chamchamcham.domain.report.PlantingStatistics;
import com.chamchamcham.domain.report.PropagationStatistics;
import com.chamchamcham.domain.report.TargetCount;
import com.chamchamcham.domain.report.WateringStatistics;
import com.chamchamcham.domain.report.WeedingStatistics;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.ArrayList;
import java.util.stream.Collectors;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public final class FarmingReportStatisticsResponses {
    private FarmingReportStatisticsResponses() {}

    public record CoverageResponse(
        int recordedCount,
        int targetCount
    ) {
        public int getRecordedCount() { return recordedCount; }
        public int getTargetCount() { return targetCount; }
        public static CoverageResponse from(Coverage source) {
            return new CoverageResponse(
                source.recordedCount(),
                source.targetCount()
            );
        }
    }

    public record CountDistributionResponse(
        @NonNull String code,
        @NonNull String label,
        int count,
        @NonNull BigDecimal ratePct
    ) {
        public CountDistributionResponse {
            Objects.requireNonNull(code, "code");
            Objects.requireNonNull(label, "label");
            Objects.requireNonNull(ratePct, "ratePct");
        }
        @NonNull public String getCode() { return code; }
        @NonNull public String getLabel() { return label; }
        public int getCount() { return count; }
        @NonNull public BigDecimal getRatePct() { return ratePct; }
        public static CountDistributionResponse from(CountDistribution source) {
            return new CountDistributionResponse(
                source.code(),
                source.label(),
                source.count(),
                source.ratePct()
            );
        }
    }

    public record AmountByUnitResponse(
        @NonNull String unit,
        @NonNull BigDecimal amount,
        @NonNull CoverageResponse coverage
    ) {
        public AmountByUnitResponse {
            Objects.requireNonNull(unit, "unit");
            Objects.requireNonNull(amount, "amount");
            Objects.requireNonNull(coverage, "coverage");
        }
        @NonNull public String getUnit() { return unit; }
        @NonNull public BigDecimal getAmount() { return amount; }
        @NonNull public CoverageResponse getCoverage() { return coverage; }
        public static AmountByUnitResponse from(AmountByUnit source) {
            return new AmountByUnitResponse(
                source.unit(),
                source.amount(),
                CoverageResponse.from(source.coverage())
            );
        }
    }

    public record PropagationStatisticsResponse(
        @NonNull String code,
        @NonNull String label,
        int recordCount,
        @NonNull BigDecimal recordRatePct,
        @Nullable BigDecimal totalQuantity,
        @Nullable String quantityUnit,
        @NonNull CoverageResponse quantityCoverage
    ) {
        public PropagationStatisticsResponse {
            Objects.requireNonNull(code, "code");
            Objects.requireNonNull(label, "label");
            Objects.requireNonNull(recordRatePct, "recordRatePct");
            Objects.requireNonNull(quantityCoverage, "quantityCoverage");
        }
        @NonNull public String getCode() { return code; }
        @NonNull public String getLabel() { return label; }
        public int getRecordCount() { return recordCount; }
        @NonNull public BigDecimal getRecordRatePct() { return recordRatePct; }
        @Nullable public BigDecimal getTotalQuantity() { return totalQuantity; }
        @Nullable public String getQuantityUnit() { return quantityUnit; }
        @NonNull public CoverageResponse getQuantityCoverage() { return quantityCoverage; }
        public static PropagationStatisticsResponse from(PropagationStatistics source) {
            return new PropagationStatisticsResponse(
                source.code(),
                source.label(),
                source.recordCount(),
                source.recordRatePct(),
                source.totalQuantity(),
                source.quantityUnit(),
                CoverageResponse.from(source.quantityCoverage())
            );
        }
    }

    public record MaterialCategoryStatisticsResponse(
        @NonNull String code,
        @NonNull String label,
        int recordCount,
        @NonNull BigDecimal recordRatePct,
        @NonNull BigDecimal amountKg,
        @NonNull BigDecimal amountRatePct
    ) {
        public MaterialCategoryStatisticsResponse {
            Objects.requireNonNull(code, "code");
            Objects.requireNonNull(label, "label");
            Objects.requireNonNull(recordRatePct, "recordRatePct");
            Objects.requireNonNull(amountKg, "amountKg");
            Objects.requireNonNull(amountRatePct, "amountRatePct");
        }
        @NonNull public String getCode() { return code; }
        @NonNull public String getLabel() { return label; }
        public int getRecordCount() { return recordCount; }
        @NonNull public BigDecimal getRecordRatePct() { return recordRatePct; }
        @NonNull public BigDecimal getAmountKg() { return amountKg; }
        @NonNull public BigDecimal getAmountRatePct() { return amountRatePct; }
        public static MaterialCategoryStatisticsResponse from(MaterialCategoryStatistics source) {
            return new MaterialCategoryStatisticsResponse(
                source.code(),
                source.label(),
                source.recordCount(),
                source.recordRatePct(),
                source.amountKg(),
                source.amountRatePct()
            );
        }
    }

    public record CategoryMethodStatisticsResponse(
        @NonNull String categoryCode,
        @NonNull String categoryLabel,
        @NonNull String methodCode,
        @NonNull String methodLabel,
        int recordCount,
        @NonNull BigDecimal recordRatePct
    ) {
        public CategoryMethodStatisticsResponse {
            Objects.requireNonNull(categoryCode, "categoryCode");
            Objects.requireNonNull(categoryLabel, "categoryLabel");
            Objects.requireNonNull(methodCode, "methodCode");
            Objects.requireNonNull(methodLabel, "methodLabel");
            Objects.requireNonNull(recordRatePct, "recordRatePct");
        }
        @NonNull public String getCategoryCode() { return categoryCode; }
        @NonNull public String getCategoryLabel() { return categoryLabel; }
        @NonNull public String getMethodCode() { return methodCode; }
        @NonNull public String getMethodLabel() { return methodLabel; }
        public int getRecordCount() { return recordCount; }
        @NonNull public BigDecimal getRecordRatePct() { return recordRatePct; }
        public static CategoryMethodStatisticsResponse from(CategoryMethodStatistics source) {
            return new CategoryMethodStatisticsResponse(
                source.categoryCode(),
                source.categoryLabel(),
                source.methodCode(),
                source.methodLabel(),
                source.recordCount(),
                source.recordRatePct()
            );
        }
    }

    public record CategoryAmountByUnitResponse(
        @NonNull String categoryCode,
        @NonNull String categoryLabel,
        @NonNull String unit,
        int recordCount,
        @NonNull BigDecimal amount,
        @NonNull CoverageResponse coverage
    ) {
        public CategoryAmountByUnitResponse {
            Objects.requireNonNull(categoryCode, "categoryCode");
            Objects.requireNonNull(categoryLabel, "categoryLabel");
            Objects.requireNonNull(unit, "unit");
            Objects.requireNonNull(amount, "amount");
            Objects.requireNonNull(coverage, "coverage");
        }
        @NonNull public String getCategoryCode() { return categoryCode; }
        @NonNull public String getCategoryLabel() { return categoryLabel; }
        @NonNull public String getUnit() { return unit; }
        public int getRecordCount() { return recordCount; }
        @NonNull public BigDecimal getAmount() { return amount; }
        @NonNull public CoverageResponse getCoverage() { return coverage; }
        public static CategoryAmountByUnitResponse from(CategoryAmountByUnit source) {
            return new CategoryAmountByUnitResponse(
                source.categoryCode(),
                source.categoryLabel(),
                source.unit(),
                source.recordCount(),
                source.amount(),
                CoverageResponse.from(source.coverage())
            );
        }
    }

    public record TargetCountResponse(
        @NonNull String target,
        int count
    ) {
        public TargetCountResponse {
            Objects.requireNonNull(target, "target");
        }
        @NonNull public String getTarget() { return target; }
        public int getCount() { return count; }
        public static TargetCountResponse from(TargetCount source) {
            return new TargetCountResponse(
                source.target(),
                source.count()
            );
        }
    }

    public record HarvestPartStatisticsResponse(
        @NonNull String code,
        @NonNull String label,
        int recordCount,
        @NonNull BigDecimal recordRatePct,
        @Nullable BigDecimal knownAmountKg,
        @Nullable BigDecimal amountRatePct,
        @NonNull CoverageResponse amountCoverage
    ) {
        public HarvestPartStatisticsResponse {
            Objects.requireNonNull(code, "code");
            Objects.requireNonNull(label, "label");
            Objects.requireNonNull(recordRatePct, "recordRatePct");
            Objects.requireNonNull(amountCoverage, "amountCoverage");
        }
        @NonNull public String getCode() { return code; }
        @NonNull public String getLabel() { return label; }
        public int getRecordCount() { return recordCount; }
        @NonNull public BigDecimal getRecordRatePct() { return recordRatePct; }
        @Nullable public BigDecimal getKnownAmountKg() { return knownAmountKg; }
        @Nullable public BigDecimal getAmountRatePct() { return amountRatePct; }
        @NonNull public CoverageResponse getAmountCoverage() { return amountCoverage; }
        public static HarvestPartStatisticsResponse from(HarvestPartStatistics source) {
            return new HarvestPartStatisticsResponse(
                source.code(),
                source.label(),
                source.recordCount(),
                source.recordRatePct(),
                source.knownAmountKg(),
                source.amountRatePct(),
                CoverageResponse.from(source.amountCoverage())
            );
        }
    }

    public record GrowthPeriodRangeResponse(
        int minMonths,
        int maxMonths
    ) {
        public int getMinMonths() { return minMonths; }
        public int getMaxMonths() { return maxMonths; }
        public static GrowthPeriodRangeResponse from(GrowthPeriodRange source) {
            return new GrowthPeriodRangeResponse(
                source.minMonths(),
                source.maxMonths()
            );
        }
    }

    public record CommonStatisticsResponse(
        int recordCount,
        @Nullable LocalDate firstWorkedOn,
        @Nullable LocalDate lastWorkedOn,
        int workedDayCount,
        @Nullable BigDecimal averageIntervalDays,
        int photoAttachedRecordCount,
        @Nullable BigDecimal photoAttachmentRatePct,
        @NonNull List<CountDistributionResponse> weatherDistribution,
        @Nullable BigDecimal averageTemperatureC
    ) {
        public CommonStatisticsResponse {
            Objects.requireNonNull(weatherDistribution, "weatherDistribution");
        }
        public int getRecordCount() { return recordCount; }
        @Nullable public LocalDate getFirstWorkedOn() { return firstWorkedOn; }
        @Nullable public LocalDate getLastWorkedOn() { return lastWorkedOn; }
        public int getWorkedDayCount() { return workedDayCount; }
        @Nullable public BigDecimal getAverageIntervalDays() { return averageIntervalDays; }
        public int getPhotoAttachedRecordCount() { return photoAttachedRecordCount; }
        @Nullable public BigDecimal getPhotoAttachmentRatePct() { return photoAttachmentRatePct; }
        @NonNull public List<CountDistributionResponse> getWeatherDistribution() { return weatherDistribution; }
        @Nullable public BigDecimal getAverageTemperatureC() { return averageTemperatureC; }
        public static CommonStatisticsResponse from(CommonOnlyStatistics source) {
            return new CommonStatisticsResponse(
                source.getRecordCount(),
                source.getFirstWorkedOn(),
                source.getLastWorkedOn(),
                source.getWorkedDayCount(),
                source.getAverageIntervalDays(),
                source.getPhotoAttachedRecordCount(),
                source.getPhotoAttachmentRatePct(),
                source.getWeatherDistribution().stream().map(CountDistributionResponse::from).collect(Collectors.toCollection(ArrayList::new)),
                source.getAverageTemperatureC()
            );
        }
    }

    public record PlantingStatisticsResponse(
        int recordCount,
        @Nullable LocalDate firstWorkedOn,
        @Nullable LocalDate lastWorkedOn,
        int workedDayCount,
        @Nullable BigDecimal averageIntervalDays,
        int photoAttachedRecordCount,
        @Nullable BigDecimal photoAttachmentRatePct,
        @NonNull List<CountDistributionResponse> weatherDistribution,
        @Nullable BigDecimal averageTemperatureC,
        @NonNull List<CountDistributionResponse> plantingMethodDistribution,
        @NonNull List<PropagationStatisticsResponse> propagationMethods
    ) {
        public PlantingStatisticsResponse {
            Objects.requireNonNull(weatherDistribution, "weatherDistribution");
            Objects.requireNonNull(plantingMethodDistribution, "plantingMethodDistribution");
            Objects.requireNonNull(propagationMethods, "propagationMethods");
        }
        public int getRecordCount() { return recordCount; }
        @Nullable public LocalDate getFirstWorkedOn() { return firstWorkedOn; }
        @Nullable public LocalDate getLastWorkedOn() { return lastWorkedOn; }
        public int getWorkedDayCount() { return workedDayCount; }
        @Nullable public BigDecimal getAverageIntervalDays() { return averageIntervalDays; }
        public int getPhotoAttachedRecordCount() { return photoAttachedRecordCount; }
        @Nullable public BigDecimal getPhotoAttachmentRatePct() { return photoAttachmentRatePct; }
        @NonNull public List<CountDistributionResponse> getWeatherDistribution() { return weatherDistribution; }
        @Nullable public BigDecimal getAverageTemperatureC() { return averageTemperatureC; }
        @NonNull public List<CountDistributionResponse> getPlantingMethodDistribution() { return plantingMethodDistribution; }
        @NonNull public List<PropagationStatisticsResponse> getPropagationMethods() { return propagationMethods; }
        public static PlantingStatisticsResponse from(PlantingStatistics source) {
            return new PlantingStatisticsResponse(
                source.getRecordCount(),
                source.getFirstWorkedOn(),
                source.getLastWorkedOn(),
                source.getWorkedDayCount(),
                source.getAverageIntervalDays(),
                source.getPhotoAttachedRecordCount(),
                source.getPhotoAttachmentRatePct(),
                source.getWeatherDistribution().stream().map(CountDistributionResponse::from).collect(Collectors.toCollection(ArrayList::new)),
                source.getAverageTemperatureC(),
                source.getPlantingMethodDistribution().stream().map(CountDistributionResponse::from).collect(Collectors.toCollection(ArrayList::new)),
                source.getPropagationMethods().stream().map(PropagationStatisticsResponse::from).collect(Collectors.toCollection(ArrayList::new))
            );
        }
    }

    public record WateringStatisticsResponse(
        int recordCount,
        @Nullable LocalDate firstWorkedOn,
        @Nullable LocalDate lastWorkedOn,
        int workedDayCount,
        @Nullable BigDecimal averageIntervalDays,
        int photoAttachedRecordCount,
        @Nullable BigDecimal photoAttachmentRatePct,
        @NonNull List<CountDistributionResponse> weatherDistribution,
        @Nullable BigDecimal averageTemperatureC,
        @NonNull List<CountDistributionResponse> amountDistribution,
        @NonNull List<CountDistributionResponse> methodDistribution
    ) {
        public WateringStatisticsResponse {
            Objects.requireNonNull(weatherDistribution, "weatherDistribution");
            Objects.requireNonNull(amountDistribution, "amountDistribution");
            Objects.requireNonNull(methodDistribution, "methodDistribution");
        }
        public int getRecordCount() { return recordCount; }
        @Nullable public LocalDate getFirstWorkedOn() { return firstWorkedOn; }
        @Nullable public LocalDate getLastWorkedOn() { return lastWorkedOn; }
        public int getWorkedDayCount() { return workedDayCount; }
        @Nullable public BigDecimal getAverageIntervalDays() { return averageIntervalDays; }
        public int getPhotoAttachedRecordCount() { return photoAttachedRecordCount; }
        @Nullable public BigDecimal getPhotoAttachmentRatePct() { return photoAttachmentRatePct; }
        @NonNull public List<CountDistributionResponse> getWeatherDistribution() { return weatherDistribution; }
        @Nullable public BigDecimal getAverageTemperatureC() { return averageTemperatureC; }
        @NonNull public List<CountDistributionResponse> getAmountDistribution() { return amountDistribution; }
        @NonNull public List<CountDistributionResponse> getMethodDistribution() { return methodDistribution; }
        public static WateringStatisticsResponse from(WateringStatistics source) {
            return new WateringStatisticsResponse(
                source.getRecordCount(),
                source.getFirstWorkedOn(),
                source.getLastWorkedOn(),
                source.getWorkedDayCount(),
                source.getAverageIntervalDays(),
                source.getPhotoAttachedRecordCount(),
                source.getPhotoAttachmentRatePct(),
                source.getWeatherDistribution().stream().map(CountDistributionResponse::from).collect(Collectors.toCollection(ArrayList::new)),
                source.getAverageTemperatureC(),
                source.getAmountDistribution().stream().map(CountDistributionResponse::from).collect(Collectors.toCollection(ArrayList::new)),
                source.getMethodDistribution().stream().map(CountDistributionResponse::from).collect(Collectors.toCollection(ArrayList::new))
            );
        }
    }

    public record FertilizingStatisticsResponse(
        int recordCount,
        @Nullable LocalDate firstWorkedOn,
        @Nullable LocalDate lastWorkedOn,
        int workedDayCount,
        @Nullable BigDecimal averageIntervalDays,
        int photoAttachedRecordCount,
        @Nullable BigDecimal photoAttachmentRatePct,
        @NonNull List<CountDistributionResponse> weatherDistribution,
        @Nullable BigDecimal averageTemperatureC,
        @Nullable BigDecimal totalAmountKg,
        @Nullable BigDecimal averageAmountKg,
        @NonNull CoverageResponse amountCoverage,
        @NonNull List<MaterialCategoryStatisticsResponse> materialCategories,
        @NonNull List<CountDistributionResponse> methodDistribution,
        @NonNull List<CategoryMethodStatisticsResponse> categoryMethods
    ) {
        public FertilizingStatisticsResponse {
            Objects.requireNonNull(weatherDistribution, "weatherDistribution");
            Objects.requireNonNull(amountCoverage, "amountCoverage");
            Objects.requireNonNull(materialCategories, "materialCategories");
            Objects.requireNonNull(methodDistribution, "methodDistribution");
            Objects.requireNonNull(categoryMethods, "categoryMethods");
        }
        public int getRecordCount() { return recordCount; }
        @Nullable public LocalDate getFirstWorkedOn() { return firstWorkedOn; }
        @Nullable public LocalDate getLastWorkedOn() { return lastWorkedOn; }
        public int getWorkedDayCount() { return workedDayCount; }
        @Nullable public BigDecimal getAverageIntervalDays() { return averageIntervalDays; }
        public int getPhotoAttachedRecordCount() { return photoAttachedRecordCount; }
        @Nullable public BigDecimal getPhotoAttachmentRatePct() { return photoAttachmentRatePct; }
        @NonNull public List<CountDistributionResponse> getWeatherDistribution() { return weatherDistribution; }
        @Nullable public BigDecimal getAverageTemperatureC() { return averageTemperatureC; }
        @Nullable public BigDecimal getTotalAmountKg() { return totalAmountKg; }
        @Nullable public BigDecimal getAverageAmountKg() { return averageAmountKg; }
        @NonNull public CoverageResponse getAmountCoverage() { return amountCoverage; }
        @NonNull public List<MaterialCategoryStatisticsResponse> getMaterialCategories() { return materialCategories; }
        @NonNull public List<CountDistributionResponse> getMethodDistribution() { return methodDistribution; }
        @NonNull public List<CategoryMethodStatisticsResponse> getCategoryMethods() { return categoryMethods; }
        public static FertilizingStatisticsResponse from(FertilizingStatistics source) {
            return new FertilizingStatisticsResponse(
                source.getRecordCount(),
                source.getFirstWorkedOn(),
                source.getLastWorkedOn(),
                source.getWorkedDayCount(),
                source.getAverageIntervalDays(),
                source.getPhotoAttachedRecordCount(),
                source.getPhotoAttachmentRatePct(),
                source.getWeatherDistribution().stream().map(CountDistributionResponse::from).collect(Collectors.toCollection(ArrayList::new)),
                source.getAverageTemperatureC(),
                source.getTotalAmountKg(),
                source.getAverageAmountKg(),
                CoverageResponse.from(source.getAmountCoverage()),
                source.getMaterialCategories().stream().map(MaterialCategoryStatisticsResponse::from).collect(Collectors.toCollection(ArrayList::new)),
                source.getMethodDistribution().stream().map(CountDistributionResponse::from).collect(Collectors.toCollection(ArrayList::new)),
                source.getCategoryMethods().stream().map(CategoryMethodStatisticsResponse::from).collect(Collectors.toCollection(ArrayList::new))
            );
        }
    }

    public record PestControlStatisticsResponse(
        int recordCount,
        @Nullable LocalDate firstWorkedOn,
        @Nullable LocalDate lastWorkedOn,
        int workedDayCount,
        @Nullable BigDecimal averageIntervalDays,
        int photoAttachedRecordCount,
        @Nullable BigDecimal photoAttachmentRatePct,
        @NonNull List<CountDistributionResponse> weatherDistribution,
        @Nullable BigDecimal averageTemperatureC,
        @NonNull List<CountDistributionResponse> categoryDistribution,
        @NonNull List<AmountByUnitResponse> pesticideAmounts,
        @NonNull List<CategoryAmountByUnitResponse> categoryAmounts,
        @Nullable BigDecimal totalSprayAmountMl,
        @NonNull CoverageResponse sprayAmountCoverage,
        @NonNull List<TargetCountResponse> targets
    ) {
        public PestControlStatisticsResponse {
            Objects.requireNonNull(weatherDistribution, "weatherDistribution");
            Objects.requireNonNull(categoryDistribution, "categoryDistribution");
            Objects.requireNonNull(pesticideAmounts, "pesticideAmounts");
            Objects.requireNonNull(categoryAmounts, "categoryAmounts");
            Objects.requireNonNull(sprayAmountCoverage, "sprayAmountCoverage");
            Objects.requireNonNull(targets, "targets");
        }
        public int getRecordCount() { return recordCount; }
        @Nullable public LocalDate getFirstWorkedOn() { return firstWorkedOn; }
        @Nullable public LocalDate getLastWorkedOn() { return lastWorkedOn; }
        public int getWorkedDayCount() { return workedDayCount; }
        @Nullable public BigDecimal getAverageIntervalDays() { return averageIntervalDays; }
        public int getPhotoAttachedRecordCount() { return photoAttachedRecordCount; }
        @Nullable public BigDecimal getPhotoAttachmentRatePct() { return photoAttachmentRatePct; }
        @NonNull public List<CountDistributionResponse> getWeatherDistribution() { return weatherDistribution; }
        @Nullable public BigDecimal getAverageTemperatureC() { return averageTemperatureC; }
        @NonNull public List<CountDistributionResponse> getCategoryDistribution() { return categoryDistribution; }
        @NonNull public List<AmountByUnitResponse> getPesticideAmounts() { return pesticideAmounts; }
        @NonNull public List<CategoryAmountByUnitResponse> getCategoryAmounts() { return categoryAmounts; }
        @Nullable public BigDecimal getTotalSprayAmountMl() { return totalSprayAmountMl; }
        @NonNull public CoverageResponse getSprayAmountCoverage() { return sprayAmountCoverage; }
        @NonNull public List<TargetCountResponse> getTargets() { return targets; }
        public static PestControlStatisticsResponse from(PestControlStatistics source) {
            return new PestControlStatisticsResponse(
                source.getRecordCount(),
                source.getFirstWorkedOn(),
                source.getLastWorkedOn(),
                source.getWorkedDayCount(),
                source.getAverageIntervalDays(),
                source.getPhotoAttachedRecordCount(),
                source.getPhotoAttachmentRatePct(),
                source.getWeatherDistribution().stream().map(CountDistributionResponse::from).collect(Collectors.toCollection(ArrayList::new)),
                source.getAverageTemperatureC(),
                source.getCategoryDistribution().stream().map(CountDistributionResponse::from).collect(Collectors.toCollection(ArrayList::new)),
                source.getPesticideAmounts().stream().map(AmountByUnitResponse::from).collect(Collectors.toCollection(ArrayList::new)),
                source.getCategoryAmounts().stream().map(CategoryAmountByUnitResponse::from).collect(Collectors.toCollection(ArrayList::new)),
                (source.getTotalSprayAmountLiters() == null ? null : source.getTotalSprayAmountLiters().movePointRight(3).setScale(4)),
                CoverageResponse.from(source.getSprayAmountCoverage()),
                source.getTargets().stream().map(TargetCountResponse::from).collect(Collectors.toCollection(ArrayList::new))
            );
        }
    }

    public record WeedingStatisticsResponse(
        int recordCount,
        @Nullable LocalDate firstWorkedOn,
        @Nullable LocalDate lastWorkedOn,
        int workedDayCount,
        @Nullable BigDecimal averageIntervalDays,
        int photoAttachedRecordCount,
        @Nullable BigDecimal photoAttachmentRatePct,
        @NonNull List<CountDistributionResponse> weatherDistribution,
        @Nullable BigDecimal averageTemperatureC,
        @NonNull List<CountDistributionResponse> methodDistribution
    ) {
        public WeedingStatisticsResponse {
            Objects.requireNonNull(weatherDistribution, "weatherDistribution");
            Objects.requireNonNull(methodDistribution, "methodDistribution");
        }
        public int getRecordCount() { return recordCount; }
        @Nullable public LocalDate getFirstWorkedOn() { return firstWorkedOn; }
        @Nullable public LocalDate getLastWorkedOn() { return lastWorkedOn; }
        public int getWorkedDayCount() { return workedDayCount; }
        @Nullable public BigDecimal getAverageIntervalDays() { return averageIntervalDays; }
        public int getPhotoAttachedRecordCount() { return photoAttachedRecordCount; }
        @Nullable public BigDecimal getPhotoAttachmentRatePct() { return photoAttachmentRatePct; }
        @NonNull public List<CountDistributionResponse> getWeatherDistribution() { return weatherDistribution; }
        @Nullable public BigDecimal getAverageTemperatureC() { return averageTemperatureC; }
        @NonNull public List<CountDistributionResponse> getMethodDistribution() { return methodDistribution; }
        public static WeedingStatisticsResponse from(WeedingStatistics source) {
            return new WeedingStatisticsResponse(
                source.getRecordCount(),
                source.getFirstWorkedOn(),
                source.getLastWorkedOn(),
                source.getWorkedDayCount(),
                source.getAverageIntervalDays(),
                source.getPhotoAttachedRecordCount(),
                source.getPhotoAttachmentRatePct(),
                source.getWeatherDistribution().stream().map(CountDistributionResponse::from).collect(Collectors.toCollection(ArrayList::new)),
                source.getAverageTemperatureC(),
                source.getMethodDistribution().stream().map(CountDistributionResponse::from).collect(Collectors.toCollection(ArrayList::new))
            );
        }
    }

    public record HarvestStatisticsResponse(
        int recordCount,
        @Nullable LocalDate firstWorkedOn,
        @Nullable LocalDate lastWorkedOn,
        int workedDayCount,
        @Nullable BigDecimal averageIntervalDays,
        int photoAttachedRecordCount,
        @Nullable BigDecimal photoAttachmentRatePct,
        @NonNull List<CountDistributionResponse> weatherDistribution,
        @Nullable BigDecimal averageTemperatureC,
        @Nullable BigDecimal totalAmountKg,
        @Nullable BigDecimal averageAmountKg,
        @NonNull CoverageResponse amountCoverage,
        @Nullable LocalDate firstHarvestedOn,
        @Nullable LocalDate lastHarvestedOn,
        @NonNull List<HarvestPartStatisticsResponse> medicinalParts,
        @Nullable Integer finalGrowthPeriodMonths,
        @Nullable GrowthPeriodRangeResponse growthPeriodRangeMonths,
        @NonNull List<CountDistributionResponse> growthPeriodDistribution
    ) {
        public HarvestStatisticsResponse {
            Objects.requireNonNull(weatherDistribution, "weatherDistribution");
            Objects.requireNonNull(amountCoverage, "amountCoverage");
            Objects.requireNonNull(medicinalParts, "medicinalParts");
            Objects.requireNonNull(growthPeriodDistribution, "growthPeriodDistribution");
        }
        public int getRecordCount() { return recordCount; }
        @Nullable public LocalDate getFirstWorkedOn() { return firstWorkedOn; }
        @Nullable public LocalDate getLastWorkedOn() { return lastWorkedOn; }
        public int getWorkedDayCount() { return workedDayCount; }
        @Nullable public BigDecimal getAverageIntervalDays() { return averageIntervalDays; }
        public int getPhotoAttachedRecordCount() { return photoAttachedRecordCount; }
        @Nullable public BigDecimal getPhotoAttachmentRatePct() { return photoAttachmentRatePct; }
        @NonNull public List<CountDistributionResponse> getWeatherDistribution() { return weatherDistribution; }
        @Nullable public BigDecimal getAverageTemperatureC() { return averageTemperatureC; }
        @Nullable public BigDecimal getTotalAmountKg() { return totalAmountKg; }
        @Nullable public BigDecimal getAverageAmountKg() { return averageAmountKg; }
        @NonNull public CoverageResponse getAmountCoverage() { return amountCoverage; }
        @Nullable public LocalDate getFirstHarvestedOn() { return firstHarvestedOn; }
        @Nullable public LocalDate getLastHarvestedOn() { return lastHarvestedOn; }
        @NonNull public List<HarvestPartStatisticsResponse> getMedicinalParts() { return medicinalParts; }
        @Nullable public Integer getFinalGrowthPeriodMonths() { return finalGrowthPeriodMonths; }
        @Nullable public GrowthPeriodRangeResponse getGrowthPeriodRangeMonths() { return growthPeriodRangeMonths; }
        @NonNull public List<CountDistributionResponse> getGrowthPeriodDistribution() { return growthPeriodDistribution; }
        public static HarvestStatisticsResponse from(HarvestStatistics source) {
            return new HarvestStatisticsResponse(
                source.getRecordCount(),
                source.getFirstWorkedOn(),
                source.getLastWorkedOn(),
                source.getWorkedDayCount(),
                source.getAverageIntervalDays(),
                source.getPhotoAttachedRecordCount(),
                source.getPhotoAttachmentRatePct(),
                source.getWeatherDistribution().stream().map(CountDistributionResponse::from).collect(Collectors.toCollection(ArrayList::new)),
                source.getAverageTemperatureC(),
                source.getTotalAmountKg(),
                source.getAverageAmountKg(),
                CoverageResponse.from(source.getAmountCoverage()),
                source.getFirstHarvestedOn(),
                source.getLastHarvestedOn(),
                source.getMedicinalParts().stream().map(HarvestPartStatisticsResponse::from).collect(Collectors.toCollection(ArrayList::new)),
                source.getFinalGrowthPeriodMonths(),
                (source.getGrowthPeriodRangeMonths() == null ? null : GrowthPeriodRangeResponse.from(source.getGrowthPeriodRangeMonths())),
                source.getGrowthPeriodDistribution().stream().map(CountDistributionResponse::from).collect(Collectors.toCollection(ArrayList::new))
            );
        }
    }

    public record CycleStatisticsResponse(
        @NonNull PlantingStatisticsResponse planting,
        @NonNull WateringStatisticsResponse watering,
        @NonNull FertilizingStatisticsResponse fertilizing,
        @NonNull PestControlStatisticsResponse pestControl,
        @NonNull WeedingStatisticsResponse weeding,
        @NonNull CommonStatisticsResponse pruning,
        @NonNull HarvestStatisticsResponse harvest,
        @NonNull CommonStatisticsResponse etc
    ) {
        public CycleStatisticsResponse {
            Objects.requireNonNull(planting, "planting");
            Objects.requireNonNull(watering, "watering");
            Objects.requireNonNull(fertilizing, "fertilizing");
            Objects.requireNonNull(pestControl, "pestControl");
            Objects.requireNonNull(weeding, "weeding");
            Objects.requireNonNull(pruning, "pruning");
            Objects.requireNonNull(harvest, "harvest");
            Objects.requireNonNull(etc, "etc");
        }
        @NonNull public PlantingStatisticsResponse getPlanting() { return planting; }
        @NonNull public WateringStatisticsResponse getWatering() { return watering; }
        @NonNull public FertilizingStatisticsResponse getFertilizing() { return fertilizing; }
        @NonNull public PestControlStatisticsResponse getPestControl() { return pestControl; }
        @NonNull public WeedingStatisticsResponse getWeeding() { return weeding; }
        @NonNull public CommonStatisticsResponse getPruning() { return pruning; }
        @NonNull public HarvestStatisticsResponse getHarvest() { return harvest; }
        @NonNull public CommonStatisticsResponse getEtc() { return etc; }
        public static CycleStatisticsResponse from(CycleReportStatistics source) {
            return new CycleStatisticsResponse(
                PlantingStatisticsResponse.from(source.getPlanting()),
                WateringStatisticsResponse.from(source.getWatering()),
                FertilizingStatisticsResponse.from(source.getFertilizing()),
                PestControlStatisticsResponse.from(source.getPestControl()),
                WeedingStatisticsResponse.from(source.getWeeding()),
                CommonStatisticsResponse.from(source.getPruning()),
                HarvestStatisticsResponse.from(source.getHarvest()),
                CommonStatisticsResponse.from(source.getEtc())
            );
        }
    }
}
