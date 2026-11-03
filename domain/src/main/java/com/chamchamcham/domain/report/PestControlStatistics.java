package com.chamchamcham.domain.report;

import java.util.List;
import java.util.Objects;
import java.math.BigDecimal;
import java.time.LocalDate;

public final class PestControlStatistics {
    private int recordCount;
    private LocalDate firstWorkedOn;
    private LocalDate lastWorkedOn;
    private int workedDayCount;
    private BigDecimal averageIntervalDays;
    private int photoAttachedRecordCount;
    private BigDecimal photoAttachmentRatePct;
    private List<CountDistribution> weatherDistribution;
    private BigDecimal averageTemperatureC;
    private List<CountDistribution> categoryDistribution;
    private List<AmountByUnit> pesticideAmounts;
    private List<CategoryAmountByUnit> categoryAmounts;
    private BigDecimal totalSprayAmountLiters;
    private Coverage sprayAmountCoverage;
    private List<TargetCount> targets;

    public PestControlStatistics() {
        this.recordCount = 0;
        this.firstWorkedOn = null;
        this.lastWorkedOn = null;
        this.workedDayCount = 0;
        this.averageIntervalDays = null;
        this.photoAttachedRecordCount = 0;
        this.photoAttachmentRatePct = null;
        this.weatherDistribution = List.of();
        this.averageTemperatureC = null;
        this.categoryDistribution = List.of();
        this.pesticideAmounts = List.of();
        this.categoryAmounts = List.of();
        this.totalSprayAmountLiters = null;
        this.sprayAmountCoverage = new Coverage(0, 0);
        this.targets = List.of();
    }

    public PestControlStatistics(
        int recordCount,
        LocalDate firstWorkedOn,
        LocalDate lastWorkedOn,
        int workedDayCount,
        BigDecimal averageIntervalDays,
        int photoAttachedRecordCount,
        BigDecimal photoAttachmentRatePct,
        List<CountDistribution> weatherDistribution,
        BigDecimal averageTemperatureC,
        List<CountDistribution> categoryDistribution,
        List<AmountByUnit> pesticideAmounts,
        List<CategoryAmountByUnit> categoryAmounts,
        BigDecimal totalSprayAmountLiters,
        Coverage sprayAmountCoverage,
        List<TargetCount> targets
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
        this.categoryDistribution = Objects.requireNonNull(categoryDistribution, "categoryDistribution");
        this.pesticideAmounts = Objects.requireNonNull(pesticideAmounts, "pesticideAmounts");
        this.categoryAmounts = Objects.requireNonNull(categoryAmounts, "categoryAmounts");
        this.totalSprayAmountLiters = totalSprayAmountLiters;
        this.sprayAmountCoverage = Objects.requireNonNull(sprayAmountCoverage, "sprayAmountCoverage");
        this.targets = Objects.requireNonNull(targets, "targets");
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

    public List<CountDistribution> getCategoryDistribution() { return categoryDistribution; }

    private void setCategoryDistribution(List<CountDistribution> categoryDistribution) { this.categoryDistribution = Objects.requireNonNull(categoryDistribution, "categoryDistribution"); }

    public List<AmountByUnit> getPesticideAmounts() { return pesticideAmounts; }

    private void setPesticideAmounts(List<AmountByUnit> pesticideAmounts) { this.pesticideAmounts = Objects.requireNonNull(pesticideAmounts, "pesticideAmounts"); }

    public List<CategoryAmountByUnit> getCategoryAmounts() { return categoryAmounts; }

    private void setCategoryAmounts(List<CategoryAmountByUnit> categoryAmounts) { this.categoryAmounts = Objects.requireNonNull(categoryAmounts, "categoryAmounts"); }

    public BigDecimal getTotalSprayAmountLiters() { return totalSprayAmountLiters; }

    private void setTotalSprayAmountLiters(BigDecimal totalSprayAmountLiters) { this.totalSprayAmountLiters = totalSprayAmountLiters; }

    public Coverage getSprayAmountCoverage() { return sprayAmountCoverage; }

    private void setSprayAmountCoverage(Coverage sprayAmountCoverage) { this.sprayAmountCoverage = Objects.requireNonNull(sprayAmountCoverage, "sprayAmountCoverage"); }

    public List<TargetCount> getTargets() { return targets; }

    private void setTargets(List<TargetCount> targets) { this.targets = Objects.requireNonNull(targets, "targets"); }

    public static PestControlStatistics empty() { return new PestControlStatistics(); }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof PestControlStatistics that)) return false;
        return recordCount == that.recordCount
            && Objects.equals(firstWorkedOn, that.firstWorkedOn)
            && Objects.equals(lastWorkedOn, that.lastWorkedOn)
            && workedDayCount == that.workedDayCount
            && Objects.equals(averageIntervalDays, that.averageIntervalDays)
            && photoAttachedRecordCount == that.photoAttachedRecordCount
            && Objects.equals(photoAttachmentRatePct, that.photoAttachmentRatePct)
            && Objects.equals(weatherDistribution, that.weatherDistribution)
            && Objects.equals(averageTemperatureC, that.averageTemperatureC)
            && Objects.equals(categoryDistribution, that.categoryDistribution)
            && Objects.equals(pesticideAmounts, that.pesticideAmounts)
            && Objects.equals(categoryAmounts, that.categoryAmounts)
            && Objects.equals(totalSprayAmountLiters, that.totalSprayAmountLiters)
            && Objects.equals(sprayAmountCoverage, that.sprayAmountCoverage)
            && Objects.equals(targets, that.targets);
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
        result = 31 * result + Objects.hashCode(categoryDistribution);
        result = 31 * result + Objects.hashCode(pesticideAmounts);
        result = 31 * result + Objects.hashCode(categoryAmounts);
        result = 31 * result + Objects.hashCode(totalSprayAmountLiters);
        result = 31 * result + Objects.hashCode(sprayAmountCoverage);
        result = 31 * result + Objects.hashCode(targets);
        return result;
    }

    @Override
    public String toString() {
        return "PestControlStatistics(" + "recordCount=" + recordCount + ", " + "firstWorkedOn=" + firstWorkedOn + ", " + "lastWorkedOn=" + lastWorkedOn + ", " + "workedDayCount=" + workedDayCount + ", " + "averageIntervalDays=" + averageIntervalDays + ", " + "photoAttachedRecordCount=" + photoAttachedRecordCount + ", " + "photoAttachmentRatePct=" + photoAttachmentRatePct + ", " + "weatherDistribution=" + weatherDistribution + ", " + "averageTemperatureC=" + averageTemperatureC + ", " + "categoryDistribution=" + categoryDistribution + ", " + "pesticideAmounts=" + pesticideAmounts + ", " + "categoryAmounts=" + categoryAmounts + ", " + "totalSprayAmountLiters=" + totalSprayAmountLiters + ", " + "sprayAmountCoverage=" + sprayAmountCoverage + ", " + "targets=" + targets + ")";
    }
}
