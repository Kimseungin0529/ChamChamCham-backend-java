package com.chamchamcham.domain.coaching.reportfeedback;

import com.chamchamcham.domain.crop.Crop;
import com.chamchamcham.domain.crop.CropUsePartCategory;
import com.chamchamcham.domain.farm.Farm;
import com.chamchamcham.domain.farming.FarmingRecord;
import com.chamchamcham.domain.farming.WorkType;
import com.chamchamcham.domain.member.Member;
import com.chamchamcham.domain.report.CommonOnlyStatistics;
import com.chamchamcham.domain.report.Coverage;
import com.chamchamcham.domain.report.CycleReportStatistics;
import com.chamchamcham.domain.report.FarmingCycleReport;
import com.chamchamcham.domain.report.FarmingCycleReportProjection;
import com.chamchamcham.domain.report.FarmingCycleReportStatus;
import com.chamchamcham.domain.report.FarmingCycleStartBasis;
import com.chamchamcham.domain.report.FertilizingStatistics;
import com.chamchamcham.domain.report.HarvestStatistics;
import com.chamchamcham.domain.report.PestControlStatistics;
import com.chamchamcham.domain.report.PlantingStatistics;
import com.chamchamcham.domain.report.WateringStatistics;
import com.chamchamcham.domain.report.WeedingStatistics;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReportFeedbackTest {
    private final String sourceFingerprint = "a".repeat(64);
    private final Member member = member("member@example.com");
    private final Farm farm = farm(member);
    private final Crop crop = crop();
    private final LocalDateTime finishedAt = LocalDateTime.of(2026, 7, 13, 9, 0);
    private final FarmingRecord finalHarvest = record(member, farm, crop, WorkType.HARVEST, finishedAt);
    private final FarmingCycleReport report = report(member, farm, crop, finalHarvest, finishedAt);

    @Test
    void pendingFeedbackBelongsToOneReportWorkType() {
        var feedback = ReportFeedback.pending(member, report, WorkType.WATERING, sourceFingerprint);

        assertThat(feedback.getWorkType()).isEqualTo(WorkType.WATERING);
        assertThat(feedback.getReport()).isSameAs(report);
        assertThat(feedback.getStatus()).isEqualTo(ReportFeedbackStatus.PENDING);
        assertThat(feedback.getSourceFingerprint()).isEqualTo(sourceFingerprint);

    }

    @Test
    void readyFeedbackStoresOneWorkTypeSummaryAndOrderedItemsWithoutASectionCountCap() {
        var feedback = ReportFeedback.pending(member, report, WorkType.WATERING, sourceFingerprint);

        feedback.markReady(
            "이번 관수 작업은 간격을 안정적으로 유지했어요.",
            List.of(
                item(ReportFeedbackItemSection.COMPARISON, "직전보다 기록 1회 증가", "직전 재배보다 물 주기 기록이 한 번 늘었어요."),
                item(ReportFeedbackItemSection.STRENGTH, "관수 4회", "관수를 네 번 기록해 흐름을 확인하기 좋았어요."),
                item(ReportFeedbackItemSection.IMPROVEMENT, "관수량 누락", "다음에는 관수량도 함께 기록하세요."),
                item(ReportFeedbackItemSection.NEXT_ACTION, "다음 관수", "관수 전에 토양 수분을 확인하세요."),
                item(ReportFeedbackItemSection.NEXT_ACTION, "관수 직후", "관수량과 토양 상태를 함께 기록하세요.")),
            List.of(Map.of("id", "report:current")),
            "PASS",
            List.of(),
            "test-chat",
            "test-embedding");

        assertThat(feedback.getStatus()).isEqualTo(ReportFeedbackStatus.READY);
        assertThat(feedback.getSummary()).isEqualTo("이번 관수 작업은 간격을 안정적으로 유지했어요.");
        assertThat(feedback.items().stream().map(ReportFeedbackItem::getDisplayOrder).toList()).containsExactly(0, 1, 2, 3, 4);
        assertThat(feedback.items().stream().map(ReportFeedbackItem::getSection).toList())
            .containsExactly(
                ReportFeedbackItemSection.COMPARISON,
                ReportFeedbackItemSection.STRENGTH,
                ReportFeedbackItemSection.IMPROVEMENT,
                ReportFeedbackItemSection.NEXT_ACTION,
                ReportFeedbackItemSection.NEXT_ACTION);

    }

    @Test
    void readyFeedbackPermitsASummaryWithoutForcedItems() {
        var feedback = ReportFeedback.pending(member, report, WorkType.WATERING, sourceFingerprint);

        feedback.markReady(
            "이번 관수 기록의 흐름을 확인했어요.",
            List.of(),
            List.of(),
            "PASS",
            List.of(),
            "test-chat",
            "test-embedding");

        assertThat(feedback.getStatus()).isEqualTo(ReportFeedbackStatus.READY);
        assertThat(feedback.items()).isEmpty();

    }

    @Test
    void readyFeedbackRejectsBlankSummaryAndBlankItemValues() {
        assertThatThrownBy(() -> {
            ReportFeedback.pending(member, report, WorkType.WATERING, sourceFingerprint).markReady(
                " ",
                List.of(item(ReportFeedbackItemSection.STRENGTH, "관수", "관수 기록이 좋습니다.")),
                List.of(),
                "PASS",
                List.of(),
                "test-chat",
                "test-embedding");
        }).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> {
            ReportFeedback.pending(member, report, WorkType.WATERING, sourceFingerprint).markReady(
                "요약",
                List.of(item(ReportFeedbackItemSection.STRENGTH, " ", "관수 기록이 좋습니다.")),
                List.of(),
                "PASS",
                List.of(),
                "test-chat",
                "test-embedding");
        }).isInstanceOf(IllegalArgumentException.class);

    }

    @Test
    void failedFeedbackCanRetryWithItsPreviousInputCleared() {
        var latestFingerprint = "b".repeat(64);
        var feedback = ReportFeedback.pending(member, report, WorkType.WATERING, sourceFingerprint);
        feedback.attachInputSnapshot(Map.of("schemaVersion", 3));
        feedback.markFailed("STRUCTURED_OUTPUT_INVALID");

        feedback.retry(latestFingerprint);

        assertThat(feedback.getStatus()).isEqualTo(ReportFeedbackStatus.PENDING);
        assertThat(feedback.getSourceFingerprint()).isEqualTo(latestFingerprint);
        assertThat(feedback.getFailureCode()).isNull();
        assertThat(feedback.getInputSnapshot()).isNull();

    }

    @Test
    void readyFeedbackBecomesStaleAndRetriesWithTheLatestFingerprint() {
        var feedback = ReportFeedback.pending(member, report, WorkType.WATERING, sourceFingerprint);
        feedback.attachInputSnapshot(Map.of("schemaVersion", 3));
        feedback.markReady(
            "관수 작업 요약",
            List.of(),
            List.of(),
            "PASS",
            List.of(),
            "test-chat",
            "test-embedding");

        feedback.markStale();

        assertThat(feedback.getStatus()).isEqualTo(ReportFeedbackStatus.STALE);
        assertThat(feedback.getSourceFingerprint()).isEqualTo(sourceFingerprint);
        assertThat(feedback.getFailureCode()).isNull();
        assertThat(feedback.getInputSnapshot()).isNull();

        var latestFingerprint = "b".repeat(64);
        feedback.retry(latestFingerprint);

        assertThat(feedback.getStatus()).isEqualTo(ReportFeedbackStatus.PENDING);
        assertThat(feedback.getSourceFingerprint()).isEqualTo(latestFingerprint);
        assertThat(feedback.getInputSnapshot()).isNull();

    }

    @Test
    void stalePlaceholderHasNoSourceFingerprintOrPreparedInput() {
        var feedback = ReportFeedback.stalePlaceholder(member, report, WorkType.FERTILIZING);

        assertThat(feedback.getStatus()).isEqualTo(ReportFeedbackStatus.STALE);
        assertThat(feedback.getSourceFingerprint()).isNull();
        assertThat(feedback.getInputSnapshot()).isNull();
        assertThat(feedback.getFailureCode()).isNull();

    }

    @Test
    void legacyFeedbackBackfillsAValidFingerprintWithoutChangingReadyStatus() {
        var legacy = new ReportFeedback(null, member, report, WorkType.WATERING, ReportFeedbackStatus.PENDING, null, null, List.of(), null, List.of(), null, null, null);
        legacy.attachInputSnapshot(Map.of("schemaVersion", 3));
        legacy.markReady(
            "관수 작업 요약",
            List.of(),
            List.of(),
            "PASS",
            List.of(),
            "test-chat",
            "test-embedding");

        legacy.backfillSourceFingerprint(sourceFingerprint);

        assertThat(legacy.getSourceFingerprint()).isEqualTo(sourceFingerprint);
        assertThat(legacy.getStatus()).isEqualTo(ReportFeedbackStatus.READY);
        assertThat(legacy.getInputSnapshot()).isEqualTo(Map.of("schemaVersion", 3));

    }

    @Test
    void fingerprintBackfillNeverOverwritesAnExistingValue() {
        var feedback = ReportFeedback.pending(member, report, WorkType.WATERING, sourceFingerprint);

        feedback.backfillSourceFingerprint("b".repeat(64));

        assertThat(feedback.getSourceFingerprint()).isEqualTo(sourceFingerprint);
        assertThat(feedback.getStatus()).isEqualTo(ReportFeedbackStatus.PENDING);

    }

    @Test
    void pendingAndRetryRejectMalformedFingerprints() {
        assertThatThrownBy(() -> {
            ReportFeedback.pending(member, report, WorkType.WATERING, "not-a-sha256");
        }).isInstanceOf(IllegalArgumentException.class);

        var stale = ReportFeedback.stalePlaceholder(member, report, WorkType.WATERING);
        assertThatThrownBy(() -> { stale.retry("A".repeat(64)); }).isInstanceOf(IllegalArgumentException.class);

    }

    @Test
    void statisticsReturnsTheRecordCountForEveryWorkType() {
        var statistics = new CycleReportStatistics(new PlantingStatistics(1, null, null, 0, null, 0, null, List.of(), null, List.of(), List.of()),
                new WateringStatistics(2, null, null, 0, null, 0, null, List.of(), null, List.of(), List.of()),
                new FertilizingStatistics(3, null, null, 0, null, 0, null, List.of(), null, null, null, new Coverage(0, 0), List.of(), List.of(), List.of(), List.of()),
                new PestControlStatistics(4, null, null, 0, null, 0, null, List.of(), null, List.of(), List.of(), List.of(), null, new Coverage(0, 0), List.of()),
                new WeedingStatistics(5, null, null, 0, null, 0, null, List.of(), null, List.of()),
                new CommonOnlyStatistics(6, null, null, 0, null, 0, null, List.of(), null),
                new HarvestStatistics(7, null, null, 0, null, 0, null, List.of(), null, null, null, new Coverage(0, 0), null, null, List.of(), null, null, List.of()),
                new CommonOnlyStatistics(8, null, null, 0, null, 0, null, List.of(), null));

        assertThat(Arrays.asList(WorkType.values()).stream().map(statistics::recordCountFor).toList())
            .containsExactly(1, 2, 3, 4, 5, 6, 7, 8);

    }
    @Test
    void invalidReadyInputAndRetryLeaveStateUnchanged() {
        var feedback = ReportFeedback.pending(member, report, WorkType.WATERING, sourceFingerprint);
        feedback.attachInputSnapshot(Map.of("schemaVersion", 3));
        assertThatThrownBy(() -> feedback.markReady("summary", List.of(item(ReportFeedbackItemSection.STRENGTH, "basis", "\u00a0")),
                List.of(), "PASS", List.of(), "chat", "embedding")).isInstanceOf(IllegalArgumentException.class);
        assertThat(feedback.getStatus()).isEqualTo(ReportFeedbackStatus.PENDING);
        assertThat(feedback.getSummary()).isNull();
        assertThat(feedback.items()).isEmpty();
        feedback.markFailed("FAILURE");
        assertThatThrownBy(() -> feedback.retry("invalid")).isInstanceOf(IllegalArgumentException.class);
        assertThat(feedback.getStatus()).isEqualTo(ReportFeedbackStatus.FAILED);
        assertThat(feedback.getFailureCode()).isEqualTo("FAILURE");
        assertThat(feedback.getInputSnapshot()).containsEntry("schemaVersion", 3);
    }

    @Test
    void staleClearsFailedInputOnceAndLegacyStaleCannotBackfill() {
        var feedback = ReportFeedback.pending(member, report, WorkType.WATERING, sourceFingerprint);
        feedback.attachInputSnapshot(Map.of("schemaVersion", 3));
        feedback.markFailed("FAILURE");
        feedback.markStale();
        feedback.markStale();
        assertThat(feedback.getStatus()).isEqualTo(ReportFeedbackStatus.STALE);
        assertThat(feedback.getFailureCode()).isNull();
        assertThat(feedback.getInputSnapshot()).isNull();
        assertThat(feedback.getSourceFingerprint()).isEqualTo(sourceFingerprint);
        var placeholder = ReportFeedback.stalePlaceholder(member, report, WorkType.WATERING);
        assertThatThrownBy(() -> placeholder.backfillSourceFingerprint(sourceFingerprint)).isInstanceOf(IllegalStateException.class);
        assertThat(placeholder.getSourceFingerprint()).isNull();
    }

    @Test
    void readyFeedbackRejectsFailureAndSnapshotAndReturnsDetachedItemList() {
        var feedback = ReportFeedback.pending(member, report, WorkType.WATERING, sourceFingerprint);
        feedback.markReady("summary", List.of(item(ReportFeedbackItemSection.NEXT_ACTION, "basis", "text")),
                List.of(), "PASS", List.of(), "chat", "embedding");
        feedback.items().clear();
        assertThat(feedback.items()).hasSize(1);
        assertThatThrownBy(() -> feedback.markFailed("FAILURE")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> feedback.attachInputSnapshot(Map.of())).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> feedback.retry(sourceFingerprint)).isInstanceOf(IllegalStateException.class);
        assertThat(feedback.getStatus()).isEqualTo(ReportFeedbackStatus.READY);
    }

    private Member member(String email) {
        return new Member(UUID.randomUUID(), email, null, com.chamchamcham.domain.member.MemberStatus.ACTIVE,
                null, null, null, null, null, null, "ROLE_USER", null, null);
    }

    private Farm farm(Member member) {
        return new Farm(UUID.randomUUID(), member, "약초농장", "강원도 평창군", null, null, null, null, null, null,
                false, new java.util.ArrayList<>(),
                new com.chamchamcham.domain.farm.FarmDataSource());
    }

    private Crop crop() {
        return new Crop(UUID.randomUUID(), 422, "황기", CropUsePartCategory.ROOT_BARK);
    }

    private FarmingRecord record(Member member, Farm farm, Crop crop, WorkType workType, LocalDateTime at) {
        return new FarmingRecord(UUID.randomUUID(), member, farm, crop, workType, at, "맑음", 24, "기록",
                com.chamchamcham.domain.farming.EntryMode.MANUAL, false);
    }
    private FarmingCycleReport report(Member member, Farm farm, Crop crop, FarmingRecord finalHarvest, LocalDateTime at) {
        return FarmingCycleReport.create(member, farm, crop, new FarmingCycleReportProjection(
                FarmingCycleReportStatus.COMPLETED, at.minusMonths(6), at, FarmingCycleStartBasis.FIRST_RECORD,
                finalHarvest, 1, CycleReportStatistics.empty()));
    }
    private ReportFeedbackItemDraft item(ReportFeedbackItemSection section, String basis, String text) {
        return new ReportFeedbackItemDraft(section, basis, text);
    }
}
