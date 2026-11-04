package com.chamchamcham.application.coaching.chat;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import java.util.*;
import org.junit.jupiter.api.DisplayName;
import com.chamchamcham.application.coaching.recordfeedback.generation.RecordFeedbackContextKt;
import com.chamchamcham.application.coaching.reportfeedback.generation.ReportFeedbackContextKt;

class CoachingStructuredResultTest {

    @Test
    @DisplayName("insufficient evidence result uses unknown risk and low confidence")
    void insufficient_evidence_result_uses_unknown_risk_and_low_confidence() throws Exception {
        var result = CoachingStructuredResult.Companion.insufficientEvidence("자료가 부족합니다.");
        assertThat(result.getSummary()).isEqualTo("자료가 부족합니다.");
        assertThat(result.getRiskLevel()).isEqualTo(CoachingRiskLevel.UNKNOWN);
        assertThat(result.getConfidence()).isEqualTo(0.0);
        assertThat(result.getRecommendations()).isEmpty();
        assertThat(result.getFollowUpQuestions()).isNotEmpty();
    }

    @Test
    @DisplayName("structured result exposes default record quality and limitations for record feedback")
    void structured_result_exposes_default_record_quality_and_limitations_for_record_feedback() throws Exception {
        var result = CoachingStructuredResult.Companion.insufficientEvidence("근거 부족");
        assertThat(result.getRecordQuality().getScore()).isEqualTo(CoachingRecordQualityScore.UNKNOWN);
        assertThat(result.getRecordQuality().getMissingOrWeakFields()).isEmpty();
        assertThat(result.getLimitations()).contains("근거가 부족해 보수적으로 판단했습니다.");
    }
}
