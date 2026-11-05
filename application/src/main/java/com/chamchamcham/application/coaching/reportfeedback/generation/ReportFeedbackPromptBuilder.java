package com.chamchamcham.application.coaching.reportfeedback.generation;

import com.chamchamcham.application.coaching.common.CoachingPromptLabelsKt;
import com.chamchamcham.application.coaching.common.CoachingTextPolicy;
import com.chamchamcham.domain.crop.CropUsePartCategory;
import com.chamchamcham.domain.farming.*;
import org.springframework.stereotype.Component;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;

@Component
public class ReportFeedbackPromptBuilder {
    public ReportFeedbackPrompt build(ReportFeedbackContext context, List<ReportFeedbackEvidence> evidence) {
        Objects.requireNonNull(context); Objects.requireNonNull(evidence);
        var allowed = new ArrayList<String>(); allowed.add("- report:" + context.getReport().getId() + " : 현재 완료 리포트");
        context.getRecords().forEach(it -> allowed.add("- record:" + it.getId() + " : 대상 영농기록"));
        if (context.getPreviousReport() != null) allowed.add("- report:" + context.getPreviousReport().getId() + " : 지난 재배 리포트");
        evidence.forEach(it -> allowed.add("- " + it.getId() + " : " + it.getTitle()));
        var text = new StringBuilder(); text.append("허용 evidenceRefs:\n").append(String.join("\n", allowed)).append("\n\n");
        text.append("대상 리포트: ").append(context.getReport().getFarmName()).append(" / ").append(context.getReport().getCropName()).append('\n');
        text.append("대상 작업: ").append(CoachingPromptLabelsKt.toCoachingText(context.getWorkType())).append('\n');
        text.append("기간: ").append(context.getReport().getStartsAt()).append('~').append(context.getReport().getEndsAt()).append("\n현재 작업 통계:\n");
        formatStatistics(context.getWorkType(), context.getReport().getStatistics(), "").forEach(it -> text.append("- ").append(it).append('\n'));
        text.append(formatPreviousReport(context)).append("\n서버가 계산한 지난 재배의 동일 작업 비교:\n");
        if (context.getComparisons().isEmpty()) text.append("없음\n"); else context.getComparisons().forEach(it -> text.append("- ").append(formatComparison(it)).append('\n'));
        text.append("대상 기록:\n").append(String.join("\n", context.getRecords().stream().map(this::formatRecord).toList())).append("\n공식 기술 문서:\n");
        String documents = String.join("\n", evidence.stream().map(it -> "- " + it.getId() + ": " + it.getTitle() + " / " + it.getContent()).toList());
        text.append(blank(documents) ? "없음" : documents);
        return new ReportFeedbackPrompt(systemPrompt(), text.toString());
    }
    private String systemPrompt() { return "당신은 약용작물 재배 회고 코치다. 제공된 근거에 없는 수치나 사실을 만들지 않는다.\n지정된 대상 작업 타입 하나만 회고하고 다른 작업을 비교하거나 권고하지 않는다.\n지난 재배 비교는 제공된 동일 작업 통계만 사용하고 이전 기록이나 메모를 본 것처럼 말하지 않는다.\nsummary, comparisons, strengths, improvements, nextActions를 구조화해 응답한다.\nsummary는 이번 재배에서 확인한 핵심을 작업과 기록 중심으로 균형 있게 요약한다.\ncomparisons는 지난 재배와 달라진 사실만 설명하고 평가나 권고를 넣지 않는다.\nstrengths는 근거에서 확인한 잘한 행동과 그 행동이 도움이 된 이유를 함께 설명한다.\nimprovements는 부족한 점, 그 점이 판단이나 관리에 미친 영향, 앞으로 보완할 방향을 함께 설명한다.\nimprovements는 작업 간격, 양, 방법, 시기처럼 실제 재배 행동에서 개선점을 먼저 찾는다.\n사진 첨부나 기록 누락 같은 기록 습관 이야기는 재배 행동에서 개선점을 찾을 수 없을 때만 사용한다.\n공식 기술 문서가 있으면 이번 작업 통계와 기록을 문서의 권장 방법에 비추어 개선 방향을 구체적으로 제시한다.\n자료가 부족해도 판단이 어렵거나 해석이 제한됐다는 설명으로 끝내지 않고, 지금 확인한 사실로 시도할 수 있는 재배 행동을 안내한다.\nnextActions는 다음 작업에서 언제 무엇을 어떻게 할지 실행 가능한 한 가지 재배 행동으로 작성한다.\n사용자에게 내부 보고서나 시스템을 설명하지 말고 농부가 남긴 작업과 기록을 먼저 말한다.\n공식 기술 문서를 근거로 사용해도 문서를 문장의 주어로 내세우지 않는다.\n다음 개선점 예시는 형식만 참고하고 내용을 복사하지 않는다.\n나쁜 예: \"정보가 없어 해석이 제한됐어요.\"\n나쁜 예: \"사진이 없어 상태를 알기 어려웠어요. 다음에는 사진을 남겨 보세요.\" (재배 행동에 개선점이 있는데도 기록 습관만 말하는 경우)\n좋은 예: \"물 주는 간격이 중간에 크게 벌어졌어요. 생육 초기에는 간격을 일정하게 지켜 보세요.\"\nstrengths, improvements, nextActions는 각각 정확히 1개의 항목으로 응답한다.\n서버가 계산한 비교값이 있으면 comparisons는 정확히 1개의 항목으로 응답한다.\n서버가 계산한 비교값이 없으면 comparisons는 반드시 빈 배열로 응답한다.\n각 배열 항목의 text는 줄바꿈이나 목록 기호 없이 하나의 문단으로 작성한다.\n한 문단 안에는 자연스럽게 이어지는 여러 문장을 작성해도 된다.\nsummary는 20~65자로 작성한다.\ncomparisons의 text는 20~65자로 작성한다.\nstrengths, improvements, nextActions의 text는 각각 20~65자로 작성한다.\nJSON을 반환하기 전에 공백과 문장부호를 포함한 summary와 모든 text의 글자 수가 20~65자인지 확인한다.\n최소 길이를 맞출 때 의미 없는 표현을 덧붙이지 말고 근거, 판단, 실행 방법을 보강해 다시 쓴다.\n65자를 넘으면 문장을 자르지 말고 핵심 내용을 남겨 다시 쓴다.\n각 항목은 basis, text, evidenceRefs를 가져야 한다.\nevidenceRefs에는 허용 evidenceRefs에 나열된 값을 정확히 그대로 사용한다.\n통계 필드명이나 통계값은 evidenceRefs로 사용하지 않는다.\n기술 문서가 없더라도 현재 리포트와 대상 기록을 근거로 strengths, improvements, nextActions를 각각 작성한다.\n공식 기술 문서가 필요한 기술적 주장은 문서 근거 없이 만들지 않는다.\n서버가 계산한 비교값을 그대로 사용하고 다시 계산하지 않는다.\ncomparison, strength, improvement, next-action 사이에 같은 내용을 반복하지 않는다.\nsummary와 모든 text는 친근한 존댓말로 끝낸다.\nsummary, comparisons, strengths는 \"~했어요.\"처럼 회고형 존댓말로 작성한다.\nimprovements는 부족한 점을 부드럽게 설명하고 보완 방향은 \"~해 보세요.\"처럼 제안한다.\nnextActions는 \"~하세요.\"처럼 분명한 행동형 존댓말로 작성한다." + "\n" + CoachingTextPolicy.getPromptInstructions(); }
    private String formatPreviousReport(ReportFeedbackContext context) {
        var p = context.getPreviousReport(); if (p == null) return "지난 재배 리포트 없음";
        var text = new StringBuilder("지난 재배 리포트(report:" + p.getId() + "): " + p.getStartsAt() + "~" + p.getEndsAt() + "\n");
        formatStatistics(context.getWorkType(), p.getStatistics(), "지난 재배 ").forEach(it -> text.append("- ").append(it).append('\n'));
        return trim(text.toString());
    }
    private String formatComparison(ReportFeedbackComparison c) {
        String unit = unitText(c.getUnit()); if (unit == null) unit = c.getUnit();
        var difference = c.getDifference();
        String direction = difference.signum() > 0 ? difference.abs().toPlainString() + unit + " 늘었어요." : difference.signum() < 0 ? difference.abs().toPlainString() + unit + " 줄었어요." : "변화가 없어요.";
        String relative = c.getRelativeChangePct() == null ? "" : " 변화율은 " + c.getRelativeChangePct().abs().setScale(0, RoundingMode.HALF_UP).toPlainString() + "퍼센트예요.";
        String coverage = c.getCurrentCoverage() == null || c.getPreviousCoverage() == null ? "" : " 입력 범위는 이번 " + c.getCurrentCoverage().recordedCount() + "/" + c.getCurrentCoverage().targetCount() + "건, 지난 재배 " + c.getPreviousCoverage().recordedCount() + "/" + c.getPreviousCoverage().targetCount() + "건이에요.";
        return "지난 재배보다 " + c.getMetricLabel() + subjectParticle(c.getMetricLabel()) + " " + direction + relative + coverage;
    }
    private String subjectParticle(String text) { if (text.isEmpty()) return "가"; char last = text.charAt(text.length() - 1); return last < '\uAC00' || last > '\uD7A3' || (last - '\uAC00') % 28 == 0 ? "가" : "이"; }
    private String formatRecord(ReportFeedbackRecord record) {
        var text = new StringBuilder("- record:" + record.getId() + " " + record.getWorkedAt() + " " + CoachingPromptLabelsKt.toCoachingText(record.getWorkType()) + " / 메모: " + record.getMemo());
        formatRecordDetails(record.getWorkType(), record.getDetails()).forEach(it -> text.append("\n  - ").append(it));
        return text.toString();
    }
    private List<String> formatRecordDetails(WorkType type, Map<String, Object> details) {
        var result = new ArrayList<String>();
        var weather = details.get("weatherCondition"); if (weather != null && !blank(weather.toString())) result.add("기록 당시 날씨: " + weather);
        var temperature = number(details, "weatherTemperature"); if (temperature != null) result.add("기록 당시 기온: " + temperature + "도");
        if (details.get("hasPhoto") instanceof Boolean photo) result.add("사진 첨부: " + (photo ? "있음" : "없음"));
        Map<String, Object> d;
        switch (type) {
            case PLANTING -> { d = nested(details, "planting"); if (d != null) {
                add(result, "심기: ", plantingMethodText(categoryCode(d, "plantingMethod")));
                add(result, "모종 만든 방법: ", propagationText(categoryCode(d, "propagationMethod")));
                var quantity = number(d, "quantity"); String unit = text(d.get("quantityUnit")); if (quantity != null && unitText(unit) != null) result.add("심은 양: " + quantity + unitText(unit));
            } }
            case WATERING -> { d = nested(details, "watering"); if (d != null) { add(result, "물 준 양: ", irrigationAmountText(categoryCode(d, "amount"))); add(result, "물을 준 방법: ", irrigationMethodText(categoryCode(d, "method"))); } }
            case FERTILIZING -> { d = nested(details, "fertilizing"); if (d != null) {
                var name = d.get("materialName"); if (name != null && !blank(name.toString())) result.add("거름 이름: " + name);
                var amount = number(d, "amount"); String unit = unitText(text(d.get("amountUnit"))); if (amount != null && unit != null) result.add("거름 양: " + amount + unit);
                add(result, "거름을 준 방법: ", fertilizingMethodText(categoryCode(d, "applicationMethod")));
            } }
            case PEST_CONTROL -> { d = nested(details, "pestControl"); if (d != null) {
                var name = d.get("pesticideName"); if (name != null && !blank(name.toString())) result.add("약 이름: " + name);
                var amount = number(d, "pesticideAmount"); String unit = unitText(text(d.get("pesticideAmountUnit"))); if (amount != null && unit != null) result.add("약 사용량: " + amount + unit);
                var spray = number(d, "totalSprayAmountLiters"); if (spray != null) result.add("약을 섞은 물의 양: " + spray + "리터");
                var pest = d.get("pestName"); if (pest != null && !blank(pest.toString())) result.add("관리 대상: " + pest);
            } }
            case WEEDING -> { d = nested(details, "weeding"); if (d != null) add(result, "풀을 정리한 방법: ", weedingMethodText(categoryCode(d, "method"))); }
            case HARVEST -> { d = nested(details, "harvest"); if (d != null) {
                var amount = number(d, "amountKg"); if (amount != null) result.add("수확량: " + amount + "킬로그램");
                add(result, "수확한 부위: ", cropPartText(categoryCode(d, "medicinalPart")));
                add(result, "수확한 곳: ", harvestSourceText(categoryCode(d, "harvestSource")));
                var period = number(d, "growthPeriod"); String unit = growthPeriodUnitText(text(d.get("growthPeriodUnit"))); if (period != null && unit != null) result.add("기른 기간: " + period.intValue() + unit);
                if (d.get("isLastHarvest") instanceof Boolean last) result.add("마지막 수확: " + (last ? "네" : "아니요"));
            } }
            case PRUNING, ETC -> { }
        }
        return result;
    }
    private List<String> formatStatistics(WorkType type, Map<String, Object> statistics, String prefix) {
        var result = new ArrayList<String>();
        var count = number(statistics, "recordCount"); if (count != null) result.add(prefix + "기록 횟수: " + count.intValue() + "회");
        if (statistics.get("firstWorkedOn") != null) result.add(prefix + "첫 작업일: " + statistics.get("firstWorkedOn"));
        if (statistics.get("lastWorkedOn") != null) result.add(prefix + "마지막 작업일: " + statistics.get("lastWorkedOn"));
        var interval = number(statistics, "averageIntervalDays"); if (interval != null) result.add(prefix + "평균 작업 간격: " + interval + "일");
        var photos = number(statistics, "photoAttachedRecordCount"); if (photos != null) result.add(prefix + "사진을 붙인 기록: " + photos.intValue() + "회");
        var temperature = number(statistics, "averageTemperatureC"); if (temperature != null) result.add(prefix + "평균 기온: " + temperature + "도");
        switch (type) {
            case PLANTING -> result.addAll(distribution(statistics, "propagationMethods", "code", prefix, code -> { String label = plantingMethodText(code); return label == null ? propagationText(code) : label; }, "심은 방법"));
            case WATERING -> { result.addAll(distribution(statistics, "amountDistribution", "code", prefix, this::irrigationAmountText, "물 준 양")); result.addAll(distribution(statistics, "methodDistribution", "code", prefix, this::irrigationMethodText, "물을 준 방법")); }
            case FERTILIZING -> {
                var total = number(statistics, "totalAmountKg"); if (total != null) result.add(prefix + "거름 총량: " + total + "킬로그램");
                var average = number(statistics, "averageAmountKg"); if (average != null) result.add(prefix + "한 번 평균 거름 양: " + average + "킬로그램");
                result.addAll(distribution(statistics, "materialDistribution", "label", prefix, value -> blank(value) ? null : value, "거름 이름"));
                result.addAll(distribution(statistics, "methodDistribution", "code", prefix, this::fertilizingMethodText, "거름을 준 방법"));
            }
            case PEST_CONTROL -> {
                result.addAll(distribution(statistics, "categoryDistribution", "label", prefix, value -> blank(value) ? null : value, "약 이름"));
                for (var row : listOfMaps(statistics, "pesticideAmounts")) { var amount = number(row, "amount"); String unit = unitText(text(row.get("unit"))); if (amount != null && unit != null) result.add(prefix + "약 사용량: " + amount + unit); }
                var total = number(statistics, "totalSprayAmountLiters"); if (total != null) result.add(prefix + "약을 섞은 물의 총량: " + total + "리터");
                for (var row : listOfMaps(statistics, "targets")) { String target = text(row.get("target")); var targetCount = number(row, "count"); if (target != null && !blank(target) && targetCount != null) result.add(prefix + "관리 대상: " + target + " " + targetCount.intValue() + "회"); }
            }
            case WEEDING -> result.addAll(distribution(statistics, "methodDistribution", "code", prefix, this::weedingMethodText, "풀을 정리한 방법"));
            case HARVEST -> {
                var total = number(statistics, "totalAmountKg"); if (total != null) result.add(prefix + "총수확량: " + total + "킬로그램");
                var average = number(statistics, "averageAmountKg"); if (average != null) result.add(prefix + "한 번 평균 수확량: " + average + "킬로그램");
                result.addAll(distribution(statistics, "medicinalParts", "code", prefix, this::cropPartText, "수확한 부위"));
                var period = number(statistics, "finalGrowthPeriodMonths"); if (period != null) result.add(prefix + "마지막 수확까지 기른 기간: " + period.intValue() + "개월");
            }
            case PRUNING, ETC -> { }
        }
        return result;
    }
    private List<String> distribution(Map<String, Object> map, String listKey, String codeKey, String prefix, Function<String, String> labeler, String label) {
        var result = new ArrayList<String>();
        for (var row : listOfMaps(map, listKey)) { String code = text(row.get(codeKey)); if (code == null) continue; String value = labeler.apply(code); if (value == null) continue; var count = number(row, "count"); if (count == null) count = number(row, "recordCount"); if (count != null) result.add(prefix + label + ": " + value + " " + count.intValue() + "회"); }
        return result;
    }
    private void add(List<String> list, String label, String value) { if (value != null) list.add(label + value); }
    private Number number(Map<String, Object> map, String key) { return map.get(key) instanceof Number number ? number : null; }
    private String text(Object value) { return value == null ? null : value.toString(); }
    private Map<String, Object> nested(Map<String, Object> map, String key) { return asStringMap(map.get(key)); }
    private String categoryCode(Map<String, Object> map, String key) { var nested = nested(map, key); String code = nested == null ? null : text(nested.get("code")); return code == null || blank(code) ? null : code; }
    private List<Map<String, Object>> listOfMaps(Map<String, Object> map, String key) { var result = new ArrayList<Map<String, Object>>(); if (map.get(key) instanceof List<?> list) for (var item : list) { var row = asStringMap(item); if (row != null) result.add(row); } return result; }
    private Map<String, Object> asStringMap(Object value) { if (!(value instanceof Map<?, ?> map)) return null; var result = new LinkedHashMap<String, Object>(); map.forEach((key, item) -> { if (key instanceof String string) result.put(string, item); }); return result; }
    private <E extends Enum<E>> E find(E[] values, String code) { for (E value : values) if (value.name().equals(code)) return value; return null; }
    private String propagationText(String code) { var value = find(PropagationMethod.values(), code); return value == null ? null : CoachingPromptLabelsKt.toCoachingText(value); }
    private String plantingMethodText(String code) { var value = find(PlantingMethod.values(), code); return value == null ? null : CoachingPromptLabelsKt.toCoachingText(value); }
    private String irrigationAmountText(String code) { var value = find(IrrigationAmount.values(), code); return value == null ? null : CoachingPromptLabelsKt.toCoachingText(value); }
    private String irrigationMethodText(String code) { var value = find(IrrigationMethod.values(), code); return value == null ? null : CoachingPromptLabelsKt.toCoachingText(value); }
    private String fertilizingMethodText(String code) { var value = find(FertilizingMethod.values(), code); return value == null ? null : CoachingPromptLabelsKt.toCoachingText(value); }
    private String weedingMethodText(String code) { var value = find(WeedingMethod.values(), code); return value == null ? null : CoachingPromptLabelsKt.toCoachingText(value); }
    private String cropPartText(String code) { var value = find(CropUsePartCategory.values(), code); return value == null ? null : CoachingPromptLabelsKt.toCoachingText(value); }
    private String harvestSourceText(String code) { var value = find(HarvestSource.values(), code); return value == null ? null : CoachingPromptLabelsKt.toCoachingText(value); }
    private String growthPeriodUnitText(String code) { if (code == null) return null; return switch (code) { case "YEAR" -> "년"; case "MONTH" -> "개월"; default -> null; }; }
    private String unitText(String code) { if (code == null) return null; return switch (code) { case "KG" -> "킬로그램"; case "G" -> "그램"; case "ML" -> "밀리리터"; case "L" -> "리터"; case "JU" -> "포기"; default -> null; }; }
    private static boolean blank(String text) { return text.chars().allMatch(c -> Character.isWhitespace(c) || Character.isSpaceChar(c)); }
    private static String trim(String text) { int start = 0, end = text.length(); while (start < end && (Character.isWhitespace(text.charAt(start)) || Character.isSpaceChar(text.charAt(start)))) start++; while (end > start && (Character.isWhitespace(text.charAt(end - 1)) || Character.isSpaceChar(text.charAt(end - 1)))) end--; return text.substring(start, end); }
}
