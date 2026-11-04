package com.chamchamcham.application.report;

import com.chamchamcham.domain.crop.Crop;
import com.chamchamcham.domain.crop.CropUsePartCategory;
import com.chamchamcham.domain.farm.Farm;
import com.chamchamcham.domain.farming.FarmingCycleReportSourceRepository;
import com.chamchamcham.domain.farming.FarmingCycleReportSourceSnapshot;
import com.chamchamcham.domain.farming.FarmingRecord;
import com.chamchamcham.domain.farming.EntryMode;
import com.chamchamcham.domain.farming.FertilizerAmountUnit;
import com.chamchamcham.domain.farming.FertilizingMethod;
import com.chamchamcham.domain.farming.FertilizingRecord;
import com.chamchamcham.domain.farming.HarvestRecord;
import com.chamchamcham.domain.farming.HarvestSource;
import com.chamchamcham.domain.farming.IrrigationAmount;
import com.chamchamcham.domain.farming.IrrigationMethod;
import com.chamchamcham.domain.farming.PesticideAmountUnit;
import com.chamchamcham.domain.farming.PestControlRecord;
import com.chamchamcham.domain.farming.PlantingMethod;
import com.chamchamcham.domain.farming.PlantingRecord;
import com.chamchamcham.domain.farming.SeedAmountUnit;
import com.chamchamcham.domain.farming.SprayAmountUnit;
import com.chamchamcham.domain.farming.WateringRecord;
import com.chamchamcham.domain.farming.WeedingMethod;
import com.chamchamcham.domain.farming.WeedingRecord;
import com.chamchamcham.domain.farming.WorkType;
import com.chamchamcham.domain.member.Member;
import com.chamchamcham.domain.pesticide.Pest;
import com.chamchamcham.domain.pesticide.Pesticide;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.*;
import com.chamchamcham.domain.farming.EntryMode;
import com.chamchamcham.domain.farming.HarvestSource;
import org.junit.jupiter.api.DisplayName;

class FarmingCycleReportSourceLoaderTest {
    private final UUID memberId = uuid("00000000-0000-0000-0000-000000000101");
    private final UUID farmId = uuid("00000000-0000-0000-0000-000000000102");
    private final UUID cropId = uuid("00000000-0000-0000-0000-000000000103");
    private final Member member = new Member(memberId, "member@example.com", null);
    private final Farm farm = new Farm(farmId, member, "약초농장", "서울시 강남구");
    private final Crop crop = new Crop(cropId, 422, "황기", CropUsePartCategory.ROOT_BARK);
    private final ReportScope scope = new ReportScope(memberId, farmId, cropId);

    @Test
    @DisplayName("loader normalizes merged fields without free text category fallback")
    void loader_normalizes_merged_fields_without_free_text_category_fallback() throws Exception {
        var repository = new FakeSourceRepository(fullSnapshot());
        var loader = new FarmingCycleReportSourceLoader(repository);
        var records = loader.load(scope);
        assertThat(records.stream().filter(it -> Objects.equals(it.getWorkType(), WorkType.PLANTING)).findFirst().orElseThrow().getPlanting().getQuantity()).isEqualByComparingTo("1250.0000");
        assertThat(records.stream().filter(it -> Objects.equals(it.getWorkType(), WorkType.PLANTING)).findFirst().orElseThrow().getPlanting().getQuantityUnit()).isEqualTo("G");
        assertThat(records.stream().filter(it -> Objects.equals(it.getWorkType(), WorkType.WATERING)).findFirst().orElseThrow().getWatering().getAmount()).isEqualTo(new CategoryRef("NORMAL", "보통"));
        var fertilizing = records.stream().filter(it -> Objects.equals(it.getWorkType(), WorkType.FERTILIZING)).findFirst().orElseThrow().getFertilizing();
        assertThat(fertilizing.getMaterialName()).isEqualTo("유기질비료");
        assertThat(fertilizing.getAmount()).isEqualByComparingTo("250");
        assertThat(fertilizing.getAmountUnit()).isEqualTo("ML");
        assertThat(fertilizing.getAmountKg()).isNull();
        var pestControl = records.stream().filter(it -> Objects.equals(it.getWorkType(), WorkType.PEST_CONTROL)).findFirst().orElseThrow().getPestControl();
        assertThat(pestControl.getPesticideName()).isEqualTo("가가방");
        assertThat(pestControl.getPestName()).isEqualTo("진딧물");
        assertThat(records.stream().filter(it -> Objects.equals(it.getWorkType(), WorkType.PEST_CONTROL)).findFirst().orElseThrow().getPestControl().getPesticideAmountUnit()).isEqualTo("ML");
        assertThat(records.stream().filter(it -> Objects.equals(it.getWorkType(), WorkType.PEST_CONTROL)).findFirst().orElseThrow().getPestControl().getTotalSprayAmountLiters()).isEqualByComparingTo("10.0000");
        assertThat(records.stream().filter(it -> Objects.equals(it.getWorkType(), WorkType.WEEDING)).findFirst().orElseThrow().getWeeding().getMethod()).isEqualTo(new CategoryRef("HAND", "손으로 뽑기"));
        assertThat(records.stream().filter(it -> Objects.equals(it.getWorkType(), WorkType.HARVEST)).findFirst().orElseThrow().getHarvest().getAmountKg()).isNull();
        assertThat(records.stream().filter(it -> Objects.equals(it.getWorkType(), WorkType.HARVEST)).findFirst().orElseThrow().getHarvest().getGrowthPeriodMonths()).isEqualTo(24);
        assertThat(records.stream().filter(it -> Objects.equals(it.getWorkType(), WorkType.HARVEST)).findFirst().orElseThrow().getHarvest().isLastHarvest()).isTrue();
        assertThat(records.stream().filter(it -> Objects.equals(it.getWorkType(), WorkType.PRUNING)).findFirst().orElseThrow().getPlanting()).isNull();
        assertThat(records.stream().filter(it -> Objects.equals(it.getWorkType(), WorkType.ETC)).findFirst().orElseThrow().getHasPhoto()).isTrue();
        assertThat(records.stream().filter(it -> Objects.equals(it.getWorkType(), WorkType.PLANTING)).findFirst().orElseThrow().getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 7, 1, 8, 1));
    }

    @Test
    @DisplayName("loader preserves sub gram precision before report aggregation")
    void loader_preserves_sub_gram_precision_before_report_aggregation() throws Exception {
        var fertilizing = record("00000000-0000-0000-0000-000000000209", WorkType.FERTILIZING, 9);
        var snapshot = new FarmingCycleReportSourceSnapshot(java.util.Arrays.asList(fertilizing), Map.of(), Map.of(), Map.ofEntries(Map.entry(Objects.requireNonNull(fertilizing.getId()), new FertilizingRecord(null, fertilizing, "유기질비료", new BigDecimal("0.01"), FertilizerAmountUnit.G, null))), Map.of(), Map.of(), Map.of(), Set.of());
        var source = new FarmingCycleReportSourceLoader(new FakeSourceRepository(snapshot)).load(scope).get(0).getFertilizing();
        assertThat(source.getAmount()).isEqualByComparingTo("0.0100");
        assertThat(source.getAmountKg()).isEqualByComparingTo("0.00001");
    }

    @Test
    @DisplayName("loader fails when required detail is missing")
    void loader_fails_when_required_detail_is_missing() throws Exception {
        var planting = record("00000000-0000-0000-0000-000000000201", WorkType.PLANTING, 1);
        var repository = new FakeSourceRepository(emptySnapshot(java.util.Arrays.asList(planting)));
        var loader = new FarmingCycleReportSourceLoader(repository);
        assertThatThrownBy(() -> loader.load(scope)).isInstanceOf(IllegalStateException.class).hasMessage("Missing detail for record " + planting.getId() + "");
    }
    private FarmingCycleReportSourceSnapshot fullSnapshot() {
        var planting = record("00000000-0000-0000-0000-000000000201", WorkType.PLANTING, 1);
        var watering = record("00000000-0000-0000-0000-000000000202", WorkType.WATERING, 2);
        var fertilizing = record("00000000-0000-0000-0000-000000000203", WorkType.FERTILIZING, 3);
        var pestControl = record("00000000-0000-0000-0000-000000000204", WorkType.PEST_CONTROL, 4);
        var weeding = record("00000000-0000-0000-0000-000000000205", WorkType.WEEDING, 5);
        var pruning = record("00000000-0000-0000-0000-000000000206", WorkType.PRUNING, 6);
        var harvest = record("00000000-0000-0000-0000-000000000207", WorkType.HARVEST, 7);
        var etc = record("00000000-0000-0000-0000-000000000208", WorkType.ETC, 8);
        return new FarmingCycleReportSourceSnapshot(java.util.Arrays.asList(planting, watering, fertilizing, pestControl, weeding, pruning, harvest, etc), Map.ofEntries(Map.entry(Objects.requireNonNull(planting.getId()), new PlantingRecord(null, planting, PlantingMethod.SEED, new BigDecimal("1250.0000"), SeedAmountUnit.G, null, null, null))), Map.ofEntries(Map.entry(Objects.requireNonNull(watering.getId()), new WateringRecord(null, watering, IrrigationAmount.NORMAL, IrrigationMethod.DRIP))), Map.ofEntries(Map.entry(Objects.requireNonNull(fertilizing.getId()), new FertilizingRecord(null, fertilizing, "유기질비료", new BigDecimal("250.0000"), FertilizerAmountUnit.ML, FertilizingMethod.SOIL))), Map.ofEntries(Map.entry(Objects.requireNonNull(pestControl.getId()), new PestControlRecord(null, pestControl, new Pesticide(uuid("00000000-0000-0000-0000-000000000301"), "만코제브 수화제", "가가방"), new BigDecimal("30.0000"), PesticideAmountUnit.ML, new BigDecimal("10000.0000"), SprayAmountUnit.ML, new Pest(uuid("00000000-0000-0000-0000-000000000302"), "진딧물")))), Map.ofEntries(Map.entry(Objects.requireNonNull(weeding.getId()), new WeedingRecord(null, weeding, WeedingMethod.HAND))), Map.ofEntries(Map.entry(Objects.requireNonNull(harvest.getId()), new HarvestRecord(null, harvest, null, CropUsePartCategory.ROOT_BARK, HarvestSource.CULTIVATED, 24, true))), Set.of(Objects.requireNonNull(etc.getId())));
    }
    private FarmingCycleReportSourceSnapshot emptySnapshot(List<FarmingRecord> records) {
        return new FarmingCycleReportSourceSnapshot(records, Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Set.of());
    }
    private FarmingRecord record(String id, WorkType workType, long hour) {
        var record = new FarmingRecord(uuid(id), member, farm, crop, workType, LocalDateTime.of(2026, 7, 1, 8, 0).plusHours(hour), "맑음", 24, "memo", EntryMode.MANUAL, false);
        org.springframework.test.util.ReflectionTestUtils.setField(record, "createdAt", LocalDateTime.of(2026, 7, 1, 8, 0).plusMinutes(hour));
        return record;
    }
    private UUID uuid(String value) {
        return UUID.fromString(value);
    }
    private static final class FakeSourceRepository implements FarmingCycleReportSourceRepository {
        private final FarmingCycleReportSourceSnapshot snapshot;
        private FakeSourceRepository(FarmingCycleReportSourceSnapshot snapshot) { this.snapshot = snapshot; }
        @Override public FarmingCycleReportSourceSnapshot load(UUID memberId, UUID farmId, UUID cropId) { return snapshot; }
    }
}
