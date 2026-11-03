package com.chamchamcham.domain.report;

import java.util.List;
import java.util.Objects;
import java.math.BigDecimal;
import java.time.LocalDate;

public final class PlantingStatistics {
    private int recordCount;
    private LocalDate firstWorkedOn;
    private LocalDate lastWorkedOn;
    private int workedDayCount;
    private BigDecimal averageIntervalDays;
    private int photoAttachedRecordCount;
    private BigDecimal photoAttachmentRatePct;
    private List<CountDistribution> weatherDistribution;
    private BigDecimal averageTemperatureC;
    private List<CountDistribution> plantingMethodDistribution;
    private List<PropagationStatistics> propagationMethods;

    public PlantingStatistics() {
        this.recordCount = 0;
        this.firstWorkedOn = null;
        this.lastWorkedOn = null;
        this.workedDayCount = 0;
        this.averageIntervalDays = null;
        this.photoAttachedRecordCount = 0;
        this.photoAttachmentRatePct = null;
        this.weatherDistribution = List.of();
        this.averageTemperatureC = null;
        this.plantingMethodDistribution = List.of();
        this.propagationMethods = List.of();
    }

    public PlantingStatistics(
        int recordCount,
        LocalDate firstWorkedOn,
        LocalDate lastWorkedOn,
        int workedDayCount,
        BigDecimal averageIntervalDays,
        int photoAttachedRecordCount,
        BigDecimal photoAttachmentRatePct,
        List<CountDistribution> weatherDistribution,
        BigDecimal averageTemperatureC,
        List<CountDistribution> plantingMethodDistribution,
        List<PropagationStatistics> propagationMethods
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
        this.plantingMethodDistribution = Objects.requireNonNull(plantingMethodDistribution, "plantingMethodDistribution");
        this.propagationMethods = Objects.requireNonNull(propagationMethods, "propagationMethods");
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

    public List<CountDistribution> getPlantingMethodDistribution() { return plantingMethodDistribution; }

    private void setPlantingMethodDistribution(List<CountDistribution> plantingMethodDistribution) { this.plantingMethodDistribution = Objects.requireNonNull(plantingMethodDistribution, "plantingMethodDistribution"); }

    public List<PropagationStatistics> getPropagationMethods() { return propagationMethods; }

    private void setPropagationMethods(List<PropagationStatistics> propagationMethods) { this.propagationMethods = Objects.requireNonNull(propagationMethods, "propagationMethods"); }

    public static PlantingStatistics empty() { return new PlantingStatistics(); }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof PlantingStatistics that)) return false;
        return recordCount == that.recordCount
            && Objects.equals(firstWorkedOn, that.firstWorkedOn)
            && Objects.equals(lastWorkedOn, that.lastWorkedOn)
            && workedDayCount == that.workedDayCount
            && Objects.equals(averageIntervalDays, that.averageIntervalDays)
            && photoAttachedRecordCount == that.photoAttachedRecordCount
            && Objects.equals(photoAttachmentRatePct, that.photoAttachmentRatePct)
            && Objects.equals(weatherDistribution, that.weatherDistribution)
            && Objects.equals(averageTemperatureC, that.averageTemperatureC)
            && Objects.equals(plantingMethodDistribution, that.plantingMethodDistribution)
            && Objects.equals(propagationMethods, that.propagationMethods);
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
        result = 31 * result + Objects.hashCode(plantingMethodDistribution);
        result = 31 * result + Objects.hashCode(propagationMethods);
        return result;
    }

    @Override
    public String toString() {
        return "PlantingStatistics(" + "recordCount=" + recordCount + ", " + "firstWorkedOn=" + firstWorkedOn + ", " + "lastWorkedOn=" + lastWorkedOn + ", " + "workedDayCount=" + workedDayCount + ", " + "averageIntervalDays=" + averageIntervalDays + ", " + "photoAttachedRecordCount=" + photoAttachedRecordCount + ", " + "photoAttachmentRatePct=" + photoAttachmentRatePct + ", " + "weatherDistribution=" + weatherDistribution + ", " + "averageTemperatureC=" + averageTemperatureC + ", " + "plantingMethodDistribution=" + plantingMethodDistribution + ", " + "propagationMethods=" + propagationMethods + ")";
    }
}
