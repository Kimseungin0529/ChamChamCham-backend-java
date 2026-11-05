package com.chamchamcham.application.coaching.recordfeedback.lifecycle;

import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.chamchamcham.domain.coaching.recordfeedback.*;
import com.chamchamcham.domain.farming.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class RecordFeedbackQueryService {
    private final FarmingRecordRepository farmingRecordRepository;
    private final RecordFeedbackRepository feedbackRepository;
    private final RecordFeedbackLifecycleService lifecycleService;
    public RecordFeedbackQueryService(FarmingRecordRepository farmingRecordRepository, RecordFeedbackRepository feedbackRepository, RecordFeedbackLifecycleService lifecycleService) { this.farmingRecordRepository = Objects.requireNonNull(farmingRecordRepository); this.feedbackRepository = Objects.requireNonNull(feedbackRepository); this.lifecycleService = Objects.requireNonNull(lifecycleService); }
    @Transactional(readOnly = true)
    public RecordFeedbackDetailResult get(UUID memberId, UUID recordId) { Objects.requireNonNull(memberId, "memberId"); Objects.requireNonNull(recordId, "recordId"); return toDetailResult(findLatest(findOwnedActiveRecord(memberId, recordId))); }
    @Transactional
    public RecordFeedbackDetailResult regenerate(UUID memberId, UUID recordId) {
        Objects.requireNonNull(memberId, "memberId"); Objects.requireNonNull(recordId, "recordId");
        var current = findLatest(findOwnedActiveRecord(memberId, recordId));
        if (current.getStatus() != RecordFeedbackStatus.FAILED) throw new BusinessException(ErrorCode.RECORD_FEEDBACK_REGENERATION_NOT_ALLOWED);
        return toDetailResult(lifecycleService.retry(current));
    }
    private FarmingRecord findOwnedActiveRecord(UUID memberId, UUID recordId) { var record = farmingRecordRepository.findByIdAndMember_Id(recordId, memberId); if (record == null || record.isDeleted()) throw new BusinessException(ErrorCode.FARMING_RECORD_NOT_FOUND); return record; }
    private RecordFeedback findLatest(FarmingRecord record) { var feedback = feedbackRepository.findTopByRecord_IdOrderBySourceRevisionDesc(required(record.getId())); if (feedback == null) throw new BusinessException(ErrorCode.RECORD_FEEDBACK_NOT_FOUND); return feedback; }
    private RecordFeedbackDetailResult toDetailResult(RecordFeedback feedback) { return new RecordFeedbackDetailResult(required(feedback.getId()), required(feedback.getRecord().getId()), feedback.getStatus(), feedback.getSourceRevision(), feedback.getInputSnapshot() != null, feedback.getFailureCode(), toContent(feedback), feedback.getCreatedAt(), feedback.getUpdatedAt()); }
    private RecordFeedbackResultContent toContent(RecordFeedback feedback) {
        if (feedback.getStatus() != RecordFeedbackStatus.READY) return null;
        if (feedback.getGoodPointText() == null) throw new IllegalStateException("READY record feedback has no good point");
        var actions = feedback.nextActions();
        if (actions.size() < 2 || actions.size() > 3) throw new IllegalStateException("READY record feedback has invalid next action count");
        var results = new ArrayList<RecordFeedbackNextActionResult>();
        for (var it : actions) results.add(new RecordFeedbackNextActionResult(it.getText(), it.getDue(), it.getCategory()));
        return new RecordFeedbackResultContent(feedback.getGoodPointText(), results);
    }
    private static UUID required(UUID id) { if (id == null) throw new IllegalArgumentException("Required value was null."); return id; }
}
