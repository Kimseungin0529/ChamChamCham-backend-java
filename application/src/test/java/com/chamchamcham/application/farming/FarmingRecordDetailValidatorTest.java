package com.chamchamcham.application.farming;

import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.chamchamcham.domain.crop.CropUsePartCategory;
import com.chamchamcham.domain.farming.FertilizerAmountUnit;
import com.chamchamcham.domain.farming.PesticideAmountUnit;
import com.chamchamcham.domain.farming.PlantingMethod;
import com.chamchamcham.domain.farming.PropagationMethod;
import com.chamchamcham.domain.farming.SeedAmountUnit;
import com.chamchamcham.domain.farming.SeedlingUnit;
import com.chamchamcham.domain.farming.SprayAmountUnit;
import com.chamchamcham.domain.farming.WorkType;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.*;
import com.chamchamcham.domain.farming.EntryMode;
import com.chamchamcham.domain.farming.HarvestSource;
import org.junit.jupiter.api.DisplayName;

class FarmingRecordDetailValidatorTest {
    private final FarmingRecordDetailValidator validator = new DefaultFarmingRecordDetailValidator();
    private List<FarmingRecordDetailPayload> payloads(WorkType workType, FarmingRecordCommand.PlantingDetail planting, FarmingRecordCommand.WateringDetail watering, FarmingRecordCommand.FertilizingDetail fertilizing, FarmingRecordCommand.PestControlDetail pestControl, FarmingRecordCommand.WeedingDetail weeding, FarmingRecordCommand.HarvestDetail harvest) {
        var memberId = UUID.randomUUID();
        var farmId = UUID.randomUUID();
        var cropId = UUID.randomUUID();
        var workedAt = LocalDateTime.of(2026, 6, 1, 9, 0);
        var create = new FarmingRecordCommand.Create(memberId, farmId, cropId, workType, workedAt, "맑음", 20, "memo", planting, watering, fertilizing, pestControl, weeding, harvest, List.of(), EntryMode.MANUAL);
        var update = new FarmingRecordCommand.Update(memberId, UUID.randomUUID(), farmId, cropId, workType, workedAt, "맑음", 20, "memo", planting, watering, fertilizing, pestControl, weeding, harvest, List.of());
        return java.util.Arrays.asList(create, update);
    }

    @Test
    @DisplayName("planting requires detail")
    void planting_requires_detail() throws Exception {
        payloads(WorkType.PLANTING, null, null, null, null, null, null).forEach(payload -> {
        var exception = assertThrows(BusinessException.class, () -> validator.validate(payload));
        assertEquals(ErrorCode.FARMING_RECORD_DETAIL_REQUIRED, exception.getErrorCode());
        });
    }

    @Test
    @DisplayName("planting accepts seed method with seed amount only")
    void planting_accepts_seed_method_with_seed_amount_only() throws Exception {
        payloads(WorkType.PLANTING, new FarmingRecordCommand.PlantingDetail(PlantingMethod.SEED, BigDecimal.TEN, SeedAmountUnit.G, null, null, null), null, null, null, null, null).forEach(payload -> assertDoesNotThrow(() -> validator.validate(payload)));
    }

    @Test
    @DisplayName("planting rejects seed method without seed amount")
    void planting_rejects_seed_method_without_seed_amount() throws Exception {
        payloads(WorkType.PLANTING, new FarmingRecordCommand.PlantingDetail(PlantingMethod.SEED, null, null, null, null, null), null, null, null, null, null).forEach(payload -> {
        var exception = assertThrows(BusinessException.class, () -> validator.validate(payload));
        assertEquals(ErrorCode.FARMING_RECORD_INVALID_DETAIL, exception.getErrorCode());
        });
    }

    @Test
    @DisplayName("planting rejects seed method with seedling group filled")
    void planting_rejects_seed_method_with_seedling_group_filled() throws Exception {
        payloads(WorkType.PLANTING, new FarmingRecordCommand.PlantingDetail(PlantingMethod.SEED, BigDecimal.TEN, null, 10, SeedlingUnit.JU, null), null, null, null, null, null).forEach(payload -> {
        var exception = assertThrows(BusinessException.class, () -> validator.validate(payload));
        assertEquals(ErrorCode.FARMING_RECORD_INVALID_DETAIL, exception.getErrorCode());
        });
    }

    @Test
    @DisplayName("planting rejects seed method with propagation method filled")
    void planting_rejects_seed_method_with_propagation_method_filled() throws Exception {
        payloads(WorkType.PLANTING, new FarmingRecordCommand.PlantingDetail(PlantingMethod.SEED, BigDecimal.TEN, null, null, null, PropagationMethod.CUTTING), null, null, null, null, null).forEach(payload -> {
        var exception = assertThrows(BusinessException.class, () -> validator.validate(payload));
        assertEquals(ErrorCode.FARMING_RECORD_INVALID_DETAIL, exception.getErrorCode());
        });
    }

    @Test
    @DisplayName("planting accepts seedling method with seedling count only")
    void planting_accepts_seedling_method_with_seedling_count_only() throws Exception {
        payloads(WorkType.PLANTING, new FarmingRecordCommand.PlantingDetail(PlantingMethod.SEEDLING, null, null, 10, SeedlingUnit.JU, null), null, null, null, null, null).forEach(payload -> assertDoesNotThrow(() -> validator.validate(payload)));
    }

    @Test
    @DisplayName("planting accepts seedling method with optional propagation method")
    void planting_accepts_seedling_method_with_optional_propagation_method() throws Exception {
        payloads(WorkType.PLANTING, new FarmingRecordCommand.PlantingDetail(PlantingMethod.SEEDLING, null, null, 10, SeedlingUnit.JU, PropagationMethod.CUTTING), null, null, null, null, null).forEach(payload -> assertDoesNotThrow(() -> validator.validate(payload)));
    }

    @Test
    @DisplayName("planting rejects seedling method without seedling count")
    void planting_rejects_seedling_method_without_seedling_count() throws Exception {
        payloads(WorkType.PLANTING, new FarmingRecordCommand.PlantingDetail(PlantingMethod.SEEDLING, null, null, null, null, PropagationMethod.CUTTING), null, null, null, null, null).forEach(payload -> {
        var exception = assertThrows(BusinessException.class, () -> validator.validate(payload));
        assertEquals(ErrorCode.FARMING_RECORD_INVALID_DETAIL, exception.getErrorCode());
        });
    }

    @Test
    @DisplayName("planting rejects seedling method with seed group filled")
    void planting_rejects_seedling_method_with_seed_group_filled() throws Exception {
        payloads(WorkType.PLANTING, new FarmingRecordCommand.PlantingDetail(PlantingMethod.SEEDLING, BigDecimal.TEN, SeedAmountUnit.G, 10, null, null), null, null, null, null, null).forEach(payload -> {
        var exception = assertThrows(BusinessException.class, () -> validator.validate(payload));
        assertEquals(ErrorCode.FARMING_RECORD_INVALID_DETAIL, exception.getErrorCode());
        });
    }

    @Test
    @DisplayName("fertilizing requires detail")
    void fertilizing_requires_detail() throws Exception {
        payloads(WorkType.FERTILIZING, null, null, null, null, null, null).forEach(payload -> {
        var exception = assertThrows(BusinessException.class, () -> validator.validate(payload));
        assertEquals(ErrorCode.FARMING_RECORD_DETAIL_REQUIRED, exception.getErrorCode());
        });
    }

    @Test
    @DisplayName("fertilizing accepts detail")
    void fertilizing_accepts_detail() throws Exception {
        payloads(WorkType.FERTILIZING, null, null, new FarmingRecordCommand.FertilizingDetail("요소비료", BigDecimal.TEN, FertilizerAmountUnit.G, null), null, null, null).forEach(payload -> assertDoesNotThrow(() -> validator.validate(payload)));
    }

    @Test
    @DisplayName("pest control requires detail")
    void pest_control_requires_detail() throws Exception {
        payloads(WorkType.PEST_CONTROL, null, null, null, null, null, null).forEach(payload -> {
        var exception = assertThrows(BusinessException.class, () -> validator.validate(payload));
        assertEquals(ErrorCode.FARMING_RECORD_DETAIL_REQUIRED, exception.getErrorCode());
        });
    }

    @Test
    @DisplayName("pest control accepts detail")
    void pest_control_accepts_detail() throws Exception {
        payloads(WorkType.PEST_CONTROL, null, null, null, new FarmingRecordCommand.PestControlDetail(UUID.randomUUID(), BigDecimal.ONE, PesticideAmountUnit.ML, BigDecimal.TEN, SprayAmountUnit.ML, null), null, null).forEach(payload -> assertDoesNotThrow(() -> validator.validate(payload)));
    }

    @Test
    @DisplayName("harvest requires detail")
    void harvest_requires_detail() throws Exception {
        payloads(WorkType.HARVEST, null, null, null, null, null, null).forEach(payload -> {
        var exception = assertThrows(BusinessException.class, () -> validator.validate(payload));
        assertEquals(ErrorCode.FARMING_RECORD_DETAIL_REQUIRED, exception.getErrorCode());
        });
    }

    @Test
    @DisplayName("harvest accepts detail with known amount")
    void harvest_accepts_detail_with_known_amount() throws Exception {
        payloads(WorkType.HARVEST, null, null, null, null, null, new FarmingRecordCommand.HarvestDetail(BigDecimal.TEN, false, CropUsePartCategory.ROOT_BARK, HarvestSource.CULTIVATED, 2, false)).forEach(payload -> assertDoesNotThrow(() -> validator.validate(payload)));
    }

    @Test
    @DisplayName("harvest accepts unknown amount as null")
    void harvest_accepts_unknown_amount_as_null() throws Exception {
        payloads(WorkType.HARVEST, null, null, null, null, null, new FarmingRecordCommand.HarvestDetail(null, true, CropUsePartCategory.ROOT_BARK, HarvestSource.CULTIVATED, 2, false)).forEach(payload -> assertDoesNotThrow(() -> validator.validate(payload)));
    }

    @Test
    @DisplayName("harvest rejects missing amount without unknown flag")
    void harvest_rejects_missing_amount_without_unknown_flag() throws Exception {
        payloads(WorkType.HARVEST, null, null, null, null, null, new FarmingRecordCommand.HarvestDetail(null, false, CropUsePartCategory.ROOT_BARK, HarvestSource.CULTIVATED, 2, false)).forEach(payload -> {
        var exception = assertThrows(BusinessException.class, () -> validator.validate(payload));
        assertEquals(ErrorCode.FARMING_RECORD_INVALID_DETAIL, exception.getErrorCode());
        });
    }

    @Test
    @DisplayName("harvest rejects amount together with unknown flag")
    void harvest_rejects_amount_together_with_unknown_flag() throws Exception {
        payloads(WorkType.HARVEST, null, null, null, null, null, new FarmingRecordCommand.HarvestDetail(BigDecimal.TEN, true, CropUsePartCategory.ROOT_BARK, HarvestSource.CULTIVATED, 2, false)).forEach(payload -> {
        var exception = assertThrows(BusinessException.class, () -> validator.validate(payload));
        assertEquals(ErrorCode.FARMING_RECORD_INVALID_DETAIL, exception.getErrorCode());
        });
    }

    @Test
    @DisplayName("harvest accepts null medicinal part")
    void harvest_accepts_null_medicinal_part() throws Exception {
        payloads(WorkType.HARVEST, null, null, null, null, null, new FarmingRecordCommand.HarvestDetail(BigDecimal.TEN, false, null, HarvestSource.CULTIVATED, 2, false)).forEach(payload -> assertDoesNotThrow(() -> validator.validate(payload)));
    }

    @Test
    @DisplayName("watering weeding pruning and etc have no required detail")
    void watering_weeding_pruning_and_etc_have_no_required_detail() throws Exception {
        payloads(WorkType.WATERING, null, null, null, null, null, null).forEach(payload -> assertDoesNotThrow(() -> validator.validate(payload)));
        payloads(WorkType.WEEDING, null, null, null, null, null, null).forEach(payload -> assertDoesNotThrow(() -> validator.validate(payload)));
        payloads(WorkType.PRUNING, null, null, null, null, null, null).forEach(payload -> assertDoesNotThrow(() -> validator.validate(payload)));
        payloads(WorkType.ETC, null, null, null, null, null, null).forEach(payload -> assertDoesNotThrow(() -> validator.validate(payload)));
    }
}
