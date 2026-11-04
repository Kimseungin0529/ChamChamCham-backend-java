package com.chamchamcham.application.coaching.recordfeedback.generation;

import com.fasterxml.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.DisplayName;
import com.chamchamcham.application.coaching.recordfeedback.generation.RecordFeedbackContextKt;
import com.chamchamcham.application.coaching.reportfeedback.generation.ReportFeedbackContextKt;

class RecordFeedbackRetrievalQueryPlannerTest {
    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();
    private final RecordFeedbackRetrievalQueryPlanner planner = new RecordFeedbackRetrievalQueryPlanner();

    @Test
    @DisplayName("watering context creates crop work type memo and grounded weather queries without cycle query")
    void watering_context_creates_crop_work_type_memo_and_grounded_weather_queries_without_cycle_query() throws Exception {
        var queries = planner.plan(readFixture("today-record-feedback-watering.json")).stream().map(it -> it.getQuery()).toList();
        assertThat(queries).contains("참당귀 관수 재배 관리 약용작물", "참당귀 오전 흙 표면이 말라 보여 점적 관수함.", "참당귀 고온 생육 관리", "참당귀 강우 예보 배수 과습 병해충");
        assertThat(queries).noneMatch(it -> it.contains("일차 생육 관리"));
    }

    @Test
    @DisplayName("pest control context includes target disease query from typed detail")
    void pest_control_context_includes_target_disease_query_from_typed_detail() throws Exception {
        var queries = planner.plan(readFixture("today-record-feedback-pest-control.json")).stream().map(it -> it.getQuery()).toList();
        assertThat(queries).contains("인삼 병해충 관리 재배 관리 약용작물", "인삼 점무늬병 방제");
    }

    @Test
    @DisplayName("harvest context includes medicinal use part harvest query")
    void harvest_context_includes_medicinal_use_part_harvest_query() throws Exception {
        var queries = planner.plan(readFixture("today-record-feedback-harvest.json")).stream().map(it -> it.getQuery()).toList();
        assertThat(queries).contains("오미자 수확 재배 관리 약용작물", "약용작물 열매/과실 수확 적기 오미자");
    }

    @Test
    @DisplayName("harvest context without recorded medicinal part keeps only generic harvest timing query")
    void harvest_context_without_recorded_medicinal_part_keeps_only_generic_harvest_timing_query() throws Exception {
        var base = readFixture("today-record-feedback-harvest.json");
        var detail = ((HarvestFeedbackDetail) base.getRecord().getDetail());
        var context = base.copy(base.getSchemaVersion(), base.getMember(), base.getFarm(), base.getCrop(), base.getRecord().copy(base.getRecord().getRecordId(), base.getRecord().getSourceRevision(), base.getRecord().getWorkedAt(), base.getRecord().getWorkType(), detail.copy(detail.getHarvestAmount(), detail.getAmountUnknown(), null, detail.getHarvestSource(), detail.getGrowthPeriod(), detail.getGrowthPeriodUnit(), detail.isLastHarvest()), base.getRecord().getRecordedWeatherCondition(), base.getRecord().getRecordedTemperatureC(), base.getRecord().getMemo(), base.getRecord().getPhotoCount()), base.getWeather(), base.getWarnings());
        var queries = planner.plan(context).stream().map(it -> it.getQuery()).toList();
        assertThat(queries).contains("약용작물 수확 적기 오미자");
        assertThat(queries).noneMatch(it -> it.contains("열매/과실"));
    }

    @Test
    @DisplayName("reduced weather context does not create weather query")
    void reduced_weather_context_does_not_create_weather_query() throws Exception {
        var queries = planner.plan(readFixture("today-record-feedback-no-cycle.json"));
        assertThat(queries.stream().map(it -> it.getQuery()).toList()).contains("참당귀 잡초 관리 재배 관리 약용작물");
        assertThat(queries.stream().map(it -> it.getReason()).toList()).doesNotContain("rain_wet_weather", "hot_weather", "forecast_rain_wet_weather", "forecast_dry_weather", "forecast_hot_weather");
    }

    @Test
    @DisplayName("blank memo does not create memo query")
    void blank_memo_does_not_create_memo_query() throws Exception {
        var base = readFixture("today-record-feedback-watering.json");
        var context = base.copy(base.getSchemaVersion(), base.getMember(), base.getFarm(), base.getCrop(), base.getRecord().copy(base.getRecord().getRecordId(), base.getRecord().getSourceRevision(), base.getRecord().getWorkedAt(), base.getRecord().getWorkType(), base.getRecord().getDetail(), base.getRecord().getRecordedWeatherCondition(), base.getRecord().getRecordedTemperatureC(), "  ", base.getRecord().getPhotoCount()), base.getWeather(), base.getWarnings());
        assertThat(planner.plan(context).stream().map(it -> it.getReason()).toList()).doesNotContain("memo_text");
    }

    @Test
    @DisplayName("long memo is truncated to 120 chars")
    void long_memo_is_truncated_to_120_chars() throws Exception {
        var base = readFixture("today-record-feedback-watering.json");
        var context = base.copy(base.getSchemaVersion(), base.getMember(), base.getFarm(), base.getCrop(), base.getRecord().copy(base.getRecord().getRecordId(), base.getRecord().getSourceRevision(), base.getRecord().getWorkedAt(), base.getRecord().getWorkType(), base.getRecord().getDetail(), base.getRecord().getRecordedWeatherCondition(), base.getRecord().getRecordedTemperatureC(), "가".repeat(200), base.getRecord().getPhotoCount()), base.getWeather(), base.getWarnings());
        var memoQuery = planner.plan(context).stream().filter(it -> Objects.equals(it.getReason(), "memo_text")).findFirst().orElseThrow();
        assertThat(memoQuery.getQuery()).isEqualTo("참당귀 "+ "가".repeat(120));
    }

    @Test
    @DisplayName("wet forecast takes priority over hot weather query")
    void wet_forecast_takes_priority_over_hot_weather_query() {
        var base = readFixture("today-record-feedback-watering.json");
        var weather = Objects.requireNonNull(base.getWeather());
        var day = weather.getForecastDays().getFirst();
        var revisedDay = day.copy(day.getDate(), new BigDecimal("40.0"), day.getRainProbabilityPct(), day.getMaxTemperatureC(), day.getMinTemperatureC(), day.getHumidityPct(), day.getWindSpeedMs(), List.of("RAIN"));
        var revisedWeather = weather.copy(weather.getCurrent().copy(33, weather.getCurrent().getSkyCondition(), weather.getCurrent().getObservedAt()), List.of(revisedDay), weather.getSource());
        var context = base.copy(base.getSchemaVersion(), base.getMember(), base.getFarm(), base.getCrop(), base.getRecord(), revisedWeather, base.getWarnings());
        var reasons = planner.plan(context).stream().map(RecordFeedbackRetrievalQuery::getReason).toList();
        assertThat(reasons).contains("rain_wet_weather").doesNotContain("hot_weather");
    }

    @Test
    @DisplayName("ordinary current weather without forecast risks does not trigger weather query")
    void ordinary_current_weather_without_forecast_risks_does_not_trigger_weather_query() {
        var base = readFixture("today-record-feedback-watering.json");
        var weather = Objects.requireNonNull(base.getWeather());
        var revisedWeather = weather.copy(weather.getCurrent().copy(24, weather.getCurrent().getSkyCondition(), weather.getCurrent().getObservedAt()), List.of(), weather.getSource());
        var context = base.copy(base.getSchemaVersion(), base.getMember(), base.getFarm(), base.getCrop(), base.getRecord(), revisedWeather, base.getWarnings());
        var reasons = planner.plan(context).stream().map(RecordFeedbackRetrievalQuery::getReason).toList();
        assertThat(reasons).doesNotContain("hot_weather", "rain_wet_weather");
    }

    @Test
    @DisplayName("hot forecast without dry evidence does not retrieve dry irrigation guidance")
    void hot_forecast_without_dry_evidence_does_not_retrieve_dry_irrigation_guidance() {
        var base = readFixture("today-record-feedback-watering.json");
        var weather = Objects.requireNonNull(base.getWeather());
        var day = weather.getForecastDays().getFirst();
        var revisedDay = day.copy(day.getDate(), null, 20, new BigDecimal("32"), day.getMinTemperatureC(), new BigDecimal("50"), day.getWindSpeedMs(), List.of("HOT"));
        var revisedWeather = weather.copy(weather.getCurrent(), List.of(revisedDay), weather.getSource());
        var context = base.copy(base.getSchemaVersion(), base.getMember(), base.getFarm(), base.getCrop(), base.getRecord(), revisedWeather, base.getWarnings());
        var queries = planner.plan(context).stream().map(RecordFeedbackRetrievalQuery::getQuery).toList();
        assertThat(queries).contains("참당귀 고온 예보 생육 관리");
        assertThat(queries).noneMatch(it -> it.contains("건조") || it.contains("관수 관리"));
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
