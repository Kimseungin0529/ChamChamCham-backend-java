package com.chamchamcham.application.coaching.recordfeedback.generation;

import com.chamchamcham.domain.coaching.recordfeedback.RecordFeedbackActionCategory;
import com.chamchamcham.domain.coaching.recordfeedback.RecordFeedbackActionDue;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import java.util.*;
import org.junit.jupiter.api.DisplayName;

class RecordFeedbackContentTest {
    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();

    @Test
    @DisplayName("coaching result keeps one good point and typed next actions in JSON")
    void coaching_result_keeps_one_good_point_and_typed_next_actions_in_JSON() throws Exception {
        var json = objectMapper.writeValueAsString(validResult());
        var root = objectMapper.readTree(json);
        var goodPoint = root.get("goodPoint");
        var nextAction = root.get("nextActions").get(0);
        assertThat(fieldNames(root)).containsExactlyInAnyOrder("goodPoint", "nextActions");
        assertThat(fieldNames(goodPoint)).containsExactlyInAnyOrder("basis", "text", "evidenceRefs");
        assertThat(fieldNames(nextAction)).containsExactlyInAnyOrder("due", "category", "basis", "text", "evidenceRefs");
        assertThat(nextAction.get("due").asText()).isEqualTo("NEXT_WEEK");
        assertThat(nextAction.get("category").asText()).isEqualTo("WEATHER");
        assertThat(Arrays.asList(RecordFeedbackActionDue.values()).stream().map(it -> it.name()).toList()).containsExactly("TODAY", "THIS_WEEK", "NEXT_WEEK", "NEXT_CHECK");
        assertThat(Arrays.asList(RecordFeedbackActionCategory.values()).stream().map(it -> it.name()).toList()).containsExactly("WEATHER", "PEST_DISEASE", "IRRIGATION", "FERTILIZING", "PEST_CONTROL", "HARVEST", "CULTIVATION", "GENERAL");
    }
    private RecordFeedbackContent validResult() {
        return new RecordFeedbackContent(new RecordFeedbackGoodPoint("점적관수 방식과 토양 상태 기록", "흙 상태를 살핀 점은 잘했어요.", java.util.Arrays.asList("record:record-1")), java.util.Arrays.asList(new RecordFeedbackAction(RecordFeedbackActionDue.NEXT_WEEK, RecordFeedbackActionCategory.WEATHER, "7월 15일 비 예보", "비 오기 전 물길을 정리하세요.", java.util.Arrays.asList("weather:2026-07-15")), new RecordFeedbackAction(RecordFeedbackActionDue.NEXT_CHECK, RecordFeedbackActionCategory.IRRIGATION, "토양 상태 확인 기록", "다음에 흙 상태를 살펴보세요.", java.util.Arrays.asList("record:record-1"))));
    }
    @SafeVarargs
    private static <K> Map<K, Object> map(Map.Entry<K, ?>... entries) {
        Map<K, Object> result = new LinkedHashMap<>();
        for (var entry : entries) result.put(entry.getKey(), entry.getValue());
        return result;
    }

    private static Set<String> fieldNames(com.fasterxml.jackson.databind.JsonNode node) { Set<String> names = new LinkedHashSet<>(); node.fieldNames().forEachRemaining(names::add); return names; }

}
