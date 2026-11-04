package com.chamchamcham.application.farming;

import com.chamchamcham.domain.farming.WorkType;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.*;
import com.chamchamcham.domain.farming.EntryMode;
import com.chamchamcham.domain.farming.HarvestSource;
import org.junit.jupiter.api.DisplayName;

class WorkTypeCatalogServiceTest {
    private WorkTypeCatalogService service;

    @BeforeEach
    void setUp() throws Exception {
        service = new WorkTypeCatalogService();
    }

    @Test
    @DisplayName("listWorkTypes returns all work types in declaration order")
    void listWorkTypes_returns_all_work_types_in_declaration_order() throws Exception {
        var result = service.listWorkTypes();
        assertThat(result.stream().map(it -> it.getCode()).toList()).containsExactlyElementsOf(Arrays.asList(WorkType.values()).stream().map(it -> it.name()).toList());
    }

    @Test
    @DisplayName("PLANTING has required detail with plantingMethod as the only required field")
    void PLANTING_has_required_detail_with_plantingMethod_as_the_only_required_field() throws Exception {
        var planting = service.listWorkTypes().stream().filter(it -> Objects.equals(it.getCode(), "PLANTING")).findFirst().orElseThrow();
        assertThat(planting.getDetailRequired()).isTrue();
        assertThat(planting.getFields().stream().map(it -> it.getName()).toList()).containsExactly("plantingMethod", "seedAmount", "seedAmountUnit", "seedlingCount", "seedlingUnit", "propagationMethod");
        assertThat(planting.getFields().stream().filter(it -> it.getRequired()).toList().stream().map(it -> it.getName()).toList()).containsExactly("plantingMethod");
        var plantingMethodField = planting.getFields().stream().filter(it -> Objects.equals(it.getName(), "plantingMethod")).findFirst().orElseThrow();
        assertThat(plantingMethodField.getType()).isEqualTo(FieldValueType.ENUM);
        assertThat(plantingMethodField.getOptions()).containsExactly(new WorkTypeResult.EnumOptionSummary("SEED", "씨앗 심기"), new WorkTypeResult.EnumOptionSummary("SEEDLING", "모종 심기"));
        var propagationMethodField = planting.getFields().stream().filter(it -> Objects.equals(it.getName(), "propagationMethod")).findFirst().orElseThrow();
        assertThat(propagationMethodField.getRequired()).isFalse();
        assertThat(propagationMethodField.getOptions()).containsExactly(new WorkTypeResult.EnumOptionSummary("CUTTING", "꺾꽂이"), new WorkTypeResult.EnumOptionSummary("GRAFTING", "접붙이기"), new WorkTypeResult.EnumOptionSummary("LAYERING", "휘묻이"), new WorkTypeResult.EnumOptionSummary("DIVISION", "포기나누기"), new WorkTypeResult.EnumOptionSummary("TISSUE_CULTURE", "조직 배양"), new WorkTypeResult.EnumOptionSummary("PURCHASED", "시판 구매"));
    }

    @Test
    @DisplayName("WATERING has optional detail with two optional enum fields")
    void WATERING_has_optional_detail_with_two_optional_enum_fields() throws Exception {
        var watering = service.listWorkTypes().stream().filter(it -> Objects.equals(it.getCode(), "WATERING")).findFirst().orElseThrow();
        assertThat(watering.getDetailRequired()).isFalse();
        assertThat(watering.getFields().stream().map(it -> it.getName()).toList()).containsExactly("irrigationAmount", "irrigationMethod");
        assertThat(watering.getFields()).allMatch(it -> !it.getRequired() && Objects.equals(it.getType(), FieldValueType.ENUM));
    }

    @Test
    @DisplayName("FERTILIZING has required detail with three required fields")
    void FERTILIZING_has_required_detail_with_three_required_fields() throws Exception {
        var fertilizing = service.listWorkTypes().stream().filter(it -> Objects.equals(it.getCode(), "FERTILIZING")).findFirst().orElseThrow();
        assertThat(fertilizing.getDetailRequired()).isTrue();
        assertThat(fertilizing.getFields().stream().filter(it -> it.getRequired()).toList().stream().map(it -> it.getName()).toList()).containsExactly("materialName", "amount", "amountUnit");
        assertThat(fertilizing.getFields().stream().filter(it -> Objects.equals(it.getName(), "applicationMethod")).findFirst().orElseThrow().getRequired()).isFalse();
    }

    @Test
    @DisplayName("PEST_CONTROL has required detail with five required fields")
    void PEST_CONTROL_has_required_detail_with_five_required_fields() throws Exception {
        var pestControl = service.listWorkTypes().stream().filter(it -> Objects.equals(it.getCode(), "PEST_CONTROL")).findFirst().orElseThrow();
        assertThat(pestControl.getDetailRequired()).isTrue();
        assertThat(pestControl.getFields().stream().filter(it -> it.getRequired()).toList().stream().map(it -> it.getName()).toList()).containsExactly("pesticideId", "pesticideAmount", "pesticideAmountUnit", "totalSprayAmount", "totalSprayAmountUnit");
        assertThat(pestControl.getFields().stream().filter(it -> Objects.equals(it.getName(), "pestId")).findFirst().orElseThrow().getRequired()).isFalse();
    }

    @Test
    @DisplayName("WEEDING has optional detail with one optional enum field")
    void WEEDING_has_optional_detail_with_one_optional_enum_field() throws Exception {
        var weeding = service.listWorkTypes().stream().filter(it -> Objects.equals(it.getCode(), "WEEDING")).findFirst().orElseThrow();
        assertThat(weeding.getDetailRequired()).isFalse();
        assertThat(weeding.getFields().stream().map(it -> it.getName()).toList()).containsExactly("weedingMethod");
    }

    @Test
    @DisplayName("PRUNING has no detail requirement and no fields")
    void PRUNING_has_no_detail_requirement_and_no_fields() throws Exception {
        var pruning = service.listWorkTypes().stream().filter(it -> Objects.equals(it.getCode(), "PRUNING")).findFirst().orElseThrow();
        assertThat(pruning.getDetailRequired()).isFalse();
        assertThat(pruning.getFields()).isEmpty();
    }

    @Test
    @DisplayName("ETC has no detail requirement and no fields")
    void ETC_has_no_detail_requirement_and_no_fields() throws Exception {
        var etc = service.listWorkTypes().stream().filter(it -> Objects.equals(it.getCode(), "ETC")).findFirst().orElseThrow();
        assertThat(etc.getDetailRequired()).isFalse();
        assertThat(etc.getFields()).isEmpty();
    }

    @Test
    @DisplayName("HARVEST has required detail with optional medicinalPart and required growth period")
    void HARVEST_has_required_detail_with_optional_medicinalPart_and_required_growth_period() throws Exception {
        var harvest = service.listWorkTypes().stream().filter(it -> Objects.equals(it.getCode(), "HARVEST")).findFirst().orElseThrow();
        assertThat(harvest.getDetailRequired()).isTrue();
        assertThat(harvest.getFields().stream().map(it -> it.getName()).toList()).containsExactly("harvestAmount", "medicinalPart", "harvestSource", "growthPeriod");
        assertThat(harvest.getFields().stream().filter(it -> Objects.equals(it.getName(), "medicinalPart")).findFirst().orElseThrow().getRequired()).isFalse();
        assertThat(harvest.getFields().stream().filter(it -> Objects.equals(it.getName(), "harvestSource")).findFirst().orElseThrow().getRequired()).isFalse();
        assertThat(harvest.getFields().stream().filter(it -> Objects.equals(it.getName(), "growthPeriod")).findFirst().orElseThrow().getRequired()).isTrue();
    }

    @Test
    @DisplayName("detailRequired matches the requirement enforced by FarmingRecordDetailValidator")
    void detailRequired_matches_the_requirement_enforced_by_FarmingRecordDetailValidator() throws Exception {
        var byCode = service.listWorkTypes().stream().collect(java.util.stream.Collectors.toMap(it -> it.getCode(), it -> it));
        assertThat(byCode.get("FERTILIZING").getDetailRequired()).isTrue();
        assertThat(byCode.get("PEST_CONTROL").getDetailRequired()).isTrue();
        assertThat(byCode.get("HARVEST").getDetailRequired()).isTrue();
        assertThat(byCode.get("PLANTING").getDetailRequired()).isTrue();
        assertThat(byCode.get("WATERING").getDetailRequired()).isFalse();
        assertThat(byCode.get("WEEDING").getDetailRequired()).isFalse();
        assertThat(byCode.get("PRUNING").getDetailRequired()).isFalse();
        assertThat(byCode.get("ETC").getDetailRequired()).isFalse();
    }
}
