package com.chamchamcham.lab;

import com.chamchamcham.ApiApplication;
import com.chamchamcham.application.coaching.recordfeedback.generation.*;
import com.chamchamcham.application.report.FarmingCycleReportProjectionService;
import com.chamchamcham.domain.crop.*;
import com.chamchamcham.domain.farm.*;
import com.chamchamcham.domain.member.*;
import com.chamchamcham.domain.pesticide.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import org.springframework.ai.chat.model.*;
import org.springframework.ai.chat.prompt.*;
import org.springframework.ai.document.Document;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.*;
import org.springframework.ai.vectorstore.*;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.boot.*;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.core.env.MapPropertySource;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;
import static org.mockito.Mockito.mock;

/** 배포 산출물에 포함되지 않는 명시적 로컬 실행기. 인증/저장/비동기 파이프라인은 실제 구현이다. */
public final class RagApiLabApplication {
    static final String URL = "jdbc:postgresql://127.0.0.1:15433/ccc_rag_api";
    static final String DB = "ccc_rag_api";
    static Path root() { return Path.of(required("RAG_API_ROOT")); }
    static String ollamaUrl() {
        String value = System.getenv().getOrDefault("RAG_API_OLLAMA_URL", "http://127.0.0.1:11436");
        if (!List.of("http://127.0.0.1:11436", "http://127.0.0.1:11437").contains(value))
            throw new IllegalStateException("실습 Ollama는 지정한 루프백 주소만 허용합니다");
        return value;
    }
    static String chatModel() {
        String value = System.getenv().getOrDefault("RAG_API_CHAT_MODEL", "qwen3:4b-instruct");
        if (!List.of("qwen3:4b-instruct", "qwen2.5:7b-instruct-q4_K_M").contains(value))
            throw new IllegalStateException("승인된 실습 모델만 허용합니다");
        return value;
    }
    static String required(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) throw new IllegalStateException("Missing local setting: " + key);
        return value;
    }
    static Connection connection() throws SQLException {
        return DriverManager.getConnection(URL, DB, required("POSTGRES_PASSWORD"));
    }
    static List<String> rows(Connection connection, String query) throws SQLException {
        var values = new ArrayList<String>();
        try (var statement = connection.createStatement(); var result = statement.executeQuery(query)) {
            while (result.next()) values.add(result.getString(1));
        }
        return values;
    }
    static void guard(boolean initialize) throws Exception {
        if (!root().toRealPath().equals(Path.of("../.local/rag-api").toRealPath()))
            throw new IllegalStateException("전용 실습 폴더가 아닙니다");
        try (var connection = connection()) {
            if (!rows(connection, "select nspname from pg_namespace where nspname !~ '^pg_' and nspname <> 'information_schema' order by nspname").equals(List.of("public")))
                throw new IllegalStateException("예상하지 않은 사용자 스키마");
            String state;
            try (var statement = connection.prepareStatement("select state from lab_identity where token=?")) {
                statement.setString(1, required("LAB_TOKEN"));
                try (var result = statement.executeQuery()) {
                    if (!result.next()) throw new IllegalStateException("DB 식별 정보 불일치");
                    state = result.getString(1);
                    if (result.next()) throw new IllegalStateException("DB 식별 정보 중복");
                }
            }
            if (initialize) {
                if (!state.equals("EMPTY") || !rows(connection, "select c.relname from pg_class c join pg_namespace n on n.oid=c.relnamespace where n.nspname='public' and c.relkind in ('r','p','v','m','S','f') order by c.relname").equals(List.of("lab_identity")))
                    throw new IllegalStateException("빈 전용 DB만 초기화할 수 있습니다. 실패한 초기화는 자동 재시도하지 않습니다");
                try (var statement = connection.prepareStatement("update lab_identity set state='INITIALIZING' where token=? and state='EMPTY'")) {
                    statement.setString(1, required("LAB_TOKEN"));
                    if (statement.executeUpdate() != 1) throw new IllegalStateException("초기화 동시 실행 충돌");
                }
            } else {
                boolean recovering = "true".equals(System.getenv("RAG_API_RECOVER")) && state.equals("INITIALIZING");
                if (!state.equals("READY") && !recovering) throw new IllegalStateException("DB가 READY가 아닙니다: " + state);
                if (!recovering && !Files.readString(root().resolve("schema.txt")).equals(schema(connection)))
                    throw new IllegalStateException("예상 스키마와 다릅니다. 자동 수정하지 않습니다");
            }
        }
    }
    static String schema(Connection connection) throws SQLException {
        return String.join("\n", rows(connection, "select c.relname || ':' || a.attname || ':' || format_type(a.atttypid,a.atttypmod) from pg_class c join pg_namespace n on n.oid=c.relnamespace join pg_attribute a on a.attrelid=c.oid where n.nspname='public' and c.relkind in ('r','p','v','m','S','f') and c.relname <> 'vector_store' and a.attnum>0 and not a.attisdropped order by c.relname,a.attnum"));
    }
    public static void main(String[] ignored) throws Exception {
        boolean initialize = required("RAG_API_INITIALIZE").equals("true");
        guard(initialize); // Spring/Hibernate보다 먼저 실행. URL은 고정된 루프백 전용 DB다.
        var app = new SpringApplication(ApiApplication.class, LabConfiguration.class);
        app.addInitializers(context -> context.getEnvironment().getPropertySources().addFirst(
            new MapPropertySource("guarded-rag-api-lab", properties(initialize))));
        var context = app.run("--spring.profiles.active=test,rag-api-lab");
        if (initialize || "true".equals(System.getenv("RAG_API_RECOVER"))) {
            try (var connection = connection()) {
                Files.writeString(root().resolve("schema.txt"), schema(connection));
                try (var statement = connection.prepareStatement("update lab_identity set state='READY' where token=? and state='INITIALIZING'")) {
                    statement.setString(1, required("LAB_TOKEN"));
                    if (statement.executeUpdate() != 1) throw new IllegalStateException("초기화 완료 마커 갱신 실패");
                }
            }
        }
        Files.writeString(root().resolve("ready.json"), "{\"profile\":\"rag-api-lab\",\"port\":18080,\"weather\":\"SYNTHETIC\",\"reports\":\"STUB\"}");
    }
    static Map<String, Object> properties(boolean initialize) {
        var properties = new HashMap<String, Object>();
        properties.put("server.address", "127.0.0.1"); properties.put("server.port", 18080);
        properties.put("spring.datasource.url", URL); properties.put("spring.datasource.username", DB);
        properties.put("spring.datasource.password", required("POSTGRES_PASSWORD"));
        properties.put("spring.datasource.driver-class-name", "org.postgresql.Driver");
        properties.put("spring.jpa.hibernate.ddl-auto", initialize ? "create-only" : "validate");
        properties.put("spring.jpa.properties.hibernate.hbm2ddl.halt_on_error", true);
        properties.put("spring.jpa.open-in-view", false);
        properties.put("spring.ai.model.chat", "none");
        properties.put("spring.ai.model.embedding", "none");
        properties.put("spring.ai.ollama.base-url", ollamaUrl());
        properties.put("spring.ai.ollama.init.pull-model-strategy", "never");
        properties.put("spring.ai.vectorstore.pgvector.initialize-schema", false);
        properties.put("spring.autoconfigure.exclude", "org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreAutoConfiguration,org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration");
        properties.put("redis.host", "127.0.0.1"); properties.put("redis.port", 16379);
        properties.put("jwt.secret", required("JWT_SECRET"));
        properties.put("rag.chat.model", chatModel());
        properties.put("rag.execution.core-pool-size", 1); properties.put("rag.execution.max-pool-size", 1);
        properties.put("rag.execution.pending-timeout-millis", 900000);
        // 제외한 제공자는 잘못 호출해도 외부 네트워크로 나가지 않도록 실습 실행에서만 루프백으로 둔다.
        for (String key : List.of("policy.nongup-ez.base-url", "kma.base-url.vilage-fcst", "kma.base-url.mid-fcst", "kma.base-url.asos", "kma.base-url.uv", "openai.realtime.base-url", "auth.kakao.oidc.discovery-uri", "auth.kakao.oidc.user-info-uri", "auth.apple.oidc.jwks-uri", "auth.naver.profile-uri"))
            properties.put(key, "http://127.0.0.1:9");
        properties.put("spring.mail.host", "127.0.0.1"); properties.put("spring.mail.port", 9);
        properties.put("logging.level.org.springframework.ai", "WARN");
        return properties;
    }
    static synchronized void trace(String kind, Object value) {
        try {
            Path folder = root().resolve("traces"); Files.createDirectories(folder);
            new ObjectMapper().findAndRegisterModules().writerWithDefaultPrettyPrinter()
                .writeValue(folder.resolve(System.currentTimeMillis() + "-" + kind + "-" + UUID.randomUUID() + ".json").toFile(), value);
        } catch (Exception failure) { throw new IllegalStateException("실습 관측 기록 실패", failure); }
    }
    @TestConfiguration(proxyBeanMethods = false)
    @Profile("rag-api-lab")
    public static class LabConfiguration {
        @Bean @Primary FarmingCycleReportProjectionService labReportProjection() {
            return mock(FarmingCycleReportProjectionService.class);
        }
        @Bean @Primary RecordFeedbackWeatherPort labFixedWeather() {
            return (latitude, longitude, days) -> new RecordFeedbackLiveWeather(
                new RecordFeedbackCurrentWeather(32, "맑음", LocalDateTime.now(ZoneId.of("Asia/Seoul"))), List.of(), "SYNTHETIC_LAB");
        }
        @Bean ChatModel labChatModel() throws Exception {
            var factory = new SimpleClientHttpRequestFactory(); factory.setConnectTimeout(5000); factory.setReadTimeout(240000);
            var api = OllamaApi.builder().baseUrl(ollamaUrl())
                .restClientBuilder(RestClient.builder().requestFactory(factory)).build();
            var outputSchema = new ObjectMapper().readTree(new BeanOutputConverter<>(RecordFeedbackContent.class).getJsonSchema());
            constrainOutput(outputSchema);
            var schemaMap = new ObjectMapper().convertValue(outputSchema, Map.class);
            var runtimeOptions = new LinkedHashMap<String, Object>(Map.of("chatModel",chatModel(),"ollamaUrl",ollamaUrl(),"temperature",0,"seed",42,"context",8192,
                       "maxOutputTokens",1024,"modelTimeoutSeconds",240,"embeddingModel","bge-m3","embeddingTimeoutSeconds",240,"format",schemaMap));
            try (var input = RecordFeedbackPromptBuilder.class.getResourceAsStream("RecordFeedbackPromptBuilder.class")) {
                runtimeOptions.put("promptBuilderClassSha256", HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(input.readAllBytes())));
            }
            new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(root().resolve("runtime-options.json").toFile(), runtimeOptions);
            var model = OllamaChatModel.builder().ollamaApi(api)
                .defaultOptions(OllamaChatOptions.builder().model(chatModel()).temperature(0.0).seed(42)
                    .numCtx(8192).numPredict(1024).format(schemaMap).keepAlive("0s").build())
                .retryTemplate(org.springframework.retry.support.RetryTemplate.builder().maxAttempts(1).build()).build();
            return new ChatModel() {
                @Override public ChatOptions getDefaultOptions() { return model.getDefaultOptions(); }
                @Override public ChatResponse call(Prompt prompt) {
                    long started = System.nanoTime();
                    trace("prompt", Map.of("messages", prompt.getInstructions().stream()
                        .map(message -> Map.of("role", message.getMessageType().getValue(), "text", message.getText())).toList()));
                    try {
                        var response = model.call(prompt);
                        trace("response", Map.of("seconds", (System.nanoTime()-started)/1e9, "text", response.getResult().getOutput().getText(), "metadata", response.getMetadata()));
                        return response;
                    } catch (RuntimeException failure) {
                        trace("model-failure", Map.of("seconds", (System.nanoTime()-started)/1e9, "type", failure.getClass().getSimpleName()));
                        throw failure;
                    }
                }
            };
        }
        @Bean OllamaEmbeddingModel labEmbeddingModel() {
            var factory = new SimpleClientHttpRequestFactory(); factory.setConnectTimeout(5000); factory.setReadTimeout(240000);
            var api = OllamaApi.builder().baseUrl(ollamaUrl())
                .restClientBuilder(RestClient.builder().requestFactory(factory)).build();
            return OllamaEmbeddingModel.builder().ollamaApi(api)
                .defaultOptions(OllamaEmbeddingOptions.builder().model("bge-m3").truncate(false).build()).build();
        }
        // 기존 출력 검증의 길이/개수 조건을 모델의 생성 문법에도 전달한다. 결과 검증기는 그대로 실행한다.
        static void constrainOutput(com.fasterxml.jackson.databind.JsonNode node) {
            if (node.isObject()) {
                var properties = node.path("properties");
                if (properties.path("text").isObject()) {
                    var text = (com.fasterxml.jackson.databind.node.ObjectNode)properties.path("text");
                    text.put("minLength",15);text.put("maxLength",60);
                    // pattern이 있는 문자열은 일부 문법 변환기에서 길이 키워드보다 우선한다.
                    // 길이도 같은 식에 포함해 기존 검증 범위 안에서만 생성하게 한다.
                    text.put("pattern","^[가-힣0-9 .,!?~℃㎝%·:()（）/-]{14,58}요[.!?…。]?$");
                }
                if (properties.path("nextActions").isObject()) {
                    var actions = (com.fasterxml.jackson.databind.node.ObjectNode)properties.path("nextActions");
                    actions.put("minItems",2);actions.put("maxItems",3);
                }
                if (properties.path("evidenceRefs").isObject())
                    ((com.fasterxml.jackson.databind.node.ObjectNode)properties.path("evidenceRefs")).put("minItems",1);
            }
            node.elements().forEachRemaining(LabConfiguration::constrainOutput);
        }
        @Bean VectorStore labVectorStore(JdbcTemplate jdbc, EmbeddingModel embedding) throws Exception {
            var store = PgVectorStore.builder(jdbc, embedding).dimensions(1024).distanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)
                .indexType(PgVectorStore.PgIndexType.HNSW).initializeSchema(false).maxDocumentBatchSize(1).build();
            store.afterPropertiesSet();
            return new VectorStore() {
                public void add(List<Document> docs) { store.add(docs); }
                public void delete(List<String> ids) { throw new UnsupportedOperationException("실습 문서 삭제 금지"); }
                public void delete(Filter.Expression filter) { throw new UnsupportedOperationException("실습 문서 삭제 금지"); }
                public List<Document> similaritySearch(SearchRequest request) {
                    var docs = store.similaritySearch(request);
                    trace("retrieval", Map.of("query", request.getQuery(), "threshold", request.getSimilarityThreshold(), "topK", request.getTopK(),
                        "documents", docs.stream().map(doc -> Map.of("id", doc.getId(), "score", doc.getScore(), "text", doc.getText(), "metadata", doc.getMetadata())).toList()));
                    return docs;
                }
            };
        }
        @Bean ApplicationRunner seedLab(MemberRepository members, FarmRepository farms, CropRepository crops,
                MemberCropRepository memberCrops, PesticideRepository pesticides, PestRepository pests,
                PasswordEncoder encoder, PlatformTransactionManager manager, ObjectMapper mapper) {
            return args -> {
                var fixture = new TransactionTemplate(manager).execute(status -> {
                    var result = new LinkedHashMap<String, Object>();
                    for (String name : List.of("learner", "other")) {
                        var member = members.findByEmail(name + "@example.invalid");
                        if (member == null) member = members.save(new Member(name + "@example.invalid", encoder.encode(required("MEMBER_PASSWORD"))));
                        result.put(name + "Id", member.getId());
                        if (name.equals("other")) continue;
                        var farm = farms.findFirstByOwnerIdOrderByCreatedAtAsc(member.getId());
                        if (farm == null) {
                            farm = new Farm(member, "합성 실습 농장", "실습용 가상 주소");
                            farm.setLatitude(37.1); farm.setLongitude(128.2); farm = farms.save(farm);
                        }
                        result.put("farmId", farm.getId());
                        var cropIds = new LinkedHashMap<String, Object>();
                        for (var row : List.of(new Crop(308, "천궁", CropUsePartCategory.RHIZOME), new Crop(306, "참당귀", CropUsePartCategory.ROOT_BARK), new Crop(378, "구기자나무", CropUsePartCategory.FRUIT))) {
                            var existing = crops.findByExternalNoIn(List.of(row.getExternalNo()));
                            var crop = existing.isEmpty() ? crops.save(row) : existing.getFirst();
                            if (!crop.getName().equals(row.getName()) || crop.getUsePartCategory() != row.getUsePartCategory()) throw new IllegalStateException("실습 작물 분류 불일치");
                            if (!memberCrops.existsByMemberIdAndFarmIdAndCropId(member.getId(), farm.getId(), crop.getId())) memberCrops.save(new MemberCrop(member, farm, crop));
                            cropIds.put(crop.getName(), crop.getId());
                        }
                        result.put("crops", cropIds);
                    }
                    var pesticide = pesticides.findByItemNameAndBrandName("합성 실습 품목", "합성 실습 제품(실제 사용 금지)");
                    if (pesticide == null) pesticide = pesticides.save(new Pesticide("합성 실습 품목", "합성 실습 제품(실제 사용 금지)"));
                    var pest = pests.findByName("합성 실습 병해충(실제 진단 아님)");
                    if (pest == null) pest = pests.save(new Pest("합성 실습 병해충(실제 진단 아님)"));
                    result.put("pesticideId", pesticide.getId()); result.put("pestId", pest.getId());
                    return result;
                });
                mapper.writerWithDefaultPrettyPrinter().writeValue(root().resolve("fixtures.json").toFile(), fixture);
            };
        }
    }
}
