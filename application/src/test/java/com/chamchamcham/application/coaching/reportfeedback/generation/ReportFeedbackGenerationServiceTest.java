package com.chamchamcham.application.coaching.reportfeedback.generation;

import com.chamchamcham.application.coaching.common.RagProperties;
import com.chamchamcham.application.coaching.reportfeedback.ReportFeedbackFailureCode;
import com.chamchamcham.application.coaching.reportfeedback.ReportFeedbackGenerationFailure;
import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.chamchamcham.domain.farming.WorkType;
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
import java.nio.charset.Charset;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.*;
import org.junit.jupiter.api.DisplayName;
import com.chamchamcham.application.coaching.recordfeedback.generation.RecordFeedbackContextKt;
import com.chamchamcham.application.coaching.reportfeedback.generation.ReportFeedbackContextKt;

class ReportFeedbackGenerationServiceTest {
    private final UUID recordId = UUID.randomUUID();
    private final UUID reportId = UUID.randomUUID();
    private final UUID previousReportId = UUID.randomUUID();

    @Test
    @DisplayName("evidence validation failure is retried with an actionable instruction")
    void evidence_validation_failure_is_retried_with_an_actionable_instruction() throws Exception {
        var client = new FakeChatClient(validContent().copy(validContent().getSummary(), validContent().getComparisons(), java.util.Arrays.asList(item("흙 상태를 살핀 뒤 물을 주어 필요한 곳부터 관리한 점이 좋았어요.", java.util.Arrays.asList("unknown"))), validContent().getImprovements(), validContent().getNextActions()), validContent());
        service(client, new FakeVectorStore()).generate(context());
        assertThat(client.getAttempts()).isEqualTo(2);
        assertThat(client.requestSpec.userTexts.getLast()).contains("모든 evidenceRefs에는 허용 evidenceRefs에 나열된 값만 사용하세요.").doesNotContain("unknown_evidence");
    }

    @Test
    @DisplayName("section shape failures are retried with an actionable instruction")
    void section_shape_failures_are_retried_with_an_actionable_instruction() throws Exception {
        var invalid = validContent().copy(validContent().getSummary(), validContent().getComparisons(), List.of(), validContent().getImprovements(), validContent().getNextActions());
        var client = new FakeChatClient(invalid, validContent());
        service(client, new FakeVectorStore()).generate(context());
        assertThat(client.getAttempts()).isEqualTo(2);
        assertThat(client.requestSpec.userTexts.getLast()).contains("strengths 배열은 정확히 1개로 작성하세요.").doesNotContain("strength_count");
    }

    @Test
    @DisplayName("blank item fields retry with actionable field instructions")
    void blank_item_fields_retry_with_actionable_field_instructions() throws Exception {
        var client = new FakeChatClient(validContent().copy(validContent().getSummary(), validContent().getComparisons(), java.util.Arrays.asList(new ReportFeedbackContentItem("", "", List.of())), validContent().getImprovements(), validContent().getNextActions()), validContent());
        service(client, new FakeVectorStore()).generate(context());
        assertThat(client.getAttempts()).isEqualTo(2);
        assertThat(client.requestSpec.userTexts.getLast()).contains("strengths[0].basis에 판단 근거를 작성하세요.", "strengths[0].text에 잘한 행동과 도움이 된 이유를 공백과 문장부호를 포함해 20~65자로 작성하세요.", "strengths[0].evidenceRefs에 허용 evidenceRefs의 식별자를 하나 이상 포함하세요.").doesNotContain("strength_basis_blank", "strength_text_blank", "strength_evidence_refs_blank");
    }

    @Test
    @DisplayName("normalizes line breaks in public text before validation")
    void normalizes_line_breaks_in_public_text_before_validation() throws Exception {
        var summary = "가".repeat(10)+ "\n  " + "나".repeat(10);
        var comparison = "다".repeat(10)+ "\r\n  " + "라".repeat(10);
        var strength = "마".repeat(10)+ "\r  " + "바".repeat(10);
        var improvement = "사".repeat(10)+ "\n\n  " + "아".repeat(10);
        var nextAction = "자".repeat(10)+ " \n " + "차".repeat(10);
        var client = new FakeChatClient(validContent().copy(summary, java.util.Arrays.asList(comparisonItem(comparison, java.util.Arrays.asList("report:" + reportId + "", "report:" + previousReportId + ""))), java.util.Arrays.asList(item(strength, java.util.Arrays.asList("record:" + recordId + ""))), java.util.Arrays.asList(item(improvement, java.util.Arrays.asList("record:" + recordId + ""))), java.util.Arrays.asList(item(nextAction, java.util.Arrays.asList("record:" + recordId + "")))));
        var result = service(client, new FakeVectorStore()).generate(context()).getContent();
        assertThat(client.getAttempts()).isEqualTo(1);
        assertThat(result.getSummary()).isEqualTo("가".repeat(10)+ " " + "나".repeat(10));
        assertThat(result.getComparisons().get(0).getText()).isEqualTo("다".repeat(10)+ " " + "라".repeat(10));
        assertThat(result.getStrengths().get(0).getText()).isEqualTo("마".repeat(10)+ " " + "바".repeat(10));
        assertThat(result.getImprovements().get(0).getText()).isEqualTo("사".repeat(10)+ " " + "아".repeat(10));
        assertThat(result.getNextActions().get(0).getText()).isEqualTo("자".repeat(10)+ " " + "차".repeat(10));
    }

    @Test
    @DisplayName("length failures retry with field length range and correction direction")
    void length_failures_retry_with_field_length_range_and_correction_direction() throws Exception {
        var overlongSummary = "가".repeat(66);
        var overlongImprovement = "나".repeat(73);
        var client = new FakeChatClient(validContent().copy(overlongSummary, validContent().getComparisons(), validContent().getStrengths(), java.util.Arrays.asList(item(overlongImprovement, java.util.Arrays.asList("record:" + recordId + ""))), validContent().getNextActions()), validContent());
        service(client, new FakeVectorStore()).generate(context());
        assertThat(client.getAttempts()).isEqualTo(2);
        assertThat(client.requestSpec.userTexts.getLast()).contains("summary는 현재 66자입니다.", "improvements[0].text는 현재 73자입니다.", "공백과 문장부호를 포함해 20~65자로 줄이세요.", "부족한 점, 영향, 보완 방향을 유지하면서").doesNotContain(overlongSummary, overlongImprovement, "summary_text_length", "improvement_text_length");
    }

    @Test
    @DisplayName("minimum length failures retry with field length range and correction direction")
    void minimum_length_failures_retry_with_field_length_range_and_correction_direction() throws Exception {
        var shortSummary = "가".repeat(19);
        var shortImprovement = "나".repeat(18);
        var client = new FakeChatClient(validContent().copy(shortSummary, validContent().getComparisons(), validContent().getStrengths(), java.util.Arrays.asList(item(shortImprovement, java.util.Arrays.asList("record:" + recordId + ""))), validContent().getNextActions()), validContent());
        service(client, new FakeVectorStore()).generate(context());
        assertThat(client.getAttempts()).isEqualTo(2);
        assertThat(client.requestSpec.userTexts.getLast()).contains("summary는 현재 19자입니다.", "improvements[0].text는 현재 18자입니다.", "공백과 문장부호를 포함해 20~65자로 늘리세요.", "부족한 점, 영향, 보완 방향을 유지하면서").doesNotContain(shortSummary, shortImprovement, "summary_text_length", "improvement_text_length");
    }

    @Test
    @DisplayName("English summary is accepted without retry")
    void English_summary_is_accepted_without_retry() throws Exception {
        var generatedText = "WATERING 작업 기록의 전체 흐름과 관리 방향을 확인했어요.";
        var client = new FakeChatClient(validContent().copy(generatedText, validContent().getComparisons(), validContent().getStrengths(), validContent().getImprovements(), validContent().getNextActions()));
        service(client, new FakeVectorStore()).generate(context());
        assertThat(client.getAttempts()).isEqualTo(1);
    }

    @Test
    @DisplayName("English item is accepted without retry")
    void English_item_is_accepted_without_retry() throws Exception {
        var generatedText = "다음에는 DRIP 방식으로 물을 준 뒤 흙 속 수분과 젖은 깊이를 확인하세요.";
        var client = new FakeChatClient(validContent().copy(validContent().getSummary(), validContent().getComparisons(), validContent().getStrengths(), validContent().getImprovements(), java.util.Arrays.asList(item(generatedText, java.util.Arrays.asList("record:" + recordId + "")))));
        service(client, new FakeVectorStore()).generate(context());
        assertThat(client.getAttempts()).isEqualTo(1);
    }

    @Test
    @DisplayName("English comparison is accepted without retry")
    void English_comparison_is_accepted_without_retry() throws Exception {
        var generatedText = "WATERING 기록이 직전보다 늘어 작업 흐름의 변화를 확인했어요.";
        var client = new FakeChatClient(validContent().copy(validContent().getSummary(), java.util.Arrays.asList(comparisonItem(generatedText, java.util.Arrays.asList("report:" + reportId + "", "report:" + previousReportId + ""))), validContent().getStrengths(), validContent().getImprovements(), validContent().getNextActions()));
        service(client, new FakeVectorStore()).generate(context());
        assertThat(client.getAttempts()).isEqualTo(1);
    }

    @Test
    @DisplayName("unavailable comparison is retried with an empty array instruction")
    void unavailable_comparison_is_retried_with_an_empty_array_instruction() throws Exception {
        var generatedText = "직전 재배보다 물 주기 기록이 한 번 늘었어요.";
        var client = new FakeChatClient(validContent().copy(validContent().getSummary(), java.util.Arrays.asList(comparisonItem(generatedText, java.util.Arrays.asList("report:" + reportId + ""))), validContent().getStrengths(), validContent().getImprovements(), validContent().getNextActions()), validContent().copy(validContent().getSummary(), List.of(), validContent().getStrengths(), validContent().getImprovements(), validContent().getNextActions()));
        var unavailableContext = context().copy(context().getSchemaVersion(), context().getWorkType(), context().getReport(), context().getRecords(), null, List.of(), context().getWarnings());
        service(client, new FakeVectorStore()).generate(unavailableContext);
        assertThat(client.getAttempts()).isEqualTo(2);
        assertThat(client.requestSpec.userTexts.getLast()).contains("comparisons는 빈 배열로 반환하세요.").doesNotContain(generatedText, "comparison_not_available");
    }

    @Test
    @DisplayName("missing comparison report reference is retried without exposing report ids")
    void missing_comparison_report_reference_is_retried_without_exposing_report_ids() throws Exception {
        var client = new FakeChatClient(validContent().copy(validContent().getSummary(), java.util.Arrays.asList(comparisonItem("직전 재배보다 물 주기 기록이 한 번 늘어 흐름이 안정됐어요.", java.util.Arrays.asList("report:" + previousReportId + ""))), validContent().getStrengths(), validContent().getImprovements(), validContent().getNextActions()), validContent());
        service(client, new FakeVectorStore()).generate(context());
        assertThat(client.getAttempts()).isEqualTo(2);
        var retryInstruction = after(client.requestSpec.userTexts.getLast(), "직전 응답은");
        assertThat(retryInstruction).contains("comparisons[0].evidenceRefs에 현재 리포트 근거를 "+
        "허용 evidenceRefs에서 선택해 포함하세요.").doesNotContain(reportId.toString(), previousReportId.toString(), "comparison_current_report_ref_required");
    }

    @Test
    @DisplayName("English output remains valid on the first attempt")
    void English_output_remains_valid_on_the_first_attempt() throws Exception {
        var content = validContent().copy("WATERING 작업 기록의 전체 흐름과 관리 방향을 확인했어요.", validContent().getComparisons(), validContent().getStrengths(), validContent().getImprovements(), validContent().getNextActions());
        var client = new FakeChatClient(content, content);
        assertThat(service(client, new FakeVectorStore()).generate(context()).getContent()).isEqualTo(content);
        assertThat(client.getAttempts()).isEqualTo(1);
    }

    @Test
    @DisplayName("unknown evidence value is not echoed into retry prompt")
    void unknown_evidence_value_is_not_echoed_into_retry_prompt() throws Exception {
        var privateValue = "private-model-value";
        var client = new FakeChatClient(validContent().copy(validContent().getSummary(), validContent().getComparisons(), java.util.Arrays.asList(item("흙 상태를 살핀 뒤 물을 주어 필요한 곳부터 관리한 점이 좋았어요.", java.util.Arrays.asList(privateValue))), validContent().getImprovements(), validContent().getNextActions()), validContent());
        service(client, new FakeVectorStore()).generate(context());
        assertThat(client.requestSpec.userTexts.getLast()).contains("모든 evidenceRefs에는 허용 evidenceRefs에 나열된 값만 사용하세요.").doesNotContain(privateValue, "unknown_evidence");
    }

    @Test
    @DisplayName("two unknown evidence attempts fail only as structured output invalid")
    void two_unknown_evidence_attempts_fail_only_as_structured_output_invalid() throws Exception {
        var invalid = validContent().copy(validContent().getSummary(), validContent().getComparisons(), java.util.Arrays.asList(item("흙 상태를 살핀 뒤 물을 주어 필요한 곳부터 관리한 점이 좋았어요.", java.util.Arrays.asList("unknown"))), validContent().getImprovements(), validContent().getNextActions());
        var client = new FakeChatClient(invalid, invalid);
        assertThatThrownBy(() -> service(client, new FakeVectorStore()).generate(context())).isInstanceOfSatisfying(ReportFeedbackGenerationFailure.class, it -> assertThat(it.getCode()).isEqualTo(ReportFeedbackFailureCode.STRUCTURED_OUTPUT_INVALID));
        assertThat(client.getAttempts()).isEqualTo(2);
    }

    @Test
    @DisplayName("entity time chat failure is not retried as invalid structured output")
    void entity_time_chat_failure_is_not_retried_as_invalid_structured_output() throws Exception {
        var client = new FakeChatClient(new BusinessException(ErrorCode.RAG_CHAT_UNAVAILABLE));
        assertThatThrownBy(() -> service(client, new FakeVectorStore()).generate(context())).isInstanceOfSatisfying(ReportFeedbackGenerationFailure.class, it -> assertThat(it.getCode()).isEqualTo(ReportFeedbackFailureCode.CHAT_UNAVAILABLE));
        assertThat(client.getAttempts()).isEqualTo(1);
    }

    @Test
    @DisplayName("structured output parse failure still receives one retry")
    void structured_output_parse_failure_still_receives_one_retry() throws Exception {
        var client = new FakeChatClient(new RuntimeException("parse failed"), validContent());
        service(client, new FakeVectorStore()).generate(context());
        assertThat(client.getAttempts()).isEqualTo(2);
        assertThat(client.requestSpec.userTexts.getLast()).contains("설명이나 Markdown 없이 JSON Schema의 필드명과 타입을 그대로 따른 완전한 JSON만 반환하세요.", "summary는 문자열이고 comparisons, strengths, improvements, nextActions는 배열입니다.").doesNotContain("structured_output_parse_failed");
    }

    @Test
    @DisplayName("two parse failures expose only the safe parse diagnostic")
    void two_parse_failures_expose_only_the_safe_parse_diagnostic() throws Exception {
        var rawMessage = "private generated response";
        var client = new FakeChatClient(new RuntimeException(rawMessage), new RuntimeException(rawMessage));
        assertThatThrownBy(() -> service(client, new FakeVectorStore()).generate(context())).isInstanceOfSatisfying(ReportFeedbackGenerationFailure.class, it -> {
        assertThat(it.getCode()).isEqualTo(ReportFeedbackFailureCode.STRUCTURED_OUTPUT_INVALID);
        assertThat(it.getCause().getMessage()).isEqualTo("structured_output_parse_failed").doesNotContain(rawMessage);
        });
        assertThat(client.getAttempts()).isEqualTo(2);
        assertThat(client.requestSpec.userTexts.getLast()).contains("JSON Schema의 필드명과 타입").doesNotContain(rawMessage, "structured_output_parse_failed");
    }

    @Test
    @DisplayName("mixed work type records are rejected before retrieval")
    void mixed_work_type_records_are_rejected_before_retrieval() {
        var vectorStore = new FakeVectorStore();
        var base = context();
        var r = base.getRecords().getFirst();
        var records = new ArrayList<>(base.getRecords());
        records.add(r.copy(r.getId(), r.getWorkedAt(), WorkType.FERTILIZING, r.getMemo(), r.getDetails()));
        var mixed = base.copy(base.getSchemaVersion(), base.getWorkType(), base.getReport(), records, base.getPreviousReport(), base.getComparisons(), base.getWarnings());
        assertThatThrownBy(() -> service(new FakeChatClient(validContent()), vectorStore).generate(mixed)).isInstanceOfSatisfying(ReportFeedbackGenerationFailure.class, it -> assertThat(it.getCode()).isEqualTo(ReportFeedbackFailureCode.INVALID_CONTEXT));
        assertThat(vectorStore.requests).isEmpty();
    }

    @Test
    @DisplayName("record-backed output is valid without a retrieved technical document")
    void record_backed_output_is_valid_without_a_retrieved_technical_document() throws Exception {
        var result = service(new FakeChatClient(validContent()), new FakeVectorStore()).generate(context());
        assertThat(result.getContent().getStrengths()).hasSize(1);
        assertThat(result.getCitations().stream().map(it -> it.get("id")).toList()).containsExactly("report:" + reportId + "", "report:" + previousReportId + "", "record:" + recordId + "");
    }
    private ReportFeedbackGenerationService service(FakeChatClient client, FakeVectorStore vectorStore) {
        return new ReportFeedbackGenerationService(client, vectorStore, new ReportFeedbackRetrievalQueryPlanner(), new ReportFeedbackPromptBuilder(), new RagProperties(new RagProperties.Embedding("test-embedding"), new RagProperties.Chat("test-chat"), new RagProperties.Retrieval(), new RagProperties.Execution()));
    }
    private ReportFeedbackContext context() {
        return new ReportFeedbackContext(ReportFeedbackContextKt.REPORT_FEEDBACK_CONTEXT_SCHEMA_VERSION, WorkType.WATERING, new ReportFeedbackReport(reportId, "약초농장", "황기", LocalDateTime.of(2026, 3, 1, 9, 0), LocalDateTime.of(2026, 7, 1, 9, 0), 1, map(new AbstractMap.SimpleEntry<>("recordCount", 1))), java.util.Arrays.asList(new ReportFeedbackRecord(recordId, LocalDateTime.of(2026, 4, 1, 9, 0), WorkType.WATERING, "점적관수를 했어요.", Map.of())), new ReportFeedbackPreviousReport(previousReportId, LocalDateTime.of(2025, 3, 1, 9, 0), LocalDateTime.of(2025, 7, 1, 9, 0), 1, map(new AbstractMap.SimpleEntry<>("recordCount", 1))), java.util.Arrays.asList(new ReportFeedbackComparison("recordCount", "기록 횟수", new BigDecimal("2"), new BigDecimal("1"), new BigDecimal("1"), new BigDecimal("100"), "회", null, null)), List.of());
    }
    private ReportFeedbackContent validContent() {
        return new ReportFeedbackContent("이번 물 주기 기록의 전체 흐름과 관리 방향을 확인했어요.", java.util.Arrays.asList(comparisonItem("직전 재배보다 물 주기 기록이 한 번 늘어 흐름이 안정됐어요.", java.util.Arrays.asList("report:" + reportId + "", "report:" + previousReportId + ""))), java.util.Arrays.asList(item("흙 상태를 살핀 뒤 물을 주어 필요한 곳부터 관리한 점이 좋았어요.", java.util.Arrays.asList("record:" + recordId + ""))), java.util.Arrays.asList(item("물의 양이 알맞았는지 흙 속 수분까지 확인해 기록할 필요가 있어요.", java.util.Arrays.asList("record:" + recordId + ""))), java.util.Arrays.asList(item("다음에는 물을 준 뒤 흙 속까지 젖었는지 손으로 확인해 기록하세요.", java.util.Arrays.asList("record:" + recordId + ""))));
    }
    private ReportFeedbackContentItem item(String text, List<String> evidenceRefs) {
        return new ReportFeedbackContentItem("관수 기록 1회", text, evidenceRefs);
    }
    private ReportFeedbackContentItem comparisonItem(String text, List<String> evidenceRefs) {
        return new ReportFeedbackContentItem("직전보다 기록 1회 증가", text, evidenceRefs);
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

    private static String after(String text, String delimiter) { int i = text.indexOf(delimiter); return i < 0 ? text : text.substring(i + delimiter.length()); }
    private static String before(String text, String delimiter) { int i = text.indexOf(delimiter); return i < 0 ? text : text.substring(0, i); }

}
