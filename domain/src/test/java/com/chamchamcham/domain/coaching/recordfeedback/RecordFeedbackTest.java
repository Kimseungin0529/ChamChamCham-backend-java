package com.chamchamcham.domain.coaching.recordfeedback;

import com.chamchamcham.domain.crop.Crop;
import com.chamchamcham.domain.crop.CropUsePartCategory;
import com.chamchamcham.domain.farm.Farm;
import com.chamchamcham.domain.farming.FarmingRecord;
import com.chamchamcham.domain.farming.WorkType;
import com.chamchamcham.domain.member.Member;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecordFeedbackTest {
    private final Member member = member("member@example.com");
    private final Farm farm = farm(member);
    private final Crop crop = crop();
    private final FarmingRecord record = record(member, farm, crop, WorkType.WATERING, LocalDateTime.of(2026, 6, 15, 8, 30));

    @Test
    void readyFeedbackStoresAGoodPointAndOrderedNextActions() {
        var feedback = RecordFeedback.pending(member, record, 1);

        feedback.markReady(
            "관수 기록",
            "관수 기록이 구체적입니다.",
            List.of(
                action("오늘 토양을 확인하세요."),
                action("이번 주 배수로를 점검하세요.")),
            List.of(Map.of("id", "record:" + (record.getId()) + "")),
            "PASS",
            List.of(),
            "test-chat",
            "test-embedding");

        assertThat(feedback.getStatus()).isEqualTo(RecordFeedbackStatus.READY);
        assertThat(feedback.getGoodPointBasis()).isEqualTo("관수 기록");
        assertThat(feedback.getGoodPointText()).isEqualTo("관수 기록이 구체적입니다.");
        assertThat(feedback.nextActions().stream().map(RecordFeedbackNextAction::getDisplayOrder).toList()).containsExactly(0, 1);
        assertThat(feedback.nextActions().stream().map(RecordFeedbackNextAction::getText).toList())
            .containsExactly("오늘 토양을 확인하세요.", "이번 주 배수로를 점검하세요.");
        assertThat(feedback.getCitations()).containsExactly(Map.of("id", "record:" + (record.getId()) + ""));
        assertThat(feedback.getInputSnapshot()).isNull();

    }

    @Test
    void readyFeedbackRejectsActionCountsOutsideTwoToThree() {
        assertThatThrownBy(() -> {
            RecordFeedback.pending(member, record, 1).markReady(
                "관수 기록",
                "관수 기록이 구체적입니다.",
                List.of(action("오늘 토양을 확인하세요.")),
                List.of(),
                "PASS",
                List.of(),
                "test-chat",
                "test-embedding");
        }).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> {
            RecordFeedback.pending(member, record, 1).markReady(
                "관수 기록",
                "관수 기록이 구체적입니다.",
                java.util.stream.IntStream.range(0, 4).mapToObj(it -> action("행동 " + (it + 1))).toList(),
                List.of(),
                "PASS",
                List.of(),
                "test-chat",
                "test-embedding");
        }).isInstanceOf(IllegalArgumentException.class);

    }
    @Test
    void invalidReadyInputLeavesPendingFeedbackUnchanged() {
        var feedback = RecordFeedback.pending(member, record, 1);
        feedback.attachInputSnapshot(Map.of("schemaVersion", 3));
        assertThatThrownBy(() -> feedback.markReady("basis", "good", List.of(action("one"), action("\u00a0")),
                List.of(), "PASS", List.of(), "chat", "embedding"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(feedback.getStatus()).isEqualTo(RecordFeedbackStatus.PENDING);
        assertThat(feedback.getGoodPointText()).isNull();
        assertThat(feedback.nextActions()).isEmpty();
        assertThat(feedback.getInputSnapshot()).containsEntry("schemaVersion", 3);
    }

    @Test
    void retryClearsFailedInputAndRejectsPendingFeedback() {
        var feedback = RecordFeedback.pending(member, record, 1);
        feedback.attachInputSnapshot(Map.of("schemaVersion", 3));
        feedback.markFailed("FAILURE");
        feedback.retry();
        assertThat(feedback.getStatus()).isEqualTo(RecordFeedbackStatus.PENDING);
        assertThat(feedback.getFailureCode()).isNull();
        assertThat(feedback.getInputSnapshot()).isNull();
        assertThatThrownBy(feedback::retry).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void staleIsIdempotentPreservesInputAndRejectsFailedFeedback() {
        var feedback = RecordFeedback.pending(member, record, 1);
        feedback.attachInputSnapshot(Map.of("schemaVersion", 3));
        feedback.markStale();
        feedback.markStale();
        assertThat(feedback.getStatus()).isEqualTo(RecordFeedbackStatus.STALE);
        assertThat(feedback.getInputSnapshot()).containsEntry("schemaVersion", 3);
        assertThatThrownBy(() -> feedback.markFailed("FAILURE")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> feedback.attachInputSnapshot(Map.of())).isInstanceOf(IllegalStateException.class);
        var failed = RecordFeedback.pending(member, record, 2);
        failed.markFailed("FAILURE");
        assertThatThrownBy(failed::markStale).isInstanceOf(IllegalStateException.class);
        assertThat(failed.getStatus()).isEqualTo(RecordFeedbackStatus.FAILED);
    }

    @Test
    void sourceRevisionMustBePositive() {
        assertThatThrownBy(() -> RecordFeedback.pending(member, record, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void returnedNextActionsCannotMutateFeedback() {
        var feedback = RecordFeedback.pending(member, record, 1);
        feedback.markReady("basis", "good", List.of(action("one"), action("two")),
                List.of(), "PASS", List.of(), "chat", "embedding");
        feedback.nextActions().clear();
        assertThat(feedback.nextActions()).hasSize(2);
        assertThatThrownBy(() -> feedback.markReady("basis", "good", List.of(action("one"), action("two")),
                List.of(), "PASS", List.of(), "chat", "embedding")).isInstanceOf(IllegalStateException.class);
        assertThat(feedback.getStatus()).isEqualTo(RecordFeedbackStatus.READY);
    }

    private Member member(String email) {
        return new Member(UUID.randomUUID(), email, null, com.chamchamcham.domain.member.MemberStatus.ACTIVE,
                null, null, null, null, null, null, "ROLE_USER", null, null);
    }

    private Farm farm(Member member) {
        return new Farm(UUID.randomUUID(), member, "약초농장", "강원도 평창군", null, null, null, null, null, null,
                false, new java.util.ArrayList<>(), new com.chamchamcham.domain.farm.FarmDataSource());
    }

    private Crop crop() {
        return new Crop(UUID.randomUUID(), 422, "황기", CropUsePartCategory.ROOT_BARK);
    }

    private FarmingRecord record(Member member, Farm farm, Crop crop, WorkType workType, LocalDateTime at) {
        return new FarmingRecord(UUID.randomUUID(), member, farm, crop, workType, at, "맑음", 24, "기록",
                com.chamchamcham.domain.farming.EntryMode.MANUAL, false);
    }
    private RecordFeedbackNextActionDraft action(String text) {
        return new RecordFeedbackNextActionDraft(RecordFeedbackActionDue.THIS_WEEK,
                RecordFeedbackActionCategory.CULTIVATION, "관수 기록", text);
    }
}
