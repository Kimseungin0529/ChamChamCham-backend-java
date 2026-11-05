package com.chamchamcham.application.coaching.reportfeedback.generation;

import com.chamchamcham.application.coaching.recordfeedback.generation.RecordFeedbackGenerationService;
import com.chamchamcham.application.coaching.recordfeedback.generation.RecordFeedbackContext;
import com.chamchamcham.application.coaching.recordfeedback.lifecycle.*;
import com.chamchamcham.application.coaching.reportfeedback.lifecycle.*;
import com.chamchamcham.domain.farming.WorkType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import static org.assertj.core.api.Assertions.*;

class ReportFeedbackJavaContractTest {
    private final ObjectMapper mapper = Jackson2ObjectMapperBuilder.json().build();

    @Test
    void fingerprintHasAnIndependentCanonicalLiteral() {
        var fingerprint = new ReportFeedbackContextFingerprint(mapper);
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("z", List.of(new BigDecimal("2.00"), 3L));
        snapshot.put("a", new BigDecimal("1.0"));
        assertThat(fingerprint.calculate(snapshot)).isEqualTo("b02b902601adda5a264f6d461cafbfa7dc19f8653b98bfb260c885c4d44cedf7");
        snapshot.put("a", 2);
        assertThat(fingerprint.calculate(snapshot)).isNotEqualTo("b02b902601adda5a264f6d461cafbfa7dc19f8653b98bfb260c885c4d44cedf7");
    }

    @Test
    void contextAndReportJsonPreserveMissingNullAndDefaultRevision() throws Exception {
        var node = (ObjectNode) mapper.valueToTree(context());
        node.remove("comparisons");
        ((ObjectNode) node.get("report")).remove("sourceRevision");
        var restored = mapper.treeToValue(node, ReportFeedbackContext.class);
        assertThat(restored.getComparisons()).isEmpty();
        assertThat(restored.getReport().getSourceRevision()).isEqualTo(1);
        ((ObjectNode) node.get("report")).putNull("sourceRevision");
        assertThat(mapper.treeToValue(node, ReportFeedbackContext.class).getReport().getSourceRevision()).isZero();
        node.putNull("comparisons");
        assertThatThrownBy(() -> mapper.treeToValue(node, ReportFeedbackContext.class)).isInstanceOf(Exception.class);
        node.remove("comparisons");
        node.remove("warnings");
        assertThatThrownBy(() -> mapper.treeToValue(node, ReportFeedbackContext.class)).isInstanceOf(Exception.class);
    }

    @Test
    void contextMapsAndListsKeepAliasesAndValueEquality() {
        var statistics = new LinkedHashMap<String, Object>(); statistics.put("recordCount", 1);
        var records = new ArrayList<ReportFeedbackRecord>();
        var base = context(); var report = base.getReport();
        var aliasedReport = new ReportFeedbackReport(report.getId(), report.getFarmName(), report.getCropName(), report.getStartsAt(), report.getEndsAt(), 1, statistics);
        var value = new ReportFeedbackContext(3, WorkType.WATERING, aliasedReport, records, null, List.of(), List.of());
        assertThat(value.getReport().getStatistics()).isSameAs(statistics);
        assertThat(value.getRecords()).isSameAs(records);
        statistics.put("recordCount", 2); records.add(base.getRecords().getFirst());
        assertThat(value.getReport().getStatistics()).containsEntry("recordCount", 2);
        assertThat(value.getRecords()).hasSize(1);
        assertThat(value).isEqualTo(new ReportFeedbackContext(3, WorkType.WATERING, aliasedReport, records, null, List.of(), List.of()));
    }

    @Test
    void preparationKeepsAfterCommitAsyncAndGenerationKeepsNoRemoteTransaction() throws Exception {
        var recordOn = RecordFeedbackPreparationListener.class.getMethod("on", RecordFeedbackPreparationRequested.class);
        var reportOn = ReportFeedbackPreparationHandler.class.getMethod("on", ReportFeedbackPreparationRequested.class);
        for (var method : List.of(recordOn, reportOn)) {
            assertThat(method.getAnnotation(TransactionalEventListener.class).phase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
            assertThat(method.getAnnotation(Async.class).value()).isEqualTo("coachingTaskExecutor");
            assertThat(java.lang.reflect.Modifier.isFinal(method.getModifiers())).isFalse();
            assertThat(java.lang.reflect.Modifier.isFinal(method.getDeclaringClass().getModifiers())).isFalse();
        }
        var transactions = new AnnotationTransactionAttributeSource();
        assertThat(transactions.getTransactionAttribute(RecordFeedbackGenerationService.class.getMethod("generate", RecordFeedbackContext.class, Integer.class), RecordFeedbackGenerationService.class)).isNull();
        assertThat(transactions.getTransactionAttribute(ReportFeedbackGenerationService.class.getMethod("generate", ReportFeedbackContext.class), ReportFeedbackGenerationService.class)).isNull();
    }

    private ReportFeedbackContext context() {
        var day = LocalDateTime.of(2026, 4, 1, 9, 0);
        var id = UUID.fromString("00000000-0000-0000-0000-000000000001");
        return new ReportFeedbackContext(3, WorkType.WATERING, new ReportFeedbackReport(id, "약초밭", "황기", day, day.plusDays(3), 1, Map.of("recordCount", 1)), List.of(new ReportFeedbackRecord(id, day, WorkType.WATERING, "물 주기", Map.of())), null, List.of(), List.of());
    }
}
