package com.chamchamcham.application.coaching.recordfeedback.generation;

import com.chamchamcham.domain.coaching.recordfeedback.RecordFeedbackActionCategory;
import com.chamchamcham.domain.coaching.recordfeedback.RecordFeedbackActionDue;
import com.chamchamcham.application.coaching.common.RagProperties;
import com.chamchamcham.application.coaching.common.RagSourceType;
import com.chamchamcham.application.coaching.recordfeedback.RecordFeedbackFailureCode;
import com.chamchamcham.application.coaching.recordfeedback.RecordFeedbackGenerationFailure;
import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.ResponseEntity;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.StructuredOutputConverter;
import org.springframework.ai.document.Document;
import org.springframework.ai.template.TemplateRenderer;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.Resource;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import java.nio.charset.Charset;
import java.util.function.Consumer;
import java.util.*;
import org.junit.jupiter.api.DisplayName;
import com.chamchamcham.application.coaching.recordfeedback.generation.RecordFeedbackContextKt;
import com.chamchamcham.application.coaching.reportfeedback.generation.ReportFeedbackContextKt;

class RecordFeedbackGenerationServiceTest {
    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();
    private final RecordFeedbackContext context = readFixture("today-record-feedback-watering.json");

    @Test
    @DisplayName("generates only from context and official document evidence")
    void generates_only_from_context_and_official_document_evidence() throws Exception {
        var chatClient = new FakeChatClient(validResult("doc-1", RecordFeedbackContextKt.recordCitationId(context)));
        var vectorStore = new FakeVectorStore(java.util.Arrays.asList(officialDocument("doc-1", "약용작물 관수 후 토양 수분과 배수 상태를 확인한다.")));
        var generationResult = service(vectorStore, chatClient).generate(context, 2);
        assertThat(generationResult.getContent().getNextActions()).hasSize(2);
        assertThat(generationResult.getCitations().stream().map(it -> it.get("id")).toList()).contains("doc-1", RecordFeedbackContextKt.recordCitationId(context));
        assertThat(chatClient.requestSpec.advisorUseCount).isZero();
        assertThat(chatClient.requestSpec.userText).contains(RecordFeedbackContextKt.recordCitationId(context));
        assertThat(vectorStore.requests).allSatisfy(it -> {
        assertThat(it.getFilterExpression().toString()).contains("sourceType").contains("TECH_DOCUMENT").contains("cropName").contains("참당귀").contains("GENERAL");
        });
    }

    @Test
    @DisplayName("deduplicates context warnings in generation audit")
    void deduplicates_context_warnings_in_generation_audit() throws Exception {
        var result = serviceWithDocuments(java.util.Arrays.asList(officialDocument("doc-1", "약용작물 관수 후 토양 수분과 배수 상태를 확인한다.")), new FakeChatClient(validResult("doc-1", RecordFeedbackContextKt.recordCitationId(context)))).generate(context.copy(context.getSchemaVersion(), context.getMember(), context.getFarm(), context.getCrop(), context.getRecord(), context.getWeather(), java.util.Arrays.asList("weather_location_unavailable", "weather_location_unavailable")), null);
        assertThat(result.getAuditWarnings()).containsExactly("weather_location_unavailable");
    }

    @Test
    @DisplayName("rejects an unsupported context schema before retrieval")
    void rejects_an_unsupported_context_schema_before_retrieval() throws Exception {
        assertThatThrownBy(() -> {
        serviceWithDocuments(java.util.Arrays.asList(officialDocument("doc-1", "약용작물 관수 후 토양 수분과 배수 상태를 확인한다.")), new FakeChatClient(validResult("doc-1", RecordFeedbackContextKt.recordCitationId(context)))).generate(context.copy("record-feedback-context.v1", context.getMember(), context.getFarm(), context.getCrop(), context.getRecord(), context.getWeather(), context.getWarnings()), null);
        }).isInstanceOfSatisfying(RecordFeedbackGenerationFailure.class, it -> assertThat(it.getCode()).isEqualTo(RecordFeedbackFailureCode.INVALID_CONTEXT));
    }

    @Test
    @DisplayName("rejects a context without the farmer memo before retrieval")
    void rejects_a_context_without_the_farmer_memo_before_retrieval() throws Exception {
        assertThatThrownBy(() -> {
        serviceWithDocuments(java.util.Arrays.asList(officialDocument("doc-1", "약용작물 관수 후 토양 수분과 배수 상태를 확인한다.")), new FakeChatClient(validResult("doc-1", RecordFeedbackContextKt.recordCitationId(context)))).generate(context.copy(context.getSchemaVersion(), context.getMember(), context.getFarm(), context.getCrop(), context.getRecord().copy(context.getRecord().getRecordId(), context.getRecord().getSourceRevision(), context.getRecord().getWorkedAt(), context.getRecord().getWorkType(), context.getRecord().getDetail(), context.getRecord().getRecordedWeatherCondition(), context.getRecord().getRecordedTemperatureC(), " ", context.getRecord().getPhotoCount()), context.getWeather(), context.getWarnings()), null);
        }).isInstanceOfSatisfying(RecordFeedbackGenerationFailure.class, it -> assertThat(it.getCode()).isEqualTo(RecordFeedbackFailureCode.INVALID_CONTEXT));
    }

    @Test
    @DisplayName("retries once when parsed output violates product validation")
    void retries_once_when_parsed_output_violates_product_validation() throws Exception {
        var invalidResult = validResult("doc-1", RecordFeedbackContextKt.recordCitationId(context)).copy(validResult("doc-1", RecordFeedbackContextKt.recordCitationId(context)).getGoodPoint(), java.util.Arrays.asList(validAction(RecordFeedbackActionDue.NEXT_CHECK, RecordFeedbackActionCategory.IRRIGATION, "토양 상태", "짧음", java.util.Arrays.asList("doc-1")), validAction(RecordFeedbackActionDue.NEXT_CHECK, RecordFeedbackActionCategory.IRRIGATION, "토양 상태", "다음 점검 때 흙 상태를 다시 살펴보세요.", java.util.Arrays.asList(RecordFeedbackContextKt.recordCitationId(context), "doc-1"))));
        var chatClient = new FakeChatClient(invalidResult, validResult("doc-1", RecordFeedbackContextKt.recordCitationId(context)));
        serviceWithDocuments(java.util.Arrays.asList(officialDocument("doc-1", "약용작물 관수 후 토양 수분과 배수 상태를 확인한다.")), chatClient).generate(context, null);
        assertThat(chatClient.getAttempts()).isEqualTo(2);
    }

    @Test
    @DisplayName("includes product validation codes in the retry prompt")
    void includes_product_validation_codes_in_the_retry_prompt() throws Exception {
        var invalidResult = validResult("doc-1", RecordFeedbackContextKt.recordCitationId(context)).copy(validResult("doc-1", RecordFeedbackContextKt.recordCitationId(context)).getGoodPoint(), java.util.Arrays.asList(validAction(RecordFeedbackActionDue.NEXT_CHECK, RecordFeedbackActionCategory.IRRIGATION, "토양 상태", "짧음", java.util.Arrays.asList("doc-1")), validAction(RecordFeedbackActionDue.NEXT_CHECK, RecordFeedbackActionCategory.IRRIGATION, "토양 상태", "다음 점검 때 흙 상태를 다시 살펴보세요.", java.util.Arrays.asList(RecordFeedbackContextKt.recordCitationId(context), "doc-1"))));
        var chatClient = new FakeChatClient(invalidResult, validResult("doc-1", RecordFeedbackContextKt.recordCitationId(context)));
        serviceWithDocuments(java.util.Arrays.asList(officialDocument("doc-1", "약용작물 관수 후 토양 수분과 배수 상태를 확인한다.")), chatClient).generate(context, null);
        assertThat(chatClient.requestSpec.userTexts).hasSize(2);
        assertThat(chatClient.requestSpec.userTexts.getFirst()).doesNotContain("next_action_0_text_length");
        assertThat(chatClient.requestSpec.userTexts.getLast()).contains("next_action_0_text_length");
    }

    @Test
    @DisplayName("retries an English violation with a fixed diagnostic code only")
    void retries_an_English_violation_with_a_fixed_diagnostic_code_only() throws Exception {
        var generatedText = "DRIP으로 물을 준 점은 잘했어요.";
        var invalidResult = validResult("doc-1", RecordFeedbackContextKt.recordCitationId(context)).copy(validItem("점적관수", generatedText, java.util.Arrays.asList(RecordFeedbackContextKt.recordCitationId(context))), validResult("doc-1", RecordFeedbackContextKt.recordCitationId(context)).getNextActions());
        var chatClient = new FakeChatClient(invalidResult, validResult("doc-1", RecordFeedbackContextKt.recordCitationId(context)));
        serviceWithDocuments(java.util.Arrays.asList(officialDocument("doc-1", "약용작물 관수 후 토양 수분과 배수 상태를 확인한다.")), chatClient).generate(context, null);
        assertThat(chatClient.getAttempts()).isEqualTo(2);
        assertThat(chatClient.requestSpec.userTexts.getLast()).contains("good_point_text_english").doesNotContain(generatedText, "DRIP으로");
    }

    @Test
    @DisplayName("two English violations fail as structured output invalid")
    void two_English_violations_fail_as_structured_output_invalid() throws Exception {
        var invalidResult = validResult("doc-1", RecordFeedbackContextKt.recordCitationId(context)).copy(validItem("점적관수", "DRIP으로 물을 준 점은 잘했어요.", java.util.Arrays.asList(RecordFeedbackContextKt.recordCitationId(context))), validResult("doc-1", RecordFeedbackContextKt.recordCitationId(context)).getNextActions());
        var chatClient = new FakeChatClient(invalidResult, invalidResult);
        assertThatThrownBy(() -> {
        serviceWithDocuments(java.util.Arrays.asList(officialDocument("doc-1", "약용작물 관수 후 토양 수분과 배수 상태를 확인한다.")), chatClient).generate(context, null);
        }).isInstanceOfSatisfying(RecordFeedbackGenerationFailure.class, it -> assertThat(it.getCode()).isEqualTo(RecordFeedbackFailureCode.STRUCTURED_OUTPUT_INVALID));
        assertThat(chatClient.getAttempts()).isEqualTo(2);
    }

    @Test
    @DisplayName("retries once when structured output parsing fails")
    void retries_once_when_structured_output_parsing_fails() throws Exception {
        var chatClient = new FakeChatClient(new RuntimeException("structured output parse failed"), validResult("doc-1", RecordFeedbackContextKt.recordCitationId(context)));
        serviceWithDocuments(java.util.Arrays.asList(officialDocument("doc-1", "약용작물 관수 후 토양 수분과 배수 상태를 확인한다.")), chatClient).generate(context, null);
        assertThat(chatClient.getAttempts()).isEqualTo(2);
    }

    @Test
    @DisplayName("fails with structured output invalid after the single retry is exhausted")
    void fails_with_structured_output_invalid_after_the_single_retry_is_exhausted() throws Exception {
        var invalidResult = validResult("doc-1", RecordFeedbackContextKt.recordCitationId(context)).copy(validItem("점적관수", "짧음", java.util.Arrays.asList("doc-1")), validResult("doc-1", RecordFeedbackContextKt.recordCitationId(context)).getNextActions());
        var chatClient = new FakeChatClient(invalidResult, invalidResult);
        assertThatThrownBy(() -> {
        serviceWithDocuments(java.util.Arrays.asList(officialDocument("doc-1", "약용작물 관수 후 토양 수분과 배수 상태를 확인한다.")), chatClient).generate(context, null);
        }).isInstanceOfSatisfying(RecordFeedbackGenerationFailure.class, it -> assertThat(it.getCode()).isEqualTo(RecordFeedbackFailureCode.STRUCTURED_OUTPUT_INVALID));
        assertThat(chatClient.getAttempts()).isEqualTo(2);
    }

    @Test
    @DisplayName("fails with insufficient evidence when no official document is retrieved")
    void fails_with_insufficient_evidence_when_no_official_document_is_retrieved() throws Exception {
        assertThatThrownBy(() -> serviceWithDocuments(List.of(), new FakeChatClient(validResult("doc-1", RecordFeedbackContextKt.recordCitationId(context)))).generate(context, null)).isInstanceOfSatisfying(RecordFeedbackGenerationFailure.class, it -> assertThat(it.getCode()).isEqualTo(RecordFeedbackFailureCode.INSUFFICIENT_EVIDENCE));
    }

    @Test
    @DisplayName("context only retrieved documents are insufficient evidence")
    void context_only_retrieved_documents_are_insufficient_evidence() throws Exception {
        assertThatThrownBy(() -> serviceWithDocuments(java.util.Arrays.asList(contextOnlyDocument("record-doc")), new FakeChatClient(validResult("doc-1", RecordFeedbackContextKt.recordCitationId(context)))).generate(context, null)).isInstanceOfSatisfying(RecordFeedbackGenerationFailure.class, it -> assertThat(it.getCode()).isEqualTo(RecordFeedbackFailureCode.INSUFFICIENT_EVIDENCE));
    }

    @Test
    @DisplayName("official document with blank id is insufficient evidence without llm call")
    void official_document_with_blank_id_is_insufficient_evidence_without_llm_call() throws Exception {
        var chatClient = new FakeChatClient(validResult("doc-1", RecordFeedbackContextKt.recordCitationId(context)));
        assertThatThrownBy(() -> {
        serviceWithDocuments(java.util.Arrays.asList(officialDocument(" ", "약용작물 관수 후 토양 수분과 배수 상태를 확인한다.")), chatClient).generate(context, null);
        }).isInstanceOfSatisfying(RecordFeedbackGenerationFailure.class, it -> assertThat(it.getCode()).isEqualTo(RecordFeedbackFailureCode.INSUFFICIENT_EVIDENCE));
        assertThat(chatClient.getAttempts()).isZero();
    }

    @Test
    @DisplayName("official document with blank text is insufficient evidence without llm call")
    void official_document_with_blank_text_is_insufficient_evidence_without_llm_call() throws Exception {
        var chatClient = new FakeChatClient(validResult("doc-1", RecordFeedbackContextKt.recordCitationId(context)));
        assertThatThrownBy(() -> {
        serviceWithDocuments(java.util.Arrays.asList(officialDocument("doc-1", " ")), chatClient).generate(context, null);
        }).isInstanceOfSatisfying(RecordFeedbackGenerationFailure.class, it -> assertThat(it.getCode()).isEqualTo(RecordFeedbackFailureCode.INSUFFICIENT_EVIDENCE));
        assertThat(chatClient.getAttempts()).isZero();
    }

    @Test
    @DisplayName("returns server citation metadata after product output validation")
    void returns_server_citation_metadata_after_product_output_validation() throws Exception {
        var generated = serviceWithDocuments(java.util.Arrays.asList(officialDocument("doc-1", "약용작물 관수 후 토양 수분과 배수 상태를 확인한다.")), new FakeChatClient(validResult("doc-1", RecordFeedbackContextKt.recordCitationId(context)))).generate(context, null);
        var documentCitation = generated.getCitations().stream().filter(citation -> Objects.equals(citation.get("id"), "doc-1")).findFirst().orElseThrow();
        assertThat(documentCitation.get("title")).isEqualTo("농업기술길잡이 007 약용작물");
        assertThat(documentCitation.get("page")).isEqualTo(123);
        assertThat(documentCitation.get("source")).isEqualTo("guide.pdf");
        assertThat(documentCitation.get("sourceType")).isEqualTo(RagSourceType.TECH_DOCUMENT.name());
        var recordCitation = generated.getCitations().stream().filter(citation -> Objects.equals(citation.get("id"), RecordFeedbackContextKt.recordCitationId(context))).findFirst().orElseThrow();
        assertThat(recordCitation.get("title")).isEqualTo("대상 영농기록");
        assertThat(recordCitation.get("sourceType")).isEqualTo(RagSourceType.FARMING_RECORD.name());
    }

    @Test
    @DisplayName("maps vector store runtime failures to retrieval failed")
    void maps_vector_store_runtime_failures_to_retrieval_failed() throws Exception {
        assertThatThrownBy(() -> {
        service(new FakeVectorStore(new IllegalStateException("vector unavailable")), new FakeChatClient(validResult("doc-1", RecordFeedbackContextKt.recordCitationId(context)))).generate(context, null);
        }).isInstanceOfSatisfying(RecordFeedbackGenerationFailure.class, it -> assertThat(it.getCode()).isEqualTo(RecordFeedbackFailureCode.RETRIEVAL_FAILED));
    }

    @Test
    @DisplayName("maps chat runtime failures to chat unavailable without structured output retry")
    void maps_chat_runtime_failures_to_chat_unavailable_without_structured_output_retry() throws Exception {
        var chatClient = new FakeChatClient(validResult("doc-1", RecordFeedbackContextKt.recordCitationId(context))).withCallFailure(new IllegalStateException("chat unavailable"));
        assertThatThrownBy(() -> {
        serviceWithDocuments(java.util.Arrays.asList(officialDocument("doc-1", "약용작물 관수 후 토양 수분과 배수 상태를 확인한다.")), chatClient).generate(context, null);
        }).isInstanceOfSatisfying(RecordFeedbackGenerationFailure.class, it -> assertThat(it.getCode()).isEqualTo(RecordFeedbackFailureCode.CHAT_UNAVAILABLE));
        assertThat(chatClient.getAttempts()).isZero();
    }

    @Test
    @DisplayName("maps entity time chat failures to chat unavailable without structured output retry")
    void maps_entity_time_chat_failures_to_chat_unavailable_without_structured_output_retry() throws Exception {
        var chatClient = new FakeChatClient(new BusinessException(ErrorCode.RAG_CHAT_UNAVAILABLE));
        assertThatThrownBy(() -> {
        serviceWithDocuments(java.util.Arrays.asList(officialDocument("doc-1", "약용작물 관수 후 토양 수분과 배수 상태를 확인한다.")), chatClient).generate(context, null);
        }).isInstanceOfSatisfying(RecordFeedbackGenerationFailure.class, it -> assertThat(it.getCode()).isEqualTo(RecordFeedbackFailureCode.CHAT_UNAVAILABLE));
        assertThat(chatClient.getAttempts()).isEqualTo(1);
    }
    private RecordFeedbackGenerationService serviceWithDocuments(List<Document> documents, ChatClient chatClient) {
        return service(new FakeVectorStore(documents), chatClient);
    }
    private RecordFeedbackGenerationService service(FakeVectorStore vectorStore, ChatClient chatClient) {
        return new RecordFeedbackGenerationService(chatClient, vectorStore, new RecordFeedbackRetrievalQueryPlanner(), new RecordFeedbackPromptBuilder(), new RagProperties(new RagProperties.Embedding("test-embedding"), new RagProperties.Chat("test-chat"), new RagProperties.Retrieval(), new RagProperties.Execution()));
    }
    private RecordFeedbackContext readFixture(String name) {
        var resource = Objects.requireNonNull(getClass().getClassLoader().getResource("coaching/rag/" + name), "Missing fixture: " + name);
        try (var input = resource.openStream()) { return objectMapper.readValue(input, RecordFeedbackContext.class); }
        catch (java.io.IOException exception) { throw new java.io.UncheckedIOException(exception); }
    }
    private Document officialDocument(String id, String text) {
        Map<String, Object> metadata = map(new AbstractMap.SimpleEntry<>("sourceType", RagSourceType.TECH_DOCUMENT.name()), new AbstractMap.SimpleEntry<>("documentTitle", "농업기술길잡이 007 약용작물"), new AbstractMap.SimpleEntry<>("page", 123), new AbstractMap.SimpleEntry<>("pdfPath", "/data/rag/medicinal-plants/raw/pdfs/guide.pdf"));
        if (id.isBlank()) return new Document("placeholder-id", text, metadata) { @Override public String getId() { return " "; } };
        return Document.builder().id(id).text(text).metadata(metadata).build();
    }
    private Document contextOnlyDocument(String id) {
        return Document.builder().id(id).text("대상 영농기록 context").metadata("sourceType", RagSourceType.FARMING_RECORD.name()).metadata("documentTitle", "대상 영농기록").build();
    }
    private RecordFeedbackContent validResult(String documentCitationId, String recordCitationId) {
        return new RecordFeedbackContent(validItem("점적관수", "흙 상태를 살핀 점은 잘했어요.", java.util.Arrays.asList(recordCitationId)), java.util.Arrays.asList(validAction(RecordFeedbackActionDue.THIS_WEEK, RecordFeedbackActionCategory.WEATHER, "비 예보", "비 오기 전 물길을 정리하세요.", java.util.Arrays.asList("weather:2026-07-04")), validAction(RecordFeedbackActionDue.NEXT_CHECK, RecordFeedbackActionCategory.IRRIGATION, "토양 상태", "다음에 흙 상태를 살펴보세요.", java.util.Arrays.asList(recordCitationId, documentCitationId))));
    }
    private RecordFeedbackGoodPoint validItem(String basis, String text, List<String> refs) {
        return new RecordFeedbackGoodPoint(basis, text, refs);
    }
    private RecordFeedbackAction validAction(RecordFeedbackActionDue due, RecordFeedbackActionCategory category, String basis, String text, List<String> refs) {
        return new RecordFeedbackAction(due, category, basis, text, refs);
    }
    @SafeVarargs
    private static <K> Map<K, Object> map(Map.Entry<K, ?>... entries) {
        Map<K, Object> result = new LinkedHashMap<>();
        for (var entry : entries) result.put(entry.getKey(), entry.getValue());
        return result;
    }

    private static final class FakeVectorStore implements VectorStore {
        private final List<Document> documents;
        private final RuntimeException exception;
        private final List<SearchRequest> requests = new ArrayList<>();
        private SearchRequest lastRequest;
        private int searchCalls;
        private FakeVectorStore() { this(List.of(), null); }
        private FakeVectorStore(List<Document> documents) { this(documents, null); }
        private FakeVectorStore(RuntimeException exception) { this(List.of(), exception); }
        private FakeVectorStore(List<Document> documents, RuntimeException exception) { this.documents = documents; this.exception = exception; }
        @Override public void add(List<Document> documents) {}
        @Override public void delete(List<String> ids) {}
        @Override public void delete(Filter.Expression expression) {}
        @Override public List<Document> similaritySearch(SearchRequest request) {
            requests.add(request); lastRequest = request; searchCalls++;
            if (exception != null) throw exception;
            return documents;
        }
    }
    private static final class FakeChatClient implements ChatClient {
        private final Object[] responses;
        private RuntimeException callException;
        private int attempts;
        private final FakeRequestSpec requestSpec;
        private FakeChatClient(Object... responses) { this.responses = responses; this.requestSpec = new FakeRequestSpec(this); }
        private FakeChatClient withCallFailure(RuntimeException failure) { callException = failure; return this; }
        int getAttempts() { return attempts; }
        Object nextResponse() {
            var result = responses[Math.min(attempts, responses.length - 1)];
            attempts++;
            if (result instanceof RuntimeException failure) throw failure;
            return result;
        }
        @Override public ChatClientRequestSpec prompt() { return requestSpec.spec; }
        @Override public ChatClientRequestSpec prompt(String content) { return requestSpec.spec; }
        @Override public ChatClientRequestSpec prompt(Prompt prompt) { return requestSpec.spec; }
        @Override public Builder mutate() { throw new UnsupportedOperationException("mutate is not used"); }
    }
    private static final class FakeRequestSpec {
        private String systemText = "";
        private String userText = "";
        private final List<String> userTexts = new ArrayList<>();
        private int advisorUseCount;
        private int advisorCount;
        private final ChatClient.ChatClientRequestSpec spec;
        private FakeRequestSpec(FakeChatClient client) {
            var response = org.mockito.Mockito.mock(ChatClient.CallResponseSpec.class, invocation -> {
                if (invocation.getMethod().getName().equals("entity") && invocation.getArgument(0) instanceof Class<?> type) return type.cast(client.nextResponse());
                throw new UnsupportedOperationException("response method is not used");
            });
            spec = org.mockito.Mockito.mock(ChatClient.ChatClientRequestSpec.class, invocation -> {
                switch (invocation.getMethod().getName()) {
                    case "system" -> { if (invocation.getArgument(0) instanceof String text) systemText = text; }
                    case "user" -> { if (invocation.getArgument(0) instanceof String text) { userText = text; userTexts.add(text); } }
                    case "advisors" -> { advisorUseCount++; advisorCount += invocation.getArguments().length; }
                    case "call" -> { if (client.callException != null) throw client.callException; return response; }
                    default -> { }
                }
                return invocation.getMock();
            });
        }
    }

}
