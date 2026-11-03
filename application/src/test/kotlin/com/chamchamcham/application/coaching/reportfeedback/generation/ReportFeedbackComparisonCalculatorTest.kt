package com.chamchamcham.application.coaching.reportfeedback.generation

import com.chamchamcham.domain.report.WeedingStatistics
import com.chamchamcham.domain.report.CommonOnlyStatistics
import com.chamchamcham.domain.farming.WorkType
import com.chamchamcham.domain.report.AmountByUnit
import com.chamchamcham.domain.report.Coverage
import com.chamchamcham.domain.report.CycleReportStatistics
import com.chamchamcham.domain.report.FertilizingStatistics
import com.chamchamcham.domain.report.HarvestStatistics
import com.chamchamcham.domain.report.PestControlStatistics
import com.chamchamcham.domain.report.PlantingStatistics
import com.chamchamcham.domain.report.PropagationStatistics
import com.chamchamcham.domain.report.WateringStatistics
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class ReportFeedbackComparisonCalculatorTest {
    private val calculator = ReportFeedbackComparisonCalculator()

    @Test
    fun `common metrics use stable order and deterministic differences`() {
        val current = CycleReportStatistics(
            PlantingStatistics.empty(),
            WateringStatistics(6, null, null, 5, decimal("2.5"), 0, decimal("75.0"), emptyList(), null, emptyList(), emptyList()),
            FertilizingStatistics.empty(),
            PestControlStatistics.empty(),
            WeedingStatistics.empty(),
            CommonOnlyStatistics.empty(),
            HarvestStatistics.empty(),
            CommonOnlyStatistics.empty(),
        )
        val previous = CycleReportStatistics(
            PlantingStatistics.empty(),
            WateringStatistics(4, null, null, 4, decimal("4.0"), 0, decimal("50.0"), emptyList(), null, emptyList(), emptyList()),
            FertilizingStatistics.empty(),
            PestControlStatistics.empty(),
            WeedingStatistics.empty(),
            CommonOnlyStatistics.empty(),
            HarvestStatistics.empty(),
            CommonOnlyStatistics.empty(),
        )

        val comparisons = calculator.calculate(WorkType.WATERING, current, previous)

        assertThat(comparisons.map { it.metricKey }).containsExactly(
            "recordCount",
            "workedDayCount",
            "averageIntervalDays",
            "photoAttachmentRatePct",
        )
        assertComparison(comparisons[0], "기록 횟수", "6", "4", "2", "50", "회")
        assertComparison(comparisons[1], "작업한 날짜 수", "5", "4", "1", "25", "일")
        assertComparison(comparisons[2], "평균 작업 간격", "2.5", "4", "-1.5", "-37.5", "일")
        assertComparison(comparisons[3], "사진 첨부 비율", "75", "50", "25", "50", "%")
    }

    @Test
    fun `previous zero omits relative percentage and null metrics are omitted`() {
        val current = CycleReportStatistics(
            PlantingStatistics.empty(),
            WateringStatistics(2, null, null, 2, null, 0, decimal("40"), emptyList(), null, emptyList(), emptyList()),
            FertilizingStatistics.empty(),
            PestControlStatistics.empty(),
            WeedingStatistics.empty(),
            CommonOnlyStatistics.empty(),
            HarvestStatistics.empty(),
            CommonOnlyStatistics.empty(),
        )
        val previous = CycleReportStatistics(
            PlantingStatistics.empty(),
            WateringStatistics(0, null, null, 0, decimal("3"), 0, null, emptyList(), null, emptyList(), emptyList()),
            FertilizingStatistics.empty(),
            PestControlStatistics.empty(),
            WeedingStatistics.empty(),
            CommonOnlyStatistics.empty(),
            HarvestStatistics.empty(),
            CommonOnlyStatistics.empty(),
        )

        val comparisons = calculator.calculate(WorkType.WATERING, current, previous)

        assertThat(comparisons.map { it.metricKey }).containsExactly("recordCount", "workedDayCount")
        assertThat(comparisons).allMatch { it.relativeChangePct == null }
        assertThat(comparisons.map { it.difference }).allSatisfy { assertThat(it).isEqualByComparingTo("2") }
    }

    @Test
    fun `planting compares the same propagation and unit and preserves coverage`() {
        val currentCoverage = Coverage(2, 3)
        val previousCoverage = Coverage(1, 2)
        val current = CycleReportStatistics(
            PlantingStatistics(
            3,
            null,
            null,
            2,
            null,
            0,
            null,
            emptyList(),
            null,
            emptyList(),
            listOf(
                    propagation("SEEDLING", "모종", "12", "JU", currentCoverage),
                    propagation("SEED", "씨앗", "300", "G", Coverage(1, 1)),
                ),
        ),
            WateringStatistics.empty(),
            FertilizingStatistics.empty(),
            PestControlStatistics.empty(),
            WeedingStatistics.empty(),
            CommonOnlyStatistics.empty(),
            HarvestStatistics.empty(),
            CommonOnlyStatistics.empty(),
        )
        val previous = CycleReportStatistics(
            PlantingStatistics(
            2,
            null,
            null,
            2,
            null,
            0,
            null,
            emptyList(),
            null,
            emptyList(),
            listOf(
                    propagation("SEED", "씨앗", "250", "ML", Coverage(1, 1)),
                    propagation("SEEDLING", "모종", "10", "JU", previousCoverage),
                ),
        ),
            WateringStatistics.empty(),
            FertilizingStatistics.empty(),
            PestControlStatistics.empty(),
            WeedingStatistics.empty(),
            CommonOnlyStatistics.empty(),
            HarvestStatistics.empty(),
            CommonOnlyStatistics.empty(),
        )

        val comparisons = calculator.calculate(WorkType.PLANTING, current, previous)

        assertThat(comparisons.map { it.metricKey }).containsExactly(
            "recordCount",
            "workedDayCount",
            "planting.quantity.SEEDLING.JU",
        )
        val quantity = comparisons.last()
        assertComparison(quantity, "모종 심은 양", "12", "10", "2", "20", "JU")
        assertThat(quantity.currentCoverage).isEqualTo(currentCoverage)
        assertThat(quantity.previousCoverage).isEqualTo(previousCoverage)
    }

    @Test
    fun `fertilizing and harvest compare normalized kilograms only for the selected work`() {
        val current = CycleReportStatistics(
            PlantingStatistics.empty(),
            WateringStatistics.empty(),
            FertilizingStatistics(
                2,
                null,
                null,
                2,
                null,
                0,
                null,
                emptyList(),
                null,
                decimal("1.5000"),
                null,
                Coverage(2, 3),
                emptyList(),
                emptyList(),
                emptyList(),
                emptyList(),
            ),
            PestControlStatistics.empty(),
            WeedingStatistics.empty(),
            CommonOnlyStatistics.empty(),
            HarvestStatistics(
                4,
                null,
                null,
                3,
                null,
                0,
                null,
                emptyList(),
                null,
                decimal("18.0000"),
                null,
                Coverage(3, 4),
                null,
                null,
                emptyList(),
                null,
                null,
                emptyList(),
            ),
            CommonOnlyStatistics.empty(),
        )
        val previous = CycleReportStatistics(
            PlantingStatistics.empty(),
            WateringStatistics.empty(),
            FertilizingStatistics(
                1,
                null,
                null,
                1,
                null,
                0,
                null,
                emptyList(),
                null,
                decimal("1.0000"),
                null,
                Coverage(1, 2),
                emptyList(),
                emptyList(),
                emptyList(),
                emptyList(),
            ),
            PestControlStatistics.empty(),
            WeedingStatistics.empty(),
            CommonOnlyStatistics.empty(),
            HarvestStatistics(
                3,
                null,
                null,
                3,
                null,
                0,
                null,
                emptyList(),
                null,
                decimal("12.0000"),
                null,
                Coverage(3, 3),
                null,
                null,
                emptyList(),
                null,
                null,
                emptyList(),
            ),
            CommonOnlyStatistics.empty(),
        )

        val fertilizing = calculator.calculate(WorkType.FERTILIZING, current, previous)
        val harvest = calculator.calculate(WorkType.HARVEST, current, previous)

        assertThat(fertilizing.map { it.metricKey }).containsExactly(
            "recordCount",
            "workedDayCount",
            "fertilizing.totalAmountKg",
        )
        assertComparison(fertilizing.last(), "거름 총량", "1.5", "1", "0.5", "50", "KG")
        assertThat(fertilizing.last().currentCoverage).isEqualTo(Coverage(2, 3))
        assertThat(fertilizing).noneMatch { it.metricKey.startsWith("harvest") }

        assertThat(harvest.map { it.metricKey }).containsExactly(
            "recordCount",
            "workedDayCount",
            "harvest.totalAmountKg",
        )
        assertComparison(harvest.last(), "총수확량", "18", "12", "6", "50", "KG")
        assertThat(harvest.last().previousCoverage).isEqualTo(Coverage(3, 3))
        assertThat(harvest).noneMatch { it.metricKey.startsWith("fertilizing") }
    }

    @Test
    fun `pest control matches pesticide units and preserves spray coverage`() {
        val current = CycleReportStatistics(
            PlantingStatistics.empty(),
            WateringStatistics.empty(),
            FertilizingStatistics.empty(),
            PestControlStatistics(
                3,
                null,
                null,
                3,
                null,
                0,
                null,
                emptyList(),
                null,
                emptyList(),
                listOf(
                    AmountByUnit("ML", decimal("120"), Coverage(2, 3)),
                    AmountByUnit("G", decimal("40"), Coverage(1, 3)),
                ),
                emptyList(),
                decimal("30"),
                Coverage(3, 3),
                emptyList(),
            ),
            WeedingStatistics.empty(),
            CommonOnlyStatistics.empty(),
            HarvestStatistics.empty(),
            CommonOnlyStatistics.empty(),
        )
        val previous = CycleReportStatistics(
            PlantingStatistics.empty(),
            WateringStatistics.empty(),
            FertilizingStatistics.empty(),
            PestControlStatistics(
                2,
                null,
                null,
                2,
                null,
                0,
                null,
                emptyList(),
                null,
                emptyList(),
                listOf(
                    AmountByUnit("G", decimal("35"), Coverage(1, 2)),
                    AmountByUnit("L", decimal("1"), Coverage(1, 2)),
                ),
                emptyList(),
                decimal("24"),
                Coverage(2, 2),
                emptyList(),
            ),
            WeedingStatistics.empty(),
            CommonOnlyStatistics.empty(),
            HarvestStatistics.empty(),
            CommonOnlyStatistics.empty(),
        )

        val comparisons = calculator.calculate(WorkType.PEST_CONTROL, current, previous)

        assertThat(comparisons.map { it.metricKey }).containsExactly(
            "recordCount",
            "workedDayCount",
            "pestControl.pesticideAmount.G",
            "pestControl.totalSprayAmountLiters",
        )
        val pesticide = comparisons[2]
        assertComparison(pesticide, "약 사용량", "40", "35", "5", "14.29", "G")
        assertThat(pesticide.currentCoverage).isEqualTo(Coverage(1, 3))
        assertThat(pesticide.previousCoverage).isEqualTo(Coverage(1, 2))
        assertThat(comparisons).noneMatch { it.metricKey.endsWith(".ML") || it.metricKey.endsWith(".L") }

        val spray = comparisons.last()
        assertComparison(spray, "약을 섞은 물의 총량", "30", "24", "6", "25", "L")
        assertThat(spray.currentCoverage).isEqualTo(Coverage(3, 3))
        assertThat(spray.previousCoverage).isEqualTo(Coverage(2, 2))
    }

    private fun propagation(
        code: String,
        label: String,
        quantity: String,
        unit: String,
        coverage: Coverage,
    ) = PropagationStatistics(code, label, coverage.targetCount, decimal("100"), decimal(quantity), unit, coverage)

    private fun assertComparison(
        actual: ReportFeedbackComparison,
        label: String,
        current: String,
        previous: String,
        difference: String,
        relative: String?,
        unit: String,
    ) {
        assertThat(actual.metricLabel).isEqualTo(label)
        assertThat(actual.currentValue).isEqualByComparingTo(current)
        assertThat(actual.previousValue).isEqualByComparingTo(previous)
        assertThat(actual.difference).isEqualByComparingTo(difference)
        if (relative == null) {
            assertThat(actual.relativeChangePct).isNull()
        } else {
            assertThat(actual.relativeChangePct).isEqualByComparingTo(relative)
        }
        assertThat(actual.unit).isEqualTo(unit)
    }

    private fun decimal(value: String) = BigDecimal(value)
}
