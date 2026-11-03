package com.chamchamcham.domain.report;

import java.util.List;
import java.util.Objects;
import java.math.BigDecimal;
import java.time.LocalDate;

public final class FertilizingStatistics {
    private int recordCount;
    private LocalDate firstWorkedOn;
    private LocalDate lastWorkedOn;
    private int workedDayCount;
    private BigDecimal averageIntervalDays;
    private int photoAttachedRecordCount;
    private BigDecimal photoAttachmentRatePct;
    private List<CountDistribution> weatherDistribution;
    private BigDecimal averageTemperatureC;
    private BigDecimal totalAmountKg;
    private BigDecimal averageAmountKg;
    private Coverage amountCoverage;
    private List<CountDistribution> materialDistribution;
    private List<MaterialCategoryStatistics> materialCategories;
    private List<CountDistribution> methodDistribution;
    private List<CategoryMethodStatistics> categoryMethods;

    public FertilizingStatistics() {
        this.recordCount = 0;
        this.firstWorkedOn = null;
        this.lastWorkedOn = null;
        this.workedDayCount = 0;
        this.averageIntervalDays = null;
        this.photoAttachedRecordCount = 0;
        this.photoAttachmentRatePct = null;
        this.weatherDistribution = List.of();
        this.averageTemperatureC = null;
        this.totalAmountKg = null;
        this.averageAmountKg = null;
        this.amountCoverage = new Coverage(0, 0);
        this.materialDistribution = List.of();
        this.materialCategories = List.of();
        this.methodDistribution = List.of();
        this.categoryMethods = List.of();
    }

    public FertilizingStatistics(
        int recordCount,
        LocalDate firstWorkedOn,
        LocalDate lastWorkedOn,
        int workedDayCount,
        BigDecimal averageIntervalDays,
        int photoAttachedRecordCount,
        BigDecimal photoAttachmentRatePct,
        List<CountDistribution> weatherDistribution,
        BigDecimal averageTemperatureC,
        BigDecimal totalAmountKg,
        BigDecimal averageAmountKg,
        Coverage amountCoverage,
        List<CountDistribution> materialDistribution,
        List<MaterialCategoryStatistics> materialCategories,
        List<CountDistribution> methodDistribution,
        List<CategoryMethodStatistics> categoryMethods
    ) {
        this.recordCount = recordCount;
        this.firstWorkedOn = firstWorkedOn;
        this.lastWorkedOn = lastWorkedOn;
        this.workedDayCount = workedDayCount;
        this.averageIntervalDays = averageIntervalDays;
        this.photoAttachedRecordCount = photoAttachedRecordCount;
        this.photoAttachmentRatePct = photoAttachmentRatePct;
        this.weatherDistribution = Objects.requireNonNull(weatherDistribution, "weatherDistribution");
        this.averageTemperatureC = averageTemperatureC;
        this.totalAmountKg = totalAmountKg;
        this.averageAmountKg = averageAmountKg;
        this.amountCoverage = Objects.requireNonNull(amountCoverage, "amountCoverage");
        this.materialDistribution = Objects.requireNonNull(materialDistribution, "materialDistribution");
        this.materialCategories = Objects.requireNonNull(materialCategories, "materialCategories");
        this.methodDistribution = Objects.requireNonNull(methodDistribution, "methodDistribution");
        this.categoryMethods = Objects.requireNonNull(categoryMethods, "categoryMethods");
    }

    public int getRecordCount() { return recordCount; }

    private void setRecordCount(int recordCount) { this.recordCount = recordCount; }

    public LocalDate getFirstWorkedOn() { return firstWorkedOn; }

    private void setFirstWorkedOn(LocalDate firstWorkedOn) { this.firstWorkedOn = firstWorkedOn; }

    public LocalDate getLastWorkedOn() { return lastWorkedOn; }

    private void setLastWorkedOn(LocalDate lastWorkedOn) { this.lastWorkedOn = lastWorkedOn; }

    public int getWorkedDayCount() { return workedDayCount; }

    private void setWorkedDayCount(int workedDayCount) { this.workedDayCount = workedDayCount; }

    public BigDecimal getAverageIntervalDays() { return averageIntervalDays; }

    private void setAverageIntervalDays(BigDecimal averageIntervalDays) { this.averageIntervalDays = averageIntervalDays; }

    public int getPhotoAttachedRecordCount() { return photoAttachedRecordCount; }

    private void setPhotoAttachedRecordCount(int photoAttachedRecordCount) { this.photoAttachedRecordCount = photoAttachedRecordCount; }

    public BigDecimal getPhotoAttachmentRatePct() { return photoAttachmentRatePct; }

    private void setPhotoAttachmentRatePct(BigDecimal photoAttachmentRatePct) { this.photoAttachmentRatePct = photoAttachmentRatePct; }

    public List<CountDistribution> getWeatherDistribution() { return weatherDistribution; }

    private void setWeatherDistribution(List<CountDistribution> weatherDistribution) { this.weatherDistribution = Objects.requireNonNull(weatherDistribution, "weatherDistribution"); }

    public BigDecimal getAverageTemperatureC() { return averageTemperatureC; }

    private void setAverageTemperatureC(BigDecimal averageTemperatureC) { this.averageTemperatureC = averageTemperatureC; }

    public BigDecimal getTotalAmountKg() { return totalAmountKg; }

    private void setTotalAmountKg(BigDecimal totalAmountKg) { this.totalAmountKg = totalAmountKg; }

    public BigDecimal getAverageAmountKg() { return averageAmountKg; }

    private void setAverageAmountKg(BigDecimal averageAmountKg) { this.averageAmountKg = averageAmountKg; }

    public Coverage getAmountCoverage() { return amountCoverage; }

    private void setAmountCoverage(Coverage amountCoverage) { this.amountCoverage = Objects.requireNonNull(amountCoverage, "amountCoverage"); }

    public List<CountDistribution> getMaterialDistribution() { return materialDistribution; }

    private void setMaterialDistribution(List<CountDistribution> materialDistribution) { this.materialDistribution = Objects.requireNonNull(materialDistribution, "materialDistribution"); }

    public List<MaterialCategoryStatistics> getMaterialCategories() { return materialCategories; }

    private void setMaterialCategories(List<MaterialCategoryStatistics> materialCategories) { this.materialCategories = Objects.requireNonNull(materialCategories, "materialCategories"); }

    public List<CountDistribution> getMethodDistribution() { return methodDistribution; }

    private void setMethodDistribution(List<CountDistribution> methodDistribution) { this.methodDistribution = Objects.requireNonNull(methodDistribution, "methodDistribution"); }

    public List<CategoryMethodStatistics> getCategoryMethods() { return categoryMethods; }

    private void setCategoryMethods(List<CategoryMethodStatistics> categoryMethods) { this.categoryMethods = Objects.requireNonNull(categoryMethods, "categoryMethods"); }

    public static FertilizingStatistics empty() { return new FertilizingStatistics(); }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof FertilizingStatistics that)) return false;
        return recordCount == that.recordCount
            && Objects.equals(firstWorkedOn, that.firstWorkedOn)
            && Objects.equals(lastWorkedOn, that.lastWorkedOn)
            && workedDayCount == that.workedDayCount
            && Objects.equals(averageIntervalDays, that.averageIntervalDays)
            && photoAttachedRecordCount == that.photoAttachedRecordCount
            && Objects.equals(photoAttachmentRatePct, that.photoAttachmentRatePct)
            && Objects.equals(weatherDistribution, that.weatherDistribution)
            && Objects.equals(averageTemperatureC, that.averageTemperatureC)
            && Objects.equals(totalAmountKg, that.totalAmountKg)
            && Objects.equals(averageAmountKg, that.averageAmountKg)
            && Objects.equals(amountCoverage, that.amountCoverage)
            && Objects.equals(materialDistribution, that.materialDistribution)
            && Objects.equals(materialCategories, that.materialCategories)
            && Objects.equals(methodDistribution, that.methodDistribution)
            && Objects.equals(categoryMethods, that.categoryMethods);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(recordCount);
        result = 31 * result + Objects.hashCode(firstWorkedOn);
        result = 31 * result + Objects.hashCode(lastWorkedOn);
        result = 31 * result + Objects.hashCode(workedDayCount);
        result = 31 * result + Objects.hashCode(averageIntervalDays);
        result = 31 * result + Objects.hashCode(photoAttachedRecordCount);
        result = 31 * result + Objects.hashCode(photoAttachmentRatePct);
        result = 31 * result + Objects.hashCode(weatherDistribution);
        result = 31 * result + Objects.hashCode(averageTemperatureC);
        result = 31 * result + Objects.hashCode(totalAmountKg);
        result = 31 * result + Objects.hashCode(averageAmountKg);
        result = 31 * result + Objects.hashCode(amountCoverage);
        result = 31 * result + Objects.hashCode(materialDistribution);
        result = 31 * result + Objects.hashCode(materialCategories);
        result = 31 * result + Objects.hashCode(methodDistribution);
        result = 31 * result + Objects.hashCode(categoryMethods);
        return result;
    }

    @Override
    public String toString() {
        return "FertilizingStatistics(" + "recordCount=" + recordCount + ", " + "firstWorkedOn=" + firstWorkedOn + ", " + "lastWorkedOn=" + lastWorkedOn + ", " + "workedDayCount=" + workedDayCount + ", " + "averageIntervalDays=" + averageIntervalDays + ", " + "photoAttachedRecordCount=" + photoAttachedRecordCount + ", " + "photoAttachmentRatePct=" + photoAttachmentRatePct + ", " + "weatherDistribution=" + weatherDistribution + ", " + "averageTemperatureC=" + averageTemperatureC + ", " + "totalAmountKg=" + totalAmountKg + ", " + "averageAmountKg=" + averageAmountKg + ", " + "amountCoverage=" + amountCoverage + ", " + "materialDistribution=" + materialDistribution + ", " + "materialCategories=" + materialCategories + ", " + "methodDistribution=" + methodDistribution + ", " + "categoryMethods=" + categoryMethods + ")";
    }
}
