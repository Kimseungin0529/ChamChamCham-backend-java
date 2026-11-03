package com.chamchamcham.application.report

import com.chamchamcham.domain.farming.WorkType
import com.chamchamcham.domain.report.AmountByUnit
import com.chamchamcham.domain.report.CategoryAmountByUnit
import com.chamchamcham.domain.report.CategoryMethodStatistics
import com.chamchamcham.domain.report.CommonOnlyStatistics
import com.chamchamcham.domain.report.CountDistribution
import com.chamchamcham.domain.report.Coverage
import com.chamchamcham.domain.report.CycleReportStatistics
import com.chamchamcham.domain.report.FertilizingStatistics
import com.chamchamcham.domain.report.GrowthPeriodRange
import com.chamchamcham.domain.report.HarvestPartStatistics
import com.chamchamcham.domain.report.HarvestStatistics
import com.chamchamcham.domain.report.MaterialCategoryStatistics
import com.chamchamcham.domain.report.PestControlStatistics
import com.chamchamcham.domain.report.PlantingStatistics
import com.chamchamcham.domain.report.PropagationStatistics
import com.chamchamcham.domain.report.TargetCount
import com.chamchamcham.domain.report.WateringStatistics
import com.chamchamcham.domain.report.WeedingStatistics
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

@Component
class CycleReportStatisticsCalculator {
    fun calculate(records: List<CycleReportSourceRecord>): CycleReportStatistics {
        val grouped = records.groupBy { it.workType }
        return CycleReportStatistics(
            calculatePlanting(grouped[WorkType.PLANTING].orEmpty()),
            calculateWatering(grouped[WorkType.WATERING].orEmpty()),
            calculateFertilizing(grouped[WorkType.FERTILIZING].orEmpty()),
            calculatePestControl(grouped[WorkType.PEST_CONTROL].orEmpty()),
            calculateWeeding(grouped[WorkType.WEEDING].orEmpty()),
            commonOnly(grouped[WorkType.PRUNING].orEmpty()),
            calculateHarvest(grouped[WorkType.HARVEST].orEmpty()),
            commonOnly(grouped[WorkType.ETC].orEmpty()),
        )
    }

    private fun calculatePlanting(records: List<CycleReportSourceRecord>): PlantingStatistics {
        val common = common(records)
        val details = records.mapNotNull { it.planting }
        return PlantingStatistics(
            common.recordCount,
            common.firstWorkedOn,
            common.lastWorkedOn,
            common.workedDayCount,
            common.averageIntervalDays,
            common.photoAttachedRecordCount,
            common.photoAttachmentRatePct,
            common.weatherDistribution,
            common.averageTemperatureC,
            distribution(
                values = details.map(PlantingReportSource::plantingMethod),
                denominator = records.size,
            ),
            details
                .groupBy { it.propagationMethod ?: it.plantingMethod }
                .map { (method, items) ->
                    val knownQuantities = items.mapNotNull { it.quantity }
                    PropagationStatistics(
                        method.code,
                        method.label,
                        items.size,
                        requireNotNull(StatisticsMath.percentage(items.size, records.size)),
                        knownQuantities.sumOrNullScale4(),
                        items.mapNotNull { it.quantityUnit }.distinct().singleOrNull(),
                        Coverage(knownQuantities.size, items.size),
                    )
                }
                .sortedWith(compareByDescending<PropagationStatistics> { it.recordCount }.thenBy { it.code }),
        )
    }

    private fun calculateWatering(records: List<CycleReportSourceRecord>): WateringStatistics {
        val common = common(records)
        return WateringStatistics(
            common.recordCount,
            common.firstWorkedOn,
            common.lastWorkedOn,
            common.workedDayCount,
            common.averageIntervalDays,
            common.photoAttachedRecordCount,
            common.photoAttachmentRatePct,
            common.weatherDistribution,
            common.averageTemperatureC,
            distribution(records.map { it.watering?.amount }, records.size),
            distribution(records.map { it.watering?.method }, records.size),
        )
    }

    private fun calculateFertilizing(records: List<CycleReportSourceRecord>): FertilizingStatistics {
        val common = common(records)
        val details = records.mapNotNull { it.fertilizing }
        val knownAmounts = details.mapNotNull { it.amountKg }
        val totalAmount = knownAmounts.sumOrNullScale4()
        return FertilizingStatistics(
            common.recordCount,
            common.firstWorkedOn,
            common.lastWorkedOn,
            common.workedDayCount,
            common.averageIntervalDays,
            common.photoAttachedRecordCount,
            common.photoAttachmentRatePct,
            common.weatherDistribution,
            common.averageTemperatureC,
            totalAmount,
            StatisticsMath.average(knownAmounts),
            Coverage(knownAmounts.size, records.size),
            distribution(
                details.map { CategoryRef(it.materialName, it.materialName) },
                records.size,
            ),
            calculateMaterialCategories(details, records.size, totalAmount),
            distribution(records.map { it.fertilizing?.applicationMethod }, records.size),
            calculateCategoryMethods(details),
        )
    }

    private fun calculatePestControl(records: List<CycleReportSourceRecord>): PestControlStatistics {
        val common = common(records)
        val details = records.mapNotNull { it.pestControl }
        val knownSprayAmounts = details.mapNotNull { it.totalSprayAmountLiters }
        return PestControlStatistics(
            common.recordCount,
            common.firstWorkedOn,
            common.lastWorkedOn,
            common.workedDayCount,
            common.averageIntervalDays,
            common.photoAttachedRecordCount,
            common.photoAttachmentRatePct,
            common.weatherDistribution,
            common.averageTemperatureC,
            distribution(
                details.map { CategoryRef(it.pesticideId.toString(), it.pesticideName) },
                records.size,
            ),
            details
                .groupBy { it.pesticideAmountUnit }
                .map { (unit, items) ->
                    val knownAmounts = items.mapNotNull { it.pesticideAmount }
                    AmountByUnit(unit, knownAmounts.sumScale4(), Coverage(knownAmounts.size, items.size))
                }
                .sortedBy { it.unit },
            details
                .groupBy { CategoryRef(it.pesticideId.toString(), it.pesticideName) to it.pesticideAmountUnit }
                .map { (key, items) ->
                    val (category, unit) = key
                    val knownAmounts = items.mapNotNull { it.pesticideAmount }
                    CategoryAmountByUnit(
                        category.code,
                        category.label,
                        unit,
                        items.size,
                        knownAmounts.sumScale4(),
                        Coverage(knownAmounts.size, items.size),
                    )
                }
                .sortedWith(compareBy<CategoryAmountByUnit> { it.categoryCode }.thenBy { it.unit }),
            knownSprayAmounts.sumOrNullScale4(),
            Coverage(knownSprayAmounts.size, records.size),
            details
                .mapNotNull { it.pestName?.trim()?.takeIf(String::isNotEmpty) }
                .groupingBy { it }
                .eachCount()
                .map { (target, count) -> TargetCount(target, count) }
                .sortedWith(compareByDescending<TargetCount> { it.count }.thenBy { it.target }),
        )
    }

    private fun calculateWeeding(records: List<CycleReportSourceRecord>): WeedingStatistics {
        val common = common(records)
        return WeedingStatistics(
            common.recordCount,
            common.firstWorkedOn,
            common.lastWorkedOn,
            common.workedDayCount,
            common.averageIntervalDays,
            common.photoAttachedRecordCount,
            common.photoAttachmentRatePct,
            common.weatherDistribution,
            common.averageTemperatureC,
            distribution(records.map { it.weeding?.method }, records.size),
        )
    }

    private fun calculateHarvest(records: List<CycleReportSourceRecord>): HarvestStatistics {
        val common = common(records)
        val details = records.mapNotNull { it.harvest }
        val knownAmounts = details.mapNotNull { it.amountKg }
        val totalAmount = knownAmounts.sumOrNullScale4()
        val harvestDates = records.map { it.workedAt.toLocalDate() }
        return HarvestStatistics(
            common.recordCount,
            common.firstWorkedOn,
            common.lastWorkedOn,
            common.workedDayCount,
            common.averageIntervalDays,
            common.photoAttachedRecordCount,
            common.photoAttachmentRatePct,
            common.weatherDistribution,
            common.averageTemperatureC,
            totalAmount,
            StatisticsMath.average(knownAmounts),
            Coverage(knownAmounts.size, records.size),
            harvestDates.minOrNull(),
            harvestDates.maxOrNull(),
            calculateHarvestParts(details, records.size, totalAmount),
            records
                .sortedWith(compareBy(CycleReportSourceRecord::workedAt, CycleReportSourceRecord::id))
                .mapNotNull { it.harvest }
                .lastOrNull { it.isLastHarvest }
                ?.growthPeriodMonths,
            details
                .mapNotNull { it.growthPeriodMonths }
                .takeIf { it.isNotEmpty() }
                ?.let { GrowthPeriodRange(requireNotNull(it.minOrNull()), requireNotNull(it.maxOrNull())) },
            distribution(
                values = details.mapNotNull { detail ->
                    detail.growthPeriodMonths?.let { months ->
                        CategoryRef(code = months.toString(), label = "${months}개월")
                    }
                },
                denominator = records.size,
            ),
        )
    }

    private fun commonOnly(records: List<CycleReportSourceRecord>): CommonOnlyStatistics {
        val common = common(records)
        return CommonOnlyStatistics(
            common.recordCount,
            common.firstWorkedOn,
            common.lastWorkedOn,
            common.workedDayCount,
            common.averageIntervalDays,
            common.photoAttachedRecordCount,
            common.photoAttachmentRatePct,
            common.weatherDistribution,
            common.averageTemperatureC,
        )
    }

    private fun calculateMaterialCategories(
        details: List<FertilizingReportSource>,
        recordCount: Int,
        totalAmount: BigDecimal?,
    ): List<MaterialCategoryStatistics> {
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) == 0) return emptyList()
        return details
            .filter { it.amountKg != null }
            .groupBy { it.materialName }
            .map { (materialName, items) ->
                val amount = items.mapNotNull { it.amountKg }.sumScale4()
                MaterialCategoryStatistics(
                    materialName,
                    materialName,
                    items.size,
                    requireNotNull(StatisticsMath.percentage(items.size, recordCount)),
                    amount,
                    requireNotNull(StatisticsMath.percentage(amount, totalAmount)),
                )
            }
            .sortedWith(compareByDescending<MaterialCategoryStatistics> { it.recordCount }.thenBy { it.code })
    }

    private fun calculateCategoryMethods(
        details: List<FertilizingReportSource>,
    ): List<CategoryMethodStatistics> =
        details
            .groupBy { it.materialName }
            .toSortedMap()
            .flatMap { (materialName, materialItems) ->
                materialItems
                    .map { it.applicationMethod ?: CategoryRef("MISSING", "미입력") }
                    .groupingBy { it }
                    .eachCount()
                    .map { (method, count) ->
                        CategoryMethodStatistics(
                            materialName,
                            materialName,
                            method.code,
                            method.label,
                            count,
                            requireNotNull(StatisticsMath.percentage(count, materialItems.size)),
                        )
                    }
                    .sortedBy { it.methodCode }
            }

    private fun calculateHarvestParts(
        details: List<HarvestReportSource>,
        recordCount: Int,
        totalAmount: BigDecimal?,
    ): List<HarvestPartStatistics> =
        details
            .mapNotNull { detail -> detail.medicinalPart?.let { it to detail } }
            .groupBy(keySelector = { it.first }, valueTransform = { it.second })
            .map { (part, items) ->
                val knownAmounts = items.mapNotNull { it.amountKg }
                val partAmount = knownAmounts.sumOrNullScale4()
                HarvestPartStatistics(
                    part.code,
                    part.label,
                    items.size,
                    requireNotNull(StatisticsMath.percentage(items.size, recordCount)),
                    partAmount,
                    partAmount?.let { amount ->
                        totalAmount?.takeIf { it.compareTo(BigDecimal.ZERO) != 0 }?.let {
                            StatisticsMath.percentage(amount, it)
                        }
                    },
                    Coverage(knownAmounts.size, items.size),
                )
            }
            .sortedWith(compareByDescending<HarvestPartStatistics> { it.recordCount }.thenBy { it.code })

    private fun common(records: List<CycleReportSourceRecord>): CommonValues {
        val dates = records.map { it.workedAt.toLocalDate() }
        val photoCount = records.count { it.hasPhoto }
        return CommonValues(
            recordCount = records.size,
            firstWorkedOn = dates.minOrNull(),
            lastWorkedOn = dates.maxOrNull(),
            workedDayCount = dates.distinct().size,
            averageIntervalDays = StatisticsMath.averageIntervalDays(dates),
            photoAttachedRecordCount = photoCount,
            photoAttachmentRatePct = StatisticsMath.percentage(photoCount, records.size),
            weatherDistribution = distribution(
                values = records.map {
                    it.weatherCondition.trim().takeIf(String::isNotEmpty)
                        ?.let { value -> CategoryRef(value, value) }
                },
                denominator = records.size,
            ),
            averageTemperatureC = StatisticsMath.average(
                records.map { BigDecimal(it.weatherTemperature) },
            ),
        )
    }

    private fun distribution(values: List<CategoryRef?>, denominator: Int): List<CountDistribution> =
        values.map { it ?: CategoryRef("MISSING", "미입력") }
            .groupingBy { it }
            .eachCount()
            .map { (item, count) ->
                CountDistribution(item.code, item.label, count, requireNotNull(StatisticsMath.percentage(count, denominator)))
            }
            .sortedWith(compareByDescending<CountDistribution> { it.count }.thenBy { it.code })

    private fun List<BigDecimal>.sumScale4(): BigDecimal =
        fold(BigDecimal.ZERO) { total, value -> total + value }.setScale(4, RoundingMode.HALF_UP)

    private fun List<BigDecimal>.sumOrNullScale4(): BigDecimal? =
        takeIf { it.isNotEmpty() }?.sumScale4()

    private data class CommonValues(
        val recordCount: Int,
        val firstWorkedOn: LocalDate?,
        val lastWorkedOn: LocalDate?,
        val workedDayCount: Int,
        val averageIntervalDays: BigDecimal?,
        val photoAttachedRecordCount: Int,
        val photoAttachmentRatePct: BigDecimal?,
        val weatherDistribution: List<CountDistribution>,
        val averageTemperatureC: BigDecimal?,
    )
}
