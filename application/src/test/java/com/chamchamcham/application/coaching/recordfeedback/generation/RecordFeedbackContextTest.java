package com.chamchamcham.application.coaching.recordfeedback.generation;

import com.chamchamcham.domain.crop.CropUsePartCategory;
import com.chamchamcham.domain.farming.IrrigationAmount;
import com.chamchamcham.domain.farming.IrrigationMethod;
import com.chamchamcham.domain.farming.WeedingMethod;
import com.chamchamcham.domain.farming.WorkType;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import java.util.*;
import org.junit.jupiter.api.DisplayName;
import com.chamchamcham.application.coaching.recordfeedback.generation.RecordFeedbackContextKt;
import com.chamchamcham.application.coaching.reportfeedback.generation.ReportFeedbackContextKt;

class RecordFeedbackContextTest {
    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();

    @Test
    @DisplayName("watering fixture deserializes into stable v2 context contract")
    void watering_fixture_deserializes_into_stable_v2_context_contract() throws Exception {
        var context = readFixture("today-record-feedback-watering.json");
        assertThat(context.getSchemaVersion()).isEqualTo("record-feedback-context.v2");
        assertThat(context.getCrop().getName()).isEqualTo("참당귀");
        assertThat(context.getCrop().getUsePartCategory()).isEqualTo(CropUsePartCategory.ROOT_BARK);
        assertThat(context.getRecord().getWorkType()).isEqualTo(WorkType.WATERING);
        assertThat(context.getRecord().getDetail()).isEqualTo(new WateringFeedbackDetail(IrrigationAmount.NORMAL, IrrigationMethod.DRIP));
        assertThat(context.getWeather().getForecastDays()).hasSize(3);
        assertThat(context.getWeather().getForecastDays().getFirst().getDate().toString()).isEqualTo("2026-07-04");
        assertThat(context.getWeather().getForecastDays().getFirst().getRiskFlags()).contains("HEAVY_RAIN");
        assertThat(Arrays.stream(RecordFeedbackContext.class.getDeclaredFields()).map(java.lang.reflect.Field::getName).toList()).doesNotContain("recentRecords", "workTypeStats", "cropCycle");
    }

    @Test
    @DisplayName("reduced weather fixture keeps weather nullable and warning explicit")
    void reduced_weather_fixture_keeps_weather_nullable_and_warning_explicit() throws Exception {
        var context = readFixture("today-record-feedback-no-cycle.json");
        assertThat(context.getWeather()).isNull();
        assertThat(context.getWarnings()).containsExactly("weather_location_unavailable");
        assertThat(context.getRecord().getWorkType()).isEqualTo(WorkType.WEEDING);
        assertThat(context.getRecord().getDetail()).isEqualTo(new WeedingFeedbackDetail(WeedingMethod.HAND));
    }
    private RecordFeedbackContext readFixture(String name) {
        var resource = Objects.requireNonNull(getClass().getClassLoader().getResource("coaching/rag/" + name), "Missing fixture: " + name);
        try (var input = resource.openStream()) { return objectMapper.readValue(input, RecordFeedbackContext.class); }
        catch (java.io.IOException exception) { throw new java.io.UncheckedIOException(exception); }
    }
    @SafeVarargs
    private static <K> Map<K, Object> map(Map.Entry<K, ?>... entries) {
        Map<K, Object> result = new LinkedHashMap<>();
        for (var entry : entries) result.put(entry.getKey(), entry.getValue());
        return result;
    }

}
