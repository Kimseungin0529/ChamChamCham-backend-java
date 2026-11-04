package com.chamchamcham.application.coaching.recordfeedback.generation;

import com.chamchamcham.application.coaching.common.CoachingPromptLabelsKt;
import com.chamchamcham.application.coaching.common.CoachingTextPolicy;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class RecordFeedbackPromptBuilder {
    public RecordFeedbackPrompt build(RecordFeedbackContext context, List<RecordFeedbackRetrievalQuery> queries, List<RecordFeedbackEvidence> evidence) {
        Objects.requireNonNull(context); Objects.requireNonNull(queries); Objects.requireNonNull(evidence);
        return new RecordFeedbackPrompt(systemPrompt(), trimIndent("            피드백 대상 기록:\n            " + formatContext(context) + "\n\n            검색 쿼리:\n            " + String.join("\n", queries.stream().map(it -> "- " + it.getQuery()).toList()) + "\n\n            허용 citationIds:\n            " + formatAllowedCitationIds(context, evidence) + "\n\n            공식문서 근거:\n            " + formatEvidence(evidence)));
    }
    private String systemPrompt() { return "너는 약용작물 영농기록 피드백 보조자다.\n입력된 RecordFeedbackContext와 검색된 공식문서 근거만 사용한다.\n사진은 분석하지 않는다. photoCount는 사진 첨부 수로만 해석한다.\n의학적 효능, 복용법, 질병 치료 효과를 소비자 건강 조언처럼 말하지 않는다.\n면적, 등록 라벨, 희석 기준이 없으면 정확한 비료량이나 농약량을 invent하지 않는다.\n귀농 청년도 바로 이해할 수 있는 쉬운 말로 짧고 구체적으로 쓴다.\n불확실한 판단은 단정하지 않는다. \"~일 수 있습니다\"보다 \"기록만으로는 단정하기 어렵고, 먼저 확인하세요\"처럼 쓴다.\n생육기간, 개화 후 일수, 수확 적기처럼 기준이 다른 값은 직접 비교하지 않는다.\n수확 후 가공, 건조, 저장 조언은 보조 점검 수준으로만 다룬다.\n예보는 확정된 날씨처럼 단정하지 않는다. \"비가 올 예정이니\"보다 \"비 예보가 있으니\"처럼 표현한다.\n잘한 점은 정확히 1개만 작성한다.\n잘한 점 text는 \"<기록의 구체 행동>한 점은 잘했어요.\" 형식으로 작성한다.\n잘한 점에는 이유, 조언, 다음 행동을 덧붙이지 않는다.\n다음 행동은 2~3개만 작성한다.\n다음 행동 text는 농부가 바로 실행할 현장 작업만 한 문장으로 작성한다.\n모든 text는 친근한 존댓말로 끝낸다.\n다음 행동은 \"~하세요.\"처럼, 잘한 점은 \"~했어요.\"처럼 작성한다.\n현재 날씨, 예보, 기록 내용, 생육 상황, 행동의 이유와 효과를 text에 쓰지 않는다.\n전문용어와 추상 표현(관수, 배수 상태, 병해충 흔적, 생육 상태)을 쓰지 않는다.\n\"관수\" 대신 \"물을 주세요\"처럼 일상말을 쓴다.\n다음 행동에는 어디를, 무엇을, 어떻게 볼지 또는 할지를 포함한다.\n병해충 행동은 공식문서 근거에 있는 눈으로 확인할 수 있는 증상만 구체적으로 쓴다.\n근거에 없는 병 이름, 증상, 약 이름을 만들지 않는다.\n기록 작성, 사진 촬영, 메모 작성은 기본 행동으로 추천하지 않는다.\n각 항목은 basis, text, evidenceRefs를 반드시 가진다.\n잘한 점 text는 15~23자를 목표로 작성한다.\nnextActions의 각 text는 15~25자를 목표로 작성한다.\n모든 text는 최대 60자까지 허용한다. 강제로 자르지 말고, 길이를 맞춰 다시 쓴다.\nbasis와 evidenceRefs는 내부 검증용이다. text에 basis, 근거, citation id를 직접 쓰지 않는다.\nnextActions의 due는 TODAY, THIS_WEEK, NEXT_WEEK, NEXT_CHECK 중 하나만 사용한다.\nnextActions의 category는 WEATHER, PEST_DISEASE, IRRIGATION, FERTILIZING, PEST_CONTROL, HARVEST, CULTIVATION, GENERAL 중 하나만 사용한다.\n날씨 행동은 weather:current 또는 weather:<forecast-date> 근거가 있을 때만 작성한다.\n병해충 행동은 공식문서 근거가 있을 때만 작성한다.\n행동은 물 주기, 막힌 곳 치우기, 잎 살펴보기, 라벨 확인처럼 구체적으로 작성한다.\ncitationIds는 허용 citationIds에 명시된 값만 사용한다.\n대상 영농기록을 근거로 삼을 때는 record citation id를 사용한다.\n날씨 근거를 삼을 때는 weather:current 또는 weather:<forecast-date> citation id를 사용한다.\n공식문서를 근거로 삼을 때는 공식문서 근거의 [id]만 사용한다.\n농부에게 보여줄 text에는 chunkId나 UUID를 직접 쓰지 않는다.\n응답은 RecordFeedbackContent JSON schema만 따른다.\nJSON 최상위 필드는 goodPoint와 nextActions만 둔다." + "\n" + CoachingTextPolicy.getPromptInstructions(); }
    private String formatContext(RecordFeedbackContext context) {
        var r = context.getRecord(); var weather = context.getWeather();
        return trim("- 농장: " + context.getFarm().getName() + " (" + context.getFarm().getRoadAddress() + ")\n" +
            "- 영농 경력: " + (context.getMember().getExperienceLevel() == null ? "미상" : context.getMember().getExperienceLevel() + "년") + "\n" +
            "- 작물: " + context.getCrop().getName() + "\n" +
            "- 쓰는 부위: " + CoachingPromptLabelsKt.toCoachingText(context.getCrop().getUsePartCategory()) + "\n" +
            "- 기록 시각: " + r.getWorkedAt() + "\n" +
            "- 작업 유형: " + CoachingPromptLabelsKt.toCoachingText(r.getWorkType()) + "\n" +
            "- 작업 상세: " + formatDetail(r.getDetail()) + "\n" +
            "- 메모: " + r.getMemo() + "\n" +
            "- 사진 수: " + r.getPhotoCount() + "장\n" +
            "- 기록 당시 날씨: " + r.getRecordedWeatherCondition() + ", " + r.getRecordedTemperatureC() + "도\n" +
            "- 현재 날씨: " + formatCurrentWeather(weather) + "\n" +
            "- 예보: " + formatForecast(weather == null ? Collections.emptyList() : weather.getForecastDays()) + "\n");
    }
    private String formatCurrentWeather(RecordFeedbackLiveWeather weather) { if (weather == null) return "없음"; var c = weather.getCurrent(); return c.getSkyCondition() + ", " + c.getTemperatureC() + "도, " + c.getObservedAt() + " 관측"; }
    private String formatForecast(List<RecordFeedbackForecastDay> forecast) {
        if (forecast.isEmpty()) return "없음";
        return String.join(" | ", forecast.stream().map(it -> it.getDate() + " 비 " + value(it.getRainfallMm()) + "밀리미터, 비 올 확률 " + value(it.getRainProbabilityPct()) + "퍼센트, 최고 " + value(it.getMaxTemperatureC()) + "도, 최저 " + value(it.getMinTemperatureC()) + "도, 습도 " + value(it.getHumidityPct()) + "퍼센트, 바람 " + value(it.getWindSpeedMs()) + "미터/초").toList());
    }
    private String value(Object value) { return value == null ? "미상" : value.toString(); }
    private String formatDetail(RecordFeedbackWorkDetail detail) {
        var parts = new ArrayList<String>();
        switch (detail) {
            case PlantingFeedbackDetail d -> {
                parts.add("심기: " + CoachingPromptLabelsKt.toCoachingText(d.getPlantingMethod()));
                if (d.getSeedAmount() != null) parts.add("씨앗 양: " + d.getSeedAmount() + (d.getSeedAmountUnit() == null ? "" : CoachingPromptLabelsKt.toCoachingText(d.getSeedAmountUnit())));
                if (d.getSeedlingCount() != null) parts.add("모종 수: " + d.getSeedlingCount() + (d.getSeedlingUnit() == null ? "" : CoachingPromptLabelsKt.toCoachingText(d.getSeedlingUnit())));
                if (d.getPropagationMethod() != null) parts.add("모종 만든 방법: " + CoachingPromptLabelsKt.toCoachingText(d.getPropagationMethod()));
            }
            case WateringFeedbackDetail d -> {
                if (d.getIrrigationAmount() != null) parts.add("물 준 양: " + CoachingPromptLabelsKt.toCoachingText(d.getIrrigationAmount()));
                if (d.getIrrigationMethod() != null) parts.add("물을 준 방법: " + CoachingPromptLabelsKt.toCoachingText(d.getIrrigationMethod()));
            }
            case FertilizingFeedbackDetail d -> {
                parts.add("거름 이름: " + d.getMaterialName()); parts.add("거름 양: " + d.getAmount() + CoachingPromptLabelsKt.toCoachingText(d.getAmountUnit()));
                if (d.getApplicationMethod() != null) parts.add("거름을 준 방법: " + CoachingPromptLabelsKt.toCoachingText(d.getApplicationMethod()));
            }
            case PestControlFeedbackDetail d -> {
                parts.add("약 이름: " + d.getPesticideName()); parts.add("약 사용량: " + d.getPesticideAmount() + CoachingPromptLabelsKt.toCoachingText(d.getPesticideAmountUnit()));
                String unit = CoachingPromptLabelsKt.toSprayAmountCoachingText(d.getTotalSprayAmountUnit());
                if (unit != null) parts.add("약을 섞은 물의 양: " + d.getTotalSprayAmount() + unit);
                if (d.getPestName() != null) parts.add("관리 대상: " + d.getPestName());
            }
            case WeedingFeedbackDetail d -> { if (d.getWeedingMethod() != null) parts.add("풀을 정리한 방법: " + CoachingPromptLabelsKt.toCoachingText(d.getWeedingMethod())); }
            case HarvestFeedbackDetail d -> {
                if (d.getHarvestAmount() != null) parts.add("수확량: " + d.getHarvestAmount() + "킬로그램");
                if (d.getMedicinalPart() != null) parts.add("수확한 부위: " + CoachingPromptLabelsKt.toCoachingText(d.getMedicinalPart()));
                parts.add("기른 곳: " + CoachingPromptLabelsKt.toCoachingText(d.getHarvestSource()));
                if (d.getGrowthPeriod() != null && d.getGrowthPeriodUnit() != null) { String unit = CoachingPromptLabelsKt.toGrowthPeriodCoachingText(d.getGrowthPeriodUnit()); if (unit != null) parts.add("기른 기간: " + d.getGrowthPeriod() + unit); }
                parts.add("마지막 수확: " + (d.isLastHarvest() ? "네" : "아니요"));
            }
            case CommonFeedbackDetail ignored -> { return "공통 상세 없음"; }
        }
        return String.join(", ", parts);
    }
    private String formatEvidence(List<RecordFeedbackEvidence> evidence) {
        var parts = new ArrayList<String>();
        for (var it : evidence) if (!trim(it.getId()).isEmpty()) parts.add("[" + it.getId() + "] " + it.getTitle() + (it.getPage() == null ? "" : " p." + it.getPage()) + "\n" + it.getContent());
        return parts.isEmpty() ? "검색된 공식문서 근거 없음" : String.join("\n\n", parts);
    }
    private String formatAllowedCitationIds(RecordFeedbackContext context, List<RecordFeedbackEvidence> evidence) {
        var text = new StringBuilder("- " + context.recordCitationId() + " : 대상 영농기록 context\n");
        if (context.getWeather() != null) { text.append("- weather:current : 현재 날씨 context\n"); context.getWeather().getForecastDays().forEach(it -> text.append("- weather:").append(it.getDate()).append(" : 예보 날씨 context\n")); }
        evidence.stream().filter(it -> !trim(it.getId()).isEmpty()).forEach(it -> text.append("- ").append(it.getId()).append(" : ").append(it.getTitle()).append('\n'));
        return trim(text.toString());
    }
    private static String trim(String text) { int start = 0, end = text.length(); while (start < end && whitespace(text.charAt(start))) start++; while (end > start && whitespace(text.charAt(end - 1))) end--; return text.substring(start, end); }
    private static boolean whitespace(char c) { return Character.isWhitespace(c) || Character.isSpaceChar(c); }
    private static String trimIndent(String text) {
        var lines = Arrays.asList(text.split("\r\n|\n|\r", -1));
        int indent = lines.stream().filter(it -> !trim(it).isEmpty()).mapToInt(it -> { int i = 0; while (i < it.length() && whitespace(it.charAt(i))) i++; return i; }).min().orElse(0);
        int start = !lines.isEmpty() && trim(lines.getFirst()).isEmpty() ? 1 : 0;
        int end = lines.size() > start && trim(lines.getLast()).isEmpty() ? lines.size() - 1 : lines.size();
        var result = new ArrayList<String>();
        for (int i = start; i < end; i++) { String line = lines.get(i); result.add(trim(line).isEmpty() ? "" : line.substring(Math.min(indent, line.length()))); }
        return String.join("\n", result);
    }
}
