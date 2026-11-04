package com.chamchamcham.application.farming;

import com.chamchamcham.domain.crop.CropUsePartCategory;
import com.chamchamcham.domain.farming.FertilizerAmountUnit;
import com.chamchamcham.domain.farming.FertilizingMethod;
import com.chamchamcham.domain.farming.HarvestSource;
import com.chamchamcham.domain.farming.IrrigationAmount;
import com.chamchamcham.domain.farming.IrrigationMethod;
import com.chamchamcham.domain.farming.PesticideAmountUnit;
import com.chamchamcham.domain.farming.PlantingMethod;
import com.chamchamcham.domain.farming.PropagationMethod;
import com.chamchamcham.domain.farming.SeedAmountUnit;
import com.chamchamcham.domain.farming.SeedlingUnit;
import com.chamchamcham.domain.farming.SprayAmountUnit;
import com.chamchamcham.domain.farming.WeedingMethod;
import com.chamchamcham.domain.farming.WorkType;
import java.util.List;
import java.util.Arrays;

// API의 작업 상세 요청 필드와 함께 수동으로 맞추는 카탈로그다.
public final class WorkTypeFieldCatalog {
    private WorkTypeFieldCatalog() {}

    public static List<WorkTypeResult.FieldSummary> fieldsFor(WorkType workType) {
        return switch (workType) {
            case PLANTING -> PLANTING_FIELDS;
            case WATERING -> WATERING_FIELDS;
            case FERTILIZING -> FERTILIZING_FIELDS;
            case PEST_CONTROL -> PEST_CONTROL_FIELDS;
            case WEEDING -> WEEDING_FIELDS;
            case HARVEST -> HARVEST_FIELDS;
            case PRUNING, ETC -> List.of();
        };
    }

    private static final List<WorkTypeResult.FieldSummary> PLANTING_FIELDS = java.util.Arrays.asList(new WorkTypeResult.FieldSummary("plantingMethod", FieldValueType.ENUM, true, Arrays.asList(PlantingMethod.values()).stream().map(it -> new WorkTypeResult.EnumOptionSummary(it.name(), it.getLabel())).toList()), new WorkTypeResult.FieldSummary("seedAmount", FieldValueType.DECIMAL, false, List.of()), new WorkTypeResult.FieldSummary("seedAmountUnit", FieldValueType.ENUM, false, Arrays.asList(SeedAmountUnit.values()).stream().map(it -> new WorkTypeResult.EnumOptionSummary(it.name(), it.getLabel())).toList()), new WorkTypeResult.FieldSummary("seedlingCount", FieldValueType.INT, false, List.of()), new WorkTypeResult.FieldSummary("seedlingUnit", FieldValueType.ENUM, false, Arrays.asList(SeedlingUnit.values()).stream().map(it -> new WorkTypeResult.EnumOptionSummary(it.name(), it.getLabel())).toList()), new WorkTypeResult.FieldSummary("propagationMethod", FieldValueType.ENUM, false, Arrays.asList(PropagationMethod.values()).stream().map(it -> new WorkTypeResult.EnumOptionSummary(it.name(), it.getLabel())).toList()));

    private static final List<WorkTypeResult.FieldSummary> WATERING_FIELDS = java.util.Arrays.asList(new WorkTypeResult.FieldSummary("irrigationAmount", FieldValueType.ENUM, false, Arrays.asList(IrrigationAmount.values()).stream().map(it -> new WorkTypeResult.EnumOptionSummary(it.name(), it.getLabel())).toList()), new WorkTypeResult.FieldSummary("irrigationMethod", FieldValueType.ENUM, false, Arrays.asList(IrrigationMethod.values()).stream().map(it -> new WorkTypeResult.EnumOptionSummary(it.name(), it.getLabel())).toList()));

    private static final List<WorkTypeResult.FieldSummary> FERTILIZING_FIELDS = java.util.Arrays.asList(new WorkTypeResult.FieldSummary("materialName", FieldValueType.STRING, true, List.of()), new WorkTypeResult.FieldSummary("amount", FieldValueType.DECIMAL, true, List.of()), new WorkTypeResult.FieldSummary("amountUnit", FieldValueType.ENUM, true, Arrays.asList(FertilizerAmountUnit.values()).stream().map(it -> new WorkTypeResult.EnumOptionSummary(it.name(), it.getLabel())).toList()), new WorkTypeResult.FieldSummary("applicationMethod", FieldValueType.ENUM, false, Arrays.asList(FertilizingMethod.values()).stream().map(it -> new WorkTypeResult.EnumOptionSummary(it.name(), it.getLabel())).toList()));

    private static final List<WorkTypeResult.FieldSummary> PEST_CONTROL_FIELDS = java.util.Arrays.asList(new WorkTypeResult.FieldSummary("pesticideId", FieldValueType.STRING, true, List.of()), new WorkTypeResult.FieldSummary("pesticideAmount", FieldValueType.DECIMAL, true, List.of()), new WorkTypeResult.FieldSummary("pesticideAmountUnit", FieldValueType.ENUM, true, Arrays.asList(PesticideAmountUnit.values()).stream().map(it -> new WorkTypeResult.EnumOptionSummary(it.name(), it.getLabel())).toList()), new WorkTypeResult.FieldSummary("totalSprayAmount", FieldValueType.DECIMAL, true, List.of()), new WorkTypeResult.FieldSummary("totalSprayAmountUnit", FieldValueType.ENUM, true, Arrays.asList(SprayAmountUnit.values()).stream().map(it -> new WorkTypeResult.EnumOptionSummary(it.name(), it.getLabel())).toList()), new WorkTypeResult.FieldSummary("pestId", FieldValueType.STRING, false, List.of()));

    private static final List<WorkTypeResult.FieldSummary> WEEDING_FIELDS = java.util.Arrays.asList(new WorkTypeResult.FieldSummary("weedingMethod", FieldValueType.ENUM, false, Arrays.asList(WeedingMethod.values()).stream().map(it -> new WorkTypeResult.EnumOptionSummary(it.name(), it.getLabel())).toList()));

    private static final List<WorkTypeResult.FieldSummary> HARVEST_FIELDS = java.util.Arrays.asList(new WorkTypeResult.FieldSummary("harvestAmount", FieldValueType.DECIMAL, true, List.of()), new WorkTypeResult.FieldSummary("medicinalPart", FieldValueType.ENUM, false, Arrays.asList(CropUsePartCategory.values()).stream().map(it -> new WorkTypeResult.EnumOptionSummary(it.name(), it.getLabel())).toList()), new WorkTypeResult.FieldSummary("harvestSource", FieldValueType.ENUM, false, Arrays.asList(HarvestSource.values()).stream().map(it -> new WorkTypeResult.EnumOptionSummary(it.name(), it.getLabel())).toList()), new WorkTypeResult.FieldSummary("growthPeriod", FieldValueType.INT, true, List.of()));
}
