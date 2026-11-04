package com.chamchamcham.application.coaching.recordfeedback.lifecycle;

import com.chamchamcham.application.coaching.common.RagAuditStatus;
import com.chamchamcham.application.coaching.recordfeedback.*;
import com.chamchamcham.application.coaching.recordfeedback.generation.*;
import com.chamchamcham.domain.coaching.recordfeedback.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class RecordFeedbackGenerationProcessor {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(RecordFeedbackGenerationProcessor.class);
    private static final String INVALID_PRODUCT_OUTPUT_PREFIX = "invalid product output: ";
    private static final Pattern SAFE_VALIDATION_DIAGNOSTIC = Pattern.compile("^(good_point|next_action_[0-9]+)_(basis_blank|text_blank|evidence_refs_blank|evidence_ref_blank|text_length|text_tone|text_english)$|^(action_count|weather_action_without_weather_evidence|pest_disease_action_without_document_evidence)$");
    private final RecordFeedbackRepository feedbackRepository;
    private final RecordFeedbackGenerationService generationService;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate writeTransaction;
    public RecordFeedbackGenerationProcessor(RecordFeedbackRepository feedbackRepository, RecordFeedbackGenerationService generationService, ObjectMapper objectMapper, PlatformTransactionManager transactionManager) {
        this.feedbackRepository = Objects.requireNonNull(feedbackRepository); this.generationService = Objects.requireNonNull(generationService); this.objectMapper = Objects.requireNonNull(objectMapper); writeTransaction = new TransactionTemplate(Objects.requireNonNull(transactionManager)); writeTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }
    public void generate(RecordFeedbackGenerationRequested event) {
        Objects.requireNonNull(event, "event");
        var pending = loadPendingGeneration(event);
        if (pending == null) return;
        RecordFeedbackGenerationResult result;
        try { result = generationService.generate(pending.context(), null); }
        catch (RecordFeedbackGenerationFailure failure) { logger.warn("record feedback generation failed: code={}, diagnostics={}", failure.getCode(), String.join(",", safeValidationDiagnosticCodes(failure))); finalizeFailure(event, pending.snapshot(), failure.getCode()); return; }
        catch (RuntimeException exception) { logger.error("unexpected record feedback generation failure", exception); finalizeFailure(event, pending.snapshot(), RecordFeedbackFailureCode.UNEXPECTED); return; }
        finalizeReady(event, pending.snapshot(), result);
    }
    private ValidGeneration loadPendingGeneration(RecordFeedbackGenerationRequested event) {
        GenerationRead read = writeTransaction.execute(status -> {
            var feedback = feedbackRepository.findByIdAndMember_Id(event.getFeedbackId(), event.getMemberId());
            if (feedback == null) return null;
            var snapshot = feedback.getInputSnapshot();
            if (snapshot == null || !matches(feedback, event, snapshot)) return null;
            RecordFeedbackContext context;
            try { context = objectMapper.convertValue(snapshot, RecordFeedbackContext.class); }
            catch (RuntimeException exception) { context = null; }
            return context == null ? new MalformedGeneration(snapshot) : new ValidGeneration(context, snapshot);
        });
        if (read instanceof ValidGeneration valid) return valid;
        if (read instanceof MalformedGeneration malformed) finalizeFailure(event, malformed.snapshot(), RecordFeedbackFailureCode.INVALID_CONTEXT_SNAPSHOT);
        return null;
    }
    private void finalizeReady(RecordFeedbackGenerationRequested event, Map<String, Object> snapshot, RecordFeedbackGenerationResult result) {
        writeTransaction.executeWithoutResult(status -> {
            var feedback = feedbackRepository.findByIdAndMemberIdForUpdate(event.getFeedbackId(), event.getMemberId());
            if (feedback == null || !matches(feedback, event, snapshot)) return;
            var actions = new ArrayList<RecordFeedbackNextActionDraft>();
            for (var it : result.getContent().getNextActions()) actions.add(new RecordFeedbackNextActionDraft(it.getDue(), it.getCategory(), it.getBasis(), it.getText()));
            feedback.markReady(result.getContent().getGoodPoint().getBasis(), result.getContent().getGoodPoint().getText(), actions, result.getCitations(), result.getAuditWarnings().isEmpty() ? RagAuditStatus.PASS.name() : RagAuditStatus.WARN.name(), result.getAuditWarnings(), result.getModelInfo().getChat(), result.getModelInfo().getEmbedding());
        });
    }
    private void finalizeFailure(RecordFeedbackGenerationRequested event, Map<String, Object> snapshot, RecordFeedbackFailureCode code) {
        writeTransaction.executeWithoutResult(status -> { var feedback = feedbackRepository.findByIdAndMemberIdForUpdate(event.getFeedbackId(), event.getMemberId()); if (feedback != null && matches(feedback, event, snapshot)) feedback.markFailed(code.name()); });
    }
    private boolean matches(RecordFeedback feedback, RecordFeedbackGenerationRequested event, Map<String, Object> snapshot) { return feedback.getStatus() == RecordFeedbackStatus.PENDING && Objects.equals(feedback.getRecord().getId(), event.getRecordId()) && feedback.getSourceRevision() == event.getSourceRevision() && Objects.equals(feedback.getInputSnapshot(), snapshot); }
    private List<String> safeValidationDiagnosticCodes(RecordFeedbackGenerationFailure failure) {
        String message = failure.getCause() == null ? null : failure.getCause().getMessage();
        if (message == null || !message.startsWith(INVALID_PRODUCT_OUTPUT_PREFIX)) return Collections.emptyList();
        var diagnostics = new LinkedHashSet<String>();
        for (String value : message.substring(INVALID_PRODUCT_OUTPUT_PREFIX.length()).split(",", -1)) { String warning = trim(value); if (warning.split(":", 2)[0].equals("unknown_evidence")) diagnostics.add("unknown_evidence"); else if (SAFE_VALIDATION_DIAGNOSTIC.matcher(warning).matches()) diagnostics.add(warning); }
        return switch (diagnostics.size()) { case 0 -> Collections.emptyList(); case 1 -> Collections.singletonList(diagnostics.iterator().next()); default -> new ArrayList<>(diagnostics); };
    }
    private static String trim(String text) { int start = 0, end = text.length(); while (start < end && (Character.isWhitespace(text.charAt(start)) || Character.isSpaceChar(text.charAt(start)))) start++; while (end > start && (Character.isWhitespace(text.charAt(end - 1)) || Character.isSpaceChar(text.charAt(end - 1)))) end--; return text.substring(start, end); }
    private sealed interface GenerationRead permits ValidGeneration, MalformedGeneration {}
    private record ValidGeneration(RecordFeedbackContext context, Map<String, Object> snapshot) implements GenerationRead {}
    private record MalformedGeneration(Map<String, Object> snapshot) implements GenerationRead {}
}
