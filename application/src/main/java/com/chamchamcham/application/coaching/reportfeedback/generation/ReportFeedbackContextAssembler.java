package com.chamchamcham.application.coaching.reportfeedback.generation;

import com.chamchamcham.application.coaching.reportfeedback.*;
import com.chamchamcham.application.report.*;
import com.chamchamcham.domain.farming.WorkType;
import com.chamchamcham.domain.report.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Component
public class ReportFeedbackContextAssembler {
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
    private final FarmingCycleReportRepository reportRepository;
    private final FarmingCycleReportQueryRepository reportQueryRepository;
    private final FarmingCycleReportSourceLoader sourceLoader;
    private final FarmingCyclePartitioner partitioner;
    private final ObjectMapper objectMapper;
    private final ReportFeedbackComparisonCalculator comparisonCalculator;
    public ReportFeedbackContextAssembler(FarmingCycleReportRepository reportRepository, FarmingCycleReportQueryRepository reportQueryRepository, FarmingCycleReportSourceLoader sourceLoader, FarmingCyclePartitioner partitioner, ObjectMapper objectMapper, ReportFeedbackComparisonCalculator comparisonCalculator) { this.reportRepository = Objects.requireNonNull(reportRepository); this.reportQueryRepository = Objects.requireNonNull(reportQueryRepository); this.sourceLoader = Objects.requireNonNull(sourceLoader); this.partitioner = Objects.requireNonNull(partitioner); this.objectMapper = Objects.requireNonNull(objectMapper); this.comparisonCalculator = Objects.requireNonNull(comparisonCalculator); }
    @Transactional(readOnly = true)
    public ReportFeedbackContext assemble(UUID memberId, UUID reportId, WorkType workType) { Objects.requireNonNull(memberId); Objects.requireNonNull(reportId); Objects.requireNonNull(workType); var report = reportRepository.findByIdAndMember_Id(reportId, memberId); if (report == null) throw invalidContext(); return assemble(memberId, report, workType); }
    @Transactional(readOnly = true)
    public ReportFeedbackContext assemble(UUID memberId, FarmingCycleReport report, WorkType workType) {
        Objects.requireNonNull(memberId); Objects.requireNonNull(report); Objects.requireNonNull(workType);
        if (!Objects.equals(report.getMember().getId(), memberId) || report.getStatus() != FarmingCycleReportStatus.COMPLETED || report.getEndsAt() == null) throw invalidContext();
        var scope = new ReportScope(memberId, required(report.getFarm().getId()), required(report.getCrop().getId()));
        CycleSlice target = null;
        for (var slice : partitioner.partition(sourceLoader.load(scope))) {
            if (slice.getStatus() == FarmingCycleReportStatus.COMPLETED && Objects.equals(slice.getFinalHarvestRecordId(), report.getFinalHarvestRecord() == null ? null : report.getFinalHarvestRecord().getId())) { if (target != null) throw invalidContext(); target = slice; }
        }
        if (target == null) throw invalidContext();
        var records = new ArrayList<ReportFeedbackRecord>();
        for (var it : target.getRecords()) if (it.getWorkType() == workType) records.add(new ReportFeedbackRecord(it.getId(), it.getWorkedAt(), it.getWorkType(), it.getMemo(), toFeedbackDetails(it)));
        if (records.isEmpty()) throw invalidContext();
        var previous = reportQueryRepository.findPreviousCompleted(memberId, required(report.getFarm().getId()), required(report.getCrop().getId()), required(report.getEndsAt()), required(report.getFinalHarvestRecord() == null ? null : report.getFinalHarvestRecord().getId()));
        var previousContext = previous == null || previous.getStatistics().recordCountFor(workType) == 0 ? null : new ReportFeedbackPreviousReport(required(previous.getId()), previous.getStartsAt(), required(previous.getEndsAt()), previous.getSourceRevision(), toWorkTypeMap(previous.getStatistics(), workType));
        var comparisons = previousContext == null ? Collections.<ReportFeedbackComparison>emptyList() : comparisonCalculator.calculate(workType, report.getStatistics(), previous.getStatistics());
        return new ReportFeedbackContext(ReportFeedbackContext.REPORT_FEEDBACK_CONTEXT_SCHEMA_VERSION, workType, new ReportFeedbackReport(required(report.getId()), report.getFarm().getName(), report.getCrop().getName(), report.getStartsAt(), required(report.getEndsAt()), report.getSourceRevision(), toWorkTypeMap(report.getStatistics(), workType)), records, previousContext, comparisons, previous == null ? Collections.singletonList("previous_report_unavailable") : previousContext == null ? Collections.singletonList("previous_work_type_unavailable") : Collections.emptyList());
    }
    private Map<String, Object> toWorkTypeMap(CycleReportStatistics statistics, WorkType type) {
        Object selected = switch (type) { case PLANTING -> statistics.getPlanting(); case WATERING -> statistics.getWatering(); case FERTILIZING -> statistics.getFertilizing(); case PEST_CONTROL -> statistics.getPestControl(); case WEEDING -> statistics.getWeeding(); case PRUNING -> statistics.getPruning(); case HARVEST -> statistics.getHarvest(); case ETC -> statistics.getEtc(); };
        return objectMapper.convertValue(selected, MAP_TYPE);
    }
    private Map<String, Object> toFeedbackDetails(CycleReportSourceRecord record) {
        var result = new LinkedHashMap<String, Object>(); result.put("weatherCondition", record.getWeatherCondition()); result.put("weatherTemperature", record.getWeatherTemperature()); result.put("hasPhoto", record.getHasPhoto());
        String name = null; Object detail = null;
        switch (record.getWorkType()) {
            case PLANTING -> { name = "planting"; detail = record.getPlanting(); } case WATERING -> { name = "watering"; detail = record.getWatering(); }
            case FERTILIZING -> { name = "fertilizing"; detail = record.getFertilizing(); } case PEST_CONTROL -> { name = "pestControl"; detail = record.getPestControl(); }
            case WEEDING -> { name = "weeding"; detail = record.getWeeding(); } case HARVEST -> { name = "harvest"; detail = record.getHarvest(); }
            case PRUNING, ETC -> { }
        }
        if (detail != null) result.put(name, objectMapper.convertValue(detail, MAP_TYPE));
        return Collections.unmodifiableMap(result);
    }
    private static <T> T required(T value) { if (value == null) throw new IllegalArgumentException("Required value was null."); return value; }
    private static ReportFeedbackGenerationFailure invalidContext() { return new ReportFeedbackGenerationFailure(ReportFeedbackFailureCode.INVALID_CONTEXT); }
}
