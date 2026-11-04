package com.chamchamcham.application.coaching.reportfeedback.generation;

import com.chamchamcham.domain.farming.WorkType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.*;
import org.junit.jupiter.api.DisplayName;
import com.chamchamcham.application.coaching.reportfeedback.generation.ReportFeedbackContextKt;

class ReportFeedbackContextFingerprintTest {
    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();
    private final ReportFeedbackContextFingerprint fingerprint = new ReportFeedbackContextFingerprint(objectMapper);

    @Test
    @DisplayName("map insertion order does not change the context fingerprint")
    void map_insertion_order_does_not_change_the_context_fingerprint() throws Exception {
        var first = context(map(new AbstractMap.SimpleEntry<>("count", 2), new AbstractMap.SimpleEntry<>("amount", 3)), java.util.Arrays.asList(record(UUID.fromString("00000000-0000-0000-0000-000000000001"))));
        var second = context(map(new AbstractMap.SimpleEntry<>("amount", 3), new AbstractMap.SimpleEntry<>("count", 2)), java.util.Arrays.asList(record(UUID.fromString("00000000-0000-0000-0000-000000000001"))));
        assertThat(fingerprint.calculate(first)).isEqualTo(fingerprint.calculate(second));
    }

    @Test
    @DisplayName("nested snapshot map insertion order does not change the fingerprint")
    void nested_snapshot_map_insertion_order_does_not_change_the_fingerprint() throws Exception {
        var first = map(new AbstractMap.SimpleEntry<>("report", map(new AbstractMap.SimpleEntry<>("statistics", map(new AbstractMap.SimpleEntry<>("count", 2), new AbstractMap.SimpleEntry<>("amount", 3))), new AbstractMap.SimpleEntry<>("id", "report-1"))), new AbstractMap.SimpleEntry<>("records", java.util.Arrays.asList(map(new AbstractMap.SimpleEntry<>("memo", "점적관수"), new AbstractMap.SimpleEntry<>("id", "record-1")))));
        var second = map(new AbstractMap.SimpleEntry<>("records", java.util.Arrays.asList(map(new AbstractMap.SimpleEntry<>("id", "record-1"), new AbstractMap.SimpleEntry<>("memo", "점적관수")))), new AbstractMap.SimpleEntry<>("report", map(new AbstractMap.SimpleEntry<>("id", "report-1"), new AbstractMap.SimpleEntry<>("statistics", map(new AbstractMap.SimpleEntry<>("amount", 3), new AbstractMap.SimpleEntry<>("count", 2))))));
        assertThat(fingerprint.calculate(first)).isEqualTo(fingerprint.calculate(second));
    }

    @Test
    @DisplayName("scientific plain trailing zero and integral numeric forms share a fingerprint")
    void scientific_plain_trailing_zero_and_integral_numeric_forms_share_a_fingerprint() throws Exception {
        var fingerprints = java.util.Arrays.asList(new BigDecimal("1E+3"), new BigDecimal("1000"), new BigDecimal("1000.0"), 1000, 1000L).stream().map(amount -> fingerprint.calculate(map(new AbstractMap.SimpleEntry<>("amount", amount)))).toList();
        assertThat(fingerprints).containsOnly(fingerprints.getFirst());
    }

    @Test
    @DisplayName("context matches its persisted untyped snapshot after numeric json round trip")
    void context_matches_its_persisted_untyped_snapshot_after_numeric_json_round_trip() throws Exception {
        var currentContext = context(map(new AbstractMap.SimpleEntry<>("recordCount", 1), new AbstractMap.SimpleEntry<>("amount", new BigDecimal("1E+3")), new AbstractMap.SimpleEntry<>("ratio", new BigDecimal("1.2300"))), java.util.Arrays.asList(record(UUID.fromString("00000000-0000-0000-0000-000000000001"))));
        Map<String, Object> snapshot = objectMapper.convertValue(currentContext, SNAPSHOT_TYPE);
        Map<String, Object> persistedSnapshot = objectMapper.readValue(objectMapper.writeValueAsBytes(snapshot), SNAPSHOT_TYPE);
        assertThat(fingerprint.calculate(currentContext)).isEqualTo(fingerprint.calculate(persistedSnapshot));
    }

    @Test
    @DisplayName("array order changes the context fingerprint")
    void array_order_changes_the_context_fingerprint() throws Exception {
        var firstRecord = record(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        var secondRecord = record(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        var first = context(map(new AbstractMap.SimpleEntry<>("recordCount", 2)), java.util.Arrays.asList(firstRecord, secondRecord));
        var second = context(map(new AbstractMap.SimpleEntry<>("recordCount", 2)), java.util.Arrays.asList(secondRecord, firstRecord));
        assertThat(fingerprint.calculate(first)).isNotEqualTo(fingerprint.calculate(second));
    }

    @Test
    @DisplayName("fingerprint is lowercase sha256 hex")
    void fingerprint_is_lowercase_sha256_hex() throws Exception {
        assertThat(fingerprint.calculate(context(map(new AbstractMap.SimpleEntry<>("recordCount", 2)), java.util.Arrays.asList(record(UUID.fromString("00000000-0000-0000-0000-000000000001")))))).matches("^[0-9a-f]{64}$");
    }
    private ReportFeedbackContext context(Map<String, Object> statistics, List<ReportFeedbackRecord> records) {
        return new ReportFeedbackContext(ReportFeedbackContextKt.REPORT_FEEDBACK_CONTEXT_SCHEMA_VERSION, WorkType.WATERING, new ReportFeedbackReport(UUID.fromString("00000000-0000-0000-0000-000000000101"), "약초농장", "황기", LocalDateTime.of(2026, 3, 1, 9, 0), LocalDateTime.of(2026, 7, 1, 9, 0), 7, statistics), records, null, List.of(), List.of());
    }
    private ReportFeedbackRecord record(UUID id) {
        return new ReportFeedbackRecord(id, LocalDateTime.of(2026, 4, 1, 9, 0), WorkType.WATERING, "점적관수", map(new AbstractMap.SimpleEntry<>("method", "DRIP"), new AbstractMap.SimpleEntry<>("amount", "LOW")));
    }
    private static final TypeReference<Map<String, Object>> SNAPSHOT_TYPE = new TypeReference<>() {};
    @SafeVarargs
    private static <K> Map<K, Object> map(Map.Entry<K, ?>... entries) {
        Map<K, Object> result = new LinkedHashMap<>();
        for (var entry : entries) result.put(entry.getKey(), entry.getValue());
        return result;
    }

}
