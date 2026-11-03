package com.chamchamcham.domain.coaching.reportfeedback;

import com.chamchamcham.domain.common.BaseTimeEntity;
import com.chamchamcham.domain.crop.Crop;
import com.chamchamcham.domain.crop.CropUsePartCategory;
import com.chamchamcham.domain.farm.Farm;
import com.chamchamcham.domain.farming.FarmingRecord;
import com.chamchamcham.domain.farming.WorkType;
import com.chamchamcham.domain.member.Member;
import com.chamchamcham.domain.report.CycleReportStatistics;
import com.chamchamcham.domain.report.FarmingCycleReport;
import com.chamchamcham.domain.report.FarmingCycleReportProjection;
import com.chamchamcham.domain.report.FarmingCycleReportStatus;
import com.chamchamcham.domain.report.FarmingCycleStartBasis;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:report-feedback-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;INIT=CREATE DOMAIN IF NOT EXISTS JSONB AS JSON",
    "spring.jpa.properties.hibernate.type.json_format_mapper=com.chamchamcham.domain.coaching.reportfeedback.TestReportFeedbackJsonFormatMapper"
})
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ReportFeedbackRepositoryTest {
    @Autowired private TestEntityManager entityManager;
    @Autowired private ReportFeedbackRepository repository;
    private final LocalDateTime completedAt = LocalDateTime.of(2026, 7, 1, 9, 0);
    private Member member;
    private FarmingCycleReport report;
    @BeforeEach
    void setUp() {
        member = persist(member("member@example.com"));
        Farm farm = persist(farm(member));
        Crop crop = persist(crop());
        FarmingRecord finalHarvest = persist(record(member, farm, crop, WorkType.HARVEST, completedAt));
        report = persist(report(member, farm, crop, finalHarvest, completedAt));
    }

    @Test
    void differentWorkTypesCoexistForTheSameReport() {
        repository.saveAll(
            List.of(
                pendingFeedback(WorkType.WATERING),
                pendingFeedback(WorkType.HARVEST)));
        repository.flush();
        entityManager.clear();

        var feedbacks = repository.findAllByReport_IdAndMember_Id(
            java.util.Objects.requireNonNull(report.getId()),
            java.util.Objects.requireNonNull(member.getId()));

        assertThat(feedbacks.stream().map(it -> it.getWorkType()).toList())
            .containsExactlyInAnyOrder(WorkType.WATERING, WorkType.HARVEST);

    }

    @Test
    void theSameWorkTypeCannotBeDuplicatedForOneReport() {
        repository.saveAndFlush(pendingFeedback(WorkType.WATERING));
        repository.save(pendingFeedback(WorkType.WATERING));

        assertThatThrownBy(() -> { repository.flush(); }).isInstanceOf(DataIntegrityViolationException.class);

    }

    @Test
    void feedbackCanBeLockedByReportMemberAndWorkType() {
        var saved = repository.saveAndFlush(pendingFeedback(WorkType.WATERING));
        entityManager.clear();

        var locked = repository.findByReportAndWorkTypeForUpdate(
            java.util.Objects.requireNonNull(report.getId()),
            java.util.Objects.requireNonNull(member.getId()),
            WorkType.WATERING);

        assertThat(locked.getId()).isEqualTo(saved.getId());

    }

    @Test
    void feedbackCollectionCanBeLockedByReportAndMember() {
        repository.saveAllAndFlush(
            List.of(
                pendingFeedback(WorkType.WATERING),
                pendingFeedback(WorkType.HARVEST)));
        entityManager.clear();

        var locked = repository.findAllByReportAndMemberForUpdate(
            java.util.Objects.requireNonNull(report.getId()),
            java.util.Objects.requireNonNull(member.getId()));

        assertThat(locked.stream().map(ReportFeedback::getWorkType).toList())
            .containsExactlyInAnyOrder(WorkType.WATERING, WorkType.HARVEST);

    }

    @Test
    void staleStatusPreservesItsSourceFingerprint() {
        var sourceFingerprint = "a".repeat(64);
        var saved = repository.saveAndFlush(
            timestamped(ReportFeedback.pending(member, report, WorkType.WATERING, sourceFingerprint)));

        saved.markStale();
        repository.flush();
        entityManager.clear();

        var reloaded = repository.findById(java.util.Objects.requireNonNull(saved.getId())).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(ReportFeedbackStatus.STALE);
        assertThat(reloaded.getSourceFingerprint()).isEqualTo(sourceFingerprint);

    }

    @Test
    void onlyPendingFeedbackOlderThanTheCutoffFailsWithProcessingTimeout() {
        var cutoff = completedAt.minusMinutes(2);
        var oldPending = repository.saveAndFlush(
            pendingFeedback(WorkType.WATERING, completedAt.minusMinutes(3)));
        var recentPending = repository.saveAndFlush(
            pendingFeedback(WorkType.HARVEST, completedAt.minusMinutes(1)));
        var oldFailed = repository.saveAndFlush(
            failedFeedback(WorkType.FERTILIZING, completedAt.minusMinutes(3)));
        entityManager.clear();

        var expiredCount = repository.failPendingUpdatedBefore(
            cutoff,
            completedAt,
            "PROCESSING_TIMEOUT");
        entityManager.clear();

        assertThat(expiredCount).isEqualTo(1);
        assertThat(repository.findById(java.util.Objects.requireNonNull(oldPending.getId())).orElseThrow().getStatus())
            .isEqualTo(ReportFeedbackStatus.FAILED);
        assertThat(repository.findById(java.util.Objects.requireNonNull(oldPending.getId())).orElseThrow().getFailureCode())
            .isEqualTo("PROCESSING_TIMEOUT");
        assertThat(repository.findById(java.util.Objects.requireNonNull(recentPending.getId())).orElseThrow().getStatus())
            .isEqualTo(ReportFeedbackStatus.PENDING);
        assertThat(repository.findById(java.util.Objects.requireNonNull(oldFailed.getId())).orElseThrow().getFailureCode())
            .isEqualTo("OTHER_FAILURE");

    }
    @Test
    void readyItemsPersistInDisplayOrderWithTheirParent() {
        var feedback = pendingFeedback(WorkType.WATERING);
        feedback.markReady("summary", List.of(
                new ReportFeedbackItemDraft(ReportFeedbackItemSection.STRENGTH, "basis", "first"),
                new ReportFeedbackItemDraft(ReportFeedbackItemSection.NEXT_ACTION, "basis", "second")),
                List.of(), "PASS", List.of(), "chat", "embedding");
        repository.saveAndFlush(feedback);
        entityManager.clear();
        var reloaded = repository.findById(feedback.getId()).orElseThrow();
        assertThat(reloaded.items()).extracting(ReportFeedbackItem::getDisplayOrder).containsExactly(0, 1);
        assertThat(reloaded.items()).extracting(ReportFeedbackItem::getText).containsExactly("first", "second");
        assertThat(reloaded.items()).allMatch(item -> item.getReportFeedback() == reloaded);
    }

    private Member member(String email) {
        return new Member(null, email, null, com.chamchamcham.domain.member.MemberStatus.ACTIVE,
                null, null, null, null, null, null, "ROLE_USER", null, null);
    }

    private Farm farm(Member member) {
        return new Farm(null, member, "약초농장", "강원도 평창군", null, null, null, null, null, null,
                false, new java.util.ArrayList<>(), new com.chamchamcham.domain.farm.FarmDataSource());
    }

    private Crop crop() {
        return new Crop(null, 422, "황기", CropUsePartCategory.ROOT_BARK);
    }

    private FarmingRecord record(Member member, Farm farm, Crop crop, WorkType workType, LocalDateTime at) {
        return new FarmingRecord(null, member, farm, crop, workType, at, "맑음", 24, "기록",
                com.chamchamcham.domain.farming.EntryMode.MANUAL, false);
    }
    private FarmingCycleReport report(Member member, Farm farm, Crop crop, FarmingRecord finalHarvest, LocalDateTime at) {
        return FarmingCycleReport.create(member, farm, crop, new FarmingCycleReportProjection(
                FarmingCycleReportStatus.COMPLETED, at.minusMonths(6), at, FarmingCycleStartBasis.FIRST_RECORD,
                finalHarvest, 1, CycleReportStatistics.empty()));
    }
    private void setTimestamps(BaseTimeEntity entity, LocalDateTime at) {
        try {
            var created = BaseTimeEntity.class.getDeclaredField("createdAt");
            created.setAccessible(true);
            created.set(entity, at);
            var updated = BaseTimeEntity.class.getDeclaredField("updatedAt");
            updated.setAccessible(true);
            updated.set(entity, at);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }

    private <T extends BaseTimeEntity> T persist(T entity) {
        setTimestamps(entity, completedAt);
        return entityManager.persistAndFlush(entity);
    }
    private ReportFeedback pendingFeedback(WorkType workType) { return pendingFeedback(workType, completedAt); }
    private ReportFeedback pendingFeedback(WorkType workType, LocalDateTime updatedAt) {
        var feedback = ReportFeedback.pending(member, report, workType, "a".repeat(64));
        setTimestamps(feedback, updatedAt);
        return feedback;
    }
    private ReportFeedback failedFeedback(WorkType workType, LocalDateTime updatedAt) {
        var feedback = pendingFeedback(workType, updatedAt);
        feedback.markFailed("OTHER_FAILURE");
        return feedback;
    }
    private ReportFeedback timestamped(ReportFeedback feedback) {
        setTimestamps(feedback, completedAt);
        return feedback;
    }
}

@SpringBootConfiguration
@EnableAutoConfiguration
@EntityScan(basePackages = "com.chamchamcham.domain")
@EnableJpaRepositories(basePackages = "com.chamchamcham.domain")
class ReportFeedbackRepositoryTestApplication {}
