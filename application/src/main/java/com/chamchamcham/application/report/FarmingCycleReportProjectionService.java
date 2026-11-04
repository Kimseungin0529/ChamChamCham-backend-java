package com.chamchamcham.application.report;

import com.chamchamcham.application.coaching.reportfeedback.lifecycle.ReportFeedbackLifecycleService;
import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.chamchamcham.domain.crop.CropRepository;
import com.chamchamcham.domain.farm.FarmRepository;
import com.chamchamcham.domain.farming.FarmingRecordRepository;
import com.chamchamcham.domain.farming.WorkType;
import com.chamchamcham.domain.report.FarmingCycleReport;
import com.chamchamcham.domain.report.FarmingCycleReportProjection;
import com.chamchamcham.domain.report.FarmingCycleReportRepository;
import com.chamchamcham.domain.report.FarmingCycleReportStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class FarmingCycleReportProjectionService {
    private static final int STATISTICS_SCHEMA_VERSION = 3;
    private final FarmRepository farmRepository;
    private final CropRepository cropRepository;
    private final FarmingRecordRepository farmingRecordRepository;
    private final FarmingCycleReportSourceLoader sourceLoader;
    private final FarmingCyclePartitioner partitioner;
    private final CycleReportStatisticsCalculator statisticsCalculator;
    private final FarmingCycleReportRepository reportRepository;
    private final ReportFeedbackLifecycleService reportFeedbackLifecycleService;

    public FarmingCycleReportProjectionService(FarmRepository farmRepository, CropRepository cropRepository,
            FarmingRecordRepository farmingRecordRepository, FarmingCycleReportSourceLoader sourceLoader,
            FarmingCyclePartitioner partitioner, CycleReportStatisticsCalculator statisticsCalculator,
            FarmingCycleReportRepository reportRepository, ReportFeedbackLifecycleService reportFeedbackLifecycleService) {
        this.farmRepository = Objects.requireNonNull(farmRepository, "farmRepository");
        this.cropRepository = Objects.requireNonNull(cropRepository, "cropRepository");
        this.farmingRecordRepository = Objects.requireNonNull(farmingRecordRepository, "farmingRecordRepository");
        this.sourceLoader = Objects.requireNonNull(sourceLoader, "sourceLoader");
        this.partitioner = Objects.requireNonNull(partitioner, "partitioner");
        this.statisticsCalculator = Objects.requireNonNull(statisticsCalculator, "statisticsCalculator");
        this.reportRepository = Objects.requireNonNull(reportRepository, "reportRepository");
        this.reportFeedbackLifecycleService = Objects.requireNonNull(reportFeedbackLifecycleService, "reportFeedbackLifecycleService");
    }

    public void rebuild(ReportScope scope) {
        var farm = farmRepository.findOwnedByIdForReportUpdate(scope.getFarmId(), scope.getMemberId());
        if (farm == null) throw new BusinessException(ErrorCode.FARM_NOT_FOUND);
        var crop = cropRepository.findById(scope.getCropId()).orElseThrow(() -> new BusinessException(ErrorCode.CROP_NOT_FOUND));
        var existing = reportRepository.findAllCurrent(scope.getMemberId(), scope.getFarmId(), scope.getCropId());
        var slices = partitioner.partition(sourceLoader.load(scope));
        var unmatched = new LinkedHashMap<UUID, FarmingCycleReport>();
        for (var report : existing) unmatched.put(requireId(report.getId()), report);
        for (int index = 0; index < slices.size(); index++) {
            var slice = slices.get(index);
            var matched = takeMatchingReport(unmatched, slice,
                index == slices.size() - 1 && slice.getStatus() == FarmingCycleReportStatus.COMPLETED);
            var projection = toProjection(slice);
            FarmingCycleReport report;
            if (matched != null) {
                matched.applyProjection(projection);
                report = matched;
            } else {
                report = FarmingCycleReport.create(farm.getOwner(), farm, crop, projection);
            }
            reportRepository.save(report);
            if (report.getStatus() == FarmingCycleReportStatus.COMPLETED) {
                Set<WorkType> workTypes = slice.getRecords().stream().map(CycleReportSourceRecord::getWorkType)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
                if (slice.getRecords().size() == 1) workTypes = Collections.singleton(workTypes.iterator().next());
                reportFeedbackLifecycleService.reconcile(report, workTypes);
            }
        }
        var obsolete = unmatched.values().stream().filter(FarmingCycleReport::supersede).collect(Collectors.toCollection(ArrayList::new));
        if (!obsolete.isEmpty()) reportRepository.saveAll(obsolete);
    }

    public void rebuildAll(Collection<ReportScope> scopes) {
        scopes.stream().distinct().sorted(Comparator.comparing(ReportScope::getMemberId)
            .thenComparing(ReportScope::getFarmId).thenComparing(ReportScope::getCropId)).forEach(this::rebuild);
    }

    private FarmingCycleReport takeMatchingReport(Map<UUID, FarmingCycleReport> unmatched, CycleSlice slice, boolean allowActiveCompletion) {
        var exactCompleted = slice.getFinalHarvestRecordId() == null ? null : unmatched.values().stream()
            .filter(r -> r.getStatus() == FarmingCycleReportStatus.COMPLETED && r.getFinalHarvestRecord() != null
                && Objects.equals(r.getFinalHarvestRecord().getId(), slice.getFinalHarvestRecordId()))
            .findFirst().orElse(null);
        var activeCandidates = unmatched.values().stream().filter(r -> r.getStatus() == FarmingCycleReportStatus.ACTIVE).collect(Collectors.toCollection(ArrayList::new));
        var active = activeCandidates.size() == 1 ? activeCandidates.getFirst() : null;
        var selected = exactCompleted != null ? exactCompleted
            : slice.getStatus() == FarmingCycleReportStatus.ACTIVE || allowActiveCompletion ? active : null;
        if (selected != null) unmatched.remove(requireId(selected.getId()));
        return selected;
    }

    private FarmingCycleReportProjection toProjection(CycleSlice slice) {
        return new FarmingCycleReportProjection(slice.getStatus(), slice.getRecords().getFirst().getWorkedAt(),
            slice.getStatus() == FarmingCycleReportStatus.COMPLETED ? slice.getRecords().getLast().getWorkedAt() : null,
            slice.getStartBasis(), slice.getFinalHarvestRecordId() == null ? null
                : farmingRecordRepository.getReferenceById(slice.getFinalHarvestRecordId()),
            STATISTICS_SCHEMA_VERSION, statisticsCalculator.calculate(slice.getRecords()));
    }

    private static UUID requireId(UUID id) {
        if (id == null) throw new IllegalArgumentException("Required value was null.");
        return id;
    }
}
