package com.chamchamcham.application.coaching.reportfeedback.generation;

import com.chamchamcham.domain.farming.WorkType;
import com.chamchamcham.domain.report.*;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Component
public class ReportFeedbackComparisonCalculator {
    public List<ReportFeedbackComparison> calculate(WorkType workType, CycleReportStatistics current, CycleReportStatistics previous) {
        Objects.requireNonNull(workType); Objects.requireNonNull(current); Objects.requireNonNull(previous);
        var result = new ArrayList<ReportFeedbackComparison>();
        var c = commonFor(current, workType); var p = commonFor(previous, workType);
        compare(result, "recordCount", "기록 횟수", BigDecimal.valueOf(c.recordCount()), BigDecimal.valueOf(p.recordCount()), "회", null, null);
        compare(result, "workedDayCount", "작업한 날짜 수", BigDecimal.valueOf(c.workedDayCount()), BigDecimal.valueOf(p.workedDayCount()), "일", null, null);
        compare(result, "averageIntervalDays", "평균 작업 간격", c.averageIntervalDays(), p.averageIntervalDays(), "일", null, null);
        compare(result, "photoAttachmentRatePct", "사진 첨부 비율", c.photoAttachmentRatePct(), p.photoAttachmentRatePct(), "%", null, null);
        switch (workType) {
            case PLANTING -> {
                var previousByKey = new LinkedHashMap<Map.Entry<String, String>, PropagationStatistics>();
                previous.getPlanting().getPropagationMethods().forEach(it -> previousByKey.put(new AbstractMap.SimpleImmutableEntry<>(it.code(), it.quantityUnit()), it));
                var quantities = new ArrayList<>(current.getPlanting().getPropagationMethods());
                quantities.sort(Comparator.comparing(PropagationStatistics::code).thenComparing(PropagationStatistics::quantityUnit, Comparator.nullsFirst(Comparator.naturalOrder())));
                for (var it : quantities) { String unit = it.quantityUnit(); if (unit == null) continue; var before = previousByKey.get(new AbstractMap.SimpleImmutableEntry<>(it.code(), unit)); if (before == null) continue; compare(result, "planting.quantity." + it.code() + "." + unit, it.label() + " 심은 양", it.totalQuantity(), before.totalQuantity(), unit, it.quantityCoverage(), before.quantityCoverage()); }
            }
            case FERTILIZING -> compare(result, "fertilizing.totalAmountKg", "거름 총량", current.getFertilizing().getTotalAmountKg(), previous.getFertilizing().getTotalAmountKg(), "KG", current.getFertilizing().getAmountCoverage(), previous.getFertilizing().getAmountCoverage());
            case PEST_CONTROL -> {
                var previousByUnit = new LinkedHashMap<String, AmountByUnit>(); previous.getPestControl().getPesticideAmounts().forEach(it -> previousByUnit.put(it.unit(), it));
                var amounts = new ArrayList<>(current.getPestControl().getPesticideAmounts()); amounts.sort(Comparator.comparing(AmountByUnit::unit));
                for (var it : amounts) { var before = previousByUnit.get(it.unit()); if (before != null) compare(result, "pestControl.pesticideAmount." + it.unit(), "약 사용량", it.amount(), before.amount(), it.unit(), it.coverage(), before.coverage()); }
                compare(result, "pestControl.totalSprayAmountLiters", "약을 섞은 물의 총량", current.getPestControl().getTotalSprayAmountLiters(), previous.getPestControl().getTotalSprayAmountLiters(), "L", current.getPestControl().getSprayAmountCoverage(), previous.getPestControl().getSprayAmountCoverage());
            }
            case HARVEST -> compare(result, "harvest.totalAmountKg", "총수확량", current.getHarvest().getTotalAmountKg(), previous.getHarvest().getTotalAmountKg(), "KG", current.getHarvest().getAmountCoverage(), previous.getHarvest().getAmountCoverage());
            case WATERING, WEEDING, PRUNING, ETC -> { }
        }
        return Collections.unmodifiableList(result);
    }
    private void compare(List<ReportFeedbackComparison> result, String key, String label, BigDecimal current, BigDecimal previous, String unit, Coverage currentCoverage, Coverage previousCoverage) {
        if (current == null || previous == null) return;
        var difference = current.subtract(previous);
        result.add(new ReportFeedbackComparison(key, label, current.stripTrailingZeros(), previous.stripTrailingZeros(), difference.stripTrailingZeros(), previous.compareTo(BigDecimal.ZERO) == 0 ? null : difference.multiply(BigDecimal.valueOf(100)).divide(previous, 2, RoundingMode.HALF_UP).stripTrailingZeros(), unit, currentCoverage, previousCoverage));
    }
    private CommonComparableStatistics commonFor(CycleReportStatistics statistics, WorkType type) {
        return switch (type) {
            case PLANTING -> common(statistics.getPlanting()); case WATERING -> common(statistics.getWatering());
            case FERTILIZING -> common(statistics.getFertilizing()); case PEST_CONTROL -> common(statistics.getPestControl());
            case WEEDING -> common(statistics.getWeeding()); case PRUNING -> common(statistics.getPruning());
            case HARVEST -> common(statistics.getHarvest()); case ETC -> common(statistics.getEtc());
        };
    }
    private CommonComparableStatistics common(PlantingStatistics s) { return new CommonComparableStatistics(s.getRecordCount(), s.getWorkedDayCount(), s.getAverageIntervalDays(), s.getPhotoAttachmentRatePct()); }
    private CommonComparableStatistics common(WateringStatistics s) { return new CommonComparableStatistics(s.getRecordCount(), s.getWorkedDayCount(), s.getAverageIntervalDays(), s.getPhotoAttachmentRatePct()); }
    private CommonComparableStatistics common(FertilizingStatistics s) { return new CommonComparableStatistics(s.getRecordCount(), s.getWorkedDayCount(), s.getAverageIntervalDays(), s.getPhotoAttachmentRatePct()); }
    private CommonComparableStatistics common(PestControlStatistics s) { return new CommonComparableStatistics(s.getRecordCount(), s.getWorkedDayCount(), s.getAverageIntervalDays(), s.getPhotoAttachmentRatePct()); }
    private CommonComparableStatistics common(WeedingStatistics s) { return new CommonComparableStatistics(s.getRecordCount(), s.getWorkedDayCount(), s.getAverageIntervalDays(), s.getPhotoAttachmentRatePct()); }
    private CommonComparableStatistics common(CommonOnlyStatistics s) { return new CommonComparableStatistics(s.getRecordCount(), s.getWorkedDayCount(), s.getAverageIntervalDays(), s.getPhotoAttachmentRatePct()); }
    private CommonComparableStatistics common(HarvestStatistics s) { return new CommonComparableStatistics(s.getRecordCount(), s.getWorkedDayCount(), s.getAverageIntervalDays(), s.getPhotoAttachmentRatePct()); }
    private record CommonComparableStatistics(int recordCount, int workedDayCount, BigDecimal averageIntervalDays, BigDecimal photoAttachmentRatePct) {}
}
