package com.chamchamcham.domain.coaching.recordfeedback;

import com.chamchamcham.domain.farming.FarmingRecord;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.Nullable;

public interface RecordFeedbackRepository extends JpaRepository<RecordFeedback, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select record from FarmingRecord record where record.id = :recordId")
    @Nullable
    FarmingRecord findRecordByIdForFeedbackUpdate(@Param("recordId") UUID recordId);

    @Nullable
    RecordFeedback findByRecord_IdAndSourceRevision(UUID recordId, long sourceRevision);

    @Nullable
    RecordFeedback findTopByRecord_IdOrderBySourceRevisionDesc(UUID recordId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<RecordFeedback> findAllByRecord_IdOrderBySourceRevisionDesc(UUID recordId);

    @Nullable
    RecordFeedback findByIdAndMember_Id(UUID id, UUID memberId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select feedback from RecordFeedback feedback where feedback.id = :id and feedback.member.id = :memberId")
    @Nullable
    RecordFeedback findByIdAndMemberIdForUpdate(@Param("id") UUID id, @Param("memberId") UUID memberId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        value = """
            update record_feedback
            set status = 'FAILED', failure_code = :failureCode, updated_at = :failedAt
            where status = 'PENDING' and updated_at < :cutoff
        """,
        nativeQuery = true)
    int failPendingUpdatedBefore(@Param("cutoff") LocalDateTime cutoff, @Param("failedAt") LocalDateTime failedAt, @Param("failureCode") String failureCode);
}
