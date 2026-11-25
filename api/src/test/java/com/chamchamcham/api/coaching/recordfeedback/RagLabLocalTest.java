package com.chamchamcham.api.coaching.recordfeedback;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.document.Document;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.*;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import static org.assertj.core.api.Assertions.assertThat;

/** 명시적 실행 전용 실습 도구. 전체 Spring 앱·외부 제공자·답변 모델을 시작하지 않는다. */
@EnabledIfEnvironmentVariable(named = "RUN_RAG_LAB", matches = "true")
class RagLabLocalTest {
    private final ObjectMapper mapper = Jackson2ObjectMapperBuilder.json().build();

    @Test
    void ingest_or_search_the_preserved_official_corpus() throws Exception {
        Path root = Path.of(System.getenv("RAG_LAB_ROOT")).toRealPath();
        assertThat(root).isEqualTo(Path.of("../.local/rag").toRealPath());
        String action = System.getenv("RAG_LAB_ACTION");
        assertThat(action).isIn("ingest", "search");
        var jdbc = new JdbcTemplate(new DriverManagerDataSource(
                "jdbc:postgresql://127.0.0.1:15432/ccc_rag_lab", "ccc_rag_lab", System.getenv("RAG_LAB_PASSWORD")));
        assertThat(Objects.equals(jdbc.queryForObject("select token from lab_identity where schema_version=1", String.class),
                System.getenv("RAG_LAB_TOKEN"))).as("실습 DB 식별 정보 불일치").isTrue();
        assertThat(jdbc.queryForList("select nspname from pg_namespace where nspname !~ '^pg_' and nspname <> 'information_schema'", String.class))
                .containsExactly("public");
        assertThat(jdbc.queryForList("select extname from pg_extension", String.class))
                .isSubsetOf("plpgsql", "vector", "hstore", "uuid-ossp");
        assertThat(jdbc.queryForList("select c.relname from pg_class c join pg_namespace n on n.oid=c.relnamespace "
                + "where n.nspname='public' and c.relkind in ('r','p','v','m','S','f')", String.class))
                .contains("lab_identity").isSubsetOf("lab_identity", "vector_store");
        boolean existing = Boolean.TRUE.equals(jdbc.queryForObject("select to_regclass('public.vector_store') is not null", Boolean.class));
        if (existing) validateSchema(jdbc);
        else assertThat(action).as("빈 DB에는 먼저 ingest를 실행하세요").isEqualTo("ingest");
        var embedding = OllamaEmbeddingModel.builder()
                .ollamaApi(OllamaApi.builder().baseUrl("http://127.0.0.1:11435").build())
                .defaultOptions(OllamaEmbeddingOptions.builder().model("bge-m3").truncate(false).build()).build();
        var store = PgVectorStore.builder(jdbc, embedding).dimensions(1024)
                .distanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)
                .indexType(PgVectorStore.PgIndexType.HNSW).initializeSchema(!existing).maxDocumentBatchSize(1).build();
        store.afterPropertiesSet();
        validateSchema(jdbc);
        List<Document> docs = new ArrayList<>();
        for (JsonNode row : mapper.readTree(root.resolve("corpus/chunks.json").toFile())) {
            docs.add(new Document(row.path("id").asText(), row.path("text").asText(),
                    mapper.convertValue(row.path("metadata"), new TypeReference<Map<String, Object>>() {})));
        }
        assertThat(docs).hasSize(2);
        var ids = docs.stream().map(Document::getId).toList();
        assertThat(jdbc.queryForList("select id::text from vector_store", String.class)).isSubsetOf(ids);
        int added = 0;
        for (Document doc : docs) {
            boolean present = jdbc.queryForObject("select count(*) from vector_store where id=?::uuid", Integer.class, doc.getId()) == 1;
            if (!present && action.equals("ingest")) {
                store.add(List.of(doc));
                added++;
            }
            // 기존 값은 덮어쓰지 않는다. 예상과 다르면 확인할 수 있게 실패한다.
            assertThat(jdbc.queryForObject("select content from vector_store where id=?::uuid", String.class, doc.getId())).isEqualTo(doc.getText());
            assertThat(mapper.readTree(jdbc.queryForObject("select metadata::text from vector_store where id=?::uuid", String.class, doc.getId())))
                    .isEqualTo(mapper.valueToTree(doc.getMetadata()));
        }
        assertThat(jdbc.queryForObject("select count(*) from vector_store", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForList("select distinct vector_dims(embedding) from vector_store", Integer.class)).containsExactly(1024);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("action", action);
        result.put("documentCount", 2);
        result.put("added", added);
        result.put("dimension", 1024);
        if (action.equals("search")) {
            String query = System.getenv("RAG_LAB_QUERY");
            double threshold = Double.parseDouble(System.getenv("RAG_LAB_THRESHOLD"));
            assertThat(query).isNotBlank();
            assertThat(threshold).isBetween(0.0, 1.0);
            var found = store.similaritySearch(SearchRequest.builder().query(query).topK(3)
                    .similarityThreshold(threshold).filterExpression("sourceType == 'TECH_DOCUMENT' && cropName == 'GENERAL'").build());
            List<Map<String, Object>> rows = new ArrayList<>();
            for (Document doc : found) {
                assertThat(ids).contains(doc.getId());
                assertThat(doc.getScore()).isGreaterThanOrEqualTo(threshold);
                var original = docs.stream().filter(item -> item.getId().equals(doc.getId())).findFirst().orElseThrow();
                assertThat(doc.getText()).isEqualTo(original.getText());
                assertThat(doc.getMetadata()).containsAllEntriesOf(original.getMetadata());
                rows.add(Map.of("id", doc.getId(), "score", doc.getScore(), "text", doc.getText(), "metadata", doc.getMetadata()));
            }
            result.put("query", query);
            result.put("threshold", threshold);
            result.put("topK", 3);
            result.put("results", rows);
        }
        Files.createDirectories(root.resolve("results"));
        mapper.writerWithDefaultPrettyPrinter().writeValue(root.resolve("results/" + action + ".json").toFile(), result);
    }

    private void validateSchema(JdbcTemplate jdbc) {
        assertThat(jdbc.queryForList("select attname || ':' || format_type(atttypid,atttypmod) from pg_attribute "
                + "where attrelid='public.vector_store'::regclass and attnum>0 and not attisdropped order by attnum", String.class))
                .containsExactly("id:uuid", "content:text", "metadata:json", "embedding:vector(1024)");
        assertThat(jdbc.queryForObject("select count(*) from pg_indexes where schemaname='public' and tablename='vector_store' "
                + "and indexdef like '%USING hnsw (embedding vector_cosine_ops)%'", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select extversion from pg_extension where extname='vector'", String.class)).isEqualTo("0.8.5");
    }
}
