package com.chamchamcham.api.coaching.recordfeedback;

import com.chamchamcham.application.coaching.common.RagProperties;
import com.chamchamcham.application.coaching.recordfeedback.*;
import com.chamchamcham.application.coaching.recordfeedback.generation.*;
import com.fasterxml.jackson.databind.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.*;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** 로컬 전용. 실제 검색 서비스를 실행하고 답변 모델 호출 직전에 중단한다. */
@EnabledIfEnvironmentVariable(named = "RUN_LOCAL_RETRIEVAL_VERIFY", matches = "true")
class RecordFeedbackRetrievalLocalTest {
    private final ObjectMapper mapper = Jackson2ObjectMapperBuilder.json().build();

    @Test
    void official_documents_reach_prompt_through_production_retrieval() throws Exception {
        String jdbcUrl = System.getenv("RAG_VERIFY_JDBC_URL");
        String ollamaUrl = System.getenv("RAG_VERIFY_OLLAMA_URL");
        assertThat(jdbcUrl).matches("jdbc:postgresql://127\\.0\\.0\\.1:[0-9]+/ccc_rag_verify");
        assertThat(ollamaUrl).matches("http://127\\.0\\.0\\.1:[0-9]+");
        var jdbc = new JdbcTemplate(new DriverManagerDataSource(jdbcUrl, "ccc_rag_verify", System.getenv("RAG_VERIFY_DB_PASSWORD")));
        assertThat(jdbc.queryForObject("select token from local_verification_guard", String.class))
                .isEqualTo(System.getenv("RAG_VERIFY_TOKEN"));
        Path corpus = Path.of(System.getenv("RAG_VERIFY_CORPUS"));
        var embedding = OllamaEmbeddingModel.builder().ollamaApi(OllamaApi.builder().baseUrl(ollamaUrl).build())
                .defaultOptions(OllamaEmbeddingOptions.builder().model("bge-m3").truncate(false).build()).build();
        assertThat(embedding.embed("천궁 고온 관리")).hasSize(1024);
        var store = PgVectorStore.builder(jdbc, embedding).dimensions(1024)
                .distanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)
                .indexType(PgVectorStore.PgIndexType.HNSW).initializeSchema(true).maxDocumentBatchSize(1).build();
        store.afterPropertiesSet();
        List<Document> docs = new ArrayList<>();
        for (JsonNode row : mapper.readTree(corpus.resolve("chunks.json").toFile())) {
            docs.add(new Document(row.path("id").asText(), row.path("text").asText(),
                    mapper.convertValue(row.path("metadata"), new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {})));
        }
        for (Document doc : docs) store.add(List.of(doc));
        for (Document doc : docs) {
            assertThat(jdbc.queryForObject("select content from vector_store where id=?::uuid", String.class, doc.getId())).isEqualTo(doc.getText());
            var stored = mapper.readTree(jdbc.queryForObject("select metadata::text from vector_store where id=?::uuid", String.class, doc.getId()));
            assertThat(stored).isEqualTo(mapper.valueToTree(doc.getMetadata()));
        }
        int count = jdbc.queryForObject("select count(*) from vector_store", Integer.class);
        for (Document doc : docs) store.add(List.of(doc));
        assertThat(jdbc.queryForObject("select count(*) from vector_store", Integer.class)).isEqualTo(count);
        assertThat(jdbc.queryForList("select distinct vector_dims(embedding) as dims from vector_store", Integer.class)).containsExactly(1024);
        // 본문이 같아도 기록·다른 작물 문서는 인용에 섞이면 안 된다.
        for (String kind : List.of("wrong-type", "wrong-crop")) {
            var metadata = new HashMap<>(docs.getFirst().getMetadata());
            metadata.put(kind.equals("wrong-type") ? "sourceType" : "cropName", kind.equals("wrong-type") ? "FARMING_RECORD" : "검증용다른작물");
            store.add(List.of(new Document(UUID.nameUUIDFromBytes(kind.getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString(), docs.getFirst().getText(), metadata)));
        }
        JsonNode base = mapper.readTree(Files.readString(Path.of("../application/src/test/resources/coaching/rag/today-record-feedback-watering.json")));
        JsonNode cases = mapper.readTree(getClass().getResourceAsStream("/verification/rag-retrieval-cases.json"));
        List<Map<String, Object>> results = new ArrayList<>();
        for (JsonNode scenario : cases) {
            var input = base.deepCopy();
            ((com.fasterxml.jackson.databind.node.ObjectNode) input.path("crop")).put("name", "천궁").put("usePartCategory", "RHIZOME");
            // 고정 기록과 기록 당시 날씨를 사용하고 실시간 날씨는 조회하지 않는다.
            ((com.fasterxml.jackson.databind.node.ObjectNode) input).putNull("weather");
            String memo = scenario.path("memo").asText();
            if (scenario.has("suffix")) memo = memo.repeat(4) + scenario.path("suffix").asText();
            ((com.fasterxml.jackson.databind.node.ObjectNode) input.path("record")).put("memo", memo);
            var context = mapper.treeToValue(input, RecordFeedbackContext.class);
            for (boolean withMemo : List.of(false, true)) {
                for (double threshold : List.of(0.0, 0.5, 0.55)) {
                    results.add(observe(store, docs, context, scenario, withMemo, threshold));
                }
            }
        }
        // 실제 점수 조건에 맞는 근거가 없으면 답변 모델에 도달하지 않는다.
        var context = mapper.treeToValue(base, RecordFeedbackContext.class);
        var empty = observe(store, docs, context, mapper.createObjectNode().put("id", "forced-empty").putNull("source").putNull("needle"), true, 1.0);
        assertThat(empty.get("outcome")).isEqualTo("INSUFFICIENT_EVIDENCE");
        assertThat(empty.get("prompt")).isEqualTo("");
        Path output = Path.of("build/rag-local-verification");
        Files.createDirectories(output);
        mapper.writerWithDefaultPrettyPrinter().writeValue(output.resolve("comparisons.json").toFile(), results);
        mapper.writerWithDefaultPrettyPrinter().writeValue(output.resolve("database.json").toFile(),
                Map.of("documentChunks", count, "guardChunks", 2, "dimension", 1024,
                       "rows", jdbc.queryForList("select id, content, metadata::text as metadata, vector_dims(embedding) as dimensions from vector_store order by id"), "forcedEmpty", empty));
        assertThat(results).hasSize(48);
        for (var result : results) {
            if ((double) result.get("threshold") == 0.0) {
                assertThat(result.get("outcome")).isEqualTo("CAPTURED_BEFORE_CHAT");
                assertThat(((List<?>) result.get("evidence"))).hasSize(docs.size());
                if (!((JsonNode) result.get("expectedSource")).isNull())
                    assertThat(result.get("relevantPassagePresent")).isEqualTo(true);
            }
        }
    }

    private Map<String, Object> observe(PgVectorStore store, List<Document> corpus, RecordFeedbackContext context, JsonNode scenario, boolean withMemo, double threshold) {
        var trace = new ArrayList<Map<String, Object>>();
        var observed = spy(store);
        doAnswer(invocation -> {
            SearchRequest request = invocation.getArgument(0);
            List<Document> found = store.similaritySearch(request);
            for (Document doc : found) {
                var original = corpus.stream().filter(it -> it.getId().equals(doc.getId())).findFirst().orElseThrow();
                assertThat(doc.getText()).isEqualTo(original.getText());
                assertThat(doc.getMetadata()).containsAllEntriesOf(original.getMetadata());
            }
            assertThat(request.getSimilarityThreshold()).isEqualTo(threshold);
            assertThat(request.getTopK()).isEqualTo(3);
            assertThat(found).allSatisfy(doc -> {
                assertThat(doc.getMetadata().get("sourceType")).isEqualTo("TECH_DOCUMENT");
                assertThat(doc.getMetadata().get("cropName")).isIn(context.getCrop().getName(), "GENERAL");
                assertThat(doc.getScore()).isGreaterThanOrEqualTo(threshold);
            });
            trace.add(Map.of("query", request.getQuery(), "threshold", request.getSimilarityThreshold(),
                    "filter", request.getFilterExpression().toString(), "results", found.stream().map(doc -> Map.of(
                        "id", doc.getId(), "score", doc.getScore(), "sourceId", doc.getMetadata().get("sourceId"), "text", doc.getText())).toList()));
            return found;
        }).when(observed).similaritySearch(any(SearchRequest.class));
        var planner = new RecordFeedbackRetrievalQueryPlanner() {
            @Override public List<RecordFeedbackRetrievalQuery> plan(RecordFeedbackContext c) {
                var queries = super.plan(c);
                return withMemo ? queries : List.of(queries.getFirst());
            }
        };
        var prompt = new String[] {""};
        List<RecordFeedbackEvidence> evidence = new ArrayList<>();
        var builder = spy(new RecordFeedbackPromptBuilder());
        doAnswer(invocation -> {
            List<RecordFeedbackEvidence> items = invocation.getArgument(2);
            evidence.addAll(items);
            return invocation.callRealMethod();
        }).when(builder).build(any(), anyList(), anyList());
        var chat = mock(ChatClient.class);
        var spec = mock(ChatClient.ChatClientRequestSpec.class, RETURNS_SELF);
        when(chat.prompt()).thenReturn(spec);
        doAnswer(invocation -> { prompt[0] = invocation.getArgument(0); return spec; }).when(spec).user(anyString());
        when(spec.call()).thenThrow(new IllegalStateException("local verification intentionally stops before chat"));
        var defaults = new RagProperties();
        var config = new RagProperties(defaults.embedding(), defaults.chat(), new RagProperties.Retrieval(3, 20, threshold), defaults.execution());
        var service = new RecordFeedbackGenerationService(chat, observed, planner, builder, config);
        String outcome = "UNEXPECTED_SUCCESS";
        try { service.generate(context, 3); }
        catch (RecordFeedbackGenerationFailure failure) { outcome = failure.getCode().name(); }
        assertThat(outcome).isIn("CHAT_UNAVAILABLE", "INSUFFICIENT_EVIDENCE");
        if (outcome.equals("CHAT_UNAVAILABLE")) {
            assertThat(evidence).isNotEmpty();
            assertThat(evidence.stream().map(RecordFeedbackEvidence::getId).toList()).doesNotHaveDuplicates();
            for (var item : evidence) assertThat(prompt[0]).contains(item.getId(), item.getContent());
            assertThat(prompt[0]).contains(context.getRecord().getMemo());
        } else {
            assertThat(evidence).isEmpty();
            assertThat(prompt[0]).isEmpty();
        }
        if (scenario.has("expectedMemoQuery")) {
            assertThat(trace.stream().map(row -> row.get("query")).toList()).containsExactlyElementsOf(withMemo
                    ? List.of("천궁 관수 재배 관리 약용작물", scenario.path("expectedMemoQuery").asText())
                    : List.of("천궁 관수 재배 관리 약용작물"));
        }
        var expectedIds = new LinkedHashSet<String>();
        for (var query : trace) {
            @SuppressWarnings("unchecked") var found = (List<Map<String, Object>>) query.get("results");
            for (var doc : found) expectedIds.add((String) doc.get("id"));
        }
        assertThat(evidence.stream().map(RecordFeedbackEvidence::getId).toList()).containsExactlyElementsOf(expectedIds);
        String needle = scenario.path("needle").asText("");
        var relevantIds = new HashSet<String>();
        for (var query : trace) {
            @SuppressWarnings("unchecked") var found = (List<Map<String, Object>>) query.get("results");
            for (var doc : found) if (scenario.path("source").asText("").equals(doc.get("sourceId"))) relevantIds.add((String) doc.get("id"));
        }
        boolean relevant = !scenario.path("source").isNull() && evidence.stream().anyMatch(e -> relevantIds.contains(e.getId()) && e.getContent().contains(needle));
        var row = new LinkedHashMap<String, Object>();
        row.put("case", scenario.path("id").asText()); row.put("memo", context.getRecord().getMemo());
        row.put("withMemo", withMemo); row.put("threshold", threshold); row.put("queries", trace);
        row.put("evidence", evidence); row.put("expectedSource", scenario.path("source"));
        row.put("relevantPassagePresent", relevant); row.put("outcome", outcome.equals("CHAT_UNAVAILABLE") ? "CAPTURED_BEFORE_CHAT" : outcome);
        row.put("prompt", prompt[0]);
        return row;
    }
}
