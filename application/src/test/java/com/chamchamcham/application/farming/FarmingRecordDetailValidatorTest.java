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
import java.util.Arrays;
import java.util.List;
import com.chamchamcham.domain.farming.EntryMode;
import com.chamchamcham.domain.farming.HarvestSource;
import org.junit.jupiter.api.DisplayName;

class FarmingRecordDetailValidatorTest {
    private final FarmingRecordDetailValidator validator = new DefaultFarmingRecordDetailValidator();
    private List<FarmingRecordDetailPayload> payloads(WorkType workType, FarmingRecordCommand.PlantingDetail planting,
        FarmingRecordCommand.WateringDetail watering, FarmingRecordCommand.FertilizingDetail fertilizing,
        FarmingRecordCommand.PestControlDetail pestControl, FarmingRecordCommand.WeedingDetail weeding, FarmingRecordCommand.HarvestDetail harvest) {
        var memberId = UUID.randomUUID();
        var farmId = UUID.randomUUID();
        var cropId = UUID.randomUUID();
        var workedAt = LocalDateTime.of(2026, 6, 1, 9, 0);
        var create = new FarmingRecordCommand.Create(memberId, farmId, cropId, workType, workedAt, "맑음", 20, "memo", planting, watering, fertilizing,
            pestControl, weeding, harvest, List.of(), EntryMode.MANUAL);
        var update = new FarmingRecordCommand.Update(memberId, UUID.randomUUID(), farmId, cropId, workType, workedAt, "맑음", 20, "memo", planting, watering,
            fertilizing, pestControl, weeding, harvest, List.of());
        return java.util.Arrays.asList(create, update);
    }

    @Test
    @DisplayName("planting requires detail")
    void plantingRequiresDetail() throws Exception {
        payloads(WorkType.PLANTING, null, null, null, null, null, null).forEach(payload -> {
            var exception = assertThrows(BusinessException.class, () -> validator.validate(payload));
            assertEquals(ErrorCode.FARMING_RECORD_DETAIL_REQUIRED, exception.getErrorCode());
        });
    }

    @Test
    @DisplayName("planting accepts seed method with seed amount only")
    void plantingAcceptsSeedMethodWithSeedAmountOnly() throws Exception {
        payloads(WorkType.PLANTING, new FarmingRecordCommand.PlantingDetail(PlantingMethod.SEED, BigDecimal.TEN, SeedAmountUnit.G, null, null, null), null,
            null, null, null, null).forEach(payload -> assertDoesNotThrow(() -> validator.validate(payload)));
    }

    @Test
    @DisplayName("planting rejects seed method without seed amount")
    void plantingRejectsSeedMethodWithoutSeedAmount() throws Exception {
        payloads(WorkType.PLANTING, new FarmingRecordCommand.PlantingDetail(PlantingMethod.SEED, null, null, null, null, null), null, null, null, null,
            null).forEach(payload -> {
            var exception = assertThrows(BusinessException.class, () -> validator.validate(payload));
            assertEquals(ErrorCode.FARMING_RECORD_INVALID_DETAIL, exception.getErrorCode());
        });
    }

    @Test
    @DisplayName("planting rejects seed method with seedling group filled")
    void plantingRejectsSeedMethodWithSeedlingGroupFilled() throws Exception {
        payloads(WorkType.PLANTING, new FarmingRecordCommand.PlantingDetail(PlantingMethod.SEED, BigDecimal.TEN, null, 10, SeedlingUnit.JU, null), null,
            null, null, null, null).forEach(payload -> {
            var exception = assertThrows(BusinessException.class, () -> validator.validate(payload));
            assertEquals(ErrorCode.FARMING_RECORD_INVALID_DETAIL, exception.getErrorCode());
        });
    }

    @Test
    @DisplayName("planting rejects seed method with propagation method filled")
    void plantingRejectsSeedMethodWithPropagationMethodFilled() throws Exception {
        payloads(WorkType.PLANTING, new FarmingRecordCommand.PlantingDetail(PlantingMethod.SEED, BigDecimal.TEN, null, null, null,
            PropagationMethod.CUTTING), null, null, null, null, null).forEach(payload -> {
            var exception = assertThrows(BusinessException.class, () -> validator.validate(payload));
            assertEquals(ErrorCode.FARMING_RECORD_INVALID_DETAIL, exception.getErrorCode());
        });
    }

    @Test
    @DisplayName("planting accepts seedling method with seedling count only")
    void plantingAcceptsSeedlingMethodWithSeedlingCountOnly() throws Exception {
        payloads(WorkType.PLANTING, new FarmingRecordCommand.PlantingDetail(PlantingMethod.SEEDLING, null, null, 10, SeedlingUnit.JU, null), null, null,
            null, null, null).forEach(payload -> assertDoesNotThrow(() -> validator.validate(payload)));
    }

    @Test
    @DisplayName("planting accepts seedling method with optional propagation method")
    void plantingAcceptsSeedlingMethodWithOptionalPropagationMethod() throws Exception {
        payloads(WorkType.PLANTING, new FarmingRecordCommand.PlantingDetail(PlantingMethod.SEEDLING, null, null, 10, SeedlingUnit.JU,
            PropagationMethod.CUTTING), null, null, null, null, null).forEach(payload -> assertDoesNotThrow(() -> validator.validate(payload)));
    }

    @Test
    @DisplayName("planting rejects seedling method without seedling count")
    void plantingRejectsSeedlingMethodWithoutSeedlingCount() throws Exception {
        payloads(WorkType.PLANTING, new FarmingRecordCommand.PlantingDetail(PlantingMethod.SEEDLING, null, null, null, null, PropagationMethod.CUTTING),
            null, null, null, null, null).forEach(payload -> {
            var exception = assertThrows(BusinessException.class, () -> validator.validate(payload));
            assertEquals(ErrorCode.FARMING_RECORD_INVALID_DETAIL, exception.getErrorCode());
        });
    }

    @Test
    @DisplayName("planting rejects seedling method with seed group filled")
    void plantingRejectsSeedlingMethodWithSeedGroupFilled() throws Exception {
        payloads(WorkType.PLANTING, new FarmingRecordCommand.PlantingDetail(PlantingMethod.SEEDLING, BigDecimal.TEN, SeedAmountUnit.G, 10, null, null),
            null, null, null, null, null).forEach(payload -> {
            var exception = assertThrows(BusinessException.class, () -> validator.validate(payload));
            assertEquals(ErrorCode.FARMING_RECORD_INVALID_DETAIL, exception.getErrorCode());
        });
    }

    @Test
    @DisplayName("fertilizing requires detail")
    void fertilizingRequiresDetail() throws Exception {
        payloads(WorkType.FERTILIZING, null, null, null, null, null, null).forEach(payload -> {
            var exception = assertThrows(BusinessException.class, () -> validator.validate(payload));
            assertEquals(ErrorCode.FARMING_RECORD_DETAIL_REQUIRED, exception.getErrorCode());
        });
    }

    @Test
    @DisplayName("fertilizing accepts detail")
    void fertilizingAcceptsDetail() throws Exception {
        payloads(WorkType.FERTILIZING, null, null, new FarmingRecordCommand.FertilizingDetail("요소비료", BigDecimal.TEN, FertilizerAmountUnit.G, null), null,
            null, null).forEach(payload -> assertDoesNotThrow(() -> validator.validate(payload)));
    }

    @Test
    @DisplayName("pest control requires detail")
    void pestControlRequiresDetail() throws Exception {
        payloads(WorkType.PEST_CONTROL, null, null, null, null, null, null).forEach(payload -> {
            var exception = assertThrows(BusinessException.class, () -> validator.validate(payload));
            assertEquals(ErrorCode.FARMING_RECORD_DETAIL_REQUIRED, exception.getErrorCode());
        });
    }

    @Test
    @DisplayName("pest control accepts detail")
    void pestControlAcceptsDetail() throws Exception {
        payloads(WorkType.PEST_CONTROL, null, null, null, new FarmingRecordCommand.PestControlDetail(UUID.randomUUID(), BigDecimal.ONE,
            PesticideAmountUnit.ML, BigDecimal.TEN, SprayAmountUnit.ML, null), null,
            null).forEach(payload -> assertDoesNotThrow(() -> validator.validate(payload)));
    }

    @Test
    @DisplayName("harvest requires detail")
    void harvestRequiresDetail() throws Exception {
        payloads(WorkType.HARVEST, null, null, null, null, null, null).forEach(payload -> {
            var exception = assertThrows(BusinessException.class, () -> validator.validate(payload));
            assertEquals(ErrorCode.FARMING_RECORD_DETAIL_REQUIRED, exception.getErrorCode());
        });
    }

    @Test
    @DisplayName("harvest accepts detail with known amount")
    void harvestAcceptsDetailWithKnownAmount() throws Exception {
        payloads(WorkType.HARVEST, null, null, null, null, null, new FarmingRecordCommand.HarvestDetail(BigDecimal.TEN, false,
            CropUsePartCategory.ROOT_BARK, HarvestSource.CULTIVATED, 2, false)).forEach(payload -> assertDoesNotThrow(() -> validator.validate(payload)));
    }

    @Test
    @DisplayName("harvest accepts unknown amount as null")
    void harvestAcceptsUnknownAmountAsNull() throws Exception {
        payloads(WorkType.HARVEST, null, null, null, null, null, new FarmingRecordCommand.HarvestDetail(null, true, CropUsePartCategory.ROOT_BARK,
            HarvestSource.CULTIVATED, 2, false)).forEach(payload -> assertDoesNotThrow(() -> validator.validate(payload)));
    }

    @Test
    @DisplayName("harvest rejects missing amount without unknown flag")
    void harvestRejectsMissingAmountWithoutUnknownFlag() throws Exception {
        payloads(WorkType.HARVEST, null, null, null, null, null, new FarmingRecordCommand.HarvestDetail(null, false, CropUsePartCategory.ROOT_BARK,
            HarvestSource.CULTIVATED, 2, false)).forEach(payload -> {
            var exception = assertThrows(BusinessException.class, () -> validator.validate(payload));
            assertEquals(ErrorCode.FARMING_RECORD_INVALID_DETAIL, exception.getErrorCode());
        });
    }

    @Test
    @DisplayName("harvest rejects amount together with unknown flag")
    void harvestRejectsAmountTogetherWithUnknownFlag() throws Exception {
        payloads(WorkType.HARVEST, null, null, null, null, null, new FarmingRecordCommand.HarvestDetail(BigDecimal.TEN, true,
            CropUsePartCategory.ROOT_BARK, HarvestSource.CULTIVATED, 2, false)).forEach(payload -> {
            var exception = assertThrows(BusinessException.class, () -> validator.validate(payload));
            assertEquals(ErrorCode.FARMING_RECORD_INVALID_DETAIL, exception.getErrorCode());
        });
    }

    @Test
    @DisplayName("harvest accepts null medicinal part")
    void harvestAcceptsNullMedicinalPart() throws Exception {
        payloads(WorkType.HARVEST, null, null, null, null, null, new FarmingRecordCommand.HarvestDetail(BigDecimal.TEN, false, null,
            HarvestSource.CULTIVATED, 2, false)).forEach(payload -> assertDoesNotThrow(() -> validator.validate(payload)));
    }

    @Test
    @DisplayName("watering weeding pruning and etc have no required detail")
    void wateringWeedingPruningAndEtcHaveNoRequiredDetail() throws Exception {
        payloads(WorkType.WATERING, null, null, null, null, null, null).forEach(payload -> assertDoesNotThrow(() -> validator.validate(payload)));
        payloads(WorkType.WEEDING, null, null, null, null, null, null).forEach(payload -> assertDoesNotThrow(() -> validator.validate(payload)));
        payloads(WorkType.PRUNING, null, null, null, null, null, null).forEach(payload -> assertDoesNotThrow(() -> validator.validate(payload)));
        payloads(WorkType.ETC, null, null, null, null, null, null).forEach(payload -> assertDoesNotThrow(() -> validator.validate(payload)));
    }

    @Test
    void harvestJsonRetainsOmittedDefaultsAndItsBooleanFieldName() throws Exception {
        var mapper = contractMapper();
        var detail = mapper.readValue("{\"harvestAmount\":10,\"growthPeriod\":2,\"isLastHarvest\":true}", FarmingRecordCommand.HarvestDetail.class);
        assertEquals(HarvestSource.CULTIVATED, detail.getHarvestSource());
        assertEquals(false, detail.getAmountUnknown());
        assertEquals(null, detail.getMedicinalPart());
        assertEquals(true, detail.isLastHarvest());
        var json = mapper.valueToTree(detail);
        assertEquals(true, json.get("isLastHarvest").booleanValue());
        assertEquals(false, json.has("lastHarvest"));
        assertThrows(com.fasterxml.jackson.core.JsonProcessingException.class, () -> mapper.readValue(
            "{\"harvestAmount\":10,\"growthPeriod\":2,\"isLastHarvest\":true,\"harvestSource\":null}", FarmingRecordCommand.HarvestDetail.class));
    }

    @Test
    void createAndUpdateJsonRetainCollectionAndEntryModeDefaults() throws Exception {
        var mapper = contractMapper();
        var member = "00000000-0000-0000-0000-000000000001";
        var common = "\"memberId\":\"" + member + "\",\"farmId\":\"" + member + "\",\"cropId\":\"" + member
            + "\",\"workType\":\"PRUNING\",\"workedAt\":\"2026-06-01T09:00:00\",\"weatherCondition\":\"맑음\",\"weatherTemperature\":20,\"memo\":\"memo\"";
        var create = mapper.readValue("{" + common + "}", FarmingRecordCommand.Create.class);
        assertEquals(List.of(), create.getMediaIds());
        assertEquals(EntryMode.MANUAL, create.getEntryMode());
        var update = mapper.readValue("{" + common + ",\"recordId\":\"" + member + "\"}", FarmingRecordCommand.Update.class);
        assertEquals(List.of(), update.getMediaIds());
        assertThrows(com.fasterxml.jackson.core.JsonProcessingException.class, () -> mapper.readValue(
            "{" + common + ",\"mediaIds\":null}", FarmingRecordCommand.Create.class));
        assertThrows(com.fasterxml.jackson.core.JsonProcessingException.class, () -> mapper.readValue(
            "{" + common + ",\"entryMode\":null}", FarmingRecordCommand.Create.class));
    }

    @Test
    void harvestJsonRetainsKotlinPrimitiveMissingAndNullCoercion() throws Exception {
        var mapper = contractMapper();
        for (var json : List.of(
            "{\"harvestAmount\":10}",
            "{\"harvestAmount\":10,\"growthPeriod\":null,\"isLastHarvest\":null,\"amountUnknown\":null}")) {
            var detail = mapper.readValue(json, FarmingRecordCommand.HarvestDetail.class);
            assertEquals(0, detail.getGrowthPeriod());
            assertEquals(false, detail.isLastHarvest());
            assertEquals(false, detail.getAmountUnknown());
        }
        var detail = mapper.readValue("{\"harvestAmount\":10,\"growthPeriod\":2,\"isLastHarvest\":true,\"amountUnknown\":null}",
            FarmingRecordCommand.HarvestDetail.class);
        assertEquals(2, detail.getGrowthPeriod());
        assertEquals(true, detail.isLastHarvest());
        assertEquals(false, detail.getAmountUnknown());
    }

    private com.fasterxml.jackson.databind.ObjectMapper contractMapper() {
        return new com.fasterxml.jackson.databind.ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
    }
}
