package com.chamchamcham.application.coaching.reportfeedback.generation

import com.chamchamcham.domain.farming.EntryMode

import com.chamchamcham.domain.report.WeedingStatistics
import com.chamchamcham.domain.report.PlantingStatistics
import com.chamchamcham.domain.report.HarvestStatistics
import com.chamchamcham.domain.report.PestControlStatistics
import com.chamchamcham.domain.report.CommonOnlyStatistics
import com.chamchamcham.application.coaching.reportfeedback.ReportFeedbackFailureCode
import com.chamchamcham.application.coaching.reportfeedback.ReportFeedbackGenerationFailure
import com.chamchamcham.application.report.CategoryRef
import com.chamchamcham.application.report.CycleReportSourceRecord
import com.chamchamcham.application.report.FarmingCyclePartitioner
import com.chamchamcham.application.report.FarmingCycleReportSourceLoader
import com.chamchamcham.application.report.HarvestReportSource
import com.chamchamcham.application.report.ReportScope
import com.chamchamcham.application.report.WateringReportSource
import com.chamchamcham.domain.crop.Crop
import com.chamchamcham.domain.crop.CropUsePartCategory
import com.chamchamcham.domain.farm.Farm
import com.chamchamcham.domain.farming.FarmingRecord
import com.chamchamcham.domain.farming.WorkType
import com.chamchamcham.domain.member.Member
import com.chamchamcham.domain.report.CycleReportStatistics
import com.chamchamcham.domain.report.Coverage
import com.chamchamcham.domain.report.FertilizingStatistics
import com.chamchamcham.domain.report.FarmingCycleReport
import com.chamchamcham.domain.report.FarmingCycleReportProjection
import com.chamchamcham.domain.report.FarmingCycleReportQueryRepository
import com.chamchamcham.domain.report.FarmingCycleReportRepository
import com.chamchamcham.domain.report.FarmingCycleReportStatus
import com.chamchamcham.domain.report.FarmingCycleStartBasis
import com.chamchamcham.domain.report.WateringStatistics
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class ReportFeedbackContextAssemblerTest {
    private val memberId = UUID.randomUUID()
    private val farmId = UUID.randomUUID()
    private val cropId = UUID.randomUUID()
    private val reportId = UUID.randomUUID()
    private val previousReportId = UUID.randomUUID()
    private val previousFinalHarvestId = UUID.fromString("00000000-0000-0000-0000-000000000001")
    private val targetWateringId = UUID.fromString("00000000-0000-0000-0000-000000000002")
    private val targetFertilizingId = UUID.fromString("00000000-0000-0000-0000-000000000003")
    private val targetFinalHarvestId = UUID.fromString("00000000-0000-0000-0000-000000000004")
    private val startsAt = LocalDateTime.of(2026, 3, 1, 9, 0)
    private val endsAt = LocalDateTime.of(2026, 7, 1, 9, 0)

    @Mock private lateinit var reportRepository: FarmingCycleReportRepository
    @Mock private lateinit var reportQueryRepository: FarmingCycleReportQueryRepository
    @Mock private lateinit var sourceLoader: FarmingCycleReportSourceLoader

    @Test
    fun `assemble selects the final harvest slice and keeps only the requested work type`() {
        val report = completedReport(
            id = reportId,
            finalHarvestId = targetFinalHarvestId,
            startsAt = startsAt,
            endsAt = endsAt,
            statistics = CycleReportStatistics(
                PlantingStatistics.empty(),
                WateringStatistics(4, null, null, 3, BigDecimal("2.5"), 0, BigDecimal("75"), emptyList(), null, emptyList(), emptyList()),
                FertilizingStatistics.empty(),
                PestControlStatistics.empty(),
                WeedingStatistics.empty(),
                CommonOnlyStatistics.empty(),
                HarvestStatistics.empty(),
                CommonOnlyStatistics.empty(),
            ),
        ).also { setSourceRevision(it, 7) }
        val previous = completedReport(
            id = previousReportId,
            finalHarvestId = UUID.randomUUID(),
            startsAt = startsAt.minusYears(1),
            endsAt = endsAt.minusYears(1),
            statistics = CycleReportStatistics(
                PlantingStatistics.empty(),
                WateringStatistics(3, null, null, 2, BigDecimal("4"), 0, BigDecimal("50"), emptyList(), null, emptyList(), emptyList()),
                FertilizingStatistics.empty(),
                PestControlStatistics.empty(),
                WeedingStatistics.empty(),
                CommonOnlyStatistics.empty(),
                HarvestStatistics.empty(),
                CommonOnlyStatistics.empty(),
            ),
        ).also { setSourceRevision(it, 3) }
        stubReports(report, previous)
        `when`(sourceLoader.load(ReportScope(
            memberId,
            farmId,
            cropId,
        ))).thenReturn(cycleRecords())

        val context = assembler().assemble(memberId, reportId, WorkType.WATERING)

        assertThat(context.schemaVersion).isEqualTo(3)
        assertThat(context.workType).isEqualTo(WorkType.WATERING)
        assertThat(context.records.map(ReportFeedbackRecord::id)).containsExactly(targetWateringId)
        assertThat(context.records).allMatch { it.workType == WorkType.WATERING }
        assertThat(context.report.statistics)
            .containsEntry("recordCount", 4)
            .containsEntry("averageIntervalDays", BigDecimal("2.5"))
            .doesNotContainKeys("watering", "fertilizing", "harvest")
        assertThat(context.report.sourceRevision).isEqualTo(7)
        assertThat(context.previousReport?.id).isEqualTo(previousReportId)
        assertThat(context.previousReport?.sourceRevision).isEqualTo(3)
        assertThat(context.previousReport?.statistics).containsEntry("recordCount", 3)
        assertThat(context.comparisons.map(ReportFeedbackComparison::metricKey)).containsExactly(
            "recordCount",
            "workedDayCount",
            "averageIntervalDays",
            "photoAttachmentRatePct",
        )
        assertThat(context.comparisons.first().difference).isEqualByComparingTo("1")
    }

    @Test
    fun `assemble omits previous comparison when the prior report has no matching work records`() {
        val report = completedReport(
            id = reportId,
            finalHarvestId = targetFinalHarvestId,
            startsAt = startsAt,
            endsAt = endsAt,
            statistics = CycleReportStatistics(
                PlantingStatistics.empty(),
                WateringStatistics(1, null, null, 0, null, 0, null, emptyList(), null, emptyList(), emptyList()),
                FertilizingStatistics.empty(),
                PestControlStatistics.empty(),
                WeedingStatistics.empty(),
                CommonOnlyStatistics.empty(),
                HarvestStatistics.empty(),
                CommonOnlyStatistics.empty(),
            ),
        )
        val previous = completedReport(
            id = previousReportId,
            finalHarvestId = UUID.randomUUID(),
            startsAt = startsAt.minusYears(1),
            endsAt = endsAt.minusYears(1),
            statistics = CycleReportStatistics.empty(),
        )
        stubReports(report, previous)
        `when`(sourceLoader.load(ReportScope(
            memberId,
            farmId,
            cropId,
        ))).thenReturn(cycleRecords())

        val context = assembler().assemble(memberId, reportId, WorkType.WATERING)

        assertThat(context.previousReport).isNull()
        assertThat(context.comparisons).isEmpty()
        assertThat(context.warnings).containsExactly("previous_work_type_unavailable")
    }

    @Test
    fun `assemble leaves comparisons empty when there is no previous report`() {
        val report = completedReport(
            id = reportId,
            finalHarvestId = targetFinalHarvestId,
            startsAt = startsAt,
            endsAt = endsAt,
            statistics = CycleReportStatistics(
                PlantingStatistics.empty(),
                WateringStatistics(1, null, null, 0, null, 0, null, emptyList(), null, emptyList(), emptyList()),
                FertilizingStatistics.empty(),
                PestControlStatistics.empty(),
                WeedingStatistics.empty(),
                CommonOnlyStatistics.empty(),
                HarvestStatistics.empty(),
                CommonOnlyStatistics.empty(),
            ),
        )
        stubReports(report, null)
        `when`(sourceLoader.load(ReportScope(
            memberId,
            farmId,
            cropId,
        ))).thenReturn(cycleRecords())

        val context = assembler().assemble(memberId, reportId, WorkType.WATERING)

        assertThat(context.previousReport).isNull()
        assertThat(context.comparisons).isEmpty()
        assertThat(context.warnings).containsExactly("previous_report_unavailable")
    }

    @Test
    fun `assemble calculates selected typed quantity before map conversion and preserves coverage`() {
        val report = completedReport(
            id = reportId,
            finalHarvestId = targetFinalHarvestId,
            startsAt = startsAt,
            endsAt = endsAt,
            statistics = CycleReportStatistics(
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
                    BigDecimal("1.5000"),
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
                HarvestStatistics.empty(),
                CommonOnlyStatistics.empty(),
            ),
        )
        val previous = completedReport(
            id = previousReportId,
            finalHarvestId = UUID.randomUUID(),
            startsAt = startsAt.minusYears(1),
            endsAt = endsAt.minusYears(1),
            statistics = CycleReportStatistics(
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
                    BigDecimal("1.0000"),
                    null,
                    Coverage(1, 1),
                    emptyList(),
                    emptyList(),
                    emptyList(),
                    emptyList(),
                ),
                PestControlStatistics.empty(),
                WeedingStatistics.empty(),
                CommonOnlyStatistics.empty(),
                HarvestStatistics.empty(),
                CommonOnlyStatistics.empty(),
            ),
        )
        stubReports(report, previous)
        `when`(sourceLoader.load(ReportScope(
            memberId,
            farmId,
            cropId,
        ))).thenReturn(cycleRecords())

        val comparison = assembler().assemble(memberId, reportId, WorkType.FERTILIZING)
            .comparisons.single { it.metricKey == "fertilizing.totalAmountKg" }

        assertThat(comparison.difference).isEqualByComparingTo("0.5")
        assertThat(comparison.currentCoverage).isEqualTo(Coverage(1, 2))
        assertThat(comparison.previousCoverage).isEqualTo(Coverage(1, 1))
    }

    @Test
    fun `assemble uses the canonical final harvest boundary when reports have the same end time`() {
        val report = completedReport(
            id = reportId,
            finalHarvestId = targetFinalHarvestId,
            startsAt = startsAt,
            endsAt = endsAt,
            statistics = CycleReportStatistics(
                PlantingStatistics.empty(),
                WateringStatistics(1, null, null, 0, null, 0, null, emptyList(), null, emptyList(), emptyList()),
                FertilizingStatistics.empty(),
                PestControlStatistics.empty(),
                WeedingStatistics.empty(),
                CommonOnlyStatistics.empty(),
                HarvestStatistics.empty(),
                CommonOnlyStatistics.empty(),
            ),
        )
        val previous = completedReport(
            id = previousReportId,
            finalHarvestId = previousFinalHarvestId,
            startsAt = startsAt.minusYears(1),
            endsAt = endsAt,
            statistics = CycleReportStatistics(
                PlantingStatistics.empty(),
                WateringStatistics(2, null, null, 0, null, 0, null, emptyList(), null, emptyList(), emptyList()),
                FertilizingStatistics.empty(),
                PestControlStatistics.empty(),
                WeedingStatistics.empty(),
                CommonOnlyStatistics.empty(),
                HarvestStatistics.empty(),
                CommonOnlyStatistics.empty(),
            ),
        )
        stubReports(report, previous)
        `when`(sourceLoader.load(ReportScope(
            memberId,
            farmId,
            cropId,
        ))).thenReturn(cycleRecords())

        val context = assembler().assemble(memberId, reportId, WorkType.WATERING)

        assertThat(context.previousReport?.id).isEqualTo(previousReportId)
        verify(reportQueryRepository).findPreviousCompleted(
            memberId,
            farmId,
            cropId,
            endsAt,
            targetFinalHarvestId,
        )
    }

    @Test
    fun `assemble rejects a work type absent from the target slice`() {
        val report = completedReport(
            id = reportId,
            finalHarvestId = targetFinalHarvestId,
            startsAt = startsAt,
            endsAt = endsAt,
            statistics = CycleReportStatistics.empty(),
        )
        `when`(reportRepository.findByIdAndMember_Id(reportId, memberId)).thenReturn(report)
        `when`(sourceLoader.load(ReportScope(
            memberId,
            farmId,
            cropId,
        ))).thenReturn(cycleRecords())

        assertThatThrownBy { assembler().assemble(memberId, reportId, WorkType.PRUNING) }
            .isInstanceOfSatisfying(ReportFeedbackGenerationFailure::class.java) {
                assertThat(it.code).isEqualTo(ReportFeedbackFailureCode.INVALID_CONTEXT)
            }
    }

    private fun assembler() = ReportFeedbackContextAssembler(
        reportRepository = reportRepository,
        reportQueryRepository = reportQueryRepository,
        sourceLoader = sourceLoader,
        partitioner = FarmingCyclePartitioner(),
        objectMapper = jacksonObjectMapper(),
        comparisonCalculator = ReportFeedbackComparisonCalculator(),
    )

    private fun stubReports(report: FarmingCycleReport, previous: FarmingCycleReport?) {
        `when`(reportRepository.findByIdAndMember_Id(reportId, memberId)).thenReturn(report)
        `when`(
            reportQueryRepository.findPreviousCompleted(
                memberId,
                farmId,
                cropId,
                requireNotNull(report.endsAt),
                requireNotNull(report.finalHarvestRecord?.id),
            ),
        ).thenReturn(previous)
    }

    private fun cycleRecords(): List<CycleReportSourceRecord> = listOf(
        harvestSource(previousFinalHarvestId, startsAt, isFinal = true),
        CycleReportSourceRecord(
            targetWateringId,
            startsAt,
            startsAt,
            WorkType.WATERING,
            "맑음",
            24,
            false,
            "점적관수로 수분을 보충했어요.",
            null,
            WateringReportSource(
            CategoryRef(
            "LOW",
            "적음",
        ),
            CategoryRef(
            "DRIP",
            "점적",
        ),
        ),
            null,
            null,
            null,
            null,
        ),
        CycleReportSourceRecord(
            targetFertilizingId,
            startsAt.plusDays(1),
            startsAt.plusDays(1),
            WorkType.FERTILIZING,
            "맑음",
            25,
            false,
            "밑거름을 줬어요.",
            null,
            null,
            null,
            null,
            null,
            null,
        ),
        harvestSource(targetFinalHarvestId, endsAt, isFinal = true),
    )

    private fun harvestSource(id: UUID, workedAt: LocalDateTime, isFinal: Boolean) = CycleReportSourceRecord(
            id,
            workedAt,
            workedAt,
            WorkType.HARVEST,
            "맑음",
            24,
            false,
            "마지막 수확이에요.",
            null,
            null,
            null,
            null,
            null,
            HarvestReportSource(
            BigDecimal("5.0"),
            CategoryRef(
            "ROOT_BARK",
            "뿌리/근피",
        ),
            CategoryRef("CULTIVATED", "재배"),
            null,
            null,
            12,
            isFinal,
        ),
        )

    private fun completedReport(
        id: UUID,
        finalHarvestId: UUID,
        startsAt: LocalDateTime,
        endsAt: LocalDateTime,
        statistics: CycleReportStatistics,
    ): FarmingCycleReport {
        val member = Member(memberId, "member@example.com", null)
        val farm = Farm(farmId, member, "약초농장", "강원도 평창군")
        val crop = Crop(cropId, 422, "황기", CropUsePartCategory.ROOT_BARK)
        val harvest = FarmingRecord(finalHarvestId, member, farm, crop, WorkType.HARVEST, endsAt, "맑음", 24, "최종 수확", com.chamchamcham.domain.farming.EntryMode.MANUAL, false)
        return FarmingCycleReport.create(
            member,
            farm,
            crop,
            FarmingCycleReportProjection(
                FarmingCycleReportStatus.COMPLETED,
                startsAt,
                endsAt,
                FarmingCycleStartBasis.FIRST_RECORD,
                harvest,
                1,
                statistics,
            ),
        ).also { setId(it, id) }
    }

    private fun setId(target: Any, id: UUID) {
        val field = target.javaClass.getDeclaredField("id")
        field.isAccessible = true
        field.set(target, id)
    }

    private fun setSourceRevision(target: FarmingCycleReport, revision: Long) {
        val field = target.javaClass.getDeclaredField("sourceRevision")
        field.isAccessible = true
        field.set(target, revision)
    }
}
