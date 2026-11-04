package com.chamchamcham.application.report;

import java.util.stream.Collectors;
import com.chamchamcham.application.common.OpaqueCursorCodec;
import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.chamchamcham.domain.crop.MemberCropRepository;
import com.chamchamcham.domain.farm.FarmRepository;
import com.chamchamcham.domain.report.FarmingCycleReport;
import com.chamchamcham.domain.report.FarmingCycleReportQueryRepository;
import com.chamchamcham.domain.report.FarmingCycleReportRepository;
import com.chamchamcham.domain.report.FarmingCycleReportStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class FarmingCycleReportQueryService {
    private final FarmRepository farmRepository;
    private final MemberCropRepository memberCropRepository;
    private final FarmingCycleReportRepository reportRepository;
    private final FarmingCycleReportQueryRepository queryRepository;
    private final OpaqueCursorCodec cursorCodec;

    public FarmingCycleReportQueryService(FarmRepository farmRepository, MemberCropRepository memberCropRepository,
            FarmingCycleReportRepository reportRepository, FarmingCycleReportQueryRepository queryRepository, OpaqueCursorCodec cursorCodec) {
        this.farmRepository = Objects.requireNonNull(farmRepository, "farmRepository");
        this.memberCropRepository = Objects.requireNonNull(memberCropRepository, "memberCropRepository");
        this.reportRepository = Objects.requireNonNull(reportRepository, "reportRepository");
        this.queryRepository = Objects.requireNonNull(queryRepository, "queryRepository");
        this.cursorCodec = Objects.requireNonNull(cursorCodec, "cursorCodec");
    }

    public FarmingCycleReportResult.Page listCompleted(FarmingCycleReportSearchCondition condition) throws JsonProcessingException {
        Objects.requireNonNull(condition, "condition");
        validateListScope(condition.getMemberId(), condition.getFarmIds(), condition.getCropIds());
        var cursor = decodeCursor(condition.getCursor());
        var result = queryRepository.searchCompleted(new FarmingCycleReportQueryRepository.SearchCondition(
            condition.getMemberId(), condition.getFarmIds(), condition.getCropIds(), cursor, condition.getSize() + 1));
        if (condition.getSize() < 0) throw new IllegalArgumentException("Requested element count " + condition.getSize() + " is less than zero.");
        var visibleRows = result.rows().subList(0, Math.min(result.rows().size(), condition.getSize()));
        var nextCursor = result.rows().size() > condition.getSize() && !visibleRows.isEmpty() ? encodeCursor(visibleRows.getLast()) : null;
        return new FarmingCycleReportResult.Page(visibleRows.stream().map(this::toMetadata).collect(Collectors.toCollection(ArrayList::new)), nextCursor);
    }

    public FarmingCycleReportResult.Detail getDetail(UUID memberId, UUID reportId) {
        Objects.requireNonNull(memberId, "memberId");
        Objects.requireNonNull(reportId, "reportId");
        var selected = reportRepository.findByIdAndMember_Id(reportId, memberId);
        if (selected == null || selected.getStatus() == FarmingCycleReportStatus.SUPERSEDED) throw new BusinessException(ErrorCode.REPORT_NOT_FOUND);
        return new FarmingCycleReportResult.Detail(toSnapshot(selected));
    }

    private void validateListScope(UUID memberId, Set<UUID> farmIds, Set<UUID> cropIds) {
        if (farmIds.isEmpty()) return;
        for (var farmId : farmIds) {
            if (farmRepository.findByIdAndOwnerId(farmId, memberId) == null) throw new BusinessException(ErrorCode.FARM_NOT_FOUND);
        }
        for (var cropId : cropIds) {
            if (!memberCropRepository.existsByMemberIdAndFarmIdInAndCropId(memberId, farmIds, cropId)) throw new BusinessException(ErrorCode.CROP_NOT_FOUND);
        }
    }

    private FarmingCycleReportQueryRepository.Cursor decodeCursor(String cursor) {
        if (cursor == null || isBlank(cursor)) return null;
        var payload = cursorCodec.decode(cursor, FarmingCycleReportCursorPayload.class);
        return new FarmingCycleReportQueryRepository.Cursor(payload.getEndsAt(), payload.getFinalHarvestRecordId());
    }

    private String encodeCursor(FarmingCycleReport report) throws JsonProcessingException {
        return cursorCodec.encode(new FarmingCycleReportCursorPayload(required(report.getEndsAt(), "Completed report endsAt is required"),
            required(report.getFinalHarvestRecord() == null ? null : report.getFinalHarvestRecord().getId(), "Completed report final harvest record id is required")));
    }

    private FarmingCycleReportResult.Metadata toMetadata(FarmingCycleReport report) {
        return new FarmingCycleReportResult.Metadata(required(report.getId(), "Persisted farming cycle report id is required"),
            required(report.getFarm().getId(), "Persisted farm id is required"), report.getFarm().getName(),
            required(report.getCrop().getId(), "Persisted crop id is required"), report.getCrop().getName(), report.getStatus(),
            report.getStartsAt(), report.getEndsAt(), report.getStartBasis(), report.getSourceRevision());
    }

    private FarmingCycleReportResult.Snapshot toSnapshot(FarmingCycleReport report) {
        return new FarmingCycleReportResult.Snapshot(required(report.getId(), "Persisted farming cycle report id is required"),
            required(report.getFarm().getId(), "Persisted farm id is required"), report.getFarm().getName(),
            required(report.getCrop().getId(), "Persisted crop id is required"), report.getCrop().getName(), report.getStatus(),
            report.getStartsAt(), report.getEndsAt(), report.getStartBasis(),
            report.getFinalHarvestRecord() == null ? null : report.getFinalHarvestRecord().getId(), report.getStatisticsSchemaVersion(),
            report.getSourceRevision(), report.getStatistics());
    }

    private static <T> T required(T value, String message) {
        if (value == null) throw new IllegalArgumentException(message);
        return value;
    }

    private static boolean isBlank(String value) {
        return value.chars().allMatch(c -> Character.isWhitespace(c) || Character.isSpaceChar(c));
    }
}
