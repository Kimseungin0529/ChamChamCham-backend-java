package com.chamchamcham.domain.coaching.reportfeedback;

import com.chamchamcham.domain.farming.WorkType;
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

public interface ReportFeedbackRepository extends JpaRepository<ReportFeedback, UUID> {
    List<ReportFeedback> findAllByReport_IdAndMember_Id(UUID reportId, UUID memberId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        """
            select feedback from ReportFeedback feedback
            where feedback.report.id = :reportId
              and feedback.member.id = :memberId
        """)
    List<ReportFeedback> findAllByReportAndMemberForUpdate(@Param("reportId") UUID reportId, @Param("memberId") UUID memberId);

    @Nullable
    ReportFeedback findByIdAndMember_Id(UUID id, UUID memberId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select feedback from ReportFeedback feedback where feedback.id = :id and feedback.member.id = :memberId")
    @Nullable
    ReportFeedback findByIdAndMemberIdForUpdate(@Param("id") UUID id, @Param("memberId") UUID memberId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        """
            select feedback from ReportFeedback feedback
            where feedback.report.id = :reportId
              and feedback.member.id = :memberId
              and feedback.workType = :workType
        """)
    @Nullable
    ReportFeedback findByReportAndWorkTypeForUpdate(@Param("reportId") UUID reportId, @Param("memberId") UUID memberId, @Param("workType") WorkType workType);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        value = """
            update report_feedback
            set status = 'FAILED', failure_code = :failureCode, updated_at = :failedAt
            where status = 'PENDING' and updated_at < :cutoff
        """,
        nativeQuery = true)
    int failPendingUpdatedBefore(@Param("cutoff") LocalDateTime cutoff, @Param("failedAt") LocalDateTime failedAt, @Param("failureCode") String failureCode);
}
