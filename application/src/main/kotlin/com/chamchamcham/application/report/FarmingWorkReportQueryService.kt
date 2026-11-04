package com.chamchamcham.application.report

import com.chamchamcham.application.common.OpaqueCursorCodec
import com.chamchamcham.application.coaching.common.toCoachingText
import com.chamchamcham.application.coaching.reportfeedback.lifecycle.ReportFeedbackQueryService
import com.chamchamcham.application.exception.ErrorCode
import com.chamchamcham.application.exception.business.BusinessException
import com.chamchamcham.domain.coaching.reportfeedback.ReportFeedbackStatus
import com.chamchamcham.domain.farming.FarmingRecord
import com.chamchamcham.domain.farming.FarmingWorkReportSourceRepository
import com.chamchamcham.domain.farming.FarmingWorkReportSourceSnapshot
import com.chamchamcham.domain.farming.WorkType
import com.chamchamcham.domain.report.FarmingCycleReportQueryRepository
import com.chamchamcham.domain.report.CommonOnlyStatistics
import com.chamchamcham.domain.report.CycleReportStatistics
import com.chamchamcham.domain.report.FarmingCycleReportRepository
import com.chamchamcham.domain.report.FarmingCycleReportStatus
import com.chamchamcham.domain.report.FertilizingStatistics
import com.chamchamcham.domain.report.HarvestStatistics
import com.chamchamcham.domain.report.PestControlStatistics
import com.chamchamcham.domain.report.PlantingStatistics
import com.chamchamcham.domain.report.WateringStatistics
import com.chamchamcham.domain.report.WeedingStatistics
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import java.math.BigDecimal
import java.time.LocalDate

@Service
@Transactional(readOnly = true)
class FarmingWorkReportQueryService(
    private val queryRepository: FarmingCycleReportQueryRepository,
    private val sourceRepository: FarmingWorkReportSourceRepository,
    private val partitioner: FarmingCyclePartitioner,
    private val cursorCodec: OpaqueCursorCodec,
    private val reportRepository: FarmingCycleReportRepository,
    private val feedbackQueryService: ReportFeedbackQueryService,
) {
    fun list(condition: FarmingWorkReportSearchCondition): FarmingWorkReportResult.Page {
        val result = queryRepository.searchWorkItems(
            FarmingCycleReportQueryRepository.WorkItemSearchCondition(
                condition.memberId,
                condition.farmIds,
                condition.cropIds,
                condition.workTypes,
                decodeCursor(condition.cursor),
                condition.size + 1,
            ),
        )
        val visibleRows = result.rows.take(condition.size)
        val snapshot = sourceRepository.load(
            condition.memberId,
            visibleRows.mapTo(mutableSetOf(), FarmingCycleReportQueryRepository.WorkItem::farmId),
            visibleRows.mapTo(mutableSetOf(), FarmingCycleReportQueryRepository.WorkItem::cropId),
        )
        val slicesByScope = buildSlicesByScope(visibleRows, snapshot)

        return FarmingWorkReportResult.Page(
            visibleRows.map { row ->
                row.toResult(
                    thumbnailUrl = findThumbnailUrl(row, slicesByScope[row.scopeKey()].orEmpty(), snapshot),
                )
            },
            if (result.rows.size > condition.size) {
                visibleRows.lastOrNull()?.let(::encodeCursor)
            } else {
                null
            },
        )
    }

    fun getDetail(
        memberId: UUID,
        reportId: UUID,
        workType: WorkType,
    ): FarmingWorkReportResult.Detail {
        val report = reportRepository.findByIdAndMember_Id(reportId, memberId)
            ?.takeUnless { it.status == FarmingCycleReportStatus.SUPERSEDED }
            ?: throw BusinessException(ErrorCode.REPORT_NOT_FOUND)
        val statistics = report.statistics.forWorkType(workType)
        if (statistics.common.recordCount == 0) {
            throw BusinessException(ErrorCode.WORK_REPORT_NOT_FOUND)
        }
        val feedbackStatus = if (report.status == FarmingCycleReportStatus.COMPLETED) {
            val feedback = feedbackQueryService.get(memberId, reportId).feedbacks
                .firstOrNull { it.workType == workType }
            FarmingWorkReportResult.FeedbackStatus(
            feedback?.status ?: ReportFeedbackStatus.PENDING,
            feedback?.content,
        )
        } else {
            null
        }

        return FarmingWorkReportResult.Detail(
            requireNotNull(report.id) { "Persisted farming cycle report id is required" },
            report.status,
            workType,
            workType.toCoachingText(),
            requireNotNull(report.farm.id) { "Persisted farm id is required" },
            report.farm.name,
            requireNotNull(report.crop.id) { "Persisted crop id is required" },
            report.crop.name,
            report.startsAt,
            report.endsAt,
            statistics,
            feedbackStatus,
        )
    }

    private fun buildSlicesByScope(
        rows: List<FarmingCycleReportQueryRepository.WorkItem>,
        snapshot: FarmingWorkReportSourceSnapshot,
    ): Map<WorkScope, List<CycleSlice>> =
        rows.map { row -> row.scopeKey() }
            .distinct()
            .associateWith { scope ->
                partitioner.partition(
                    snapshot.records
                        .asSequence()
                        .filter { record -> record.matches(scope) }
                        .map { record -> record.toCycleSource(snapshot) }
                        .toList(),
                )
            }

    private fun findThumbnailUrl(
        row: FarmingCycleReportQueryRepository.WorkItem,
        slices: List<CycleSlice>,
        snapshot: FarmingWorkReportSourceSnapshot,
    ): String? {
        val cycle = when (row.status) {
            FarmingCycleReportStatus.ACTIVE -> slices.singleOrNull { it.status == FarmingCycleReportStatus.ACTIVE }
            FarmingCycleReportStatus.COMPLETED -> slices.singleOrNull { slice ->
                slice.status == FarmingCycleReportStatus.COMPLETED &&
                    slice.finalHarvestRecordId == row.finalHarvestRecordId
            }
            FarmingCycleReportStatus.SUPERSEDED -> null
        } ?: return null
        val picturedRecord = cycle.records
            .asSequence()
            .filter { record -> record.workType == row.workType }
            .filter { record -> snapshot.firstImageUrlByRecordId.containsKey(record.id) }
            .maxWithOrNull(
                compareBy(
                    CycleReportSourceRecord::workedAt,
                    CycleReportSourceRecord::createdAt,
                    CycleReportSourceRecord::id,
                ),
            )
            ?: return null
        return snapshot.firstImageUrlByRecordId[picturedRecord.id]
    }

    private fun decodeCursor(cursor: String?): FarmingCycleReportQueryRepository.WorkItemCursor? {
        if (cursor.isNullOrBlank()) return null
        val payload = cursorCodec.decode(cursor, FarmingWorkReportCursorPayload::class.java)
        if (payload.version != FarmingWorkReportCursorPayload.CURRENT_VERSION) {
            throw BusinessException(ErrorCode.INVALID_CURSOR)
        }
        return FarmingCycleReportQueryRepository.WorkItemCursor(payload.lastWorkedOn, payload.status, payload.sortAt, payload.reportId, payload.workType)
    }

    private fun encodeCursor(row: FarmingCycleReportQueryRepository.WorkItem): String =
        cursorCodec.encode(
            FarmingWorkReportCursorPayload(
            FarmingWorkReportCursorPayload.CURRENT_VERSION,
            row.lastWorkedOn,
            row.status,
            row.sortAt,
            row.reportId,
            row.workType,
        ),
        )

    private fun FarmingCycleReportQueryRepository.WorkItem.toResult(
        thumbnailUrl: String?,
    ): FarmingWorkReportResult.Item =
        FarmingWorkReportResult.Item(
            reportId,
            status,
            farmId,
            farmName,
            cropId,
            cropName,
            startsAt,
            endsAt,
            finalHarvestRecordId,
            workType,
            workType.toCoachingText(),
            recordCount,
            lastWorkedOn,
            thumbnailUrl,
        )

    private fun FarmingRecord.toCycleSource(
        snapshot: FarmingWorkReportSourceSnapshot,
    ): CycleReportSourceRecord {
        val recordId = requireNotNull(id) { "Persisted farming record id is required" }
        return CycleReportSourceRecord(
            recordId,
            workedAt,
            createdAt,
            workType,
            weatherCondition,
            weatherTemperature,
            snapshot.firstImageUrlByRecordId.containsKey(recordId),
            memo,
            null,
            null,
            null,
            null,
            null,
            if (workType == WorkType.HARVEST) {
                HarvestReportSource(
            null,
            null,
            CategoryRef("CULTIVATED", "재배"),
            null,
            null,
            null,
            recordId in snapshot.finalHarvestRecordIds,
        )
            } else {
                null
            },
        )
    }

    private fun FarmingCycleReportQueryRepository.WorkItem.scopeKey(): WorkScope =
        WorkScope(farmId, cropId)

    private fun FarmingRecord.matches(scope: WorkScope): Boolean =
        farm.id == scope.farmId && crop.id == scope.cropId

    private data class WorkScope(
        val farmId: UUID,
        val cropId: UUID,
    )
}

private fun CycleReportStatistics.forWorkType(workType: WorkType): FarmingWorkReportResult.WorkStatistics =
    when (workType) {
        WorkType.PLANTING -> FarmingWorkReportResult.WorkStatistics(
            planting.toCommon(),
            planting,
            null,
            null,
            null,
            null,
            null,
        )
        WorkType.WATERING -> FarmingWorkReportResult.WorkStatistics(
            watering.toCommon(),
            null,
            watering,
            null,
            null,
            null,
            null,
        )
        WorkType.FERTILIZING -> FarmingWorkReportResult.WorkStatistics(
            fertilizing.toCommon(),
            null,
            null,
            fertilizing,
            null,
            null,
            null,
        )
        WorkType.PEST_CONTROL -> FarmingWorkReportResult.WorkStatistics(
            pestControl.toCommon(),
            null,
            null,
            null,
            pestControl,
            null,
            null,
        )
        WorkType.WEEDING -> FarmingWorkReportResult.WorkStatistics(
            weeding.toCommon(),
            null,
            null,
            null,
            null,
            weeding,
            null,
        )
        WorkType.PRUNING -> FarmingWorkReportResult.WorkStatistics(
            pruning,
            null,
            null,
            null,
            null,
            null,
            null,
        )
        WorkType.HARVEST -> FarmingWorkReportResult.WorkStatistics(
            harvest.toCommon(),
            null,
            null,
            null,
            null,
            null,
            harvest,
        )
        WorkType.ETC -> FarmingWorkReportResult.WorkStatistics(
            etc,
            null,
            null,
            null,
            null,
            null,
            null,
        )
    }

private fun PlantingStatistics.toCommon() = common(
    recordCount,
    firstWorkedOn,
    lastWorkedOn,
    workedDayCount,
    averageIntervalDays,
    photoAttachedRecordCount,
    photoAttachmentRatePct,
    weatherDistribution,
    averageTemperatureC,
)

private fun WateringStatistics.toCommon() = common(
    recordCount,
    firstWorkedOn,
    lastWorkedOn,
    workedDayCount,
    averageIntervalDays,
    photoAttachedRecordCount,
    photoAttachmentRatePct,
    weatherDistribution,
    averageTemperatureC,
)

private fun FertilizingStatistics.toCommon() = common(
    recordCount,
    firstWorkedOn,
    lastWorkedOn,
    workedDayCount,
    averageIntervalDays,
    photoAttachedRecordCount,
    photoAttachmentRatePct,
    weatherDistribution,
    averageTemperatureC,
)

private fun PestControlStatistics.toCommon() = common(
    recordCount,
    firstWorkedOn,
    lastWorkedOn,
    workedDayCount,
    averageIntervalDays,
    photoAttachedRecordCount,
    photoAttachmentRatePct,
    weatherDistribution,
    averageTemperatureC,
)

private fun WeedingStatistics.toCommon() = common(
    recordCount,
    firstWorkedOn,
    lastWorkedOn,
    workedDayCount,
    averageIntervalDays,
    photoAttachedRecordCount,
    photoAttachmentRatePct,
    weatherDistribution,
    averageTemperatureC,
)

private fun HarvestStatistics.toCommon() = common(
    recordCount,
    firstWorkedOn,
    lastWorkedOn,
    workedDayCount,
    averageIntervalDays,
    photoAttachedRecordCount,
    photoAttachmentRatePct,
    weatherDistribution,
    averageTemperatureC,
)

private fun common(
    recordCount: Int,
    firstWorkedOn: LocalDate?,
    lastWorkedOn: LocalDate?,
    workedDayCount: Int,
    averageIntervalDays: BigDecimal?,
    photoAttachedRecordCount: Int,
    photoAttachmentRatePct: BigDecimal?,
    weatherDistribution: List<com.chamchamcham.domain.report.CountDistribution>,
    averageTemperatureC: BigDecimal?,
) = CommonOnlyStatistics(
    recordCount,
    firstWorkedOn,
    lastWorkedOn,
    workedDayCount,
    averageIntervalDays,
    photoAttachedRecordCount,
    photoAttachmentRatePct,
    weatherDistribution,
    averageTemperatureC,
)
