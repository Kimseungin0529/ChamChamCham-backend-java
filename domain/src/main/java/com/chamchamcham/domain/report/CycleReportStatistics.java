package com.chamchamcham.domain.report;

import java.util.Objects;
import java.time.LocalDate;
import com.chamchamcham.domain.farming.WorkType;

public final class CycleReportStatistics {
    private PlantingStatistics planting;
    private WateringStatistics watering;
    private FertilizingStatistics fertilizing;
    private PestControlStatistics pestControl;
    private WeedingStatistics weeding;
    private CommonOnlyStatistics pruning;
    private HarvestStatistics harvest;
    private CommonOnlyStatistics etc;

    public CycleReportStatistics() {
        this.planting = PlantingStatistics.empty();
        this.watering = WateringStatistics.empty();
        this.fertilizing = FertilizingStatistics.empty();
        this.pestControl = PestControlStatistics.empty();
        this.weeding = WeedingStatistics.empty();
        this.pruning = CommonOnlyStatistics.empty();
        this.harvest = HarvestStatistics.empty();
        this.etc = CommonOnlyStatistics.empty();
    }

    public CycleReportStatistics(
        PlantingStatistics planting,
        WateringStatistics watering,
        FertilizingStatistics fertilizing,
        PestControlStatistics pestControl,
        WeedingStatistics weeding,
        CommonOnlyStatistics pruning,
        HarvestStatistics harvest,
        CommonOnlyStatistics etc
    ) {
        this.planting = Objects.requireNonNull(planting, "planting");
        this.watering = Objects.requireNonNull(watering, "watering");
        this.fertilizing = Objects.requireNonNull(fertilizing, "fertilizing");
        this.pestControl = Objects.requireNonNull(pestControl, "pestControl");
        this.weeding = Objects.requireNonNull(weeding, "weeding");
        this.pruning = Objects.requireNonNull(pruning, "pruning");
        this.harvest = Objects.requireNonNull(harvest, "harvest");
        this.etc = Objects.requireNonNull(etc, "etc");
    }

    public PlantingStatistics getPlanting() { return planting; }

    private void setPlanting(PlantingStatistics planting) { this.planting = Objects.requireNonNull(planting, "planting"); }

    public WateringStatistics getWatering() { return watering; }

    private void setWatering(WateringStatistics watering) { this.watering = Objects.requireNonNull(watering, "watering"); }

    public FertilizingStatistics getFertilizing() { return fertilizing; }

    private void setFertilizing(FertilizingStatistics fertilizing) { this.fertilizing = Objects.requireNonNull(fertilizing, "fertilizing"); }

    public PestControlStatistics getPestControl() { return pestControl; }

    private void setPestControl(PestControlStatistics pestControl) { this.pestControl = Objects.requireNonNull(pestControl, "pestControl"); }

    public WeedingStatistics getWeeding() { return weeding; }

    private void setWeeding(WeedingStatistics weeding) { this.weeding = Objects.requireNonNull(weeding, "weeding"); }

    public CommonOnlyStatistics getPruning() { return pruning; }

    private void setPruning(CommonOnlyStatistics pruning) { this.pruning = Objects.requireNonNull(pruning, "pruning"); }

    public HarvestStatistics getHarvest() { return harvest; }

    private void setHarvest(HarvestStatistics harvest) { this.harvest = Objects.requireNonNull(harvest, "harvest"); }

    public CommonOnlyStatistics getEtc() { return etc; }

    private void setEtc(CommonOnlyStatistics etc) { this.etc = Objects.requireNonNull(etc, "etc"); }

    public static CycleReportStatistics empty() { return new CycleReportStatistics(); }

    public int recordCountFor(WorkType workType) {
        return switch (workType) {
            case PLANTING -> planting.getRecordCount();
            case WATERING -> watering.getRecordCount();
            case FERTILIZING -> fertilizing.getRecordCount();
            case PEST_CONTROL -> pestControl.getRecordCount();
            case WEEDING -> weeding.getRecordCount();
            case PRUNING -> pruning.getRecordCount();
            case HARVEST -> harvest.getRecordCount();
            case ETC -> etc.getRecordCount();
        };
    }

    public LocalDate lastWorkedOnFor(WorkType workType) {
        return switch (workType) {
            case PLANTING -> planting.getLastWorkedOn();
            case WATERING -> watering.getLastWorkedOn();
            case FERTILIZING -> fertilizing.getLastWorkedOn();
            case PEST_CONTROL -> pestControl.getLastWorkedOn();
            case WEEDING -> weeding.getLastWorkedOn();
            case PRUNING -> pruning.getLastWorkedOn();
            case HARVEST -> harvest.getLastWorkedOn();
            case ETC -> etc.getLastWorkedOn();
        };
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof CycleReportStatistics that)) return false;
        return Objects.equals(planting, that.planting)
            && Objects.equals(watering, that.watering)
            && Objects.equals(fertilizing, that.fertilizing)
            && Objects.equals(pestControl, that.pestControl)
            && Objects.equals(weeding, that.weeding)
            && Objects.equals(pruning, that.pruning)
            && Objects.equals(harvest, that.harvest)
            && Objects.equals(etc, that.etc);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(planting);
        result = 31 * result + Objects.hashCode(watering);
        result = 31 * result + Objects.hashCode(fertilizing);
        result = 31 * result + Objects.hashCode(pestControl);
        result = 31 * result + Objects.hashCode(weeding);
        result = 31 * result + Objects.hashCode(pruning);
        result = 31 * result + Objects.hashCode(harvest);
        result = 31 * result + Objects.hashCode(etc);
        return result;
    }

    @Override
    public String toString() {
        return "CycleReportStatistics(" + "planting=" + planting + ", " + "watering=" + watering + ", " + "fertilizing=" + fertilizing + ", " + "pestControl=" + pestControl + ", " + "weeding=" + weeding + ", " + "pruning=" + pruning + ", " + "harvest=" + harvest + ", " + "etc=" + etc + ")";
    }
}
