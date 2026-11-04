package com.chamchamcham.application.coaching.common;

import com.chamchamcham.application.coaching.recordfeedback.RecordFeedbackFailureCode;
import com.chamchamcham.application.coaching.reportfeedback.ReportFeedbackFailureCode;
import com.chamchamcham.application.config.AsyncConfig;
import com.chamchamcham.domain.coaching.recordfeedback.RecordFeedbackRepository;
import com.chamchamcham.domain.coaching.reportfeedback.ReportFeedbackRepository;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.DisplayName;
import com.chamchamcham.application.coaching.recordfeedback.generation.RecordFeedbackContextKt;
import com.chamchamcham.application.coaching.reportfeedback.generation.ReportFeedbackContextKt;

class PendingFeedbackExpirySchedulerTest {

    @Test
    @DisplayName("processing timeout is a stable record and report failure code")
    void processing_timeout_is_a_stable_record_and_report_failure_code() throws Exception {
        assertThat(RecordFeedbackFailureCode.PROCESSING_TIMEOUT.name()).isEqualTo("PROCESSING_TIMEOUT");
        assertThat(ReportFeedbackFailureCode.PROCESSING_TIMEOUT.name()).isEqualTo("PROCESSING_TIMEOUT");
    }

    @Test
    @DisplayName("pending expiry runs every configured interval in one transaction")
    void pending_expiry_runs_every_configured_interval_in_one_transaction() throws Exception {
        assertThat(AsyncConfig.class.isAnnotationPresent(EnableScheduling.class)).isTrue();
        var expirePending = PendingFeedbackExpiryScheduler.class.getDeclaredMethod("expirePending");
        assertThat(expirePending.getAnnotation(Scheduled.class).fixedDelayString()).isEqualTo("${rag.execution.scan-interval-millis:60000}");
        assertThat(expirePending.isAnnotationPresent(Transactional.class)).isTrue();
    }

    @Test
    @DisplayName("pending expiry applies configured cutoff to both feedback types")
    void pending_expiry_applies_configured_cutoff_to_both_feedback_types() throws Exception {
        var recordRepository = Mockito.mock(RecordFeedbackRepository.class);
        var reportRepository = Mockito.mock(ReportFeedbackRepository.class);
        var scheduler = new PendingFeedbackExpiryScheduler(recordRepository, reportRepository, new RagProperties(new RagProperties.Embedding(), new RagProperties.Chat(), new RagProperties.Retrieval(), new RagProperties.Execution()));
        scheduler.expirePending();
        assertExpiryInvocation(recordRepository);
        assertExpiryInvocation(reportRepository);
    }
    private void assertExpiryInvocation(Object repository) {
        var matches = Mockito.mockingDetails(repository).getInvocations().stream().filter(it -> it.getMethod().getName().equals("failPendingUpdatedBefore")).toList();
        assertThat(matches).hasSize(1);
        var arguments = matches.getFirst().getArguments();
        var cutoff = (LocalDateTime) arguments[0];
        var failedAt = (LocalDateTime) arguments[1];
        assertThat(cutoff).isEqualTo(failedAt.minusSeconds(240));
        assertThat(arguments[2]).isEqualTo("PROCESSING_TIMEOUT");
    }
}
