package com.chamchamcham.domain.report;

import java.util.List;
import java.util.Objects;
import java.math.BigDecimal;
import java.time.LocalDate;

public final class WateringStatistics {
    private int recordCount;
    private LocalDate firstWorkedOn;
    private LocalDate lastWorkedOn;
    private int workedDayCount;
    private BigDecimal averageIntervalDays;
    private int photoAttachedRecordCount;
    private BigDecimal photoAttachmentRatePct;
    private List<CountDistribution> weatherDistribution;
    private BigDecimal averageTemperatureC;
    private List<CountDistribution> amountDistribution;
    private List<CountDistribution> methodDistribution;

    public WateringStatistics() {
        this.recordCount = 0;
        this.firstWorkedOn = null;
        this.lastWorkedOn = null;
        this.workedDayCount = 0;
        this.averageIntervalDays = null;
        this.photoAttachedRecordCount = 0;
        this.photoAttachmentRatePct = null;
        this.weatherDistribution = List.of();
        this.averageTemperatureC = null;
        this.amountDistribution = List.of();
        this.methodDistribution = List.of();
    }

    public WateringStatistics(
        int recordCount,
        LocalDate firstWorkedOn,
        LocalDate lastWorkedOn,
        int workedDayCount,
        BigDecimal averageIntervalDays,
        int photoAttachedRecordCount,
        BigDecimal photoAttachmentRatePct,
        List<CountDistribution> weatherDistribution,
        BigDecimal averageTemperatureC,
        List<CountDistribution> amountDistribution,
        List<CountDistribution> methodDistribution
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
        this.amountDistribution = Objects.requireNonNull(amountDistribution, "amountDistribution");
        this.methodDistribution = Objects.requireNonNull(methodDistribution, "methodDistribution");
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

    public List<CountDistribution> getAmountDistribution() { return amountDistribution; }

    private void setAmountDistribution(List<CountDistribution> amountDistribution) { this.amountDistribution = Objects.requireNonNull(amountDistribution, "amountDistribution"); }

    public List<CountDistribution> getMethodDistribution() { return methodDistribution; }

    private void setMethodDistribution(List<CountDistribution> methodDistribution) { this.methodDistribution = Objects.requireNonNull(methodDistribution, "methodDistribution"); }

    public static WateringStatistics empty() { return new WateringStatistics(); }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof WateringStatistics that)) return false;
        return recordCount == that.recordCount
            && Objects.equals(firstWorkedOn, that.firstWorkedOn)
            && Objects.equals(lastWorkedOn, that.lastWorkedOn)
            && workedDayCount == that.workedDayCount
            && Objects.equals(averageIntervalDays, that.averageIntervalDays)
            && photoAttachedRecordCount == that.photoAttachedRecordCount
            && Objects.equals(photoAttachmentRatePct, that.photoAttachmentRatePct)
            && Objects.equals(weatherDistribution, that.weatherDistribution)
            && Objects.equals(averageTemperatureC, that.averageTemperatureC)
            && Objects.equals(amountDistribution, that.amountDistribution)
            && Objects.equals(methodDistribution, that.methodDistribution);
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
        result = 31 * result + Objects.hashCode(amountDistribution);
        result = 31 * result + Objects.hashCode(methodDistribution);
        return result;
    }

    @Override
    public String toString() {
        return "WateringStatistics(" + "recordCount=" + recordCount + ", " + "firstWorkedOn=" + firstWorkedOn + ", " + "lastWorkedOn=" + lastWorkedOn + ", " + "workedDayCount=" + workedDayCount + ", " + "averageIntervalDays=" + averageIntervalDays + ", " + "photoAttachedRecordCount=" + photoAttachedRecordCount + ", " + "photoAttachmentRatePct=" + photoAttachmentRatePct + ", " + "weatherDistribution=" + weatherDistribution + ", " + "averageTemperatureC=" + averageTemperatureC + ", " + "amountDistribution=" + amountDistribution + ", " + "methodDistribution=" + methodDistribution + ")";
    }
}
