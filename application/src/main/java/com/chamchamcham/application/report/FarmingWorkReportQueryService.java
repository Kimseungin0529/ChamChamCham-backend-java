package com.chamchamcham.application.report;

import com.chamchamcham.application.common.OpaqueCursorCodec;
import com.chamchamcham.application.coaching.common.CoachingPromptLabelsKt;
import com.chamchamcham.application.coaching.reportfeedback.lifecycle.ReportFeedbackQueryService;
import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.chamchamcham.domain.coaching.reportfeedback.ReportFeedbackStatus;
import com.chamchamcham.domain.farming.FarmingRecord;
import com.chamchamcham.domain.farming.FarmingWorkReportSourceSnapshot;
import com.chamchamcham.domain.farming.FarmingWorkReportSourceRepository;
import com.chamchamcham.domain.farming.WorkType;
import com.chamchamcham.domain.report.CommonOnlyStatistics;
import com.chamchamcham.domain.report.FarmingCycleReportQueryRepository;
import com.chamchamcham.domain.report.PestControlStatistics;
import com.chamchamcham.domain.report.HarvestStatistics;
import com.chamchamcham.domain.report.WateringStatistics;
import com.chamchamcham.domain.report.FertilizingStatistics;
import com.chamchamcham.domain.report.FarmingCycleReportRepository;
import com.chamchamcham.domain.report.PlantingStatistics;
import com.chamchamcham.domain.report.WeedingStatistics;
import com.chamchamcham.domain.report.CycleReportStatistics;
import com.chamchamcham.domain.report.FarmingCycleReportStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class FarmingWorkReportQueryService {
    private final FarmingCycleReportQueryRepository queryRepository;
    private final FarmingWorkReportSourceRepository sourceRepository;
    private final FarmingCyclePartitioner partitioner;
    private final OpaqueCursorCodec cursorCodec;
    private final FarmingCycleReportRepository reportRepository;
    private final ReportFeedbackQueryService feedbackQueryService;

    public FarmingWorkReportQueryService(FarmingCycleReportQueryRepository queryRepository, FarmingWorkReportSourceRepository sourceRepository,
            FarmingCyclePartitioner partitioner, OpaqueCursorCodec cursorCodec, FarmingCycleReportRepository reportRepository,
            ReportFeedbackQueryService feedbackQueryService) {
        this.queryRepository = Objects.requireNonNull(queryRepository, "queryRepository");
        this.sourceRepository = Objects.requireNonNull(sourceRepository, "sourceRepository");
        this.partitioner = Objects.requireNonNull(partitioner, "partitioner");
        this.cursorCodec = Objects.requireNonNull(cursorCodec, "cursorCodec");
        this.reportRepository = Objects.requireNonNull(reportRepository, "reportRepository");
        this.feedbackQueryService = Objects.requireNonNull(feedbackQueryService, "feedbackQueryService");
    }

    public FarmingWorkReportResult.Page list(FarmingWorkReportSearchCondition condition) throws JsonProcessingException {
        Objects.requireNonNull(condition, "condition");
        var result = queryRepository.searchWorkItems(new FarmingCycleReportQueryRepository.WorkItemSearchCondition(condition.getMemberId(),
            condition.getFarmIds(), condition.getCropIds(), condition.getWorkTypes(), decodeCursor(condition.getCursor()), condition.getSize() + 1));
        if (condition.getSize() < 0) throw new IllegalArgumentException("Requested element count " + condition.getSize() + " is less than zero.");
        var visibleRows = result.rows().subList(0, Math.min(result.rows().size(), condition.getSize()));
        var farmIds = visibleRows.stream().map(FarmingCycleReportQueryRepository.WorkItem::farmId).collect(Collectors.toCollection(LinkedHashSet::new));
        var cropIds = visibleRows.stream().map(FarmingCycleReportQueryRepository.WorkItem::cropId).collect(Collectors.toCollection(LinkedHashSet::new));
        var snapshot = sourceRepository.load(condition.getMemberId(), farmIds, cropIds);
        var slicesByScope = buildSlicesByScope(visibleRows, snapshot);
        var items = visibleRows.stream().map(row -> toResult(row,
            findThumbnailUrl(row, slicesByScope.getOrDefault(scopeKey(row), List.of()), snapshot))).collect(Collectors.toCollection(ArrayList::new));
        var nextCursor = result.rows().size() > condition.getSize() && !visibleRows.isEmpty() ? encodeCursor(visibleRows.getLast()) : null;
        return new FarmingWorkReportResult.Page(items, nextCursor);
    }

    public FarmingWorkReportResult.Detail getDetail(UUID memberId, UUID reportId, WorkType workType) {
        Objects.requireNonNull(memberId, "memberId");
        Objects.requireNonNull(reportId, "reportId");
        Objects.requireNonNull(workType, "workType");
        var report = reportRepository.findByIdAndMember_Id(reportId, memberId);
        if (report == null || report.getStatus() == FarmingCycleReportStatus.SUPERSEDED) throw new BusinessException(ErrorCode.REPORT_NOT_FOUND);
        var statistics = forWorkType(report.getStatistics(), workType);
        if (statistics.getCommon().getRecordCount() == 0) throw new BusinessException(ErrorCode.WORK_REPORT_NOT_FOUND);
        FarmingWorkReportResult.FeedbackStatus feedbackStatus = null;
        if (report.getStatus() == FarmingCycleReportStatus.COMPLETED) {
            var feedback = feedbackQueryService.get(memberId, reportId).getFeedbacks().stream()
                .filter(it -> it.getWorkType() == workType).findFirst().orElse(null);
            feedbackStatus = new FarmingWorkReportResult.FeedbackStatus(feedback == null ? ReportFeedbackStatus.PENDING : feedback.getStatus(),
                feedback == null ? null : feedback.getContent());
        }
        return new FarmingWorkReportResult.Detail(required(report.getId(), "Persisted farming cycle report id is required"),
            report.getStatus(), workType, CoachingPromptLabelsKt.toCoachingText(workType),
            required(report.getFarm().getId(), "Persisted farm id is required"), report.getFarm().getName(),
            required(report.getCrop().getId(), "Persisted crop id is required"), report.getCrop().getName(),
            report.getStartsAt(), report.getEndsAt(), statistics, feedbackStatus);
    }

    private Map<WorkScope, List<CycleSlice>> buildSlicesByScope(List<FarmingCycleReportQueryRepository.WorkItem> rows, FarmingWorkReportSourceSnapshot snapshot) {
        var result = new LinkedHashMap<WorkScope, List<CycleSlice>>();
        rows.stream().map(this::scopeKey).distinct().forEach(scope -> result.put(scope,
            partitioner.partition(snapshot.records().stream().filter(record -> matches(record, scope))
                .map(record -> toCycleSource(record, snapshot)).collect(Collectors.toCollection(ArrayList::new)))));
        return result;
    }

    private String findThumbnailUrl(FarmingCycleReportQueryRepository.WorkItem row, List<CycleSlice> slices, FarmingWorkReportSourceSnapshot snapshot) {
        var candidates = slices.stream().filter(slice -> switch (row.status()) {
            case ACTIVE -> slice.getStatus() == FarmingCycleReportStatus.ACTIVE;
            case COMPLETED -> slice.getStatus() == FarmingCycleReportStatus.COMPLETED && Objects.equals(slice.getFinalHarvestRecordId(), row.finalHarvestRecordId());
            case SUPERSEDED -> false;
        }).collect(Collectors.toCollection(ArrayList::new));
        if (candidates.size() != 1) return null;
        var picturedRecord = candidates.getFirst().getRecords().stream().filter(r -> r.getWorkType() == row.workType())
            .filter(r -> snapshot.firstImageUrlByRecordId().containsKey(r.getId()))
            .max(Comparator.comparing(CycleReportSourceRecord::getWorkedAt).thenComparing(CycleReportSourceRecord::getCreatedAt)
                .thenComparing(CycleReportSourceRecord::getId)).orElse(null);
        return picturedRecord == null ? null : snapshot.firstImageUrlByRecordId().get(picturedRecord.getId());
    }

    private FarmingCycleReportQueryRepository.WorkItemCursor decodeCursor(String cursor) {
        if (cursor == null || cursor.chars().allMatch(c -> Character.isWhitespace(c) || Character.isSpaceChar(c))) return null;
        var payload = cursorCodec.decode(cursor, FarmingWorkReportCursorPayload.class);
        if (payload.getVersion() != FarmingWorkReportCursorPayload.CURRENT_VERSION) throw new BusinessException(ErrorCode.INVALID_CURSOR);
        return new FarmingCycleReportQueryRepository.WorkItemCursor(payload.getLastWorkedOn(), payload.getStatus(), payload.getSortAt(),
            payload.getReportId(), payload.getWorkType());
    }

    private String encodeCursor(FarmingCycleReportQueryRepository.WorkItem row) throws JsonProcessingException {
        return cursorCodec.encode(new FarmingWorkReportCursorPayload(FarmingWorkReportCursorPayload.CURRENT_VERSION,
            row.lastWorkedOn(), row.status(), row.getSortAt(), row.reportId(), row.workType()));
    }

    private FarmingWorkReportResult.Item toResult(FarmingCycleReportQueryRepository.WorkItem row, String thumbnailUrl) {
        return new FarmingWorkReportResult.Item(row.reportId(), row.status(), row.farmId(), row.farmName(), row.cropId(), row.cropName(),
            row.startsAt(), row.endsAt(), row.finalHarvestRecordId(), row.workType(), CoachingPromptLabelsKt.toCoachingText(row.workType()),
            row.recordCount(), row.lastWorkedOn(), thumbnailUrl);
    }

    private CycleReportSourceRecord toCycleSource(FarmingRecord record, FarmingWorkReportSourceSnapshot snapshot) {
        var id = required(record.getId(), "Persisted farming record id is required");
        return new CycleReportSourceRecord(id, record.getWorkedAt(), record.getCreatedAt(), record.getWorkType(),
            record.getWeatherCondition(), record.getWeatherTemperature(), snapshot.firstImageUrlByRecordId().containsKey(id), record.getMemo(),
            null, null, null, null, null,
            record.getWorkType() == WorkType.HARVEST ? new HarvestReportSource(null, null, new CategoryRef("CULTIVATED", "재배"),
                null, null, null, snapshot.finalHarvestRecordIds().contains(id)) : null);
    }

    private WorkScope scopeKey(FarmingCycleReportQueryRepository.WorkItem row) { return new WorkScope(row.farmId(), row.cropId()); }
    private boolean matches(FarmingRecord record, WorkScope scope) {
        return Objects.equals(record.getFarm().getId(), scope.farmId()) && Objects.equals(record.getCrop().getId(), scope.cropId());
    }
    private record WorkScope(UUID farmId, UUID cropId) {}

    private FarmingWorkReportResult.WorkStatistics forWorkType(CycleReportStatistics statistics, WorkType workType) {
        return switch (workType) {
            case PLANTING -> new FarmingWorkReportResult.WorkStatistics(toCommon(statistics.getPlanting()), statistics.getPlanting(), null, null, null, null, null);
            case WATERING -> new FarmingWorkReportResult.WorkStatistics(toCommon(statistics.getWatering()), null, statistics.getWatering(), null, null, null, null);
            case FERTILIZING -> new FarmingWorkReportResult.WorkStatistics(toCommon(statistics.getFertilizing()), null, null, statistics.getFertilizing(),
                null, null, null);
            case PEST_CONTROL -> new FarmingWorkReportResult.WorkStatistics(toCommon(statistics.getPestControl()), null, null, null,
                statistics.getPestControl(), null, null);
            case WEEDING -> new FarmingWorkReportResult.WorkStatistics(toCommon(statistics.getWeeding()), null, null, null, null, statistics.getWeeding(), null);
            case PRUNING -> new FarmingWorkReportResult.WorkStatistics(statistics.getPruning(), null, null, null, null, null, null);
            case HARVEST -> new FarmingWorkReportResult.WorkStatistics(toCommon(statistics.getHarvest()), null, null, null, null, null, statistics.getHarvest());
            case ETC -> new FarmingWorkReportResult.WorkStatistics(statistics.getEtc(), null, null, null, null, null, null);
        };
    }

    private CommonOnlyStatistics toCommon(PlantingStatistics statistics) {
        return new CommonOnlyStatistics(statistics.getRecordCount(), statistics.getFirstWorkedOn(), statistics.getLastWorkedOn(),
            statistics.getWorkedDayCount(), statistics.getAverageIntervalDays(), statistics.getPhotoAttachedRecordCount(),
            statistics.getPhotoAttachmentRatePct(), statistics.getWeatherDistribution(), statistics.getAverageTemperatureC());
    }

    private CommonOnlyStatistics toCommon(WateringStatistics statistics) {
        return new CommonOnlyStatistics(statistics.getRecordCount(), statistics.getFirstWorkedOn(), statistics.getLastWorkedOn(),
            statistics.getWorkedDayCount(), statistics.getAverageIntervalDays(), statistics.getPhotoAttachedRecordCount(),
            statistics.getPhotoAttachmentRatePct(), statistics.getWeatherDistribution(), statistics.getAverageTemperatureC());
    }

    private CommonOnlyStatistics toCommon(FertilizingStatistics statistics) {
        return new CommonOnlyStatistics(statistics.getRecordCount(), statistics.getFirstWorkedOn(), statistics.getLastWorkedOn(),
            statistics.getWorkedDayCount(), statistics.getAverageIntervalDays(), statistics.getPhotoAttachedRecordCount(),
            statistics.getPhotoAttachmentRatePct(), statistics.getWeatherDistribution(), statistics.getAverageTemperatureC());
    }

    private CommonOnlyStatistics toCommon(PestControlStatistics statistics) {
        return new CommonOnlyStatistics(statistics.getRecordCount(), statistics.getFirstWorkedOn(), statistics.getLastWorkedOn(),
            statistics.getWorkedDayCount(), statistics.getAverageIntervalDays(), statistics.getPhotoAttachedRecordCount(),
            statistics.getPhotoAttachmentRatePct(), statistics.getWeatherDistribution(), statistics.getAverageTemperatureC());
    }

    private CommonOnlyStatistics toCommon(WeedingStatistics statistics) {
        return new CommonOnlyStatistics(statistics.getRecordCount(), statistics.getFirstWorkedOn(), statistics.getLastWorkedOn(),
            statistics.getWorkedDayCount(), statistics.getAverageIntervalDays(), statistics.getPhotoAttachedRecordCount(),
            statistics.getPhotoAttachmentRatePct(), statistics.getWeatherDistribution(), statistics.getAverageTemperatureC());
    }

    private CommonOnlyStatistics toCommon(HarvestStatistics statistics) {
        return new CommonOnlyStatistics(statistics.getRecordCount(), statistics.getFirstWorkedOn(), statistics.getLastWorkedOn(),
            statistics.getWorkedDayCount(), statistics.getAverageIntervalDays(), statistics.getPhotoAttachedRecordCount(),
            statistics.getPhotoAttachmentRatePct(), statistics.getWeatherDistribution(), statistics.getAverageTemperatureC());
    }

    private static <T> T required(T value, String message) {
        if (value == null) throw new IllegalArgumentException(message);
        return value;
    }
}
