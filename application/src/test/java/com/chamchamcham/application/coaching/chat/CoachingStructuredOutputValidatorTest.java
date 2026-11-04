package com.chamchamcham.application.coaching.chat;

import com.chamchamcham.application.coaching.common.RagAuditStatus;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import java.util.*;
import org.junit.jupiter.api.DisplayName;
import com.chamchamcham.application.coaching.recordfeedback.generation.RecordFeedbackContextKt;
import com.chamchamcham.application.coaching.reportfeedback.generation.ReportFeedbackContextKt;

class CoachingStructuredOutputValidatorTest {
    private final CoachingStructuredOutputValidator validator = new CoachingStructuredOutputValidator();

    @Test
    @DisplayName("invalid confidence fails audit")
    void invalid_confidence_fails_audit() throws Exception {
        var result = new CoachingStructuredResult("요약", CoachingRiskLevel.MEDIUM, 1.4, List.of(), "진단", List.of(), List.of(), List.of(), List.of(), new CoachingRecordQuality(CoachingRecordQualityScore.UNKNOWN, List.of(), ""), List.of());
        var audit = validator.validate(result, Set.of());
        assertThat(audit.getStatus()).isEqualTo(RagAuditStatus.FAIL);
        assertThat(audit.getWarnings()).contains("invalid_confidence");
    }

    @Test
    @DisplayName("unknown citation fails audit")
    void unknown_citation_fails_audit() throws Exception {
        var result = new CoachingStructuredResult("요약", CoachingRiskLevel.LOW, 0.7, List.of(), "진단", java.util.Arrays.asList(new CoachingRecommendation(CoachingPriority.HIGH, "관수 중단", "과습", null, java.util.Arrays.asList("missing"))), List.of(), List.of(), List.of(), new CoachingRecordQuality(CoachingRecordQualityScore.UNKNOWN, List.of(), ""), List.of());
        var audit = validator.validate(result, Set.of("known"));
        assertThat(audit.getStatus()).isEqualTo(RagAuditStatus.FAIL);
        assertThat(audit.getWarnings()).contains("unknown_citation:missing");
    }

    @Test
    @DisplayName("recommendation without citation fails audit for known risk")
    void recommendation_without_citation_fails_audit_for_known_risk() throws Exception {
        var result = new CoachingStructuredResult("요약", CoachingRiskLevel.HIGH, 0.8, List.of(), "진단", java.util.Arrays.asList(new CoachingRecommendation(CoachingPriority.HIGH, "배수로 확인", "과습 위험", null, List.of())), java.util.Arrays.asList(new CoachingNextAction(CoachingActionDue.TODAY, "토양 상태 재점검", List.of())), List.of(), List.of(), new CoachingRecordQuality(CoachingRecordQualityScore.UNKNOWN, List.of(), ""), List.of());
        var audit = validator.validate(result, Set.of());
        assertThat(audit.getStatus()).isEqualTo(RagAuditStatus.FAIL);
        assertThat(audit.getWarnings()).contains("recommendation_without_citation:배수로 확인", "next_action_without_citation:토양 상태 재점검");
    }

    @Test
    @DisplayName("unknown risk with limitations and no citations can pass audit")
    void unknown_risk_with_limitations_and_no_citations_can_pass_audit() throws Exception {
        var result = CoachingStructuredResult.insufficientEvidence("근거 부족");
        var audit = validator.validate(result, Set.of());
        assertThat(audit.getStatus()).isEqualTo(RagAuditStatus.PASS);
    }

    @Test
    @DisplayName("recommendation without citation fails audit even for unknown risk")
    void recommendation_without_citation_fails_audit_even_for_unknown_risk() throws Exception {
        var result = new CoachingStructuredResult("요약", CoachingRiskLevel.UNKNOWN, 0.2, List.of(), "진단", java.util.Arrays.asList(new CoachingRecommendation(CoachingPriority.LOW, "무근거 조언", "이유 없음", null, List.of())), List.of(), List.of(), List.of(), new CoachingRecordQuality(CoachingRecordQualityScore.UNKNOWN, List.of(), ""), List.of());
        var audit = validator.validate(result, Set.of());
        assertThat(audit.getStatus()).isEqualTo(RagAuditStatus.FAIL);
        assertThat(audit.getWarnings()).contains("recommendation_without_citation:무근거 조언");
    }
}
