package com.chamchamcham.application.coaching.recordfeedback.generation;

import com.chamchamcham.application.coaching.common.CoachingTextPolicy;
import com.chamchamcham.domain.crop.CropUsePartCategory;
import com.chamchamcham.domain.farming.FertilizerAmountUnit;
import com.chamchamcham.domain.farming.FertilizingMethod;
import com.chamchamcham.domain.farming.HarvestSource;
import com.chamchamcham.domain.farming.PesticideAmountUnit;
import com.chamchamcham.domain.farming.PlantingMethod;
import com.chamchamcham.domain.farming.SeedAmountUnit;
import com.chamchamcham.domain.farming.SeedlingUnit;
import com.chamchamcham.domain.farming.WeedingMethod;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.DisplayName;
import com.chamchamcham.application.coaching.recordfeedback.generation.RecordFeedbackContextKt;
import com.chamchamcham.application.coaching.reportfeedback.generation.ReportFeedbackContextKt;

class RecordFeedbackPromptBuilderTest {
    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();
    private final RecordFeedbackPromptBuilder builder = new RecordFeedbackPromptBuilder();

    @Test
    @DisplayName("prompt includes safety rules for medicinal crop record feedback")
    void prompt_includes_safety_rules_for_medicinal_crop_record_feedback() throws Exception {
        var prompt = builder.build(readFixture("today-record-feedback-watering.json"), java.util.Arrays.asList(new RecordFeedbackRetrievalQuery("참당귀 물주기 재배 관리 약용작물", "crop_work_type")), java.util.Arrays.asList(new RecordFeedbackEvidence("doc-1", "농업기술길잡이 007 약용작물", 123, "관수 후 토양 상태를 확인한다.")));
        assertThat(prompt.getSystem()).contains("약용작물 영농기록 피드백");
        assertThat(prompt.getSystem()).contains("사진은 분석하지 않는다");
        assertThat(prompt.getSystem()).contains("의학적 효능");
        assertThat(prompt.getSystem()).contains("정확한 비료량이나 농약량을 invent하지 않는다");
        assertThat(prompt.getSystem()).contains("citationIds는 허용 citationIds에 명시된 값만 사용한다");
        assertThat(prompt.getSystem()).contains("귀농 청년도 바로 이해할 수 있는 쉬운 말");
        assertThat(prompt.getSystem()).contains("불확실한 판단은 단정하지 않는다");
        assertThat(prompt.getSystem()).contains("수확 후 가공, 건조, 저장 조언은 보조 점검 수준");
        assertThat(prompt.getSystem()).contains("예보는 확정된 날씨처럼 단정하지 않는다");
        assertThat(prompt.getSystem()).contains("잘한 점은 정확히 1개만 작성한다");
        assertThat(prompt.getSystem()).contains("다음 행동은 2~3개만 작성한다");
        assertThat(prompt.getSystem()).contains("잘한 점 text는 \"<기록의 구체 행동>한 점은 잘했어요.\" 형식으로 작성한다.", "잘한 점에는 이유, 조언, 다음 행동을 덧붙이지 않는다.", "다음 행동 text는 농부가 바로 실행할 현장 작업만 한 문장으로 작성한다.", "모든 text는 친근한 존댓말로 끝낸다.", "다음 행동은 \"~하세요.\"처럼, 잘한 점은 \"~했어요.\"처럼 작성한다.", "현재 날씨, 예보, 기록 내용, 생육 상황, 행동의 이유와 효과를 text에 쓰지 않는다.", "전문용어와 추상 표현(관수, 배수 상태, 병해충 흔적, 생육 상태)을 쓰지 않는다.", "다음 행동에는 어디를, 무엇을, 어떻게 볼지 또는 할지를 포함한다.", "병해충 행동은 공식문서 근거에 있는 눈으로 확인할 수 있는 증상만 구체적으로 쓴다.", "basis와 evidenceRefs는 내부 검증용이다. text에 basis, 근거, citation id를 직접 쓰지 않는다.", "잘한 점 text는 15~23자를 목표로 작성한다.", "nextActions의 각 text는 15~25자를 목표로 작성한다.", "모든 text는 최대 60자까지 허용한다. 강제로 자르지 말고, 길이를 맞춰 다시 쓴다.");
        assertThat(prompt.getSystem()).doesNotContain("각 text는 15~25자를 목표로 하되");
        assertThat(prompt.getSystem()).contains("응답은 RecordFeedbackContent JSON schema만 따른다");
        assertThat(prompt.getSystem()).contains(CoachingTextPolicy.INSTANCE.getPromptInstructions());
        assertThat(prompt.getSystem()).contains("goodPoint", "nextActions", "basis", "text", "evidenceRefs");
        assertThat(prompt.getSystem()).doesNotContain("summary", "riskLevel", "diagnosis", "observations", "recommendations", "follow-up", "follow up");
    }

    @Test
    @DisplayName("prompt translates target record and live weather without raw internal values")
    void prompt_translates_target_record_and_live_weather_without_raw_internal_values() throws Exception {
        var prompt = builder.build(readFixture("today-record-feedback-watering.json"), java.util.Arrays.asList(new RecordFeedbackRetrievalQuery("참당귀 물주기 재배 관리 약용작물", "crop_work_type")), java.util.Arrays.asList(new RecordFeedbackEvidence("doc-1", "농업기술길잡이 007 약용작물", 123, "관수 후 토양 상태를 확인한다.")));
        assertThat(prompt.getUser()).contains("작물: 참당귀");
        assertThat(prompt.getUser()).contains("쓰는 부위: 뿌리와 껍질");
        assertThat(prompt.getUser()).contains("작업 유형: 물 주기");
        assertThat(prompt.getUser()).contains("오전 흙 표면이 말라 보여 점적 관수함.");
        assertThat(prompt.getUser()).contains("2026-07-04", "비 22.0밀리미터", "비 올 확률 80퍼센트");
        assertThat(prompt.getUser()).contains("물 준 양: 보통 양", "물을 준 방법: 호스로 조금씩 물을 줌");
        assertThat(prompt.getUser()).contains("현재 날씨: 구름많음, 30도");
        assertThat(prompt.getUser()).contains("허용 citationIds:");
        assertThat(prompt.getUser()).contains("record:10000000-0000-0000-0000-000000000004 : 대상 영농기록 context");
        assertThat(prompt.getUser()).contains("weather:current : 현재 날씨 context");
        assertThat(prompt.getUser()).contains("weather:2026-07-04 : 예보 날씨 context");
        assertThat(prompt.getUser()).contains("[doc-1] 농업기술길잡이 007 약용작물 p.123");
        assertThat(prompt.getUser()).contains("영농 경력: 1년");
        assertThat(prompt.getUser()).contains("최저 21.4도");
        assertThat(prompt.getUser()).doesNotContain("schemaVersion", "managementType", "NON_REGISTERED_FARMER", "irrigationAmount", "irrigationMethod", "NORMAL", "DRIP", "riskFlags", "HEAVY_RAIN", "HIGH_HUMIDITY", "source=", "FAKE_WEATHER_PORT", "crop_work_type");
        assertThat(prompt.getUser()).doesNotContain("최근 기록", "작물주기", "작업 횟수");
    }

    @Test
    @DisplayName("prompt translates every typed record detail without enum names")
    void prompt_translates_every_typed_record_detail_without_enum_names() throws Exception {
        var base = readFixture("today-record-feedback-watering.json");
        var cases = java.util.Arrays.asList(new AbstractMap.SimpleEntry<>(new PlantingFeedbackDetail(PlantingMethod.SEED, new BigDecimal("1250.0000"), SeedAmountUnit.G, null, null, null), java.util.Arrays.asList("심기: 씨앗을 심음", "씨앗 양: 1250.0000그램")), new AbstractMap.SimpleEntry<>(new FertilizingFeedbackDetail("유기질비료", new BigDecimal("2500.0000"), FertilizerAmountUnit.G, FertilizingMethod.SOIL), java.util.Arrays.asList("거름 이름: 유기질비료", "거름 양: 2500.0000그램", "거름을 준 방법: 흙에 거름을 줌")), new AbstractMap.SimpleEntry<>(new PestControlFeedbackDetail("가가방", new BigDecimal("120.0000"), PesticideAmountUnit.ML, new BigDecimal("20.0000"), "ML", "점무늬병"), java.util.Arrays.asList("약 이름: 가가방", "약 사용량: 120.0000밀리리터", "약을 섞은 물의 양: 20.0000밀리리터")), new AbstractMap.SimpleEntry<>(new WeedingFeedbackDetail(WeedingMethod.HAND), java.util.Arrays.asList("풀을 정리한 방법: 손으로 풀을 뽑음")), new AbstractMap.SimpleEntry<>(new HarvestFeedbackDetail(new BigDecimal("82.0000"), false, CropUsePartCategory.ROOT_BARK, HarvestSource.CULTIVATED, 6, "MONTH", true), java.util.Arrays.asList("수확량: 82.0000킬로그램", "수확한 부위: 뿌리와 껍질", "기른 곳: 밭에서 기름", "기른 기간: 6개월", "마지막 수확: 네")));
        for (var entry : cases) {
            var detail = entry.getKey();
            var record = base.getRecord();
            var context = base.copy(base.getSchemaVersion(), base.getMember(), base.getFarm(), base.getCrop(), record.copy(record.getRecordId(), record.getSourceRevision(), record.getWorkedAt(), record.getWorkType(), detail, record.getRecordedWeatherCondition(), record.getRecordedTemperatureC(), record.getMemo(), record.getPhotoCount()), base.getWeather(), base.getWarnings());
            var prompt = builder.build(context, List.of(), List.of());
            assertThat(prompt.getUser()).contains(entry.getValue().toArray(String[]::new));
        }
    }

    @Test
    @DisplayName("prompt omits optional record details when they were not recorded")
    void prompt_omits_optional_record_details_when_they_were_not_recorded() throws Exception {
        var base = readFixture("today-record-feedback-watering.json");
        var cases = java.util.Arrays.asList(new AbstractMap.SimpleEntry<>(new WateringFeedbackDetail(null, null), java.util.Arrays.asList("물 준 양", "물을 준 방법")), new AbstractMap.SimpleEntry<>(new FertilizingFeedbackDetail("유기질비료", new BigDecimal("250"), FertilizerAmountUnit.ML, null), java.util.Arrays.asList("거름을 준 방법")), new AbstractMap.SimpleEntry<>(new PestControlFeedbackDetail("가가방", new BigDecimal("120"), PesticideAmountUnit.ML, new BigDecimal("20"), "L", null), java.util.Arrays.asList("관리 대상")), new AbstractMap.SimpleEntry<>(new WeedingFeedbackDetail(null), java.util.Arrays.asList("풀을 정리한 방법")), new AbstractMap.SimpleEntry<>(new HarvestFeedbackDetail(null, true, null, HarvestSource.CULTIVATED, null, null, false), java.util.Arrays.asList("수확량", "수확한 부위", "기른 기간")));
        for (var entry : cases) {
            var detail = entry.getKey();
            var record = base.getRecord();
            var context = base.copy(base.getSchemaVersion(), base.getMember(), base.getFarm(), base.getCrop(), record.copy(record.getRecordId(), record.getSourceRevision(), record.getWorkedAt(), record.getWorkType(), detail, record.getRecordedWeatherCondition(), record.getRecordedTemperatureC(), record.getMemo(), record.getPhotoCount()), base.getWeather(), base.getWarnings());
            var prompt = builder.build(context, List.of(), List.of());
            assertThat(prompt.getUser()).doesNotContain(entry.getValue().toArray(String[]::new)).doesNotContain("미상", "모름");
        }
    }

    @Test
    @DisplayName("prompt excludes blank evidence id from official evidence and allowed citations")
    void prompt_excludes_blank_evidence_id_from_official_evidence_and_allowed_citations() throws Exception {
        var prompt = builder.build(readFixture("today-record-feedback-watering.json"), java.util.Arrays.asList(new RecordFeedbackRetrievalQuery("참당귀 물주기 재배 관리 약용작물", "crop_work_type")), java.util.Arrays.asList(new RecordFeedbackEvidence("   ", "공백 ID 문서", 7, "공백 ID 근거는 prompt citation으로 쓰면 안 된다."), new RecordFeedbackEvidence("doc-1", "농업기술길잡이 007 약용작물", 123, "관수 후 토양 상태를 확인한다.")));
        var allowedCitationSection = before(after(prompt.getUser(), "허용 citationIds:"), "공식문서 근거:");
        var officialEvidenceSection = after(prompt.getUser(), "공식문서 근거:");
        assertThat(allowedCitationSection).contains("doc-1 : 농업기술길잡이 007 약용작물");
        assertThat(allowedCitationSection).doesNotContain("공백 ID 문서");
        assertThat(officialEvidenceSection).contains("[doc-1] 농업기술길잡이 007 약용작물 p.123");
        assertThat(officialEvidenceSection).doesNotContain("공백 ID 문서");
    }
    private RecordFeedbackContext readFixture(String name) {
        var resource = Objects.requireNonNull(getClass().getClassLoader().getResource("coaching/rag/" + name), "Missing fixture: " + name);
        try (var input = resource.openStream()) { return objectMapper.readValue(input, RecordFeedbackContext.class); }
        catch (java.io.IOException exception) { throw new java.io.UncheckedIOException(exception); }
    }
    private static String after(String text, String delimiter) { int i = text.indexOf(delimiter); return i < 0 ? text : text.substring(i + delimiter.length()); }
    private static String before(String text, String delimiter) { int i = text.indexOf(delimiter); return i < 0 ? text : text.substring(0, i); }

}
