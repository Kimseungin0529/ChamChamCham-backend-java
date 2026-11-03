package com.chamchamcham.domain.coaching.recordfeedback;

import com.chamchamcham.domain.common.BaseTimeEntity;
import com.chamchamcham.domain.crop.Crop;
import com.chamchamcham.domain.crop.CropUsePartCategory;
import com.chamchamcham.domain.farm.Farm;
import com.chamchamcham.domain.farming.EntryMode;
import com.chamchamcham.domain.farming.FarmingRecord;
import com.chamchamcham.domain.farming.WorkType;
import com.chamchamcham.domain.member.Member;
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
    "spring.datasource.url=jdbc:h2:mem:record-feedback-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;INIT=CREATE DOMAIN IF NOT EXISTS JSONB AS JSON",
    "spring.jpa.properties.hibernate.type.json_format_mapper=com.chamchamcham.domain.coaching.recordfeedback.TestRecordFeedbackJsonFormatMapper"
})
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RecordFeedbackRepositoryTest {
    @Autowired private TestEntityManager entityManager;
    @Autowired private RecordFeedbackRepository repository;
    private final LocalDateTime workedAt = LocalDateTime.of(2026, 7, 1, 9, 0);
    private Member member;
    private FarmingRecord record;
    @BeforeEach
    void setUp() {
        member = persist(member("member@example.com"));
        Farm farm = persist(farm(member));
        Crop crop = persist(crop());
        record = persist(record(member, farm, crop, WorkType.WATERING, workedAt));
    }

    @Test
    void theSameSourceRevisionCannotBeDuplicatedForOneRecord() {
        repository.saveAndFlush(pendingFeedback(1));
        repository.save(pendingFeedback(1));

        assertThatThrownBy(() -> { repository.flush(); }).isInstanceOf(DataIntegrityViolationException.class);

    }

    @Test
    void feedbackRowsAreLockedAndReturnedNewestFirst() {
        repository.saveAllAndFlush(List.of(pendingFeedback(1), pendingFeedback(2)));
        entityManager.clear();

        var feedbacks = repository.findAllByRecord_IdOrderBySourceRevisionDesc(java.util.Objects.requireNonNull(record.getId()));

        assertThat(feedbacks.stream().map(it -> it.getSourceRevision()).toList()).containsExactly(2L, 1L);

    }

    @Test
    void feedbackRevisionAllocationExposesAParentRecordLock() {
        var lockedRecord = repository.findRecordByIdForFeedbackUpdate(java.util.Objects.requireNonNull(record.getId()));

        assertThat(lockedRecord.getId()).isEqualTo(record.getId());

    }

    @Test
    void onlyPendingFeedbackOlderThanTheCutoffFailsWithProcessingTimeout() {
        var cutoff = workedAt.minusMinutes(2);
        var oldPending = repository.saveAndFlush(pendingFeedback(1, workedAt.minusMinutes(3)));
        var recentPending = repository.saveAndFlush(pendingFeedback(2, workedAt.minusMinutes(1)));
        var oldFailed = repository.saveAndFlush(
            failedFeedback(3, workedAt.minusMinutes(3)));
        entityManager.clear();

        var expiredCount = repository.failPendingUpdatedBefore(
            cutoff,
            workedAt,
            "PROCESSING_TIMEOUT");
        entityManager.clear();

        assertThat(expiredCount).isEqualTo(1);
        assertThat(repository.findById(java.util.Objects.requireNonNull(oldPending.getId())).orElseThrow().getStatus())
            .isEqualTo(RecordFeedbackStatus.FAILED);
        assertThat(repository.findById(java.util.Objects.requireNonNull(oldPending.getId())).orElseThrow().getFailureCode())
            .isEqualTo("PROCESSING_TIMEOUT");
        assertThat(repository.findById(java.util.Objects.requireNonNull(recentPending.getId())).orElseThrow().getStatus())
            .isEqualTo(RecordFeedbackStatus.PENDING);
        assertThat(repository.findById(java.util.Objects.requireNonNull(oldFailed.getId())).orElseThrow().getFailureCode())
            .isEqualTo("OTHER_FAILURE");

    }
    @Test
    void readyActionsPersistInDisplayOrderWithTheirParent() {
        var feedback = pendingFeedback(1);
        feedback.markReady("basis", "good", List.of(
                new RecordFeedbackNextActionDraft(RecordFeedbackActionDue.TODAY, RecordFeedbackActionCategory.GENERAL, "basis", "first"),
                new RecordFeedbackNextActionDraft(RecordFeedbackActionDue.THIS_WEEK, RecordFeedbackActionCategory.IRRIGATION, "basis", "second")),
                List.of(), "PASS", List.of(), "chat", "embedding");
        repository.saveAndFlush(feedback);
        entityManager.clear();
        var reloaded = repository.findById(feedback.getId()).orElseThrow();
        assertThat(reloaded.nextActions()).extracting(RecordFeedbackNextAction::getDisplayOrder).containsExactly(0, 1);
        assertThat(reloaded.nextActions()).extracting(RecordFeedbackNextAction::getText).containsExactly("first", "second");
        assertThat(reloaded.nextActions()).allMatch(action -> action.getRecordFeedback() == reloaded);
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
        setTimestamps(entity, workedAt);
        return entityManager.persistAndFlush(entity);
    }
    private RecordFeedback pendingFeedback(long sourceRevision) { return pendingFeedback(sourceRevision, workedAt); }
    private RecordFeedback pendingFeedback(long sourceRevision, LocalDateTime updatedAt) {
        var feedback = RecordFeedback.pending(member, record, sourceRevision);
        setTimestamps(feedback, updatedAt);
        return feedback;
    }
    private RecordFeedback failedFeedback(long sourceRevision, LocalDateTime updatedAt) {
        var feedback = pendingFeedback(sourceRevision, updatedAt);
        feedback.markFailed("OTHER_FAILURE");
        return feedback;
    }
}

@SpringBootConfiguration
@EnableAutoConfiguration
@EntityScan(basePackages = "com.chamchamcham.domain")
@EnableJpaRepositories(basePackages = "com.chamchamcham.domain")
class RecordFeedbackRepositoryTestApplication {}
