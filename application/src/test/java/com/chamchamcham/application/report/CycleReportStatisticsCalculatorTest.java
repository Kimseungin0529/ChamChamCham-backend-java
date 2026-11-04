package com.chamchamcham.application.report;

import com.chamchamcham.domain.farming.WorkType;
import com.chamchamcham.domain.report.CommonOnlyStatistics;
import com.chamchamcham.domain.report.Coverage;
import com.chamchamcham.domain.report.GrowthPeriodRange;
import com.chamchamcham.domain.report.PlantingStatistics;
import com.chamchamcham.domain.report.WateringStatistics;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.DisplayName;

class CycleReportStatisticsCalculatorTest {
    private final CycleReportStatisticsCalculator calculator = new CycleReportStatisticsCalculator();

    @Test
    @DisplayName("every work type receives direct common fields")
    void everyWorkTypeReceivesDirectCommonFields() throws Exception {
        var result = calculator.calculate(allWorkTypeFixture());
        assertDirectCommonFields(result.getPlanting(), 1, false);
        assertDirectCommonFields(result.getWatering(), 2, false);
        assertDirectCommonFields(result.getFertilizing(), 3, false);
        assertDirectCommonFields(result.getPestControl(), 4, false);
        assertDirectCommonFields(result.getWeeding(), 5, false);
        assertDirectCommonFields(result.getPruning(), 6, false);
        assertDirectCommonFields(result.getHarvest(), 7, true);
        assertDirectCommonFields(result.getEtc(), 8, false);
        assertThat(result.getHarvest().getPhotoAttachedRecordCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("missing work type remains an empty typed object")
    void missingWorkTypeRemainsAnEmptyTypedObject() throws Exception {
        var result = calculator.calculate(java.util.Arrays.asList(watering("LOW", "DRIP", 1)));
        assertThat(result.getPlanting()).isEqualTo(PlantingStatistics.empty());
        assertThat(result.getPruning()).isEqualTo(CommonOnlyStatistics.empty());
        assertThat(result.getEtc()).isEqualTo(CommonOnlyStatistics.empty());
    }

    @Test
    @DisplayName("planting separates seed grams and JU quantities")
    void plantingSeparatesSeedGramsAndJUQuantities() throws Exception {
        var result = calculator.calculate(java.util.Arrays.asList(seedPlanting("500", 1), seedPlanting("750", 1), cuttingPlanting(20, 2))).getPlanting();
        assertThat(result.getPropagationMethods().stream().map(it -> it.code()).toList()).containsExactly("SEED", "CUTTING");
        assertThat(result.getPropagationMethods().stream().filter(it -> Objects.equals(it.code(),
            "SEED")).findFirst().orElseThrow().totalQuantity()).isEqualByComparingTo("1250.0000");
        assertThat(result.getPropagationMethods().stream().filter(it -> Objects.equals(it.code(), "SEED")).findFirst().orElseThrow().quantityUnit()).isEqualTo("G");
        assertThat(result.getPropagationMethods().stream().filter(it -> Objects.equals(it.code(), "CUTTING")).findFirst().orElseThrow().quantityUnit()).isEqualTo("JU");
        assertThat(result.getPlantingMethodDistribution().stream().map(it -> org.assertj.core.api.Assertions.tuple(it.code(),
            it.count())).toList()).containsExactly(org.assertj.core.api.Assertions.tuple("SEED", 2), org.assertj.core.api.Assertions.tuple("SEEDLING", 1));
        assertThat(result.getPlantingMethodDistribution().stream().map(it -> org.assertj.core.api.Assertions.tuple(it.code(),
            it.ratePct())).toList()).containsExactly(org.assertj.core.api.Assertions.tuple("SEED", new BigDecimal("66.67")),
            org.assertj.core.api.Assertions.tuple("SEEDLING", new BigDecimal("33.33")));
    }

    @Test
    @DisplayName("watering includes missing selections and has no adequacy fields")
    void wateringIncludesMissingSelectionsAndHasNoAdequacyFields() throws Exception {
        var result = calculator.calculate(java.util.Arrays.asList(watering("LOW", "DRIP", 1), watering(null, "MANUAL", 1))).getWatering();
        assertThat(result.getAmountDistribution().stream().map(it -> org.assertj.core.api.Assertions.tuple(it.code(),
            it.count())).toList()).containsExactlyInAnyOrder(org.assertj.core.api.Assertions.tuple("LOW", 1),
            org.assertj.core.api.Assertions.tuple("MISSING", 1));
        assertThat(Arrays.stream(WateringStatistics.class.getDeclaredFields()).map(java.lang.reflect.Field::getName).toList()).doesNotContain("adequacy",
            "shortage", "excess");
    }

    @Test
    @DisplayName("fertilizing groups material names and nullable methods")
    void fertilizingGroupsMaterialNamesAndNullableMethods() throws Exception {
        var result = calculator.calculate(java.util.Arrays.asList(fertilizing("복합비료", "10000", "G", "10", "SOIL", 1), fertilizing("복합비료", "5000", "G", "5",
            null, 1))).getFertilizing();
        assertThat(result.getTotalAmountKg()).isEqualByComparingTo("15.0000");
        assertThat(result.getAverageAmountKg()).isEqualByComparingTo("7.50");
        assertThat(result.getAmountCoverage()).isEqualTo(new Coverage(2, 2));
        assertThat(single(result.getMaterialCategories()).amountKg()).isEqualByComparingTo("15.0000");
        assertThat(result.getMethodDistribution().stream().map(it -> org.assertj.core.api.Assertions.tuple(it.code(),
            it.count())).toList()).containsExactlyInAnyOrder(org.assertj.core.api.Assertions.tuple("SOIL", 1),
            org.assertj.core.api.Assertions.tuple("MISSING", 1));
        assertThat(result.getCategoryMethods().stream().map(it -> org.assertj.core.api.Assertions.tuple(it.categoryCode(), it.methodCode(),
            it.recordCount())).toList()).containsExactly(org.assertj.core.api.Assertions.tuple("복합비료", "MISSING", 1),
            org.assertj.core.api.Assertions.tuple("복합비료", "SOIL", 1));
    }

    @Test
    @DisplayName("fertilizer mass statistics include G but exclude ML")
    void fertilizerMassStatisticsIncludeGButExcludeML() throws Exception {
        var result = calculator.calculate(java.util.Arrays.asList(fertilizing("유기질비료", "500", "G", "0.5000", "SOIL", 1), fertilizing("액상비료", "250", "ML",
            null, "SOIL", 1))).getFertilizing();
        assertThat(result.getTotalAmountKg()).isEqualByComparingTo("0.5000");
        assertThat(result.getAmountCoverage()).isEqualTo(new Coverage(1, 2));
        assertThat(result.getMaterialDistribution().stream().map(it -> org.assertj.core.api.Assertions.tuple(it.label(),
            it.count())).toList()).containsExactlyInAnyOrder(org.assertj.core.api.Assertions.tuple("유기질비료", 1),
            org.assertj.core.api.Assertions.tuple("액상비료", 1));
    }

    @Test
    @DisplayName("fertilizer sums sub gram amounts before rounding kilograms")
    void fertilizerSumsSubGramAmountsBeforeRoundingKilograms() throws Exception {
        var result = calculator.calculate(java.util.Arrays.asList(fertilizing("유기질비료", "0.06", "G", "0.00006", "SOIL", 1), fertilizing("유기질비료", "0.06",
            "G", "0.00006", "SOIL", 2))).getFertilizing();
        assertThat(result.getTotalAmountKg()).isEqualByComparingTo("0.0001");
        assertThat(single(result.getMaterialCategories()).amountKg()).isEqualByComparingTo("0.0001");
    }

    @Test
    @DisplayName("pesticide ML and G remain separate")
    void pesticideMLAndGRemainSeparate() throws Exception {
        var result = calculator.calculate(java.util.Arrays.asList(pest("가가방", "10", "ML", "100", 1, id("9" + 1 + "")), pest("가가방", "5", "G", "50", 1,
            id("9" + 1 + "")))).getPestControl();
        assertThat(result.getPesticideAmounts().stream().map(it -> org.assertj.core.api.Assertions.tuple(it.unit(),
            it.amount())).toList()).containsExactlyInAnyOrder(org.assertj.core.api.Assertions.tuple("ML", new BigDecimal("10.0000")),
            org.assertj.core.api.Assertions.tuple("G", new BigDecimal("5.0000")));
        assertThat(result.getCategoryAmounts().stream().map(it -> org.assertj.core.api.Assertions.tuple(it.categoryCode(), it.unit(),
            it.amount())).toList()).containsExactly(org.assertj.core.api.Assertions.tuple(id("91").toString(), "G", new BigDecimal("5.0000")),
            org.assertj.core.api.Assertions.tuple(id("91").toString(), "ML", new BigDecimal("10.0000")));
        assertThat(result.getTotalSprayAmountLiters()).isEqualByComparingTo("150.0000");
    }

    @Test
    @DisplayName("pesticides with the same brand name remain separate products")
    void pesticidesWithTheSameBrandNameRemainSeparateProducts() throws Exception {
        var firstId = id("901");
        var secondId = id("902");
        var result = calculator.calculate(java.util.Arrays.asList(pest("같은이름", "10", "ML", "100", 1, firstId), pest("같은이름", "20", "ML", "100", 2,
            secondId))).getPestControl();
        assertThat(result.getCategoryDistribution().stream().map(it -> org.assertj.core.api.Assertions.tuple(it.code(),
            it.count())).toList()).containsExactlyInAnyOrder(org.assertj.core.api.Assertions.tuple(firstId.toString(), 1),
            org.assertj.core.api.Assertions.tuple(secondId.toString(), 1));
        assertThat(result.getCategoryAmounts().stream().map(it -> org.assertj.core.api.Assertions.tuple(it.categoryCode(),
            it.recordCount())).toList()).containsExactlyInAnyOrder(org.assertj.core.api.Assertions.tuple(firstId.toString(), 1),
            org.assertj.core.api.Assertions.tuple(secondId.toString(), 1));
    }

    @Test
    @DisplayName("unknown harvest amount counts the record but not quantity")
    void unknownHarvestAmountCountsTheRecordButNotQuantity() throws Exception {
        var result = calculator.calculate(java.util.Arrays.asList(harvest(null, "ROOT_BARK", 10, false, (10), false), harvest("30", "LEAF", 12, true, (12),
            false))).getHarvest();
        assertThat(result.getRecordCount()).isEqualTo(2);
        assertThat(result.getTotalAmountKg()).isEqualByComparingTo("30.0000");
        assertThat(result.getAverageAmountKg()).isEqualByComparingTo("30.00");
        assertThat(result.getAmountCoverage()).isEqualTo(new Coverage(1, 2));
        assertThat(result.getMedicinalParts().stream().mapToInt(it -> it.recordCount()).sum()).isEqualTo(2);
        assertThat(result.getMedicinalParts().stream().filter(it -> Objects.equals(it.code(), "ROOT_BARK")).findFirst().orElseThrow().knownAmountKg()).isNull();
        assertThat(result.getFinalGrowthPeriodMonths()).isEqualTo(12);
        assertThat(result.getGrowthPeriodRangeMonths()).isEqualTo(new GrowthPeriodRange(10, 12));
    }

    @Test
    @DisplayName("harvest groups records by growth period months")
    void harvestGroupsRecordsByGrowthPeriodMonths() throws Exception {
        var result = calculator.calculate(java.util.Arrays.asList(harvest("10", "ROOT_BARK", 24, false, 1, false), harvest("20", "ROOT_BARK", 24, false, 2,
            false), harvest("30", "ROOT_BARK", 36, true, 3, false))).getHarvest();
        assertThat(result.getGrowthPeriodDistribution().stream().map(it -> org.assertj.core.api.Assertions.tuple(it.code(), it.label(),
            it.count())).toList()).containsExactly(org.assertj.core.api.Assertions.tuple("24", "24개월", 2), org.assertj.core.api.Assertions.tuple("36",
            "36개월", 1));
        assertThat(result.getGrowthPeriodDistribution().stream().map(it -> org.assertj.core.api.Assertions.tuple(it.code(),
            it.ratePct())).toList()).containsExactly(org.assertj.core.api.Assertions.tuple("24", new BigDecimal("66.67")),
            org.assertj.core.api.Assertions.tuple("36", new BigDecimal("33.33")));
    }
    @Test
    void unicodeSpaceCharactersAreTrimmedBeforeWeatherGrouping() {
        var time = LocalDateTime.of(2026, 1, 1, 9, 0);
        var record = new CycleReportSourceRecord(id("999"), time, time, WorkType.WATERING,
            "\u00a0맑음\u2007\u202f", 20, false, "", null, null, null, null, null, null);
        var distribution = calculator.calculate(List.of(record)).getWatering().getWeatherDistribution();
        assertThat(distribution).containsExactly(new com.chamchamcham.domain.report.CountDistribution("맑음", "맑음", 1, new BigDecimal("100.00")));
    }

    private void assertDirectCommonFields(Object stats, int day, boolean photoAttached) throws Exception {
        assertThat((Integer) readField(stats, "recordCount")).isEqualTo(1);
        assertThat((LocalDate) readField(stats, "firstWorkedOn")).isEqualTo(date(day));
        assertThat((LocalDate) readField(stats, "lastWorkedOn")).isEqualTo(date(day));
        assertThat((Integer) readField(stats, "workedDayCount")).isEqualTo(1);
        assertThat(readField(stats, "averageIntervalDays")).isNull();
        assertThat((Integer) readField(stats, "photoAttachedRecordCount")).isEqualTo(photoAttached ? 1 : 0);
        assertThat((BigDecimal) readField(stats, "photoAttachmentRatePct")).isEqualByComparingTo(photoAttached ? "100.00" : "0.00");
        @SuppressWarnings("unchecked")
        var weather = (List<com.chamchamcham.domain.report.CountDistribution>) readField(stats, "weatherDistribution");
        assertThat(weather).extracting(x -> org.assertj.core.api.Assertions.tuple(x.code(), x.count())).containsExactly(org.assertj.core.api.Assertions.tuple("맑음", 1));
        assertThat((BigDecimal) readField(stats, "averageTemperatureC")).isEqualByComparingTo("20.00");
    }
    private Object readField(Object target, String name) throws Exception {
        var field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }
    private List<CycleReportSourceRecord> allWorkTypeFixture() {
        return java.util.Arrays.asList(seedPlanting("100", 1), watering("LOW", "DRIP", 2), fertilizing("복합비료", "10000", "G", "10", "SOIL", 3), pest("가가방",
            "5", "ML", "50", 4, id("9" + 4 + "")), weeding(5), baseRecord("06", 6, WorkType.PRUNING, false, null, null, null, null, null, null),
            harvest("30", "ROOT_BARK", 12, true, 7, true), baseRecord("08", 8, WorkType.ETC, false, null, null, null, null, null, null));
    }
    private CycleReportSourceRecord seedPlanting(String amount, int day) {
        return baseRecord("1" + day + "", day, WorkType.PLANTING, false, new PlantingReportSource(new CategoryRef("SEED", "씨앗 심기"), null,
            new BigDecimal(amount), "G"), null, null, null, null, null);
    }
    private CycleReportSourceRecord cuttingPlanting(int seedlingCount, int day) {
        return baseRecord("2" + day + "", day, WorkType.PLANTING, false, new PlantingReportSource(new CategoryRef("SEEDLING", "모종 심기"),
            new CategoryRef("CUTTING", "삽목"), new BigDecimal(seedlingCount), "JU"), null, null, null, null, null);
    }
    private CycleReportSourceRecord watering(String amount, String method, int day) {
        return baseRecord("3" + day + "", day, WorkType.WATERING, false, null, new WateringReportSource((amount == null ? null : new CategoryRef(amount,
            wateringAmountLabel(amount))), (method == null ? null : new CategoryRef(method, wateringMethodLabel(method)))), null, null, null, null);
    }
    private CycleReportSourceRecord fertilizing(String materialName, String amount, String amountUnit, String amountKg, String method, int day) {
        return baseRecord("4" + day + "", day, WorkType.FERTILIZING, false, null, null, new FertilizingReportSource(materialName, new BigDecimal(amount),
            amountUnit, (amountKg == null ? null : new BigDecimal(amountKg)), (method == null ? null : new CategoryRef(method,
            fertilizingMethodLabel(method)))), null, null, null);
    }
    private CycleReportSourceRecord pest(String pesticideName, String amount, String unit, String sprayAmountLiters, int day, UUID pesticideId) {
        return baseRecord("5" + day + "", day, WorkType.PEST_CONTROL, false, null, null, null, new PestControlReportSource(pesticideId, pesticideName,
            (amount == null ? null : new BigDecimal(amount)), unit, (sprayAmountLiters == null ? null : new BigDecimal(sprayAmountLiters)), "진딧물"), null,
            null);
    }
    private CycleReportSourceRecord weeding(int day) {
        return baseRecord("6" + day + "", day, WorkType.WEEDING, false, null, null, null, null, new WeedingReportSource(new CategoryRef("HAND", "손제초")), null);
    }
    private CycleReportSourceRecord harvest(String amountKg, String part, int growthMonths, boolean isFinal, int day, boolean hasPhoto) {
        return baseRecord("7" + day + "", day, WorkType.HARVEST, hasPhoto, null, null, null, null, null,
            new HarvestReportSource((amountKg == null ? null : new BigDecimal(amountKg)), new CategoryRef(part, harvestPartLabel(part)),
            new CategoryRef("CULTIVATED", "재배"), growthMonths, "MONTH", growthMonths, isFinal));
    }
    private CycleReportSourceRecord baseRecord(String number, int day, WorkType workType, boolean hasPhoto, PlantingReportSource planting,
        WateringReportSource watering, FertilizingReportSource fertilizing, PestControlReportSource pestControl, WeedingReportSource weeding,
        HarvestReportSource harvest) {
        return new CycleReportSourceRecord(id(number), LocalDateTime.of(2026, 1, day, 9, 0), (LocalDateTime.of(2026, 1, day, 9, 0)), workType, "맑음", 20,
            hasPhoto, "", planting, watering, fertilizing, pestControl, weeding, harvest);
    }
    private LocalDate date(int day) {
        return LocalDate.of(2026, 1, day);
    }
    private UUID id(String number) {
        var suffix = ("000000000000" + number).substring(number.length());
        return UUID.fromString("00000000-0000-0000-0000-" + suffix + "");
    }
    private String wateringAmountLabel(String code) {
        return switch (code) {
            case "LOW" -> "적음";
            default -> code;
        };
    }
    private String wateringMethodLabel(String code) {
        return switch (code) {
            case "DRIP" -> "점적관수";
            case "MANUAL" -> "수동";
            default -> code;
        };
    }
    private String fertilizingMethodLabel(String code) {
        return switch (code) {
            case "SOIL" -> "토양시비";
            default -> code;
        };
    }
    private String harvestPartLabel(String code) {
        return switch (code) {
            case "ROOT_BARK" -> "뿌리·껍질";
            case "LEAF" -> "잎";
            default -> code;
        };
    }
    private static <T> T single(List<T> values) {
        assertThat(values).hasSize(1);
        return values.getFirst();
    }


    @Test
    void emptySortedStatisticsListsRetainOriginalMutationAndNullSemantics() {
        assertSortedStatisticsContract(0);
    }

    @Test
    void singletonSortedStatisticsListsRetainOriginalMutationAndNullSemantics() {
        assertSortedStatisticsContract(1);
    }

    @Test
    void multiSortedStatisticsListsRetainReplacementButRejectStructuralMutation() {
        assertSortedStatisticsContract(2);
    }

    @Test
    void categoryMethodFlatMapResultsStayMutableForEmptySingletonAndMultiInputs() {
        for (int size : new int[] {0, 1, 2}) {
            var methods = calculator.calculate(mutabilityFixture(size)).getFertilizing().getCategoryMethods();
            assertThat(methods).hasSize(size);
            if (size == 1) assertThat(methods).extracting(it -> it.categoryCode() + ":" + it.methodCode()).containsExactly("B:SOIL");
            if (size == 2) assertThat(methods).extracting(it -> it.categoryCode() + ":" + it.methodCode()).containsExactly("A:MISSING", "B:SOIL");
            var before = new java.util.ArrayList<>(methods);
            assertThat(methods.add(null)).isTrue();
            assertThat(methods.remove((Object) null)).isTrue();
            if (size > 0) {
                var first = methods.set(0, null);
                assertThat(methods.getFirst()).isNull();
                methods.set(0, first);
                assertThat(methods.remove(0)).isSameAs(first);
                methods.add(0, first);
            }
            assertThat(methods).containsExactlyElementsOf(before);
        }
    }

    private void assertSortedStatisticsContract(int size) {
        var statistics = calculator.calculate(mutabilityFixture(size));
        org.junit.jupiter.api.Assertions.assertAll("direct sorted results size=" + size,
            () -> assertSortedListContract(statistics.getPruning().getWeatherDistribution(), size),
            () -> assertSortedListContract(statistics.getPlanting().getPropagationMethods(), size),
            () -> assertSortedListContract(statistics.getPlanting().getPlantingMethodDistribution(), size),
            () -> assertSortedListContract(statistics.getWatering().getAmountDistribution(), size),
            () -> assertSortedListContract(statistics.getWatering().getMethodDistribution(), size),
            () -> assertSortedListContract(statistics.getFertilizing().getMaterialDistribution(), size),
            () -> assertSortedListContract(statistics.getFertilizing().getMethodDistribution(), size),
            () -> assertSortedListContract(statistics.getFertilizing().getMaterialCategories(), size),
            () -> assertSortedListContract(statistics.getPestControl().getCategoryDistribution(), size),
            () -> assertSortedListContract(statistics.getPestControl().getPesticideAmounts(), size),
            () -> assertSortedListContract(statistics.getPestControl().getCategoryAmounts(), size),
            () -> assertSortedListContract(statistics.getPestControl().getTargets(), size),
            () -> assertSortedListContract(statistics.getWeeding().getMethodDistribution(), size),
            () -> assertSortedListContract(statistics.getHarvest().getMedicinalParts(), size),
            () -> assertSortedListContract(statistics.getHarvest().getGrowthPeriodDistribution(), size));
        if (size == 2) {
            assertThat(statistics.getPruning().getWeatherDistribution()).extracting(it -> it.code()).containsExactly("맑음", "비");
            assertThat(statistics.getPlanting().getPropagationMethods()).extracting(it -> it.code()).containsExactly("CUTTING", "SEED");
            assertThat(statistics.getPestControl().getPesticideAmounts()).extracting(it -> it.unit()).containsExactly("G", "ML");
            assertThat(statistics.getPestControl().getCategoryAmounts()).extracting(it -> it.categoryCode()).containsExactly(id("901").toString(), id("902").toString());
            assertThat(statistics.getPestControl().getTargets()).extracting(it -> it.target()).containsExactly("나방", "진딧물");
            assertThat(statistics.getFertilizing().getMaterialCategories()).extracting(it -> it.code()).containsExactly("A", "B");
            assertThat(statistics.getHarvest().getMedicinalParts()).extracting(it -> it.code()).containsExactly("LEAF", "ROOT_BARK");
        }
    }

    private static <T> void assertSortedListContract(List<T> values, int size) {
        assertThat(values).hasSize(size);
        assertThat(values.contains(null)).isFalse();
        if (size < 2) {
            org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class, () -> values.set(0, null));
        } else {
            var first = values.set(0, null);
            assertThat(values.getFirst()).isNull();
            org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class, () -> values.remove((Object) null));
            values.set(0, first);
        }
        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class, () -> values.add(null));
        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class, () -> values.remove(0));
        if (size == 0) {
            org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class, () -> values.remove((Object) null));
            org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class, () -> values.remove(new Object()));
        } else {
            assertThat(values.remove((Object) null)).isFalse();
            assertThat(values.remove(new Object())).isFalse();
            org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class, () -> values.remove(values.getFirst()));
        }
        assertThat(values).hasSize(size);
    }

    private List<CycleReportSourceRecord> mutabilityFixture(int size) {
        if (size == 0) return List.of();
        var records = new java.util.ArrayList<CycleReportSourceRecord>();
        records.add(seedPlanting("100", 1));
        records.add(watering("LOW", "DRIP", 1));
        records.add(fertilizing("B", "1000", "G", "1", "SOIL", 1));
        records.add(pest("가가방", "1", "ML", "1", 1, id("901")));
        records.add(weeding(1));
        records.add(harvest("1", "ROOT_BARK", 12, false, 1, false));
        var time = LocalDateTime.of(2026, 1, 1, 9, 0);
        records.add(new CycleReportSourceRecord(id("801"), time, time, WorkType.PRUNING, "비", 20, false, "", null, null, null, null, null, null));
        if (size == 2) {
            records.add(cuttingPlanting(20, 2));
            records.add(watering(null, null, 2));
            records.add(fertilizing("A", "1000", "G", "1", null, 2));
            records.add(baseRecord("52", 2, WorkType.PEST_CONTROL, false, null, null, null,
                new PestControlReportSource(id("902"), "다른제품", BigDecimal.ONE, "G", BigDecimal.ONE, "나방"), null, null));
            records.add(baseRecord("62", 2, WorkType.WEEDING, false, null, null, null, null, new WeedingReportSource(null), null));
            records.add(harvest("1", "LEAF", 24, false, 2, false));
            records.add(baseRecord("802", 2, WorkType.PRUNING, false, null, null, null, null, null, null));
        }
        return records;
    }

}
