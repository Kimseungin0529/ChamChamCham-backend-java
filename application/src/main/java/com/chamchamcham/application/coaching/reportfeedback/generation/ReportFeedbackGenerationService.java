package com.chamchamcham.application.coaching.reportfeedback.generation;

import com.chamchamcham.application.coaching.common.*;
import com.chamchamcham.application.coaching.reportfeedback.*;
import com.chamchamcham.application.exception.business.BusinessException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class ReportFeedbackGenerationService {
    private static final Pattern SAFE_ITEM_WARNING = Pattern.compile("^(comparison|strength|improvement|next_action)_(count|basis_blank|text_blank|text_length|text_paragraph|evidence_refs_blank)$");
    private static final Set<String> SAFE_RETRY_WARNINGS = Set.of("summary_blank", "summary_text_length", "comparison_not_available", "comparison_current_report_ref_required", "comparison_previous_report_ref_required", "structured_output_parse_failed");
    private static final String GENERIC_RETRY_INSTRUCTION = "JSON의 모든 필드, 항목 수, 글자 수와 evidenceRefs 조건을 다시 확인하세요.";
    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final ReportFeedbackRetrievalQueryPlanner queryPlanner;
    private final ReportFeedbackPromptBuilder promptBuilder;
    private final RagProperties ragProperties;
    public ReportFeedbackGenerationService(ChatClient chatClient, VectorStore vectorStore, ReportFeedbackRetrievalQueryPlanner queryPlanner, ReportFeedbackPromptBuilder promptBuilder, RagProperties ragProperties) { this.chatClient = Objects.requireNonNull(chatClient); this.vectorStore = Objects.requireNonNull(vectorStore); this.queryPlanner = Objects.requireNonNull(queryPlanner); this.promptBuilder = Objects.requireNonNull(promptBuilder); this.ragProperties = Objects.requireNonNull(ragProperties); }
    public ReportFeedbackGenerationResult generate(ReportFeedbackContext context) {
        Objects.requireNonNull(context, "context"); validateContext(context);
        var documents = retrieveDocuments(context);
        var evidence = new ArrayList<ReportFeedbackEvidence>();
        for (var doc : documents) { var title = doc.getMetadata().get("documentTitle"); evidence.add(new ReportFeedbackEvidence(doc.getId(), title == null ? doc.getId() : title.toString(), doc.getText() == null ? "" : doc.getText())); }
        var content = requestValidatedContent(promptBuilder.build(context, evidence), context, evidence);
        return new ReportFeedbackGenerationResult(content, citations(content, context, documents), distinct(context.getWarnings()), new RagModelInfo(ragProperties.getEmbedding().getModel(), ragProperties.getChat().getModel()));
    }
    private void validateContext(ReportFeedbackContext context) {
        Object count = context.getReport().getStatistics().get("recordCount");
        if (context.getSchemaVersion() != ReportFeedbackContext.REPORT_FEEDBACK_CONTEXT_SCHEMA_VERSION || blank(context.getReport().getFarmName()) || blank(context.getReport().getCropName()) || context.getRecords().isEmpty() || context.getRecords().stream().anyMatch(it -> it.getWorkType() != context.getWorkType()) || !(count instanceof Number number) || number.intValue() <= 0) throw new ReportFeedbackGenerationFailure(ReportFeedbackFailureCode.INVALID_CONTEXT);
    }
    private List<Document> retrieveDocuments(ReportFeedbackContext context) {
        String crop = context.getReport().getCropName().replace("'", "");
        try {
            var retrieved = new ArrayList<Document>();
            for (var query : queryPlanner.plan(context)) retrieved.addAll(vectorStore.similaritySearch(SearchRequest.builder().query(query).topK(ragProperties.getRetrieval().getTopKDefault()).similarityThreshold(ragProperties.getRetrieval().getLowSimilarityThreshold()).filterExpression("sourceType == '" + RagSourceType.TECH_DOCUMENT.name() + "' && cropName in ['" + crop + "', 'GENERAL']").build()));
            var unique = new LinkedHashMap<String, Document>();
            for (var doc : retrieved) if (RagSourceType.TECH_DOCUMENT.name().equals(doc.getMetadata().get("sourceType")) && !blank(doc.getId()) && doc.getText() != null && !blank(doc.getText())) unique.putIfAbsent(doc.getId(), doc);
            return new ArrayList<>(unique.values());
        } catch (RuntimeException exception) { throw new ReportFeedbackGenerationFailure(ReportFeedbackFailureCode.RETRIEVAL_FAILED, exception); }
    }
    private ReportFeedbackContent requestValidatedContent(ReportFeedbackPrompt prompt, ReportFeedbackContext context, List<ReportFeedbackEvidence> evidence) {
        Throwable lastFailure = null; List<String> instructions = Collections.emptyList();
        for (int attempt = 0; attempt < 2; attempt++) {
            var attemptPrompt = instructions.isEmpty() ? prompt : new ReportFeedbackPrompt(prompt.getSystem(), prompt.getUser() + "\n\n직전 응답은 내부 검증을 통과하지 못했습니다. 다음 지시를 모두 반영한 완전한 JSON만 다시 반환하세요:\n" + String.join("\n", instructions.stream().map(it -> "- " + it).toList()));
            ChatClient.CallResponseSpec response;
            try { response = chatClient.prompt().system(attemptPrompt.getSystem()).user(attemptPrompt.getUser()).call(); }
            catch (RuntimeException exception) { throw new ReportFeedbackGenerationFailure(ReportFeedbackFailureCode.CHAT_UNAVAILABLE, exception); }
            ReportFeedbackContent content;
            try { var parsed = response.entity(ReportFeedbackContent.class); if (parsed == null) throw new IllegalStateException("empty structured output"); content = parsed.normalizedParagraphs(); }
            catch (BusinessException exception) { throw new ReportFeedbackGenerationFailure(ReportFeedbackFailureCode.CHAT_UNAVAILABLE, exception); }
            catch (RuntimeException exception) { instructions = retryInstructions(Collections.singletonList("structured_output_parse_failed"), null); lastFailure = new IllegalStateException("structured_output_parse_failed"); continue; }
            var warnings = ReportFeedbackOutputValidator.validate(content, context, evidence);
            if (warnings.isEmpty()) return content;
            var diagnostics = new ArrayList<String>();
            for (String warning : warnings) diagnostics.add(warning.startsWith("unknown_evidence:") ? "unknown_evidence" : SAFE_RETRY_WARNINGS.contains(warning) || SAFE_ITEM_WARNING.matcher(warning).matches() ? warning : "invalid_output");
            var safe = distinct(diagnostics); instructions = retryInstructions(safe, content); lastFailure = new IllegalStateException(String.join(",", safe));
        }
        throw new ReportFeedbackGenerationFailure(ReportFeedbackFailureCode.STRUCTURED_OUTPUT_INVALID, lastFailure);
    }
    private List<String> retryInstructions(List<String> diagnostics, ReportFeedbackContent content) {
        var instructions = new ArrayList<String>(); for (String code : diagnostics) instructions.add(retryInstruction(code, content)); return distinct(instructions);
    }
    private String retryInstruction(String code, ReportFeedbackContent content) {
        return switch (code) {
            case "summary_blank" -> "summary에 이번 재배에서 확인한 핵심을 공백과 문장부호를 포함해 20~65자로 작성하세요.";
            case "summary_text_length" -> lengthRetryInstruction("summary", content == null ? null : content.getSummary(), "이번 재배에서 확인한 핵심을");
            case "comparison_not_available" -> "서버가 제공한 지난 재배 비교가 없으므로 comparisons는 빈 배열로 반환하세요.";
            case "comparison_current_report_ref_required" -> "comparisons[0].evidenceRefs에 현재 리포트 근거를 허용 evidenceRefs에서 선택해 포함하세요.";
            case "comparison_previous_report_ref_required" -> "comparisons[0].evidenceRefs에 지난 재배 리포트 근거를 허용 evidenceRefs에서 선택해 포함하세요.";
            case "structured_output_parse_failed" -> "설명이나 Markdown 없이 JSON Schema의 필드명과 타입을 그대로 따른 완전한 JSON만 반환하세요. summary는 문자열이고 comparisons, strengths, improvements, nextActions는 배열입니다.";
            case "unknown_evidence" -> "모든 evidenceRefs에는 허용 evidenceRefs에 나열된 값만 사용하세요.";
            case "invalid_output" -> GENERIC_RETRY_INSTRUCTION;
            default -> { var match = SAFE_ITEM_WARNING.matcher(code); yield match.matches() ? itemRetryInstruction(match.group(1), match.group(2), content) : GENERIC_RETRY_INSTRUCTION; }
        };
    }
    private String itemRetryInstruction(String section, String violation, ReportFeedbackContent content) {
        String field, role; List<ReportFeedbackContentItem> items;
        switch (section) {
            case "comparison" -> { field = "comparisons"; role = "지난 재배와 달라진 사실을"; items = content == null ? null : content.getComparisons(); }
            case "strength" -> { field = "strengths"; role = "잘한 행동과 도움이 된 이유를"; items = content == null ? null : content.getStrengths(); }
            case "improvement" -> { field = "improvements"; role = "부족한 점, 영향, 보완 방향을"; items = content == null ? null : content.getImprovements(); }
            case "next_action" -> { field = "nextActions"; role = "언제 무엇을 할지 한 가지 행동을"; items = content == null ? null : content.getNextActions(); }
            default -> { return GENERIC_RETRY_INSTRUCTION; }
        }
        return switch (violation) {
            case "count" -> field + " 배열은 정확히 1개로 작성하세요.";
            case "basis_blank" -> field + "[0].basis에 판단 근거를 작성하세요.";
            case "text_blank" -> field + "[0].text에 " + role + " 공백과 문장부호를 포함해 20~65자로 작성하세요.";
            case "text_length" -> lengthRetryInstruction(field + "[0].text", items != null && items.size() == 1 ? items.getFirst().getText() : null, role);
            case "text_paragraph" -> field + "[0].text를 줄바꿈이나 목록 기호 없이 한 문단으로 작성하세요.";
            case "evidence_refs_blank" -> field + "[0].evidenceRefs에 허용 evidenceRefs의 식별자를 하나 이상 포함하세요.";
            default -> GENERIC_RETRY_INSTRUCTION;
        };
    }
    private String lengthRetryInstruction(String path, String text, String role) {
        if (text == null) return path + "에 " + role + " 공백과 문장부호를 포함해 20~65자로 작성하세요.";
        String direction = text.length() > 65 ? "줄이세요" : text.length() < 20 ? "늘리세요" : "다시 작성하세요";
        return path + "는 현재 " + text.length() + "자입니다. " + role + " 유지하면서 공백과 문장부호를 포함해 20~65자로 " + direction + ".";
    }
    private List<Map<String, Object>> citations(ReportFeedbackContent content, ReportFeedbackContext context, List<Document> documents) {
        var byId = new LinkedHashMap<String, Document>(); documents.forEach(it -> byId.put(it.getId(), it));
        var refs = new LinkedHashSet<String>(); content.items().forEach(it -> refs.addAll(it.getItem().getEvidenceRefs()));
        var result = new ArrayList<Map<String, Object>>();
        for (String ref : refs) {
            var row = new LinkedHashMap<String, Object>();
            if (ref.startsWith("record:")) { row.put("id", ref); row.put("sourceType", RagSourceType.FARMING_RECORD.name()); }
            else if (ref.equals("report:" + context.getReport().getId()) || ref.equals("report:" + (context.getPreviousReport() == null ? null : context.getPreviousReport().getId()))) { row.put("id", ref); row.put("sourceType", "FARMING_REPORT"); }
            else if (byId.containsKey(ref)) { row.put("id", ref); row.put("sourceType", RagSourceType.TECH_DOCUMENT.name()); row.put("title", byId.get(ref).getMetadata().get("documentTitle")); }
            else continue;
            result.add(row);
        }
        return result;
    }
    private static List<String> distinct(List<String> values) { var result = new ArrayList<>(new LinkedHashSet<>(values)); return switch (result.size()) { case 0 -> Collections.emptyList(); case 1 -> Collections.singletonList(result.getFirst()); default -> result; }; }
    private static boolean blank(String text) { return text.chars().allMatch(c -> Character.isWhitespace(c) || Character.isSpaceChar(c)); }
}
