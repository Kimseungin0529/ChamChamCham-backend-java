package com.chamchamcham.api.farming;

import com.chamchamcham.ApiApplication;
import com.chamchamcham.api.rag.client.OpenClawChatModel;
import com.chamchamcham.application.coaching.recordfeedback.generation.*;
import com.chamchamcham.application.coaching.recordfeedback.lifecycle.RecordFeedbackPreparationListener;
import com.chamchamcham.application.report.FarmingCycleReportProjectionService;
import com.chamchamcham.domain.crop.*;
import com.chamchamcham.domain.farm.*;
import com.chamchamcham.domain.farming.*;
import com.chamchamcham.domain.member.*;
import com.chamchamcham.domain.pesticide.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.*;
import org.springframework.http.MediaType;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import java.math.BigDecimal;
import java.nio.file.*;
import java.sql.DriverManager;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** 실제 HTTP 컨트롤러·서비스·PostgreSQL·AI 입력 변환을 확인한다. 외부 인증/AI는 범위 밖이다. */
@SpringBootTest(classes = ApiApplication.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Tag("local-postgres")
@EnabledIfEnvironmentVariable(named = "RUN_LOCAL_RECORD_VERIFY", matches = "true")
class FarmingRecordPostgresVerificationTest {
    private static final ObjectMapper FIXTURE_MAPPER = new ObjectMapper();
    private static final List<Map<String, Object>> EVIDENCE = new ArrayList<>();
    private static final AtomicInteger CROP_NUMBER = new AtomicInteger(900000);
    private static final String MEMO = "로컬 검증용 합성 기록입니다. 실제 농업 작업이나 농약 사용을 권장하는 자료가 아닙니다.";
    private static final Map<String, String> TABLES = Map.of(
        "planting", "planting_record", "watering", "watering_record", "fertilizing", "fertilizing_record",
        "pestControl", "pest_control_record", "weeding", "weeding_record", "harvest", "harvest_record");

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;
    @Autowired MemberRepository members;
    @Autowired FarmRepository farms;
    @Autowired CropRepository crops;
    @Autowired MemberCropRepository memberCrops;
    @Autowired PesticideRepository pesticides;
    @Autowired PestRepository pests;
    @Autowired RecordFeedbackContextAssembler assembler;
    @Autowired RecordFeedbackPromptBuilder promptBuilder;
    @MockBean FarmingCycleReportProjectionService reportProjection;
    @MockBean RecordFeedbackPreparationListener preparation;
    @MockBean RecordFeedbackWeatherPort weather;
    @MockBean OpenClawChatModel chat;
    @MockBean EmbeddingModel embeddings;
    @MockBean VectorStore vectors;
    @MockBean Clock clock;
    private UUID memberId, farmId, cropId, pesticideId, pestId;

    @DynamicPropertySource
    static void dedicatedDatabase(DynamicPropertyRegistry properties) throws Exception {
        String url = Objects.requireNonNull(System.getenv("RECORD_VERIFY_JDBC_URL"));
        if (!url.matches("jdbc:postgresql://127\\.0\\.0\\.1:[0-9]+/ccc_record_verify")) {
            throw new IllegalStateException("Only the dedicated loopback verification database is allowed");
        }
        String user = Objects.requireNonNull(System.getenv("RECORD_VERIFY_DB_USER"));
        String password = Objects.requireNonNull(System.getenv("RECORD_VERIFY_DB_PASSWORD"));
        String token = Objects.requireNonNull(System.getenv("RECORD_VERIFY_TOKEN"));
        // Hibernate create-drop보다 먼저 이 실행이 만든 독립 DB임을 확인한다.
        try (var connection = DriverManager.getConnection(url, user, password);
             var statement = connection.createStatement();
             var result = statement.executeQuery("select token from local_verification_guard")) {
            if (!result.next() || !token.equals(result.getString(1)) || result.next()) {
                throw new IllegalStateException("Verification database ownership marker mismatch");
            }
        }
        properties.add("spring.datasource.url", () -> url);
        properties.add("spring.datasource.username", () -> user);
        properties.add("spring.datasource.password", () -> password);
        properties.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        properties.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        properties.add("spring.jpa.properties.hibernate.jdbc.time_zone", () -> "Asia/Seoul");
    }

    @BeforeEach
    void fixtures() {
        when(clock.instant()).thenReturn(Instant.parse("2026-11-13T00:00:00Z"));
        when(clock.getZone()).thenReturn(ZoneId.of("Asia/Seoul"));
        when(weather.fetch(anyDouble(), anyDouble(), eq(7))).thenReturn(new RecordFeedbackLiveWeather(
            new RecordFeedbackCurrentWeather(20, "맑음", LocalDateTime.of(2026, 11, 13, 9, 0)), List.of(), "SYNTHETIC_VERIFICATION"));
        var member = members.save(new Member("verify-" + UUID.randomUUID() + "@example.invalid", null));
        memberId = member.getId();
        var farm = new Farm(member, "가상 검증농장", "검증용 가상 주소");
        farm.setLatitude(37.1); farm.setLongitude(128.2);
        farm = farms.save(farm); farmId = farm.getId();
        var crop = crops.save(new Crop(CROP_NUMBER.incrementAndGet(), "참당귀", CropUsePartCategory.ROOT_BARK));
        cropId = crop.getId();
        memberCrops.save(new MemberCrop(member, farm, crop));
        var pesticide = pesticides.findByItemNameAndBrandName("검증용 가상품목", "검증용 가상제품");
        if (pesticide == null) pesticide = pesticides.save(new Pesticide("검증용 가상품목", "검증용 가상제품"));
        pesticideId = pesticide.getId();
        var pest = pests.findByName("검증용 가상병해충");
        if (pest == null) pest = pests.save(new Pest("검증용 가상병해충"));
        pestId = pest.getId();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(memberId.toString(), null, List.of()));
    }

    @AfterEach
    void noExternalCalls() {
        verify(chat, never()).call(any(org.springframework.ai.chat.prompt.Prompt.class));
        verify(embeddings, never()).call(any(org.springframework.ai.embedding.EmbeddingRequest.class));
        verifyNoInteractions(vectors);
        SecurityContextHolder.clearContext();
    }

    @AfterAll
    static void writeEvidence() throws Exception {
        var path = Path.of("build/record-local-verification/comparisons.json");
        Files.createDirectories(path.getParent());
        FIXTURE_MAPPER.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), EVIDENCE);
    }

    static Stream<JsonNode> validCases() throws Exception {
        try (var input = FarmingRecordPostgresVerificationTest.class.getResourceAsStream("/verification/record-cases.json")) {
            var cases = FIXTURE_MAPPER.readTree(Objects.requireNonNull(input));
            var rows = new ArrayList<JsonNode>(); cases.forEach(rows::add); return rows.stream();
        }
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("validCases")
    void preservesAllEightWorkTypesAcrossHttpDatabaseAndPrompt(JsonNode fixture) throws Exception {
        var input = request(fixture);
        var result = roundTrip(fixture.path("id").asText(), input);
        for (var snippet : fixture.path("promptContains")) assertThat(result.prompt()).contains(snippet.asText());
    }

    static Stream<JsonNode> enumCases() throws Exception {
        var originals = validCases().toList();
        var rows = new ArrayList<JsonNode>();
        addEnum(rows, originals.get(0), "seedAmountUnit", SeedAmountUnit.values());
        addEnum(rows, originals.get(1), "seedlingUnit", SeedlingUnit.values());
        addEnum(rows, originals.get(1), "propagationMethod", PropagationMethod.values());
        addEnum(rows, originals.get(2), "irrigationAmount", IrrigationAmount.values());
        addEnum(rows, originals.get(2), "irrigationMethod", IrrigationMethod.values());
        addEnum(rows, originals.get(3), "amountUnit", FertilizerAmountUnit.values());
        addEnum(rows, originals.get(3), "applicationMethod", FertilizingMethod.values());
        addEnum(rows, originals.get(4), "pesticideAmountUnit", PesticideAmountUnit.values());
        addEnum(rows, originals.get(4), "totalSprayAmountUnit", SprayAmountUnit.values());
        addEnum(rows, originals.get(5), "weedingMethod", WeedingMethod.values());
        addEnum(rows, originals.get(7), "medicinalPart", CropUsePartCategory.values());
        addEnum(rows, originals.get(7), "harvestSource", HarvestSource.values());
        return rows.stream();
    }

    private static void addEnum(List<JsonNode> rows, JsonNode base, String field, Enum<?>[] values) {
        JsonNode expected;
        try (var input = FarmingRecordPostgresVerificationTest.class.getResourceAsStream("/verification/enum-prompt-expectations.json")) {
            expected = FIXTURE_MAPPER.readTree(Objects.requireNonNull(input));
        } catch (java.io.IOException exception) { throw new java.io.UncheckedIOException(exception); }
        for (var value : values) {
            var fixture = (ObjectNode) base.deepCopy();
            String snippet = expected.path(field).path(value.name()).asText();
            if (snippet.isEmpty()) throw new IllegalStateException("Missing independent prompt expectation: " + field + "/" + value.name());
            fixture.put("expectedEnumPrompt", snippet);
            fixture.put("id", "enum-" + field + "-" + value.name());
            ((ObjectNode) fixture.path("detail")).put(field, value.name());
            rows.add(fixture);
        }
    }

    @ParameterizedTest(name = "enum {index}: {0}")
    @MethodSource("enumCases")
    void preservesEveryAllowedDetailEnum(JsonNode fixture) throws Exception {
        var result = roundTrip(fixture.path("id").asText(), request(fixture));
        assertThat(result.prompt()).contains(fixture.path("expectedEnumPrompt").asText());
    }

    @Test
    void optionalDetailsKeepNullsAndAbsentRowsDistinct() throws Exception {
        roundTrip("watering-absent", request("WATERING", null, null));
        roundTrip("watering-empty", request("WATERING", "watering", "{}"));
        roundTrip("watering-one-field", request("WATERING", "watering", "{\"irrigationAmount\":\"LOW\"}"));
        roundTrip("weeding-absent", request("WEEDING", null, null));
        roundTrip("weeding-empty", request("WEEDING", "weeding", "{}"));
        var seed = request(validCases().findFirst().orElseThrow());
        ((ObjectNode) seed.path("planting")).remove("seedAmountUnit");
        var seeded = roundTrip("seed-unit-absent", seed);
        assertThat(seeded.response().path("planting").path("seedAmountUnit").isNull()).isTrue();
        assertThat(seeded.prompt()).contains("씨앗 양: 12.5000").doesNotContain("씨앗 양: 12.5000그램");
        var pest = request(validCases().toList().get(4));
        ((ObjectNode) pest.path("pestControl")).remove("pestId");
        var pestResult = roundTrip("pest-absent", pest);
        assertThat(pestResult.response().path("pestControl").path("pestName").isNull()).isTrue();
        assertThat(pestResult.prompt()).doesNotContain("관리 대상:");
        var harvest = request(validCases().toList().get(7));
        ((ObjectNode) harvest.path("harvest")).put("harvestAmountUnknown", true).putNull("harvestAmount").remove("medicinalPart");
        ((ObjectNode) harvest.path("harvest")).remove("harvestSource");
        var harvested = roundTrip("harvest-unknown-default-source", harvest);
        assertThat(harvested.response().path("harvest").path("amountUnknown").asBoolean()).isTrue();
        assertThat(harvested.response().path("harvest").path("harvestSource").asText()).isEqualTo("CULTIVATED");
        assertThat(harvested.prompt()).doesNotContain("수확량:", "수확한 부위:");
    }

    @Test
    void suppliedHarvestPartCanDifferFromCropDefault() throws Exception {
        var input = request(validCases().toList().get(7));
        ((ObjectNode) input.path("harvest")).put("medicinalPart", "LEAF").put("isLastHarvest", true);
        var result = roundTrip("harvest-part-differs-from-crop", input);
        assertThat(result.prompt()).contains("쓰는 부위: 뿌리와 껍질", "수확한 부위: 잎", "마지막 수확: 네");
    }

    @Test
    void existingIdsDoNotImplyRegisteredPesticideApplication() throws Exception {
        assertThat(jdbc.queryForObject("select count(*) from pesticide_application where pesticide_id=? and pest_id=?", Long.class, pesticideId, pestId)).isZero();
        roundTrip("pesticide-unregistered-combination-is-accepted", request(validCases().toList().get(4)));
    }

    @Test
    void updatesReplaceDetailsAndRemoveOmittedOptionalDetail() throws Exception {
        var original = request(validCases().toList().get(2));
        UUID id = create(original);
        var changed = original.deepCopy(); ((ObjectNode) changed.path("watering")).put("irrigationAmount", "LOW");
        send("PATCH", "/api/v1/farming-records/" + id, changed, 200);
        observe("update-same-type", changed, id);
        changed.remove("watering");
        var capturedInput = (JsonNode) EVIDENCE.stream().filter(it -> it.get("case").equals("update-same-type")).findFirst().orElseThrow().get("input");
        assertThat(capturedInput.has("watering")).as("earlier evidence must be an immutable input snapshot").isTrue();
        send("PATCH", "/api/v1/farming-records/" + id, changed, 200);
        observe("update-omitted-optional-detail", changed, id);
        var harvest = request(validCases().toList().get(7));
        send("PATCH", "/api/v1/farming-records/" + id, harvest, 200);
        observe("update-work-type", harvest, id);
        var invalid = harvest.deepCopy(); ((ObjectNode) invalid.path("harvest")).remove("growthPeriod");
        var before = rowCounts();
        send("PATCH", "/api/v1/farming-records/" + id, invalid, 400);
        assertThat(rowCounts()).isEqualTo(before);
        observe("invalid-update-keeps-previous-data", harvest, id);
    }

    @Test
    void failureAfterOldDetailDeletionRollsBackTheWholeUpdate() throws Exception {
        var original = request(validCases().toList().get(2));
        UUID id = create(original);
        var beforeCounts = rowCounts();
        var beforeCommon = jdbc.queryForObject("select to_jsonb(r)::text from farming_record r where id=?", String.class, id);
        var beforeDetail = jdbc.queryForObject("select to_jsonb(r)::text from watering_record r where record_id=?", String.class, id);
        var beforeFeedback = jdbc.queryForList("select to_jsonb(r)::text from record_feedback r where record_id=? order by source_revision", String.class, id);
        var invalid = request(validCases().toList().get(4));
        invalid.put("memo", "수정 실패 이후 원래 기록과 작업 상세가 모두 복원되는지 확인하는 합성 메모입니다.");
        invalid.put("weatherTemperature", 25).put("workedAt", "2026-11-12T08:30:00");
        ((ObjectNode) invalid.path("pestControl")).put("pesticideId", "00000000-0000-0000-0000-000000009991");
        var response = send("PATCH", "/api/v1/farming-records/" + id, invalid, 404);
        assertThat(response.path("error").path("code").asText()).isEqualTo("PESTICIDE_001");
        assertThat(rowCounts()).isEqualTo(beforeCounts);
        assertThat(jdbc.queryForObject("select to_jsonb(r)::text from farming_record r where id=?", String.class, id)).isEqualTo(beforeCommon);
        assertThat(jdbc.queryForObject("select to_jsonb(r)::text from watering_record r where record_id=?", String.class, id)).isEqualTo(beforeDetail);
        assertThat(jdbc.queryForList("select to_jsonb(r)::text from record_feedback r where record_id=? order by source_revision", String.class, id)).isEqualTo(beforeFeedback);
        observe("service-failed-update-restores-original", original, id);
        EVIDENCE.add(Map.of("case", "service-failed-update", "input", invalid.deepCopy(), "response", response,
            "outcome", "EXPECTED_REJECTION", "status", 404, "previousCommonDetailAndFeedbackUnchanged", true));
    }

    @Test
    void unrelatedDetailIsValidatedButNotPersisted() throws Exception {
        var input = request(validCases().toList().get(2));
        input.set("fertilizing", request(validCases().toList().get(3)).path("fertilizing"));
        UUID id = create(input);
        assertThat(jdbc.queryForObject("select count(*) from fertilizing_record where record_id=?", Long.class, id)).isZero();
        var response = send("GET", "/api/v1/farming-records/" + id, null, 200).path("data");
        assertThat(response.path("fertilizing").isNull()).isTrue();
        var context = assembler.assemble(memberId, id, 1L);
        assertThat(context.getRecord().getDetail()).isInstanceOf(WateringFeedbackDetail.class);
        String prompt = promptBuilder.build(context, List.of(), List.of()).getUser();
        assertThat(prompt).doesNotContain("검증용 가상비료");
        EVIDENCE.add(Map.of("case", "unrelated-valid-detail-ignored", "input", input.deepCopy(), "response", response,
            "aiContext", mapper.valueToTree(context), "prompt", prompt, "outcome", "OBSERVED_IGNORED_DETAIL"));
        ((ObjectNode) input.path("fertilizing")).put("materialName", " ");
        var counts = rowCounts();
        var rejected = send("POST", "/api/v1/farming-records", input, 400);
        assertThat(rowCounts()).isEqualTo(counts);
        EVIDENCE.add(Map.of("case", "unrelated-invalid-detail-rejected", "input", input.deepCopy(), "response", rejected,
            "status", 400, "outcome", "EXPECTED_REJECTION"));
    }

    @Test
    void anotherMemberCannotReadOrUpdateTheRecord() throws Exception {
        var input = request(validCases().toList().get(2)); UUID id = create(input);
        var other = members.save(new Member("other-" + UUID.randomUUID() + "@example.invalid", null));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(other.getId().toString(), null, List.of()));
        var before = rowCounts();
        assertThat(send("GET", "/api/v1/farming-records/" + id, null, 403).path("error").path("code").asText()).isEqualTo("FARMING_004");
        assertThat(send("PATCH", "/api/v1/farming-records/" + id, input, 403).path("error").path("code").asText()).isEqualTo("FARMING_004");
        assertThat(send("POST", "/api/v1/farming-records", input, 404).path("error").path("code").asText()).isEqualTo("FARM_001");
        assertThat(rowCounts()).isEqualTo(before);
        EVIDENCE.add(Map.of("case", "ownership", "outcome", "EXPECTED_REJECTION", "readStatus", 403, "updateStatus", 403, "createStatus", 404));
    }

    private Map<String, Long> rowCounts() {
        var counts = new TreeMap<String, Long>();
        var tables = new ArrayList<>(TABLES.values()); tables.add("farming_record"); tables.add("record_feedback");
        for (var table : tables) counts.put(table, jdbc.queryForObject("select count(*) from " + table, Long.class));
        return counts;
    }

    static Stream<JsonNode> rejectionCases() throws Exception {
        try (var input = FarmingRecordPostgresVerificationTest.class.getResourceAsStream("/verification/record-rejections.json")) {
            var rows = new ArrayList<JsonNode>(); FIXTURE_MAPPER.readTree(Objects.requireNonNull(input)).forEach(rows::add); return rows.stream();
        }
    }

    @ParameterizedTest(name = "reject {index}: {0}")
    @MethodSource("rejectionCases")
    void rejectsInvalidRequestsWithoutPartialPersistence(JsonNode fixture) throws Exception {
        var base = validCases().filter(it -> it.path("id").equals(fixture.path("base"))).findFirst().orElseThrow();
        var input = request(base);
        for (var mutation : fixture.path("mutations")) {
            String path = mutation.path("path").asText(); int slash = path.lastIndexOf('/');
            var parent = slash == 0 ? input : (ObjectNode) input.at(path.substring(0, slash));
            String key = path.substring(slash + 1);
            if (mutation.path("op").asText().equals("remove")) parent.remove(key);
            else parent.set(key, mutation.path("value"));
        }
        var before = rowCounts();
        var result = send("POST", "/api/v1/farming-records", input, fixture.path("status").asInt());
        assertThat(result.path("success").asBoolean()).isFalse();
        assertThat(result.path("error").path("code")).isEqualTo(fixture.path("code"));
        assertThat(rowCounts()).as("failed request must roll back parent, details and feedback").isEqualTo(before);
        EVIDENCE.add(Map.of("case", fixture.path("id").asText(), "input", input, "response", result,
            "status", fixture.path("status").asInt(), "outcome", "EXPECTED_REJECTION", "countsUnchanged", true));
    }

    @Test
    void minimumAndMemoBoundariesAreAccepted() throws Exception {
        var seed = request(validCases().findFirst().orElseThrow());
        ((ObjectNode) seed.path("planting")).put("seedAmount", new BigDecimal("0.01"));
        seed.put("memo", "가".repeat(30)); roundTrip("seed-minimum-memo30", seed);
        var harvest = request(validCases().toList().get(7));
        ((ObjectNode) harvest.path("harvest")).put("harvestAmount", new BigDecimal("0.01")).put("growthPeriod", 1).put("isLastHarvest", true);
        harvest.put("memo", "가".repeat(500)); roundTrip("harvest-minimum-memo500", harvest);
    }

    @Test
    void postgresRoundsFiveDecimalPlacesRatherThanPreservingInputExactly() throws Exception {
        var input = request(validCases().toList().get(3));
        ((ObjectNode) input.path("fertilizing")).put("amount", new BigDecimal("1.23456"));
        UUID id = create(input);
        var response = send("GET", "/api/v1/farming-records/" + id, null, 200).path("data");
        var stored = jdbc.queryForObject("select amount from fertilizing_record where record_id=?", BigDecimal.class, id);
        assertThat(stored).isEqualByComparingTo("1.2346");
        assertThat(response.path("fertilizing").path("amount").decimalValue()).isEqualByComparingTo("1.2346");
        var context = assembler.assemble(memberId, id, 1L);
        assertThat(((FertilizingFeedbackDetail) context.getRecord().getDetail()).getAmount()).isEqualByComparingTo("1.2346");
        String prompt = promptBuilder.build(context, List.of(), List.of()).getUser();
        assertThat(prompt).contains("거름 양: 1.2346그램");
        EVIDENCE.add(Map.of("case", "decimal-rounding-observation", "input", input, "databaseAmount", stored.toPlainString(),
            "response", response, "aiContext", mapper.valueToTree(context), "prompt", prompt, "outcome", "OBSERVED_ROUNDING"));
    }

    @Test
    void postgresReallyIsTheDatabase() {
        assertThat(jdbc.queryForObject("select version()", String.class)).startsWith("PostgreSQL 16.");
    }

    private ObjectNode request(JsonNode fixture) {
        var request = mapper.createObjectNode();
        request.put("farmId", farmId.toString()).put("cropId", cropId.toString())
            .put("workType", fixture.path("workType").asText()).put("workedAt", "2026-11-13T08:30:00")
            .put("weatherCondition", "맑음").put("weatherTemperature", 20).put("memo", MEMO);
        if (!fixture.path("detailKey").isNull()) {
            var detail = (ObjectNode) fixture.path("detail").deepCopy();
            if (detail.has("pesticideId")) detail.put("pesticideId", pesticideId.toString());
            if (detail.has("pestId")) detail.put("pestId", pestId.toString());
            request.set(fixture.path("detailKey").asText(), detail);
        }
        return request;
    }

    private ObjectNode request(String workType, String key, String detail) throws Exception {
        var fixture = mapper.createObjectNode().put("workType", workType);
        if (key == null) fixture.putNull("detailKey");
        else { fixture.put("detailKey", key); fixture.set("detail", mapper.readTree(detail)); }
        return request(fixture);
    }

    private JsonNode send(String method, String path, ObjectNode input, int status) throws Exception {
        var builder = switch (method) { case "POST" -> post(path); case "PATCH" -> patch(path); default -> get(path); };
        if (input != null) builder.contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsBytes(input));
        var result = mvc.perform(builder).andReturn();
        assertThat(result.getResponse().getStatus()).as("%s %s response=%s", method, path, result.getResponse().getContentAsString()).isEqualTo(status);
        return mapper.readTree(result.getResponse().getContentAsByteArray());
    }

    private UUID create(ObjectNode input) throws Exception {
        return UUID.fromString(send("POST", "/api/v1/farming-records", input, 200).path("data").path("id").asText());
    }

    private Observation roundTrip(String name, ObjectNode input) throws Exception {
        return observe(name, input, create(input));
    }

    private Observation observe(String name, ObjectNode input, UUID id) throws Exception {
        var response = send("GET", "/api/v1/farming-records/" + id, null, 200).path("data");
        assertThat(response.path("workType").asText()).isEqualTo(input.path("workType").asText());
        assertThat(response.path("memo").asText()).isEqualTo(input.path("memo").asText());
        var dbCommon = mapper.readTree(jdbc.queryForObject("select to_jsonb(r)::text from farming_record r where id=?", String.class, id));
        assertThat(dbCommon.path("memo").asText()).isEqualTo(input.path("memo").asText());
        assertThat(dbCommon.path("work_type").asText()).isEqualTo(input.path("workType").asText());
        for (var field : Map.of("farmId", "farm_id", "cropId", "crop_id", "workedAt", "worked_at",
                "weatherCondition", "weather_condition", "weatherTemperature", "weather_temperature").entrySet()) {
            assertValue(input.path(field.getKey()), response.path(field.getKey()), "response common " + field.getKey());
            assertValue(input.path(field.getKey()), dbCommon.path(field.getValue()), "database common " + field.getValue());
        }
        var context = assembler.assemble(memberId, id, 1L);
        assertThat(context.getRecord().getMemo()).isEqualTo(input.path("memo").asText());
        assertThat(context.getRecord().getWorkedAt()).isEqualTo(LocalDateTime.parse(input.path("workedAt").asText()));
        assertThat(context.getRecord().getRecordedWeatherCondition()).isEqualTo(input.path("weatherCondition").asText());
        assertThat(context.getRecord().getRecordedTemperatureC()).isEqualTo(input.path("weatherTemperature").asInt());
        assertThat(context.getFarm().getFarmId().toString()).isEqualTo(input.path("farmId").asText());
        assertThat(context.getCrop().getCropId().toString()).isEqualTo(input.path("cropId").asText());
        var contextJson = mapper.valueToTree(context);
        String prompt = promptBuilder.build(context, List.of(), List.of()).getUser();
        assertThat(prompt).contains(input.path("memo").asText(), "현재 날씨: 맑음, 20도");
        var dbDetails = mapper.createObjectNode();
        for (var entry : TABLES.entrySet()) {
            var databaseRows = jdbc.queryForList("select to_jsonb(d)::text from " + entry.getValue() + " d where record_id=?", String.class, id);
            boolean supplied = input.has(entry.getKey());
            assertThat(databaseRows).as("%s detail rows", entry.getKey()).hasSize(supplied ? 1 : 0);
            if (supplied) {
                var database = mapper.readTree(databaseRows.getFirst()); dbDetails.set(entry.getKey(), database);
                var expected = input.path(entry.getKey());
                var actual = response.path(entry.getKey());
                for (var field : expected.properties()) {
                    String key = field.getKey();
                    if (key.equals("harvestAmountUnknown")) {
                        assertThat(actual.path("amountUnknown").asBoolean()).isEqualTo(field.getValue().asBoolean());
                        assertThat(database.path("harvest_amount").isNull()).isEqualTo(field.getValue().asBoolean());
                    } else {
                        assertValue(field.getValue(), actual.path(key), "response " + key);
                        String column = key.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
                        assertValue(field.getValue(), database.path(column), "database " + column);
                        if (!key.equals("pesticideId") && !key.equals("pestId")) {
                            assertValue(field.getValue(), contextJson.path("record").path("detail").path(key), "AI context " + key);
                        }
                    }
                }
            }
        }
        if (input.has("pestControl")) {
            assertThat(contextJson.path("record").path("detail").path("pesticideName").asText()).isEqualTo("검증용 가상제품");
            assertThat(prompt).contains("약을 섞은 물의 양:");
        }
        var evidence = new LinkedHashMap<String, Object>();
        evidence.put("case", name); evidence.put("input", input.deepCopy()); evidence.put("databaseCommon", dbCommon);
        evidence.put("databaseDetails", dbDetails); evidence.put("response", response);
        evidence.put("aiContext", contextJson); evidence.put("prompt", prompt); evidence.put("outcome", "PASS");
        EVIDENCE.add(evidence);
        return new Observation(id, response, prompt);
    }

    private void assertValue(JsonNode expected, JsonNode actual, String name) {
        assertThat(actual.isMissingNode()).as(name + " must be present").isFalse();
        if (expected.isNumber()) assertThat(actual.decimalValue()).as(name).isEqualByComparingTo(expected.decimalValue());
        else assertThat(actual).as(name).isEqualTo(expected);
    }

    private record Observation(UUID id, JsonNode response, String prompt) {}
}
