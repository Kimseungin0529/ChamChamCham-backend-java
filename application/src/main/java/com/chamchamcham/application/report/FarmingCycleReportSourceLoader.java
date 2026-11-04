package com.chamchamcham.application.report;

import com.chamchamcham.domain.crop.CropUsePartCategory;
import com.chamchamcham.domain.farming.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Component
public class FarmingCycleReportSourceLoader {
    private final FarmingCycleReportSourceRepository repository;

    public FarmingCycleReportSourceLoader(FarmingCycleReportSourceRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    @Transactional(readOnly = true)
    public List<CycleReportSourceRecord> load(ReportScope scope) {
        var snapshot = repository.load(scope.getMemberId(), scope.getFarmId(), scope.getCropId());
        return snapshot.records().stream().map(record -> {
            var recordId = requireId(record.getId(), "Persisted farming record id is required");
            return new CycleReportSourceRecord(recordId, record.getWorkedAt(), record.getCreatedAt(), record.getWorkType(),
                record.getWeatherCondition(), record.getWeatherTemperature(), snapshot.mediaRecordIds().contains(recordId), record.getMemo(),
                record.getWorkType() == WorkType.PLANTING ? normalizePlanting(required(snapshot.plantingByRecordId(), recordId)) : null,
                record.getWorkType() == WorkType.WATERING && snapshot.wateringByRecordId().get(recordId) != null
                    ? normalizeWatering(snapshot.wateringByRecordId().get(recordId)) : null,
                record.getWorkType() == WorkType.FERTILIZING ? normalizeFertilizing(required(snapshot.fertilizingByRecordId(), recordId)) : null,
                record.getWorkType() == WorkType.PEST_CONTROL ? normalizePestControl(required(snapshot.pestControlByRecordId(), recordId)) : null,
                record.getWorkType() == WorkType.WEEDING && snapshot.weedingByRecordId().get(recordId) != null
                    ? normalizeWeeding(snapshot.weedingByRecordId().get(recordId)) : null,
                record.getWorkType() == WorkType.HARVEST ? normalizeHarvest(required(snapshot.harvestByRecordId(), recordId)) : null);
        }).toList();
    }

    private <T> T required(Map<UUID, T> details, UUID recordId) {
        var detail = details.get(recordId);
        if (detail == null) throw new IllegalStateException("Missing detail for record " + recordId);
        return detail;
    }

    private PlantingReportSource normalizePlanting(PlantingRecord detail) {
        if (detail.getPlantingMethod() == PlantingMethod.SEED) {
            return new PlantingReportSource(toRef(detail.getPlantingMethod()), toRef(detail.getPropagationMethod()),
                scale4(detail.getSeedAmount()), detail.getSeedAmountUnit() == null ? null : detail.getSeedAmountUnit().name());
        }
        return new PlantingReportSource(toRef(detail.getPlantingMethod()), toRef(detail.getPropagationMethod()),
            detail.getSeedlingCount() == null ? null : scale4(BigDecimal.valueOf(detail.getSeedlingCount())),
            detail.getSeedlingCount() == null ? null : "JU");
    }

    private WateringReportSource normalizeWatering(WateringRecord detail) {
        return new WateringReportSource(toRef(detail.getIrrigationAmount()), toRef(detail.getIrrigationMethod()));
    }

    private FertilizingReportSource normalizeFertilizing(FertilizingRecord detail) {
        return new FertilizingReportSource(detail.getMaterialName(), scale4(detail.getAmount()), detail.getAmountUnit().name(),
            detail.getAmountUnit() == FertilizerAmountUnit.G ? detail.getAmount().divide(new BigDecimal("1000")) : null,
            toRef(detail.getApplicationMethod()));
    }

    private PestControlReportSource normalizePestControl(PestControlRecord detail) {
        return new PestControlReportSource(requireId(detail.getPesticide().getId(), "Persisted pesticide id is required"),
            detail.getPesticide().getBrandName(), scale4(detail.getPesticideAmount()), detail.getPesticideAmountUnit().name(),
            switch (detail.getTotalSprayAmountUnit()) { case ML -> scale4(detail.getTotalSprayAmount().divide(new BigDecimal("1000"))); },
            detail.getPest() == null ? null : detail.getPest().getName());
    }

    private WeedingReportSource normalizeWeeding(WeedingRecord detail) {
        return new WeedingReportSource(toRef(detail.getWeedingMethod()));
    }

    private HarvestReportSource normalizeHarvest(HarvestRecord detail) {
        return new HarvestReportSource(scale4(detail.getHarvestAmount()), toRef(detail.getMedicinalPart()), toRef(detail.getHarvestSource()),
            detail.getGrowthPeriod(), "MONTH", detail.getGrowthPeriod(), detail.isLastHarvest());
    }

    private BigDecimal scale4(BigDecimal value) { return value == null ? null : value.setScale(4, RoundingMode.HALF_UP); }

    private UUID requireId(UUID id, String message) {
        if (id == null) throw new IllegalArgumentException(message);
        return id;
    }

    private CategoryRef toRef(PlantingMethod value) {
        return value == null ? null : new CategoryRef(value.name(), value.getLabel());
    }

    private CategoryRef toRef(PropagationMethod value) {
        return value == null ? null : new CategoryRef(value.name(), value.getLabel());
    }

    private CategoryRef toRef(IrrigationAmount value) {
        return value == null ? null : new CategoryRef(value.name(), value.getLabel());
    }

    private CategoryRef toRef(IrrigationMethod value) {
        return value == null ? null : new CategoryRef(value.name(), value.getLabel());
    }

    private CategoryRef toRef(FertilizingMethod value) {
        return value == null ? null : new CategoryRef(value.name(), value.getLabel());
    }

    private CategoryRef toRef(WeedingMethod value) {
        return value == null ? null : new CategoryRef(value.name(), value.getLabel());
    }

    private CategoryRef toRef(CropUsePartCategory value) {
        return value == null ? null : new CategoryRef(value.name(), value.getLabel());
    }

    private CategoryRef toRef(HarvestSource value) {
        return value == null ? null : new CategoryRef(value.name(), value.getLabel());
    }
}
