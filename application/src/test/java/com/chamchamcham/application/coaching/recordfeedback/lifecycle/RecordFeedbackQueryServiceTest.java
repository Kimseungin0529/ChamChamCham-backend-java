package com.chamchamcham.application.coaching.recordfeedback.lifecycle;

import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.chamchamcham.domain.coaching.recordfeedback.RecordFeedback;
import com.chamchamcham.domain.coaching.recordfeedback.RecordFeedbackActionCategory;
import com.chamchamcham.domain.coaching.recordfeedback.RecordFeedbackActionDue;
import com.chamchamcham.domain.coaching.recordfeedback.RecordFeedbackNextActionDraft;
import com.chamchamcham.domain.coaching.recordfeedback.RecordFeedbackRepository;
import com.chamchamcham.domain.coaching.recordfeedback.RecordFeedbackStatus;
import com.chamchamcham.domain.crop.Crop;
import com.chamchamcham.domain.crop.CropUsePartCategory;
import com.chamchamcham.domain.farm.Farm;
import com.chamchamcham.domain.farming.FarmingRecord;
import com.chamchamcham.domain.farming.FarmingRecordRepository;
import com.chamchamcham.domain.farming.WorkType;
import com.chamchamcham.domain.member.Member;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.*;
import org.junit.jupiter.api.DisplayName;

@ExtendWith({MockitoExtension.class})
class RecordFeedbackQueryServiceTest {
    @Mock private FarmingRecordRepository farmingRecordRepository;
    @Mock private RecordFeedbackRepository feedbackRepository;
    @Mock private RecordFeedbackLifecycleService lifecycleService;
    private final Member member = new Member(UUID.randomUUID(), "member@example.com", null);
    private final Farm farm = new Farm(UUID.randomUUID(), member, "약초밭", "강원도 평창군");
    private final Crop crop = new Crop(UUID.randomUUID(), 1, "황기", CropUsePartCategory.ROOT_BARK);
    private final FarmingRecord record = new FarmingRecord(UUID.randomUUID(), member, farm, crop, WorkType.WATERING, LocalDateTime.of(2026, 7, 11, 9, 0), "맑음", 25, "관수 기록", com.chamchamcham.domain.farming.EntryMode.MANUAL, false);
    private RecordFeedbackQueryService service;

    @BeforeEach
    void setUp() throws Exception {
        service = new RecordFeedbackQueryService(farmingRecordRepository, feedbackRepository, lifecycleService);
    }

    @Test
    @DisplayName("ready feedback maps stored columns and action rows to response content")
    void ready_feedback_maps_stored_columns_and_action_rows_to_response_content() throws Exception {
        stubOwnedRecord();
        var feedback = readyFeedback();
        when(feedbackRepository.findTopByRecord_IdOrderBySourceRevisionDesc(record.getId())).thenReturn(feedback);
        var result = service.get(member.getId(), record.getId());
        assertThat(result.getStatus()).isEqualTo(RecordFeedbackStatus.READY);
        assertThat(result.getContent().getGoodPoint()).isEqualTo("점적관수로 토양 수분을 확인한 점이 좋았어요.");
        assertThat(result.getContent().getNextActions().stream().map(it -> it.getText()).toList()).containsExactly("오후에 토양 표면이 마르는지 한 번 더 확인하세요.", "이번 주 안에 배수로 주변의 막힌 흙을 정리하세요.");
        assertThat(result.getContent().getNextActions().stream().map(it -> it.getCategory()).toList()).containsExactly(RecordFeedbackActionCategory.IRRIGATION, RecordFeedbackActionCategory.CULTIVATION);
    }

    @Test
    @DisplayName("get returns the latest stale feedback")
    void get_returns_the_latest_stale_feedback() throws Exception {
        stubOwnedRecord();
        var stale = feedback(RecordFeedbackStatus.STALE, 2, null);
        when(feedbackRepository.findTopByRecord_IdOrderBySourceRevisionDesc(record.getId())).thenReturn(stale);
        var result = service.get(member.getId(), record.getId());
        assertThat(result.getFeedbackId()).isEqualTo(stale.getId());
        assertThat(result.getStatus()).isEqualTo(RecordFeedbackStatus.STALE);
    }

    @Test
    @DisplayName("regenerate retries only failed current feedback")
    void regenerate_retries_only_failed_current_feedback() throws Exception {
        stubOwnedRecord();
        var failed = feedback(RecordFeedbackStatus.FAILED, 3, "CONTEXT_ASSEMBLY_FAILED");
        var retried = feedback(RecordFeedbackStatus.PENDING, 3, null);
        when(feedbackRepository.findTopByRecord_IdOrderBySourceRevisionDesc(record.getId())).thenReturn(failed);
        when(lifecycleService.retry(failed)).thenReturn(retried);
        var result = service.regenerate(member.getId(), record.getId());
        verify(lifecycleService).retry(failed);
        assertThat(result.getStatus()).isEqualTo(RecordFeedbackStatus.PENDING);
    }

    @Test
    @DisplayName("get rejects a record not owned by the member")
    void get_rejects_a_record_not_owned_by_the_member() throws Exception {
        var otherMemberId = UUID.randomUUID();
        when(farmingRecordRepository.findByIdAndMember_Id(record.getId(), otherMemberId)).thenReturn(null);
        assertThatThrownBy(() -> service.get(otherMemberId, record.getId())).isInstanceOf(BusinessException.class).extracting("errorCode").isEqualTo(ErrorCode.FARMING_RECORD_NOT_FOUND);
    }

    @Test
    @DisplayName("get rejects a soft deleted record")
    void get_rejects_a_soft_deleted_record() throws Exception {
        record.softDelete();
        when(farmingRecordRepository.findByIdAndMember_Id(record.getId(), member.getId())).thenReturn(record);
        assertThatThrownBy(() -> service.get(member.getId(), record.getId())).isInstanceOf(BusinessException.class).extracting("errorCode").isEqualTo(ErrorCode.FARMING_RECORD_NOT_FOUND);
    }
    private RecordFeedback readyFeedback() {
        return also(feedback(RecordFeedbackStatus.PENDING, 3, null), it -> {
        it.markReady("점적관수", "점적관수로 토양 수분을 확인한 점이 좋았어요.", java.util.Arrays.asList(new RecordFeedbackNextActionDraft(RecordFeedbackActionDue.TODAY, RecordFeedbackActionCategory.IRRIGATION, "토양 표면", "오후에 토양 표면이 마르는지 한 번 더 확인하세요."), new RecordFeedbackNextActionDraft(RecordFeedbackActionDue.THIS_WEEK, RecordFeedbackActionCategory.CULTIVATION, "배수로", "이번 주 안에 배수로 주변의 막힌 흙을 정리하세요.")), List.of(), "PASS", List.of(), "chat", "embed");
        });
    }
    private RecordFeedback feedback(RecordFeedbackStatus status, long sourceRevision, String failureCode) {
        var feedback = new RecordFeedback(UUID.randomUUID(), member, record, RecordFeedbackStatus.PENDING, sourceRevision);
        switch (status) {
            case FAILED -> feedback.markFailed(Objects.requireNonNull(failureCode));
            case STALE -> feedback.markStale();
            default -> { }
        }
        ReflectionTestUtils.setField(feedback, "createdAt", LocalDateTime.of(2026, 7, 11, 10, 0));
        ReflectionTestUtils.setField(feedback, "updatedAt", LocalDateTime.of(2026, 7, 11, 10, 1));
        return feedback;
    }
    private void stubOwnedRecord() {
        when(farmingRecordRepository.findByIdAndMember_Id(record.getId(), member.getId())).thenReturn(record);
    }
    @SafeVarargs
    private static <K> Map<K, Object> map(Map.Entry<K, ?>... entries) {
        Map<K, Object> result = new LinkedHashMap<>();
        for (var entry : entries) result.put(entry.getKey(), entry.getValue());
        return result;
    }

    private static <T> T also(T value, java.util.function.Consumer<T> action) { action.accept(value); return value; }

}
