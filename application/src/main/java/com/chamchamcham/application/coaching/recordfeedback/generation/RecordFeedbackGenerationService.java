package com.chamchamcham.application.coaching.recordfeedback.generation;

import com.chamchamcham.application.coaching.common.*;
import com.chamchamcham.application.coaching.recordfeedback.*;
import com.chamchamcham.application.exception.business.BusinessException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class RecordFeedbackGenerationService {
    public static final String GENERAL_CROP_NAME = "GENERAL";
    public static final int MAX_STRUCTURED_OUTPUT_ATTEMPTS = 2;
    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final RecordFeedbackRetrievalQueryPlanner queryPlanner;
    private final RecordFeedbackPromptBuilder promptBuilder;
    private final RagProperties ragProperties;
    public RecordFeedbackGenerationService(ChatClient chatClient, VectorStore vectorStore, RecordFeedbackRetrievalQueryPlanner queryPlanner, RecordFeedbackPromptBuilder promptBuilder, RagProperties ragProperties) { this.chatClient = Objects.requireNonNull(chatClient); this.vectorStore = Objects.requireNonNull(vectorStore); this.queryPlanner = Objects.requireNonNull(queryPlanner); this.promptBuilder = Objects.requireNonNull(promptBuilder); this.ragProperties = Objects.requireNonNull(ragProperties); }
    public RecordFeedbackGenerationResult generate(RecordFeedbackContext context, Integer topK) {
        Objects.requireNonNull(context, "context");
        var warnings = requireValidContext(context);
        int count = topK == null ? ragProperties.getRetrieval().getTopKDefault() : topK;
        if (count < 1 || count > ragProperties.getRetrieval().getTopKMax()) throw new RecordFeedbackGenerationFailure(RecordFeedbackFailureCode.INVALID_GENERATION_REQUEST);
        var queries = queryPlanner.plan(context);
        var documents = retrieveDocuments(queries, count, trim(context.getCrop().getName()));
        documents.removeIf(it -> !RagSourceType.TECH_DOCUMENT.name().equals(it.getMetadata().get("sourceType")) || blank(it.getId()) || it.getText() == null || blank(it.getText()));
        if (documents.isEmpty()) throw new RecordFeedbackGenerationFailure(RecordFeedbackFailureCode.INSUFFICIENT_EVIDENCE);
        var evidence = new ArrayList<RecordFeedbackEvidence>();
        for (var it : documents) evidence.add(new RecordFeedbackEvidence(it.getId(), documentTitle(it) == null ? it.getId() : documentTitle(it), page(it), it.getText() == null ? "" : it.getText()));
        var content = callForValidatedContent(promptBuilder.build(context, queries, evidence), context, evidence);
        return new RecordFeedbackGenerationResult(content, buildCitationMaps(content, context, documents), warnings, new RagModelInfo(ragProperties.getEmbedding().getModel(), ragProperties.getChat().getModel()));
    }
    private RecordFeedbackContent callForValidatedContent(RecordFeedbackPrompt prompt, RecordFeedbackContext context, List<RecordFeedbackEvidence> evidence) {
        Throwable lastFailure = null; List<String> retryWarnings = Collections.emptyList();
        for (int attempt = 0; attempt < 2; attempt++) {
            RecordFeedbackContent content;
            var attemptPrompt = retryWarnings.isEmpty() ? prompt : new RecordFeedbackPrompt(prompt.getSystem(), prompt.getUser() + "\n\n직전 응답은 내부 검증을 통과하지 못했습니다. 다음 오류를 모두 고친 완전한 JSON만 다시 반환하세요:\n" + String.join("\n", retryWarnings.stream().map(it -> "- " + it).toList()));
            try { content = requestStructuredContent(attemptPrompt); }
            catch (StructuredOutputFailure failure) { lastFailure = failure; continue; }
            catch (ChatRuntimeFailure failure) { throw new RecordFeedbackGenerationFailure(RecordFeedbackFailureCode.CHAT_UNAVAILABLE, failure.getCause()); }
            var validation = RecordFeedbackOutputValidator.validate(content, context, evidence);
            if (validation.isEmpty()) return content;
            retryWarnings = validation;
            lastFailure = new StructuredOutputFailure("invalid product output: " + String.join(",", validation), null);
        }
        throw new RecordFeedbackGenerationFailure(RecordFeedbackFailureCode.STRUCTURED_OUTPUT_INVALID, lastFailure);
    }
    private RecordFeedbackContent requestStructuredContent(RecordFeedbackPrompt prompt) {
        ChatClient.CallResponseSpec response;
        try { response = chatClient.prompt().system(prompt.getSystem()).user(prompt.getUser()).call(); }
        catch (RuntimeException failure) { throw new ChatRuntimeFailure(failure); }
        try { var content = response.entity(RecordFeedbackContent.class); if (content == null) throw new StructuredOutputFailure("empty structured output", null); return content; }
        catch (StructuredOutputFailure failure) { throw failure; }
        catch (BusinessException failure) { throw new ChatRuntimeFailure(failure); }
        catch (RuntimeException failure) { throw new StructuredOutputFailure("structured output parse failed", failure); }
    }
    private List<String> requireValidContext(RecordFeedbackContext context) {
        if (!RecordFeedbackContext.RECORD_FEEDBACK_CONTEXT_SCHEMA_VERSION.equals(context.getSchemaVersion()) || blank(context.getFarm().getName()) || blank(context.getFarm().getRoadAddress()) || blank(context.getCrop().getName()) || blank(context.getRecord().getMemo()) || context.getRecord().getPhotoCount() < 0) throw new RecordFeedbackGenerationFailure(RecordFeedbackFailureCode.INVALID_CONTEXT);
        var warnings = new ArrayList<>(new LinkedHashSet<>(context.getWarnings()));
        return switch (warnings.size()) { case 0 -> Collections.emptyList(); case 1 -> Collections.singletonList(warnings.getFirst()); default -> warnings; };
    }
    private List<Document> retrieveDocuments(List<RecordFeedbackRetrievalQuery> queries, int count, String crop) {
        String safeCrop = crop.replace("'", "");
        try {
            var retrieved = new ArrayList<Document>();
            for (var query : queries) retrieved.addAll(vectorStore.similaritySearch(SearchRequest.builder().query(query.getQuery()).topK(count).similarityThreshold(ragProperties.getRetrieval().getLowSimilarityThreshold()).filterExpression("sourceType == '" + RagSourceType.TECH_DOCUMENT.name() + "' && cropName in ['" + safeCrop + "', '" + GENERAL_CROP_NAME + "']").build()));
            var unique = new LinkedHashMap<String, Document>();
            for (var doc : retrieved) if (RagSourceType.TECH_DOCUMENT.name().equals(doc.getMetadata().get("sourceType"))) unique.putIfAbsent(doc.getId(), doc);
            return new ArrayList<>(unique.values());
        } catch (RuntimeException failure) { throw new RecordFeedbackGenerationFailure(RecordFeedbackFailureCode.RETRIEVAL_FAILED, failure); }
    }
    private List<Map<String, Object>> buildCitationMaps(RecordFeedbackContent content, RecordFeedbackContext context, List<Document> documents) {
        var byId = new LinkedHashMap<String, Document>(); documents.forEach(it -> byId.put(it.getId(), it));
        var refs = new LinkedHashSet<String>(); refs.addAll(content.getGoodPoint().getEvidenceRefs()); content.getNextActions().forEach(it -> refs.addAll(it.getEvidenceRefs()));
        var result = new ArrayList<Map<String, Object>>();
        for (String ref : refs) {
            if (blank(ref)) continue;
            var row = new LinkedHashMap<String, Object>();
            if (ref.equals(context.recordCitationId())) { row.put("id", ref); row.put("title", "대상 영농기록"); row.put("sourceType", RagSourceType.FARMING_RECORD.name()); }
            else if (ref.startsWith("weather:")) { row.put("id", ref); row.put("title", "날씨 context"); row.put("sourceType", "WEATHER"); }
            else if (byId.containsKey(ref)) { var doc = byId.get(ref); row.put("id", doc.getId()); row.put("title", documentTitle(doc)); row.put("page", page(doc)); row.put("source", sourceFileName(doc)); row.put("sourceType", RagSourceType.TECH_DOCUMENT.name()); }
            else continue;
            result.add(row);
        }
        return result;
    }
    private String documentTitle(Document doc) { Object title = doc.getMetadata().get("documentTitle"); if (title == null) title = doc.getMetadata().get("label"); return title == null ? null : title.toString(); }
    private Integer page(Document doc) { var page = doc.getMetadata().get("page"); if (page == null) return null; try { return Integer.valueOf(page.toString()); } catch (NumberFormatException exception) { return null; } }
    private String sourceFileName(Document doc) { var path = doc.getMetadata().get("pdfPath"); if (path == null) return null; String file = path.toString(); file = file.substring(file.lastIndexOf('/') + 1); file = file.substring(file.lastIndexOf('\\') + 1); return blank(file) ? null : file; }
    private static boolean blank(String text) { return text.chars().allMatch(c -> Character.isWhitespace(c) || Character.isSpaceChar(c)); }
    private static String trim(String text) { int start = 0, end = text.length(); while (start < end && (Character.isWhitespace(text.charAt(start)) || Character.isSpaceChar(text.charAt(start)))) start++; while (end > start && (Character.isWhitespace(text.charAt(end - 1)) || Character.isSpaceChar(text.charAt(end - 1)))) end--; return text.substring(start, end); }
    private static class StructuredOutputFailure extends RuntimeException { StructuredOutputFailure(String message, Throwable cause) { super(message, cause); } }
    private static class ChatRuntimeFailure extends RuntimeException { ChatRuntimeFailure(Throwable cause) { super(cause); } }
}
