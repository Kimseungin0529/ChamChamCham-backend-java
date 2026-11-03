package com.chamchamcham.domain.report;

import java.util.List;
import java.util.Objects;
import java.math.BigDecimal;
import java.time.LocalDate;

public final class HarvestStatistics {
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
    private LocalDate firstHarvestedOn;
    private LocalDate lastHarvestedOn;
    private List<HarvestPartStatistics> medicinalParts;
    private Integer finalGrowthPeriodMonths;
    private GrowthPeriodRange growthPeriodRangeMonths;
    private List<CountDistribution> growthPeriodDistribution;

    public HarvestStatistics() {
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
        this.firstHarvestedOn = null;
        this.lastHarvestedOn = null;
        this.medicinalParts = List.of();
        this.finalGrowthPeriodMonths = null;
        this.growthPeriodRangeMonths = null;
        this.growthPeriodDistribution = List.of();
    }

    public HarvestStatistics(
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
        LocalDate firstHarvestedOn,
        LocalDate lastHarvestedOn,
        List<HarvestPartStatistics> medicinalParts,
        Integer finalGrowthPeriodMonths,
        GrowthPeriodRange growthPeriodRangeMonths,
        List<CountDistribution> growthPeriodDistribution
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
        this.firstHarvestedOn = firstHarvestedOn;
        this.lastHarvestedOn = lastHarvestedOn;
        this.medicinalParts = Objects.requireNonNull(medicinalParts, "medicinalParts");
        this.finalGrowthPeriodMonths = finalGrowthPeriodMonths;
        this.growthPeriodRangeMonths = growthPeriodRangeMonths;
        this.growthPeriodDistribution = Objects.requireNonNull(growthPeriodDistribution, "growthPeriodDistribution");
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

    public LocalDate getFirstHarvestedOn() { return firstHarvestedOn; }

    private void setFirstHarvestedOn(LocalDate firstHarvestedOn) { this.firstHarvestedOn = firstHarvestedOn; }

    public LocalDate getLastHarvestedOn() { return lastHarvestedOn; }

    private void setLastHarvestedOn(LocalDate lastHarvestedOn) { this.lastHarvestedOn = lastHarvestedOn; }

    public List<HarvestPartStatistics> getMedicinalParts() { return medicinalParts; }

    private void setMedicinalParts(List<HarvestPartStatistics> medicinalParts) { this.medicinalParts = Objects.requireNonNull(medicinalParts, "medicinalParts"); }

    public Integer getFinalGrowthPeriodMonths() { return finalGrowthPeriodMonths; }

    private void setFinalGrowthPeriodMonths(Integer finalGrowthPeriodMonths) { this.finalGrowthPeriodMonths = finalGrowthPeriodMonths; }

    public GrowthPeriodRange getGrowthPeriodRangeMonths() { return growthPeriodRangeMonths; }

    private void setGrowthPeriodRangeMonths(GrowthPeriodRange growthPeriodRangeMonths) { this.growthPeriodRangeMonths = growthPeriodRangeMonths; }

    public List<CountDistribution> getGrowthPeriodDistribution() { return growthPeriodDistribution; }

    private void setGrowthPeriodDistribution(List<CountDistribution> growthPeriodDistribution) { this.growthPeriodDistribution = Objects.requireNonNull(growthPeriodDistribution, "growthPeriodDistribution"); }

    public static HarvestStatistics empty() { return new HarvestStatistics(); }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof HarvestStatistics that)) return false;
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
            && Objects.equals(firstHarvestedOn, that.firstHarvestedOn)
            && Objects.equals(lastHarvestedOn, that.lastHarvestedOn)
            && Objects.equals(medicinalParts, that.medicinalParts)
            && Objects.equals(finalGrowthPeriodMonths, that.finalGrowthPeriodMonths)
            && Objects.equals(growthPeriodRangeMonths, that.growthPeriodRangeMonths)
            && Objects.equals(growthPeriodDistribution, that.growthPeriodDistribution);
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
        result = 31 * result + Objects.hashCode(firstHarvestedOn);
        result = 31 * result + Objects.hashCode(lastHarvestedOn);
        result = 31 * result + Objects.hashCode(medicinalParts);
        result = 31 * result + Objects.hashCode(finalGrowthPeriodMonths);
        result = 31 * result + Objects.hashCode(growthPeriodRangeMonths);
        result = 31 * result + Objects.hashCode(growthPeriodDistribution);
        return result;
    }

    @Override
    public String toString() {
        return "HarvestStatistics(" + "recordCount=" + recordCount + ", " + "firstWorkedOn=" + firstWorkedOn + ", " + "lastWorkedOn=" + lastWorkedOn + ", " + "workedDayCount=" + workedDayCount + ", " + "averageIntervalDays=" + averageIntervalDays + ", " + "photoAttachedRecordCount=" + photoAttachedRecordCount + ", " + "photoAttachmentRatePct=" + photoAttachmentRatePct + ", " + "weatherDistribution=" + weatherDistribution + ", " + "averageTemperatureC=" + averageTemperatureC + ", " + "totalAmountKg=" + totalAmountKg + ", " + "averageAmountKg=" + averageAmountKg + ", " + "amountCoverage=" + amountCoverage + ", " + "firstHarvestedOn=" + firstHarvestedOn + ", " + "lastHarvestedOn=" + lastHarvestedOn + ", " + "medicinalParts=" + medicinalParts + ", " + "finalGrowthPeriodMonths=" + finalGrowthPeriodMonths + ", " + "growthPeriodRangeMonths=" + growthPeriodRangeMonths + ", " + "growthPeriodDistribution=" + growthPeriodDistribution + ")";
    }
}
