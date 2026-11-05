package com.chamchamcham.application.coaching.chat;

import com.chamchamcham.application.coaching.common.RagAuditStatus;
import com.chamchamcham.application.coaching.common.RagProperties;
import com.chamchamcham.application.coaching.common.RagSourceType;
import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.chamchamcham.domain.crop.CropRepository;
import com.chamchamcham.domain.farm.FarmRepository;
import com.chamchamcham.domain.farming.FarmingRecordRepository;
import com.chamchamcham.domain.member.MemberRepository;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
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
import org.springframework.ai.vectorstore.filter.FilterExpressionTextParser;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.Resource;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.*;
import org.junit.jupiter.api.DisplayName;

class CoachingRagServiceTest {
    private final UUID memberId = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    @DisplayName("answer does not keep remote calls inside a transaction")
    void answer_does_not_keep_remote_calls_inside_a_transaction() throws Exception {
        var method = CoachingRagService.class.getMethod("answer", CoachingRagCommand.class);
        var transaction = new AnnotationTransactionAttributeSource().getTransactionAttribute(method, CoachingRagService.class);
        assertThat(transaction).isNull();
    }

    @Test
    @DisplayName("context lookup uses a read only transaction")
    void context_lookup_uses_a_read_only_transaction() throws Exception {
        var method = CoachingContextProvider.class.getMethod("build", CoachingRagCommand.class);
        var transaction = new AnnotationTransactionAttributeSource().getTransactionAttribute(method, CoachingContextProvider.class);
        assertThat(transaction).isNotNull();
        assertThat(transaction.isReadOnly()).isTrue();
    }

    @Test
    @DisplayName("answer returns insufficient evidence response when no documents are found")
    void answer_returns_insufficient_evidence_response_when_no_documents_are_found() throws Exception {
        var vectorStore = new FakeVectorStore(List.of());
        var service = service(vectorStore, new FakeChatClient(CoachingStructuredResult.insufficientEvidence("unused")));
        var result = service.answer(new CoachingRagCommand(memberId, "  과습 위험이 있을까?  ", null, null, null, null, null, 3));
        assertThat(result.getAnswer()).contains("현재 자료만으로는 판단할 수 없습니다");
        assertThat(result.getAudit().getStatus()).isEqualTo(RagAuditStatus.WARN);
        assertThat(result.getAudit().getWarnings()).containsExactly("no_retrieved_documents");
        assertThat(vectorStore.lastRequest.getQuery()).isEqualTo("과습 위험이 있을까?");
        assertThat(vectorStore.lastRequest.getTopK()).isEqualTo(3);
        assertThat(vectorStore.lastRequest.getFilterExpression()).isEqualTo(new FilterExpressionTextParser().parse("sourceType == 'TECH_DOCUMENT'"));
    }

    @Test
    @DisplayName("answer returns validated structured result from fixed retrieved documents")
    void answer_returns_validated_structured_result_from_fixed_retrieved_documents() throws Exception {
        var document = document("doc-1");
        var vectorStore = new FakeVectorStore(java.util.Arrays.asList(document));
        var chatClient = new FakeChatClient(structuredResult("doc-1"));
        var service = service(vectorStore, chatClient);
        var result = service.answer(new CoachingRagCommand(memberId, "과습 위험이 있을까?", null, null, null, null, null, 2));
        assertThat(result.getAudit().getStatus()).isEqualTo(RagAuditStatus.PASS);
        assertThat(result.getAudit().getCitations()).containsExactly("doc-1");
        assertThat(result.getResult().getSummary()).isEqualTo("배수 상태를 확인하세요.");
        assertThat(vectorStore.searchCalls).isEqualTo(1);
        assertThat(chatClient.requestSpec.advisorCount).isEqualTo(1);
    }

    @Test
    @DisplayName("answer preserves business exception from chat path")
    void answer_preserves_business_exception_from_chat_path() throws Exception {
        var service = service(new FakeVectorStore(java.util.Arrays.asList(document("doc-1"))), new FakeChatClient(new BusinessException(ErrorCode.RAG_CHAT_UNAVAILABLE)));
        assertThatThrownBy(() -> service.answer(new CoachingRagCommand(memberId, "과습 위험?", null, null, null, null, null, null))).isInstanceOfSatisfying(BusinessException.class, it -> assertThat(it.getErrorCode()).isEqualTo(ErrorCode.RAG_CHAT_UNAVAILABLE));
    }

    @Test
    @DisplayName("answer maps generic structured output failure")
    void answer_maps_generic_structured_output_failure() throws Exception {
        var service = service(new FakeVectorStore(java.util.Arrays.asList(document("doc-1"))), new FakeChatClient(new IllegalStateException("parse failed")));
        assertThatThrownBy(() -> service.answer(new CoachingRagCommand(memberId, "과습 위험?", null, null, null, null, null, null))).isInstanceOfSatisfying(BusinessException.class, it -> assertThat(it.getErrorCode()).isEqualTo(ErrorCode.RAG_STRUCTURED_OUTPUT_INVALID));
    }

    @Test
    @DisplayName("answer rejects topK above configured maximum")
    void answer_rejects_topK_above_configured_maximum() throws Exception {
        var service = service(new FakeVectorStore(List.of()), new FakeChatClient(CoachingStructuredResult.insufficientEvidence("unused")));
        assertThatThrownBy(() -> service.answer(new CoachingRagCommand(memberId, "과습 위험?", null, null, null, null, null, 21))).isInstanceOfSatisfying(BusinessException.class, it -> assertThat(it.getErrorCode()).isEqualTo(ErrorCode.RAG_INVALID_REQUEST));
    }

    @Test
    @DisplayName("answer rejects reversed period")
    void answer_rejects_reversed_period() throws Exception {
        var service = service(new FakeVectorStore(List.of()), new FakeChatClient(CoachingStructuredResult.insufficientEvidence("unused")));
        assertThatThrownBy(() -> {
        service.answer(new CoachingRagCommand(memberId, "과습 위험?", null, null, null, LocalDate.parse("2026-07-02"), LocalDate.parse("2026-07-01"), null));
        }).isInstanceOfSatisfying(BusinessException.class, it -> assertThat(it.getErrorCode()).isEqualTo(ErrorCode.RAG_INVALID_REQUEST));
    }
    @Test
    void answerPreservesLfInFinalUserPrompt() {
        assertFinalUserPrompt("first\nsecond");
    }

    @Test
    void answerNormalizesCrLfInFinalUserPrompt() {
        assertFinalUserPrompt("first\r\nsecond");
    }

    @Test
    void answerNormalizesCrInFinalUserPrompt() {
        assertFinalUserPrompt("first\rsecond");
    }

    private void assertFinalUserPrompt(String question) {
        var chatClient = new FakeChatClient(structuredResult("doc-1"));
        var service = service(new FakeVectorStore(List.of(document("doc-1"))), chatClient);
        service.answer(new CoachingRagCommand(memberId, question, null, null, null, null, null, null));
        assertThat(chatClient.requestSpec.userText).isEqualTo(
            "            질문:\n            first\nsecond\n\n            사용자 재배 context:");
    }

    private CoachingRagService service(FakeVectorStore vectorStore, ChatClient chatClient) {
        return new CoachingRagService(chatClient, vectorStore, new FakeContextProvider(), new CoachingRetrievalFilterBuilder(), new CoachingStructuredOutputValidator(), new RagProperties(new RagProperties.Embedding(), new RagProperties.Chat(), new RagProperties.Retrieval(), new RagProperties.Execution()));
    }
    private CoachingStructuredResult structuredResult(String citationId) {
        return new CoachingStructuredResult("배수 상태를 확인하세요.", CoachingRiskLevel.LOW, 0.8, java.util.Arrays.asList(new CoachingObservation("관수", "배수 상태 확인", java.util.Arrays.asList(citationId))), "과습 위험은 낮습니다.", java.util.Arrays.asList(new CoachingRecommendation(CoachingPriority.MEDIUM, "배수 상태 확인", "관수 기록 기반", null, java.util.Arrays.asList(citationId))), java.util.Arrays.asList(new CoachingNextAction(CoachingActionDue.TODAY, "토양 상태 확인", java.util.Arrays.asList(citationId))), List.of(), java.util.Arrays.asList(new CoachingCitationRef(citationId, "농업기술길잡이", RagSourceType.TECH_DOCUMENT, null, null, null)), new CoachingRecordQuality(CoachingRecordQualityScore.UNKNOWN, List.of(), ""), List.of());
    }
    private Document document(String id) {
        return Document.builder().id(id).text("관수 후 토양 배수 상태를 확인한다.").metadata("sourceType", RagSourceType.TECH_DOCUMENT.name()).build();
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

    private static final class FakeContextProvider extends CoachingContextProvider {
        private FakeContextProvider() { super(mock(MemberRepository.class), mock(FarmRepository.class), mock(CropRepository.class), mock(FarmingRecordRepository.class)); }
        @Override public CoachingContext build(CoachingRagCommand command) { return new CoachingContext("사용자 재배 context:"); }
    }

}
