package com.chamchamcham.application.coaching.reportfeedback.generation;

import com.chamchamcham.domain.report.WeedingStatistics;
import com.chamchamcham.domain.report.CommonOnlyStatistics;
import com.chamchamcham.domain.farming.WorkType;
import com.chamchamcham.domain.report.AmountByUnit;
import com.chamchamcham.domain.report.Coverage;
import com.chamchamcham.domain.report.CycleReportStatistics;
import com.chamchamcham.domain.report.FertilizingStatistics;
import com.chamchamcham.domain.report.HarvestStatistics;
import com.chamchamcham.domain.report.PestControlStatistics;
import com.chamchamcham.domain.report.PlantingStatistics;
import com.chamchamcham.domain.report.PropagationStatistics;
import com.chamchamcham.domain.report.WateringStatistics;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.DisplayName;

class ReportFeedbackComparisonCalculatorTest {
    private final ReportFeedbackComparisonCalculator calculator = new ReportFeedbackComparisonCalculator();

    @Test
    @DisplayName("common metrics use stable order and deterministic differences")
    void common_metrics_use_stable_order_and_deterministic_differences() throws Exception {
        var current = new CycleReportStatistics(PlantingStatistics.empty(), new WateringStatistics(6, null, null, 5, decimal("2.5"), 0, decimal("75.0"), List.of(), null, List.of(), List.of()), FertilizingStatistics.empty(), PestControlStatistics.empty(), WeedingStatistics.empty(), CommonOnlyStatistics.empty(), HarvestStatistics.empty(), CommonOnlyStatistics.empty());
        var previous = new CycleReportStatistics(PlantingStatistics.empty(), new WateringStatistics(4, null, null, 4, decimal("4.0"), 0, decimal("50.0"), List.of(), null, List.of(), List.of()), FertilizingStatistics.empty(), PestControlStatistics.empty(), WeedingStatistics.empty(), CommonOnlyStatistics.empty(), HarvestStatistics.empty(), CommonOnlyStatistics.empty());
        var comparisons = calculator.calculate(WorkType.WATERING, current, previous);
        assertThat(comparisons.stream().map(it -> it.getMetricKey()).toList()).containsExactly("recordCount", "workedDayCount", "averageIntervalDays", "photoAttachmentRatePct");
        assertComparison(comparisons.get(0), "기록 횟수", "6", "4", "2", "50", "회");
        assertComparison(comparisons.get(1), "작업한 날짜 수", "5", "4", "1", "25", "일");
        assertComparison(comparisons.get(2), "평균 작업 간격", "2.5", "4", "-1.5", "-37.5", "일");
        assertComparison(comparisons.get(3), "사진 첨부 비율", "75", "50", "25", "50", "%");
    }

    @Test
    @DisplayName("previous zero omits relative percentage and null metrics are omitted")
    void previous_zero_omits_relative_percentage_and_null_metrics_are_omitted() throws Exception {
        var current = new CycleReportStatistics(PlantingStatistics.empty(), new WateringStatistics(2, null, null, 2, null, 0, decimal("40"), List.of(), null, List.of(), List.of()), FertilizingStatistics.empty(), PestControlStatistics.empty(), WeedingStatistics.empty(), CommonOnlyStatistics.empty(), HarvestStatistics.empty(), CommonOnlyStatistics.empty());
        var previous = new CycleReportStatistics(PlantingStatistics.empty(), new WateringStatistics(0, null, null, 0, decimal("3"), 0, null, List.of(), null, List.of(), List.of()), FertilizingStatistics.empty(), PestControlStatistics.empty(), WeedingStatistics.empty(), CommonOnlyStatistics.empty(), HarvestStatistics.empty(), CommonOnlyStatistics.empty());
        var comparisons = calculator.calculate(WorkType.WATERING, current, previous);
        assertThat(comparisons.stream().map(it -> it.getMetricKey()).toList()).containsExactly("recordCount", "workedDayCount");
        assertThat(comparisons).allMatch(it -> Objects.equals(it.getRelativeChangePct(), null));
        assertThat(comparisons.stream().map(it -> it.getDifference()).toList()).allSatisfy(it -> assertThat(it).isEqualByComparingTo("2"));
    }

    @Test
    @DisplayName("planting compares the same propagation and unit and preserves coverage")
    void planting_compares_the_same_propagation_and_unit_and_preserves_coverage() throws Exception {
        var currentCoverage = new Coverage(2, 3);
        var previousCoverage = new Coverage(1, 2);
        var current = new CycleReportStatistics(new PlantingStatistics(3, null, null, 2, null, 0, null, List.of(), null, List.of(), java.util.Arrays.asList(propagation("SEEDLING", "모종", "12", "JU", currentCoverage), propagation("SEED", "씨앗", "300", "G", new Coverage(1, 1)))), WateringStatistics.empty(), FertilizingStatistics.empty(), PestControlStatistics.empty(), WeedingStatistics.empty(), CommonOnlyStatistics.empty(), HarvestStatistics.empty(), CommonOnlyStatistics.empty());
        var previous = new CycleReportStatistics(new PlantingStatistics(2, null, null, 2, null, 0, null, List.of(), null, List.of(), java.util.Arrays.asList(propagation("SEED", "씨앗", "250", "ML", new Coverage(1, 1)), propagation("SEEDLING", "모종", "10", "JU", previousCoverage))), WateringStatistics.empty(), FertilizingStatistics.empty(), PestControlStatistics.empty(), WeedingStatistics.empty(), CommonOnlyStatistics.empty(), HarvestStatistics.empty(), CommonOnlyStatistics.empty());
        var comparisons = calculator.calculate(WorkType.PLANTING, current, previous);
        assertThat(comparisons.stream().map(it -> it.getMetricKey()).toList()).containsExactly("recordCount", "workedDayCount", "planting.quantity.SEEDLING.JU");
        var quantity = comparisons.getLast();
        assertComparison(quantity, "모종 심은 양", "12", "10", "2", "20", "JU");
        assertThat(quantity.getCurrentCoverage()).isEqualTo(currentCoverage);
        assertThat(quantity.getPreviousCoverage()).isEqualTo(previousCoverage);
    }

    @Test
    @DisplayName("fertilizing and harvest compare normalized kilograms only for the selected work")
    void fertilizing_and_harvest_compare_normalized_kilograms_only_for_the_selected_work() throws Exception {
        var current = new CycleReportStatistics(PlantingStatistics.empty(), WateringStatistics.empty(), new FertilizingStatistics(2, null, null, 2, null, 0, null, List.of(), null, decimal("1.5000"), null, new Coverage(2, 3), List.of(), List.of(), List.of(), List.of()), PestControlStatistics.empty(), WeedingStatistics.empty(), CommonOnlyStatistics.empty(), new HarvestStatistics(4, null, null, 3, null, 0, null, List.of(), null, decimal("18.0000"), null, new Coverage(3, 4), null, null, List.of(), null, null, List.of()), CommonOnlyStatistics.empty());
        var previous = new CycleReportStatistics(PlantingStatistics.empty(), WateringStatistics.empty(), new FertilizingStatistics(1, null, null, 1, null, 0, null, List.of(), null, decimal("1.0000"), null, new Coverage(1, 2), List.of(), List.of(), List.of(), List.of()), PestControlStatistics.empty(), WeedingStatistics.empty(), CommonOnlyStatistics.empty(), new HarvestStatistics(3, null, null, 3, null, 0, null, List.of(), null, decimal("12.0000"), null, new Coverage(3, 3), null, null, List.of(), null, null, List.of()), CommonOnlyStatistics.empty());
        var fertilizing = calculator.calculate(WorkType.FERTILIZING, current, previous);
        var harvest = calculator.calculate(WorkType.HARVEST, current, previous);
        assertThat(fertilizing.stream().map(it -> it.getMetricKey()).toList()).containsExactly("recordCount", "workedDayCount", "fertilizing.totalAmountKg");
        assertComparison(fertilizing.getLast(), "거름 총량", "1.5", "1", "0.5", "50", "KG");
        assertThat(fertilizing.getLast().getCurrentCoverage()).isEqualTo(new Coverage(2, 3));
        assertThat(fertilizing).noneMatch(it -> it.getMetricKey().startsWith("harvest"));
        assertThat(harvest.stream().map(it -> it.getMetricKey()).toList()).containsExactly("recordCount", "workedDayCount", "harvest.totalAmountKg");
        assertComparison(harvest.getLast(), "총수확량", "18", "12", "6", "50", "KG");
        assertThat(harvest.getLast().getPreviousCoverage()).isEqualTo(new Coverage(3, 3));
        assertThat(harvest).noneMatch(it -> it.getMetricKey().startsWith("fertilizing"));
    }

    @Test
    @DisplayName("pest control matches pesticide units and preserves spray coverage")
    void pest_control_matches_pesticide_units_and_preserves_spray_coverage() throws Exception {
        var current = new CycleReportStatistics(PlantingStatistics.empty(), WateringStatistics.empty(), FertilizingStatistics.empty(), new PestControlStatistics(3, null, null, 3, null, 0, null, List.of(), null, List.of(), java.util.Arrays.asList(new AmountByUnit("ML", decimal("120"), new Coverage(2, 3)), new AmountByUnit("G", decimal("40"), new Coverage(1, 3))), List.of(), decimal("30"), new Coverage(3, 3), List.of()), WeedingStatistics.empty(), CommonOnlyStatistics.empty(), HarvestStatistics.empty(), CommonOnlyStatistics.empty());
        var previous = new CycleReportStatistics(PlantingStatistics.empty(), WateringStatistics.empty(), FertilizingStatistics.empty(), new PestControlStatistics(2, null, null, 2, null, 0, null, List.of(), null, List.of(), java.util.Arrays.asList(new AmountByUnit("G", decimal("35"), new Coverage(1, 2)), new AmountByUnit("L", decimal("1"), new Coverage(1, 2))), List.of(), decimal("24"), new Coverage(2, 2), List.of()), WeedingStatistics.empty(), CommonOnlyStatistics.empty(), HarvestStatistics.empty(), CommonOnlyStatistics.empty());
        var comparisons = calculator.calculate(WorkType.PEST_CONTROL, current, previous);
        assertThat(comparisons.stream().map(it -> it.getMetricKey()).toList()).containsExactly("recordCount", "workedDayCount", "pestControl.pesticideAmount.G", "pestControl.totalSprayAmountLiters");
        var pesticide = comparisons.get(2);
        assertComparison(pesticide, "약 사용량", "40", "35", "5", "14.29", "G");
        assertThat(pesticide.getCurrentCoverage()).isEqualTo(new Coverage(1, 3));
        assertThat(pesticide.getPreviousCoverage()).isEqualTo(new Coverage(1, 2));
        assertThat(comparisons).noneMatch(it -> it.getMetricKey().endsWith(".ML") || it.getMetricKey().endsWith(".L"));
        var spray = comparisons.getLast();
        assertComparison(spray, "약을 섞은 물의 총량", "30", "24", "6", "25", "L");
        assertThat(spray.getCurrentCoverage()).isEqualTo(new Coverage(3, 3));
        assertThat(spray.getPreviousCoverage()).isEqualTo(new Coverage(2, 2));
    }
    private PropagationStatistics propagation(String code, String label, String quantity, String unit, Coverage coverage) {
        return new PropagationStatistics(code, label, coverage.targetCount(), decimal("100"), decimal(quantity), unit, coverage);
    }
    private void assertComparison(ReportFeedbackComparison actual, String label, String current, String previous, String difference, String relative, String unit) {
        assertThat(actual.getMetricLabel()).isEqualTo(label);
        assertThat(actual.getCurrentValue()).isEqualByComparingTo(current);
        assertThat(actual.getPreviousValue()).isEqualByComparingTo(previous);
        assertThat(actual.getDifference()).isEqualByComparingTo(difference);
        if (relative == null) assertThat(actual.getRelativeChangePct()).isNull();
        else assertThat(actual.getRelativeChangePct()).isEqualByComparingTo(relative);
        assertThat(actual.getUnit()).isEqualTo(unit);
    }
    private BigDecimal decimal(String value) {
        return new BigDecimal(value);
    }
    @SafeVarargs
    private static <K> Map<K, Object> map(Map.Entry<K, ?>... entries) {
        Map<K, Object> result = new LinkedHashMap<>();
        for (var entry : entries) result.put(entry.getKey(), entry.getValue());
        return result;
    }

}
