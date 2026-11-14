package com.chamchamcham.lab;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import java.nio.file.*;
import java.util.*;
import org.springframework.ai.document.Document;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.*;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/** 실습 DB 식별과 원문/선택 본문 해시를 확인한 뒤에만 적재한다. 기존 행은 덮어쓰지 않는다. */
public final class RagApiLabIngest {
    public static void main(String[] args) throws Exception {
        RagApiLabApplication.guard(false);
        var root = RagApiLabApplication.root();
        var mapper = new ObjectMapper();
        Path corpus = root.resolve("corpus");
        for (JsonNode source : mapper.readTree(corpus.resolve("manifest.json").toFile())) {
            byte[] raw = Files.readAllBytes(corpus.resolve(source.path("rawFile").asText(source.path("sourceId").asText() + ".html")));
            if (!sha(raw).equals(source.path("rawSha256").asText())) throw new IllegalStateException("원문 해시 불일치");
            byte[] selected = Files.readAllBytes(corpus.resolve(source.path("sourceId").asText() + ".selected.txt"));
            if (!sha(selected).equals(source.path("selectedSha256").asText())) throw new IllegalStateException("선택 본문 해시 불일치");
        }
        var jdbc = new JdbcTemplate(new DriverManagerDataSource(RagApiLabApplication.URL, RagApiLabApplication.DB, RagApiLabApplication.required("POSTGRES_PASSWORD")));
        boolean existing = Boolean.TRUE.equals(jdbc.queryForObject("select to_regclass('public.vector_store') is not null", Boolean.class));
        if (existing && !jdbc.queryForList("select attname || ':' || format_type(atttypid,atttypmod) from pg_attribute where attrelid='public.vector_store'::regclass and attnum>0 and not attisdropped order by attnum", String.class)
            .equals(List.of("id:uuid", "content:text", "metadata:json", "embedding:vector(1024)"))) throw new IllegalStateException("예상하지 않은 벡터 스키마");
        var embedding = OllamaEmbeddingModel.builder().ollamaApi(OllamaApi.builder().baseUrl(RagApiLabApplication.ollamaUrl()).build())
            .defaultOptions(OllamaEmbeddingOptions.builder().model("bge-m3").truncate(false).build()).build();
        var store = PgVectorStore.builder(jdbc, embedding).dimensions(1024).distanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)
            .indexType(PgVectorStore.PgIndexType.HNSW).initializeSchema(!existing).maxDocumentBatchSize(1).build();
        store.afterPropertiesSet();
        int added = 0;
        var ids = new HashSet<String>();
        var sources = mapper.readTree(corpus.resolve("manifest.json").toFile());
        for (JsonNode row : mapper.readTree(corpus.resolve("chunks.json").toFile())) {
            String id = row.path("id").asText(); UUID.fromString(id);
            if (!ids.add(id)) throw new IllegalStateException("중복 문서 ID");
            var metadata = mapper.convertValue(row.path("metadata"), new TypeReference<Map<String, Object>>() {});
            String text = row.path("text").asText();
            String sourceId = (String)metadata.get("sourceId");
            JsonNode source = null;
            for (JsonNode item : sources) if (item.path("sourceId").asText().equals(sourceId)) source = item;
            if (source == null || !Objects.equals(metadata.get("sourceUrl"), source.path("url").asText())
                || !Objects.equals(metadata.get("selectedSha256"), source.path("selectedSha256").asText())
                || !List.of("천궁", "참당귀", "구기자나무", "GENERAL").contains(metadata.get("cropName"))
                || (Objects.equals(metadata.get("cropName"), "GENERAL") && !sourceId.equals("psis-general"))
                || !Objects.equals(metadata.get("sourceType"), "TECH_DOCUMENT")
                || text.isBlank() || !Files.readString(corpus.resolve(sourceId + ".selected.txt")).contains(text))
                throw new IllegalStateException("문서 조각과 검토된 선택 본문/출처 불일치");
            var doc = new Document(id, text, metadata);
            boolean present = jdbc.queryForObject("select count(*) from vector_store where id=?::uuid", Integer.class, id) == 1;
            if (!present) { store.add(List.of(doc)); added++; }
            if (!text.equals(jdbc.queryForObject("select content from vector_store where id=?::uuid", String.class, id))
                || !mapper.valueToTree(metadata).equals(mapper.readTree(jdbc.queryForObject("select metadata::text from vector_store where id=?::uuid", String.class, id))))
                throw new IllegalStateException("기존 문서가 다릅니다. 덮어쓰지 않습니다");
        }
        Files.createDirectories(root.resolve("results"));
        mapper.writerWithDefaultPrettyPrinter().writeValue(root.resolve("results/ingest.json").toFile(), Map.of(
            "added", added, "corpusChunks", ids.size(), "stored", jdbc.queryForObject("select count(*) from vector_store", Integer.class),
            "dimensions", jdbc.queryForList("select distinct vector_dims(embedding) from vector_store", Integer.class)));
    }
    static String sha(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));
    }
}
