package com.chamchamcham.application.report;

import com.chamchamcham.domain.farming.WorkType;
import com.chamchamcham.domain.report.*;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class CycleReportStatisticsCalculator {
    public CycleReportStatistics calculate(List<CycleReportSourceRecord> records) {
        var grouped = group(records, CycleReportSourceRecord::getWorkType);
        return new CycleReportStatistics(calculatePlanting(grouped.getOrDefault(WorkType.PLANTING, List.of())),
            calculateWatering(grouped.getOrDefault(WorkType.WATERING, List.of())),
            calculateFertilizing(grouped.getOrDefault(WorkType.FERTILIZING, List.of())),
            calculatePestControl(grouped.getOrDefault(WorkType.PEST_CONTROL, List.of())),
            calculateWeeding(grouped.getOrDefault(WorkType.WEEDING, List.of())),
            commonOnly(grouped.getOrDefault(WorkType.PRUNING, List.of())),
            calculateHarvest(grouped.getOrDefault(WorkType.HARVEST, List.of())),
            commonOnly(grouped.getOrDefault(WorkType.ETC, List.of())));
    }

    private PlantingStatistics calculatePlanting(List<CycleReportSourceRecord> records) {
        var c = common(records);
        var details = nonNull(records, CycleReportSourceRecord::getPlanting);
        var methods = new ArrayList<PropagationStatistics>();
        group(details, d -> d.getPropagationMethod() != null ? d.getPropagationMethod() : d.getPlantingMethod())
            .forEach((method, items) -> {
                var known = nonNull(items, PlantingReportSource::getQuantity);
                var units = nonNull(items, PlantingReportSource::getQuantityUnit).stream().distinct().toList();
                methods.add(new PropagationStatistics(method.getCode(), method.getLabel(), items.size(),
                    Objects.requireNonNull(StatisticsMath.percentage(items.size(), records.size())),
                    sumOrNullScale4(known), units.size() == 1 ? units.getFirst() : null,
                    new Coverage(known.size(), items.size())));
            });
        methods.sort(Comparator.comparingInt(PropagationStatistics::recordCount).reversed().thenComparing(PropagationStatistics::code));
        return new PlantingStatistics(c.recordCount(), c.firstWorkedOn(), c.lastWorkedOn(), c.workedDayCount(), c.averageIntervalDays(), c.photoAttachedRecordCount(), c.photoAttachmentRatePct(), c.weatherDistribution(), c.averageTemperatureC(),
            distribution(details.stream().map(PlantingReportSource::getPlantingMethod).toList(), records.size()), methods);
    }

    private WateringStatistics calculateWatering(List<CycleReportSourceRecord> records) {
        var c = common(records);
        return new WateringStatistics(c.recordCount(), c.firstWorkedOn(), c.lastWorkedOn(), c.workedDayCount(), c.averageIntervalDays(), c.photoAttachedRecordCount(), c.photoAttachmentRatePct(), c.weatherDistribution(), c.averageTemperatureC(),
            distribution(records.stream().map(r -> r.getWatering() == null ? null : r.getWatering().getAmount()).toList(), records.size()),
            distribution(records.stream().map(r -> r.getWatering() == null ? null : r.getWatering().getMethod()).toList(), records.size()));
    }

    private FertilizingStatistics calculateFertilizing(List<CycleReportSourceRecord> records) {
        var c = common(records);
        var details = nonNull(records, CycleReportSourceRecord::getFertilizing);
        var known = nonNull(details, FertilizingReportSource::getAmountKg);
        var total = sumOrNullScale4(known);
        return new FertilizingStatistics(c.recordCount(), c.firstWorkedOn(), c.lastWorkedOn(), c.workedDayCount(), c.averageIntervalDays(), c.photoAttachedRecordCount(), c.photoAttachmentRatePct(), c.weatherDistribution(), c.averageTemperatureC(), total, StatisticsMath.average(known),
            new Coverage(known.size(), records.size()),
            distribution(details.stream().map(d -> new CategoryRef(d.getMaterialName(), d.getMaterialName())).toList(), records.size()),
            calculateMaterialCategories(details, records.size(), total),
            distribution(records.stream().map(r -> r.getFertilizing() == null ? null : r.getFertilizing().getApplicationMethod()).toList(), records.size()),
            calculateCategoryMethods(details));
    }

    private PestControlStatistics calculatePestControl(List<CycleReportSourceRecord> records) {
        var c = common(records);
        var details = nonNull(records, CycleReportSourceRecord::getPestControl);
        var knownSprays = nonNull(details, PestControlReportSource::getTotalSprayAmountLiters);
        var amounts = new ArrayList<AmountByUnit>();
        group(details, PestControlReportSource::getPesticideAmountUnit).forEach((unit, items) -> {
            var known = nonNull(items, PestControlReportSource::getPesticideAmount);
            amounts.add(new AmountByUnit(unit, sumScale4(known), new Coverage(known.size(), items.size())));
        });
        amounts.sort(Comparator.comparing(AmountByUnit::unit));
        var categoryAmounts = new ArrayList<CategoryAmountByUnit>();
        group(details, d -> new CategoryUnit(new CategoryRef(d.getPesticideId().toString(), d.getPesticideName()), d.getPesticideAmountUnit()))
            .forEach((key, items) -> {
                var known = nonNull(items, PestControlReportSource::getPesticideAmount);
                categoryAmounts.add(new CategoryAmountByUnit(key.category().getCode(), key.category().getLabel(), key.unit(),
                    items.size(), sumScale4(known), new Coverage(known.size(), items.size())));
            });
        categoryAmounts.sort(Comparator.comparing(CategoryAmountByUnit::categoryCode).thenComparing(CategoryAmountByUnit::unit));
        var targets = new ArrayList<TargetCount>();
        var names = nonNull(details, d -> d.getPestName() == null ? null : trim(d.getPestName()));
        group(names.stream().filter(n -> !n.isEmpty()).toList(), Function.identity())
            .forEach((target, items) -> targets.add(new TargetCount(target, items.size())));
        targets.sort(Comparator.comparingInt(TargetCount::count).reversed().thenComparing(TargetCount::target));
        return new PestControlStatistics(c.recordCount(), c.firstWorkedOn(), c.lastWorkedOn(), c.workedDayCount(), c.averageIntervalDays(), c.photoAttachedRecordCount(), c.photoAttachmentRatePct(), c.weatherDistribution(), c.averageTemperatureC(),
            distribution(details.stream().map(d -> new CategoryRef(d.getPesticideId().toString(), d.getPesticideName())).toList(), records.size()),
            amounts, categoryAmounts, sumOrNullScale4(knownSprays), new Coverage(knownSprays.size(), records.size()), targets);
    }

    private WeedingStatistics calculateWeeding(List<CycleReportSourceRecord> records) {
        var c = common(records);
        return new WeedingStatistics(c.recordCount(), c.firstWorkedOn(), c.lastWorkedOn(), c.workedDayCount(), c.averageIntervalDays(), c.photoAttachedRecordCount(), c.photoAttachmentRatePct(), c.weatherDistribution(), c.averageTemperatureC(),
            distribution(records.stream().map(r -> r.getWeeding() == null ? null : r.getWeeding().getMethod()).toList(), records.size()));
    }

    private HarvestStatistics calculateHarvest(List<CycleReportSourceRecord> records) {
        var c = common(records);
        var details = nonNull(records, CycleReportSourceRecord::getHarvest);
        var known = nonNull(details, HarvestReportSource::getAmountKg);
        var total = sumOrNullScale4(known);
        var dates = records.stream().map(r -> r.getWorkedAt().toLocalDate()).toList();
        Integer finalGrowth = null;
        for (var record : records.stream().sorted(Comparator.comparing(CycleReportSourceRecord::getWorkedAt)
                .thenComparing(CycleReportSourceRecord::getId)).toList()) {
            if (record.getHarvest() != null && record.getHarvest().isLastHarvest()) finalGrowth = record.getHarvest().getGrowthPeriodMonths();
        }
        var growth = nonNull(details, HarvestReportSource::getGrowthPeriodMonths);
        return new HarvestStatistics(c.recordCount(), c.firstWorkedOn(), c.lastWorkedOn(), c.workedDayCount(), c.averageIntervalDays(), c.photoAttachedRecordCount(), c.photoAttachmentRatePct(), c.weatherDistribution(), c.averageTemperatureC(), total, StatisticsMath.average(known), new Coverage(known.size(), records.size()),
            dates.stream().min(Comparator.naturalOrder()).orElse(null), dates.stream().max(Comparator.naturalOrder()).orElse(null),
            calculateHarvestParts(details, records.size(), total), finalGrowth,
            growth.isEmpty() ? null : new GrowthPeriodRange(Collections.min(growth), Collections.max(growth)),
            distribution(growth.stream().map(months -> new CategoryRef(months.toString(), months + "개월")).toList(), records.size()));
    }

    private CommonOnlyStatistics commonOnly(List<CycleReportSourceRecord> records) {
        var c = common(records);
        return new CommonOnlyStatistics(c.recordCount(), c.firstWorkedOn(), c.lastWorkedOn(), c.workedDayCount(), c.averageIntervalDays(), c.photoAttachedRecordCount(), c.photoAttachmentRatePct(), c.weatherDistribution(), c.averageTemperatureC());
    }

    private List<MaterialCategoryStatistics> calculateMaterialCategories(List<FertilizingReportSource> details, int recordCount, BigDecimal totalAmount) {
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) == 0) return List.of();
        var result = new ArrayList<MaterialCategoryStatistics>();
        group(details.stream().filter(d -> d.getAmountKg() != null).toList(), FertilizingReportSource::getMaterialName)
            .forEach((name, items) -> {
                var amount = sumScale4(nonNull(items, FertilizingReportSource::getAmountKg));
                result.add(new MaterialCategoryStatistics(name, name, items.size(),
                    Objects.requireNonNull(StatisticsMath.percentage(items.size(), recordCount)), amount,
                    Objects.requireNonNull(StatisticsMath.percentage(amount, totalAmount))));
            });
        result.sort(Comparator.comparingInt(MaterialCategoryStatistics::recordCount).reversed().thenComparing(MaterialCategoryStatistics::code));
        return result;
    }

    private List<CategoryMethodStatistics> calculateCategoryMethods(List<FertilizingReportSource> details) {
        var result = new ArrayList<CategoryMethodStatistics>();
        new TreeMap<>(group(details, FertilizingReportSource::getMaterialName)).forEach((name, items) -> {
            var methods = new ArrayList<CategoryMethodStatistics>();
            group(items, d -> d.getApplicationMethod() != null ? d.getApplicationMethod() : new CategoryRef("MISSING", "미입력"))
                .forEach((method, values) -> methods.add(new CategoryMethodStatistics(name, name, method.getCode(), method.getLabel(),
                    values.size(), Objects.requireNonNull(StatisticsMath.percentage(values.size(), items.size())))));
            methods.sort(Comparator.comparing(CategoryMethodStatistics::methodCode));
            result.addAll(methods);
        });
        return result;
    }

    private List<HarvestPartStatistics> calculateHarvestParts(List<HarvestReportSource> details, int recordCount, BigDecimal totalAmount) {
        var result = new ArrayList<HarvestPartStatistics>();
        group(details.stream().filter(d -> d.getMedicinalPart() != null).toList(), HarvestReportSource::getMedicinalPart)
            .forEach((part, items) -> {
                var known = nonNull(items, HarvestReportSource::getAmountKg);
                var amount = sumOrNullScale4(known);
                result.add(new HarvestPartStatistics(part.getCode(), part.getLabel(), items.size(),
                    Objects.requireNonNull(StatisticsMath.percentage(items.size(), recordCount)), amount,
                    amount != null && totalAmount != null && totalAmount.compareTo(BigDecimal.ZERO) != 0
                        ? StatisticsMath.percentage(amount, totalAmount) : null,
                    new Coverage(known.size(), items.size())));
            });
        result.sort(Comparator.comparingInt(HarvestPartStatistics::recordCount).reversed().thenComparing(HarvestPartStatistics::code));
        return result;
    }

    private CommonValues common(List<CycleReportSourceRecord> records) {
        var dates = records.stream().map(r -> r.getWorkedAt().toLocalDate()).toList();
        int photos = (int) records.stream().filter(CycleReportSourceRecord::getHasPhoto).count();
        return new CommonValues(records.size(), dates.stream().min(Comparator.naturalOrder()).orElse(null),
            dates.stream().max(Comparator.naturalOrder()).orElse(null), (int) dates.stream().distinct().count(),
            StatisticsMath.averageIntervalDays(dates), photos, StatisticsMath.percentage(photos, records.size()),
            distribution(records.stream().map(r -> {
                var value = trim(r.getWeatherCondition());
                return value.isEmpty() ? null : new CategoryRef(value, value);
            }).toList(), records.size()),
            StatisticsMath.average(records.stream().map(r -> BigDecimal.valueOf(r.getWeatherTemperature())).toList()));
    }

    private List<CountDistribution> distribution(List<CategoryRef> values, int denominator) {
        var result = new ArrayList<CountDistribution>();
        group(values.stream().map(v -> v != null ? v : new CategoryRef("MISSING", "미입력")).toList(), Function.identity())
            .forEach((item, items) -> result.add(new CountDistribution(item.getCode(), item.getLabel(), items.size(),
                Objects.requireNonNull(StatisticsMath.percentage(items.size(), denominator)))));
        result.sort(Comparator.comparingInt(CountDistribution::count).reversed().thenComparing(CountDistribution::code));
        return result;
    }

    private static <T, K> Map<K, List<T>> group(List<T> values, Function<T, K> key) {
        return values.stream().collect(Collectors.groupingBy(key, LinkedHashMap::new, Collectors.toList()));
    }

    private static <T, V> List<V> nonNull(List<T> values, Function<T, V> mapper) {
        return values.stream().map(mapper).filter(Objects::nonNull).collect(Collectors.toCollection(ArrayList::new));
    }

    private BigDecimal sumScale4(List<BigDecimal> values) {
        return values.stream().reduce(BigDecimal.ZERO, BigDecimal::add).setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal sumOrNullScale4(List<BigDecimal> values) {
        return values.isEmpty() ? null : sumScale4(values);
    }

    private static String trim(String value) {
        int start = 0, end = value.length();
        while (start < end && (Character.isWhitespace(value.charAt(start)) || Character.isSpaceChar(value.charAt(start)))) start++;
        while (end > start && (Character.isWhitespace(value.charAt(end - 1)) || Character.isSpaceChar(value.charAt(end - 1)))) end--;
        return value.substring(start, end);
    }

    private record CategoryUnit(CategoryRef category, String unit) {}
    private record CommonValues(int recordCount, LocalDate firstWorkedOn, LocalDate lastWorkedOn, int workedDayCount,
        BigDecimal averageIntervalDays, int photoAttachedRecordCount, BigDecimal photoAttachmentRatePct,
        List<CountDistribution> weatherDistribution, BigDecimal averageTemperatureC) {}
}
