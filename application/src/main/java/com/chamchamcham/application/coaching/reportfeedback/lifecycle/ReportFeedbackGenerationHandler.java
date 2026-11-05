package com.chamchamcham.application.coaching.reportfeedback.lifecycle;

import com.chamchamcham.application.coaching.common.RagAuditStatus;
import com.chamchamcham.application.coaching.reportfeedback.*;
import com.chamchamcham.application.coaching.reportfeedback.generation.*;
import com.chamchamcham.domain.coaching.reportfeedback.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.*;
import java.util.regex.Pattern;

@Component
public class ReportFeedbackGenerationHandler {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ReportFeedbackGenerationHandler.class);
    private static final Pattern SAFE_ITEM_DIAGNOSTIC = Pattern.compile("^(comparison|strength|improvement|next_action)_(count|basis_blank|text_blank|text_length|text_paragraph|evidence_refs_blank)$");
    private static final Set<String> SAFE_VALIDATION_DIAGNOSTICS = Set.of("summary_blank", "summary_text_length", "comparison_not_available", "comparison_current_report_ref_required", "comparison_previous_report_ref_required", "structured_output_parse_failed", "invalid_output");
    private final ReportFeedbackRepository feedbackRepository;
    private final ReportFeedbackGenerationService generationService;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate writeTransaction;
    public ReportFeedbackGenerationHandler(ReportFeedbackRepository feedbackRepository, ReportFeedbackGenerationService generationService, ObjectMapper objectMapper, PlatformTransactionManager transactionManager) { this.feedbackRepository = Objects.requireNonNull(feedbackRepository); this.generationService = Objects.requireNonNull(generationService); this.objectMapper = Objects.requireNonNull(objectMapper); writeTransaction = new TransactionTemplate(Objects.requireNonNull(transactionManager)); writeTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW); }
    @EventListener
    public void on(ReportFeedbackGenerationRequested event) {
        Objects.requireNonNull(event, "event");
        var feedback = feedbackRepository.findByIdAndMember_Id(event.getFeedbackId(), event.getMemberId());
        if (feedback == null || !matches(feedback, event)) return;
        var snapshot = feedback.getInputSnapshot(); if (snapshot == null) return;
        ReportFeedbackContext context;
        try { context = objectMapper.convertValue(snapshot, ReportFeedbackContext.class); }
        catch (RuntimeException exception) { finishFailed(event, snapshot, ReportFeedbackFailureCode.INVALID_CONTEXT_SNAPSHOT); return; }
        if (!context.getReport().getId().equals(event.getReportId()) || context.getWorkType() != event.getWorkType()) { finishFailed(event, snapshot, ReportFeedbackFailureCode.INVALID_CONTEXT); return; }
        ReportFeedbackGenerationResult result;
        try { result = generationService.generate(context); }
        catch (ReportFeedbackGenerationFailure failure) { logger.warn("report feedback generation failed: code={}, diagnostics={}", failure.getCode(), String.join(",", safeValidationDiagnosticCodes(failure))); finishFailed(event, snapshot, failure.getCode()); return; }
        catch (RuntimeException exception) { finishFailed(event, snapshot, ReportFeedbackFailureCode.UNEXPECTED); return; }
        finishReady(event, snapshot, result);
    }
    private void finishReady(ReportFeedbackGenerationRequested event, Map<String, Object> snapshot, ReportFeedbackGenerationResult result) {
        writeTransaction.executeWithoutResult(status -> {
            var feedback = feedbackRepository.findByIdAndMemberIdForUpdate(event.getFeedbackId(), event.getMemberId());
            if (feedback == null || !matches(feedback, event) || !Objects.equals(feedback.getInputSnapshot(), snapshot)) return;
            var items = new ArrayList<ReportFeedbackItemDraft>(); result.getContent().items().forEach(it -> items.add(new ReportFeedbackItemDraft(it.getSection(), it.getItem().getBasis(), it.getItem().getText())));
            feedback.markReady(result.getContent().getSummary(), items, result.getCitations(), result.getAuditWarnings().isEmpty() ? RagAuditStatus.PASS.name() : RagAuditStatus.WARN.name(), result.getAuditWarnings(), result.getModelInfo().getChat(), result.getModelInfo().getEmbedding());
        });
    }
    private void finishFailed(ReportFeedbackGenerationRequested event, Map<String, Object> snapshot, ReportFeedbackFailureCode code) { writeTransaction.executeWithoutResult(status -> { var feedback = feedbackRepository.findByIdAndMemberIdForUpdate(event.getFeedbackId(), event.getMemberId()); if (feedback != null && matches(feedback, event) && Objects.equals(feedback.getInputSnapshot(), snapshot)) feedback.markFailed(code.name()); }); }
    private boolean matches(ReportFeedback feedback, ReportFeedbackGenerationRequested event) { return feedback.getStatus() == ReportFeedbackStatus.PENDING && Objects.equals(feedback.getReport().getId(), event.getReportId()) && feedback.getWorkType() == event.getWorkType() && Objects.equals(feedback.getSourceFingerprint(), event.getSourceFingerprint()); }
    private List<String> safeValidationDiagnosticCodes(ReportFeedbackGenerationFailure failure) {
        String message = failure.getCause() == null ? null : failure.getCause().getMessage(); if (message == null) return Collections.emptyList();
        var result = new LinkedHashSet<String>();
        for (var raw : message.split(",", -1)) { String value = trim(raw); if (value.split(":", 2)[0].equals("unknown_evidence")) result.add("unknown_evidence"); else if (SAFE_VALIDATION_DIAGNOSTICS.contains(value) || SAFE_ITEM_DIAGNOSTIC.matcher(value).matches()) result.add(value); }
        return switch (result.size()) { case 0 -> Collections.emptyList(); case 1 -> Collections.singletonList(result.iterator().next()); default -> new ArrayList<>(result); };
    }
    private static String trim(String text) { int start = 0, end = text.length(); while (start < end && (Character.isWhitespace(text.charAt(start)) || Character.isSpaceChar(text.charAt(start)))) start++; while (end > start && (Character.isWhitespace(text.charAt(end - 1)) || Character.isSpaceChar(text.charAt(end - 1)))) end--; return text.substring(start, end); }
}
