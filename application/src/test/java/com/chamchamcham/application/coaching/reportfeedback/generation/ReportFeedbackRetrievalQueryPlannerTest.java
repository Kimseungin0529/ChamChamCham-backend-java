package com.chamchamcham.application.coaching.reportfeedback.generation;

import com.chamchamcham.domain.farming.WorkType;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.*;
import org.junit.jupiter.api.DisplayName;
import com.chamchamcham.application.coaching.recordfeedback.generation.RecordFeedbackContextKt;
import com.chamchamcham.application.coaching.reportfeedback.generation.ReportFeedbackContextKt;

class ReportFeedbackRetrievalQueryPlannerTest {

    @Test
    @DisplayName("watering plan uses period and method vocabulary instead of raw counts")
    void watering_plan_uses_period_and_method_vocabulary_instead_of_raw_counts() throws Exception {
        var queries = new ReportFeedbackRetrievalQueryPlanner().plan(context(WorkType.WATERING, map(new AbstractMap.SimpleEntry<>("recordCount", 4), new AbstractMap.SimpleEntry<>("averageIntervalDays", 3.5), new AbstractMap.SimpleEntry<>("methodDistribution", java.util.Arrays.asList(map(new AbstractMap.SimpleEntry<>("code", "DRIP"), new AbstractMap.SimpleEntry<>("label", "점적관수"), new AbstractMap.SimpleEntry<>("count", 4), new AbstractMap.SimpleEntry<>("ratePct", 100)))))));
        assertThat(queries).allMatch(it -> it.contains("황기") && it.contains("관수"));
        assertThat(queries).contains("황기 관수 재배 관리");
        assertThat(queries).anyMatch(it -> it.contains("3월~7월") && it.contains("시기"));
        assertThat(queries).contains("황기 관수 간격");
        assertThat(queries).contains("황기 점적 관수 방법");
        assertThat(String.join(" ", queries)).doesNotContain("4회").doesNotContain("3.5일").doesNotContain("평균간격").doesNotContain("시비").doesNotContain("방제").doesNotContain("수확량");
    }

    @Test
    @DisplayName("pest control plan searches the recorded pest by name")
    void pest_control_plan_searches_the_recorded_pest_by_name() throws Exception {
        var queries = new ReportFeedbackRetrievalQueryPlanner().plan(context(WorkType.PEST_CONTROL, map(new AbstractMap.SimpleEntry<>("recordCount", 2), new AbstractMap.SimpleEntry<>("targets", java.util.Arrays.asList(map(new AbstractMap.SimpleEntry<>("target", "진딧물"), new AbstractMap.SimpleEntry<>("count", 2)))))));
        assertThat(queries).contains("황기 진딧물 방제");
    }

    @Test
    @DisplayName("harvest plan searches the harvested part with document vocabulary")
    void harvest_plan_searches_the_harvested_part_with_document_vocabulary() throws Exception {
        var queries = new ReportFeedbackRetrievalQueryPlanner().plan(context(WorkType.HARVEST, map(new AbstractMap.SimpleEntry<>("recordCount", 1), new AbstractMap.SimpleEntry<>("medicinalParts", java.util.Arrays.asList(map(new AbstractMap.SimpleEntry<>("code", "ROOT_BARK"), new AbstractMap.SimpleEntry<>("count", 1)))))));
        assertThat(queries).contains("황기 뿌리 수확 시기");
        assertThat(String.join(" ", queries)).doesNotContain("ROOT_BARK");
    }

    @Test
    @DisplayName("plan keeps the strongest signals within the query cap")
    void plan_keeps_the_strongest_signals_within_the_query_cap() throws Exception {
        var queries = new ReportFeedbackRetrievalQueryPlanner().plan(context(WorkType.FERTILIZING, map(new AbstractMap.SimpleEntry<>("recordCount", 6), new AbstractMap.SimpleEntry<>("averageIntervalDays", 14.0), new AbstractMap.SimpleEntry<>("materialDistribution", java.util.Arrays.asList(map(new AbstractMap.SimpleEntry<>("code", "액상비료"), new AbstractMap.SimpleEntry<>("label", "액상비료"), new AbstractMap.SimpleEntry<>("count", 4)), map(new AbstractMap.SimpleEntry<>("code", "퇴비"), new AbstractMap.SimpleEntry<>("label", "퇴비"), new AbstractMap.SimpleEntry<>("count", 1)), map(new AbstractMap.SimpleEntry<>("code", "복합비료"), new AbstractMap.SimpleEntry<>("label", "복합비료"), new AbstractMap.SimpleEntry<>("count", 1)))), new AbstractMap.SimpleEntry<>("methodDistribution", java.util.Arrays.asList(map(new AbstractMap.SimpleEntry<>("code", "SOIL"), new AbstractMap.SimpleEntry<>("count", 5)), map(new AbstractMap.SimpleEntry<>("code", "FOLIAR"), new AbstractMap.SimpleEntry<>("count", 1)))))));
        assertThat(queries).hasSizeLessThanOrEqualTo(5);
        assertThat(queries).contains("황기 액상비료 시비");
        assertThat(String.join(" ", queries)).doesNotContain("복합비료");
    }

    @Test
    @DisplayName("unknown detail codes are omitted instead of leaking raw codes")
    void unknown_detail_codes_are_omitted_instead_of_leaking_raw_codes() throws Exception {
        var queries = new ReportFeedbackRetrievalQueryPlanner().plan(context(WorkType.WATERING, map(new AbstractMap.SimpleEntry<>("recordCount", 1), new AbstractMap.SimpleEntry<>("methodDistribution", java.util.Arrays.asList(map(new AbstractMap.SimpleEntry<>("code", "NEW_METHOD"), new AbstractMap.SimpleEntry<>("count", 1)))))));
        assertThat(String.join(" ", queries)).doesNotContain("NEW_METHOD");
    }
    private ReportFeedbackContext context(WorkType workType, Map<String, Object> statistics) {
        return new ReportFeedbackContext(ReportFeedbackContextKt.REPORT_FEEDBACK_CONTEXT_SCHEMA_VERSION, workType, new ReportFeedbackReport(UUID.randomUUID(), "약초농장", "황기", LocalDateTime.of(2026, 3, 1, 9, 0), LocalDateTime.of(2026, 7, 1, 9, 0), 1, statistics), java.util.Arrays.asList(new ReportFeedbackRecord(UUID.randomUUID(), LocalDateTime.of(2026, 4, 1, 9, 0), workType, "메모", Map.of())), null, List.of(), List.of());
    }
    @SafeVarargs
    private static <K> Map<K, Object> map(Map.Entry<K, ?>... entries) {
        Map<K, Object> result = new LinkedHashMap<>();
        for (var entry : entries) result.put(entry.getKey(), entry.getValue());
        return result;
    }

}
