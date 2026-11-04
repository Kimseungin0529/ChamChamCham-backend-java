package com.chamchamcham.application.report

import com.chamchamcham.application.common.OpaqueCursorCodec
import com.chamchamcham.application.exception.ErrorCode
import com.chamchamcham.application.exception.business.BusinessException
import com.chamchamcham.domain.crop.MemberCropRepository
import com.chamchamcham.domain.farm.FarmRepository
import com.chamchamcham.domain.report.FarmingCycleReport
import com.chamchamcham.domain.report.FarmingCycleReportQueryRepository
import com.chamchamcham.domain.report.FarmingCycleReportRepository
import com.chamchamcham.domain.report.FarmingCycleReportStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class FarmingCycleReportQueryService(
    private val farmRepository: FarmRepository,
    private val memberCropRepository: MemberCropRepository,
    private val reportRepository: FarmingCycleReportRepository,
    private val queryRepository: FarmingCycleReportQueryRepository,
    private val cursorCodec: OpaqueCursorCodec,
) {
    fun listCompleted(
        condition: FarmingCycleReportSearchCondition,
    ): FarmingCycleReportResult.Page {
        validateListScope(condition.memberId, condition.farmIds, condition.cropIds)
        val cursor = decodeCursor(condition.cursor)
        val result = queryRepository.searchCompleted(
            FarmingCycleReportQueryRepository.SearchCondition(condition.memberId, condition.farmIds, condition.cropIds, cursor, condition.size + 1),
        )
        val visibleRows = result.rows.take(condition.size)
        val nextCursor = if (result.rows.size > condition.size) {
            visibleRows.lastOrNull()?.let(::encodeCursor)
        } else {
            null
        }

        return FarmingCycleReportResult.Page(
            visibleRows.map(::toMetadata),
            nextCursor,
        )
    }

    fun getDetail(
        memberId: UUID,
        reportId: UUID,
    ): FarmingCycleReportResult.Detail {
        val selected = reportRepository.findByIdAndMember_Id(reportId, memberId)
            ?.takeUnless { it.status == FarmingCycleReportStatus.SUPERSEDED }
            ?: throw BusinessException(ErrorCode.REPORT_NOT_FOUND)

        return FarmingCycleReportResult.Detail(
            toSnapshot(selected),
        )
    }

    private fun validateListScope(memberId: UUID, farmIds: Set<UUID>, cropIds: Set<UUID>) {
        if (farmIds.isEmpty()) {
            return
        }
        farmIds.forEach { farmId ->
            farmRepository.findByIdAndOwnerId(farmId, memberId)
                ?: throw BusinessException(ErrorCode.FARM_NOT_FOUND)
        }
        cropIds.forEach { cropId ->
            if (!memberCropRepository.existsByMemberIdAndFarmIdInAndCropId(memberId, farmIds, cropId)) {
                throw BusinessException(ErrorCode.CROP_NOT_FOUND)
            }
        }
    }

    private fun decodeCursor(cursor: String?): FarmingCycleReportQueryRepository.Cursor? {
        if (cursor.isNullOrBlank()) {
            return null
        }
        val payload = cursorCodec.decode(
            cursor,
            FarmingCycleReportCursorPayload::class.java,
        )
        return FarmingCycleReportQueryRepository.Cursor(payload.endsAt, payload.finalHarvestRecordId)
    }

    private fun encodeCursor(report: FarmingCycleReport): String =
        cursorCodec.encode(
            FarmingCycleReportCursorPayload(
            requireNotNull(report.endsAt) { "Completed report endsAt is required" },
            requireNotNull(report.finalHarvestRecord?.id) {
                    "Completed report final harvest record id is required"
                },
        ),
        )

    private fun toMetadata(report: FarmingCycleReport): FarmingCycleReportResult.Metadata =
        FarmingCycleReportResult.Metadata(
            requireNotNull(report.id) { "Persisted farming cycle report id is required" },
            requireNotNull(report.farm.id) { "Persisted farm id is required" },
            report.farm.name,
            requireNotNull(report.crop.id) { "Persisted crop id is required" },
            report.crop.name,
            report.status,
            report.startsAt,
            report.endsAt,
            report.startBasis,
            report.sourceRevision,
        )

    private fun toSnapshot(report: FarmingCycleReport): FarmingCycleReportResult.Snapshot =
        FarmingCycleReportResult.Snapshot(
            requireNotNull(report.id) { "Persisted farming cycle report id is required" },
            requireNotNull(report.farm.id) { "Persisted farm id is required" },
            report.farm.name,
            requireNotNull(report.crop.id) { "Persisted crop id is required" },
            report.crop.name,
            report.status,
            report.startsAt,
            report.endsAt,
            report.startBasis,
            report.finalHarvestRecord?.id,
            report.statisticsSchemaVersion,
            report.sourceRevision,
            report.statistics,
        )
}
