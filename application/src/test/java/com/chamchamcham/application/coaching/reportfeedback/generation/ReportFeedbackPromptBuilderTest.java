package com.chamchamcham.application.coaching.reportfeedback.generation;

import com.chamchamcham.application.coaching.common.CoachingTextPolicy;
import com.chamchamcham.domain.farming.HarvestSource;
import com.chamchamcham.domain.farming.WorkType;
import com.chamchamcham.domain.report.Coverage;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.*;
import org.junit.jupiter.api.DisplayName;
import com.chamchamcham.application.coaching.recordfeedback.generation.RecordFeedbackContextKt;
import com.chamchamcham.application.coaching.reportfeedback.generation.ReportFeedbackContextKt;

class ReportFeedbackPromptBuilderTest {
    private final UUID recordId = UUID.randomUUID();
    private final UUID currentReportId = UUID.randomUUID();
    private final UUID previousReportId = UUID.randomUUID();

    @Test
    @DisplayName("prompt scopes instructions statistics and allowed evidence to one work type")
    void prompt_scopes_instructions_statistics_and_allowed_evidence_to_one_work_type() throws Exception {
        var prompt = new ReportFeedbackPromptBuilder().build(context(), java.util.Arrays.asList(new ReportFeedbackEvidence("document-1", "황기 관수 기술", "관수 후 토양 수분을 확인한다.")));
        assertThat(prompt.getSystem()).contains("대상 작업 타입 하나만").contains("nextActions").contains("빈 배열").contains("nextActions는 다음 작업에서 언제 무엇을 어떻게 할지").contains("summary와 모든 text는 친근한 존댓말로 끝낸다.").contains("comparisons").contains("서버가 계산한 비교값을 그대로 사용하고 다시 계산하지 않는다.").contains("comparisons는 지난 재배와 달라진 사실만").contains("comparison, strength, improvement, next-action 사이에 같은 내용을 반복하지 않는다.").contains(CoachingTextPolicy.INSTANCE.getPromptInstructions()).doesNotContain("다음 사이클 계획");
        assertThat(prompt.getSystem()).contains("strengths, improvements, nextActions는 각각 정확히 1개의 항목으로 응답한다.", "서버가 계산한 비교값이 있으면 comparisons는 정확히 1개의 항목으로 응답한다.", "서버가 계산한 비교값이 없으면 comparisons는 반드시 빈 배열로 응답한다.", "각 배열 항목의 text는 줄바꿈이나 목록 기호 없이 하나의 문단으로 작성한다.", "한 문단 안에는 자연스럽게 이어지는 여러 문장을 작성해도 된다.", "summary는 20~65자로 작성한다.", "comparisons의 text는 20~65자로 작성한다.", "strengths, improvements, nextActions의 text는 각각 20~65자로 작성한다.", "JSON을 반환하기 전에 공백과 문장부호를 포함한 summary와 모든 text의 글자 수가 20~65자인지 확인한다.", "최소 길이를 맞출 때 의미 없는 표현을 덧붙이지 말고 근거, 판단, 실행 방법을 보강해 다시 쓴다.", "65자를 넘으면 문장을 자르지 말고 핵심 내용을 남겨 다시 쓴다.").doesNotContain("comparisons, strengths, improvements, nextActions는 근거가 없으면 빈 배열로 응답해도 된다.", "45~55자");
        assertThat(prompt.getUser()).contains("대상 작업: 물 주기").contains("기록 횟수: 4회").contains("평균 작업 간격: 3.5일").contains("물을 준 방법: 호스로 조금씩 물을 줌").contains("지난 재배 기록 횟수: 3회").contains("record:" + recordId + "").contains("report:" + currentReportId + "").contains("report:" + previousReportId + "").contains("지난 재배보다 기록 횟수가 1회 늘었어요. 변화율은 33퍼센트예요.").contains("document-1").contains("황기 관수 기술", "관수 후 토양 수분을 확인한다.").doesNotContain("직전", "33.33퍼센트", "WATERING", "recordCount", "averageIntervalDays", "details=", "DRIP", "LOW", "code=", "label=", "CategoryRef").doesNotContain("FERTILIZING").doesNotContain("수확량");
    }

    @Test
    @DisplayName("prompt defines friendly and distinct roles for every public section")
    void prompt_defines_friendly_and_distinct_roles_for_every_public_section() throws Exception {
        var prompt = new ReportFeedbackPromptBuilder().build(context(), List.of());
        assertThat(prompt.getSystem()).contains("summary는 이번 재배에서 확인한 핵심을 작업과 기록 중심으로 균형 있게 요약한다.", "comparisons는 지난 재배와 달라진 사실만 설명하고 평가나 권고를 넣지 않는다.", "strengths는 근거에서 확인한 잘한 행동과 그 행동이 도움이 된 이유를 함께 설명한다.", "improvements는 부족한 점, 그 점이 판단이나 관리에 미친 영향, 앞으로 보완할 방향을 함께 설명한다.", "improvements는 작업 간격, 양, 방법, 시기처럼 실제 재배 행동에서 개선점을 먼저 찾는다.", "사진 첨부나 기록 누락 같은 기록 습관 이야기는 재배 행동에서 개선점을 찾을 수 없을 때만 사용한다.", "공식 기술 문서가 있으면 이번 작업 통계와 기록을 문서의 권장 방법에 비추어 개선 방향을 구체적으로 제시한다.", "자료가 부족해도 판단이 어렵거나 해석이 제한됐다는 설명으로 끝내지 않고, 지금 확인한 사실로 시도할 수 있는 재배 행동을 안내한다.", "nextActions는 다음 작업에서 언제 무엇을 어떻게 할지 실행 가능한 한 가지 재배 행동으로 작성한다.", "사용자에게 내부 보고서나 시스템을 설명하지 말고 농부가 남긴 작업과 기록을 먼저 말한다.", "공식 기술 문서를 근거로 사용해도 문서를 문장의 주어로 내세우지 않는다.", "다음 개선점 예시는 형식만 참고하고 내용을 복사하지 않는다.", "나쁜 예: \"정보가 없어 해석이 제한됐어요.\"", "나쁜 예: \"사진이 없어 상태를 알기 어려웠어요. 다음에는 사진을 남겨 보세요.\"", "좋은 예: \"물 주는 간격이 중간에 크게 벌어졌어요. 생육 초기에는 간격을 일정하게 지켜 보세요.\"", "summary, comparisons, strengths는 \"~했어요.\"처럼 회고형 존댓말로 작성한다.", "improvements는 부족한 점을 부드럽게 설명하고 보완 방향은 \"~해 보세요.\"처럼 제안한다.", "nextActions는 \"~하세요.\"처럼 분명한 행동형 존댓말로 작성한다.");
        assertThat(prompt.getSystem()).doesNotContain("다음에 함께 남길 기록 항목을 안내한다.", "언제 무엇을 기록하거나 확인할지", "좋은 예: \"기록에 필요한 정보가 빠져 판단하기 어려웠어요. 다음에는 빠진 정보도 함께 기록해 보세요.\"");
    }

    @Test
    @DisplayName("comparison percentage uses half up rounding without decimals")
    void comparison_percentage_uses_half_up_rounding_without_decimals() {
        for (var entry : List.of(Map.entry(new BigDecimal("33.33"), "33"), Map.entry(new BigDecimal("33.50"), "34"), Map.entry(new BigDecimal("0.49"), "0"))) {
            var base = context();
            var c = base.getComparisons().getFirst();
            var comparison = c.copy(c.getMetricKey(), c.getMetricLabel(), c.getCurrentValue(), c.getPreviousValue(), c.getDifference(), entry.getKey(), c.getUnit(), c.getCurrentCoverage(), c.getPreviousCoverage());
            var prompt = new ReportFeedbackPromptBuilder().build(base.copy(base.getSchemaVersion(), base.getWorkType(), base.getReport(), base.getRecords(), base.getPreviousReport(), List.of(comparison), base.getWarnings()), List.of());
            assertThat(prompt.getUser()).contains("변화율은 " + entry.getValue() + "퍼센트예요.").doesNotContain("변화율은 " + entry.getKey().toPlainString() + "퍼센트예요.");
        }
    }

    @Test
    @DisplayName("comparison keeps decrease direction while rounding the absolute percentage")
    void comparison_keeps_decrease_direction_while_rounding_the_absolute_percentage() throws Exception {
        var base = context();
        var comparison = base.getComparisons().get(0).copy(base.getComparisons().get(0).getMetricKey(), base.getComparisons().get(0).getMetricLabel(), base.getComparisons().get(0).getCurrentValue(), base.getComparisons().get(0).getPreviousValue(), new BigDecimal("-1"), new BigDecimal("-12.50"), base.getComparisons().get(0).getUnit(), base.getComparisons().get(0).getCurrentCoverage(), base.getComparisons().get(0).getPreviousCoverage());
        var prompt = new ReportFeedbackPromptBuilder().build(base.copy(base.getSchemaVersion(), base.getWorkType(), base.getReport(), base.getRecords(), base.getPreviousReport(), java.util.Arrays.asList(comparison), base.getWarnings()), List.of());
        assertThat(prompt.getUser()).contains("지난 재배보다 기록 횟수가 1회 줄었어요. 변화율은 13퍼센트예요.");
    }

    @Test
    @DisplayName("comparison omits unavailable percentage instead of explaining the calculation limit")
    void comparison_omits_unavailable_percentage_instead_of_explaining_the_calculation_limit() throws Exception {
        var base = context();
        var comparison = base.getComparisons().get(0).copy(base.getComparisons().get(0).getMetricKey(), base.getComparisons().get(0).getMetricLabel(), base.getComparisons().get(0).getCurrentValue(), base.getComparisons().get(0).getPreviousValue(), base.getComparisons().get(0).getDifference(), null, base.getComparisons().get(0).getUnit(), base.getComparisons().get(0).getCurrentCoverage(), base.getComparisons().get(0).getPreviousCoverage());
        var prompt = new ReportFeedbackPromptBuilder().build(base.copy(base.getSchemaVersion(), base.getWorkType(), base.getReport(), base.getRecords(), base.getPreviousReport(), java.util.Arrays.asList(comparison), base.getWarnings()), List.of());
        assertThat(prompt.getUser()).contains("지난 재배보다 기록 횟수가 1회 늘었어요.").doesNotContain("변화율");
    }

    @Test
    @DisplayName("comparison coverage calls the prior period last cultivation")
    void comparison_coverage_calls_the_prior_period_last_cultivation() throws Exception {
        var base = context();
        var comparison = base.getComparisons().get(0).copy(base.getComparisons().get(0).getMetricKey(), base.getComparisons().get(0).getMetricLabel(), base.getComparisons().get(0).getCurrentValue(), base.getComparisons().get(0).getPreviousValue(), base.getComparisons().get(0).getDifference(), base.getComparisons().get(0).getRelativeChangePct(), base.getComparisons().get(0).getUnit(), new Coverage(3, 4), new Coverage(2, 3));
        var prompt = new ReportFeedbackPromptBuilder().build(base.copy(base.getSchemaVersion(), base.getWorkType(), base.getReport(), base.getRecords(), base.getPreviousReport(), java.util.Arrays.asList(comparison), base.getWarnings()), List.of());
        assertThat(prompt.getUser()).contains("입력 범위는 이번 3/4건, 지난 재배 2/3건이에요.").doesNotContain("직전");
    }

    @Test
    @DisplayName("unknown enum code is omitted instead of falling back to raw code or label")
    void unknown_enum_code_is_omitted_instead_of_falling_back_to_raw_code_or_label() {
        var base = context();
        Map<String, Object> statistics = map(new AbstractMap.SimpleEntry<>("recordCount", 1), new AbstractMap.SimpleEntry<>("methodDistribution", List.of(Map.of("code", "NEW_METHOD", "label", "새 방식", "count", 1, "ratePct", 100))));
        var report = base.getReport();
        var revisedReport = report.copy(report.getId(), report.getFarmName(), report.getCropName(), report.getStartsAt(), report.getEndsAt(), report.getSourceRevision(), statistics);
        var records = base.getRecords().stream().map(it -> it.copy(it.getId(), it.getWorkedAt(), it.getWorkType(), it.getMemo(), Map.<String, Object>of("watering", Map.of("method", Map.of("code", "NEW_METHOD", "label", "새 방식"))))).toList();
        var context = base.copy(base.getSchemaVersion(), base.getWorkType(), revisedReport, records, base.getPreviousReport(), base.getComparisons(), base.getWarnings());
        var prompt = new ReportFeedbackPromptBuilder().build(context, List.of());
        assertThat(prompt.getUser()).doesNotContain("NEW_METHOD", "새 방식");
    }

    @Test
    @DisplayName("prompt requires an empty comparison section when the server has no comparison")
    void prompt_requires_an_empty_comparison_section_when_the_server_has_no_comparison() throws Exception {
        var context = context().copy(context().getSchemaVersion(), context().getWorkType(), context().getReport(), context().getRecords(), null, List.of(), context().getWarnings());
        var prompt = new ReportFeedbackPromptBuilder().build(context, List.of());
        assertThat(prompt.getSystem()).contains("서버가 계산한 비교값이 없으면 comparisons는 반드시 빈 배열로 응답한다.");
    }

    @Test
    @DisplayName("planting statistics include seed and seedling as easy planting descriptions")
    void planting_statistics_include_seed_and_seedling_as_easy_planting_descriptions() throws Exception {
        var base = context();
        var plantingContext = base.copy(base.getSchemaVersion(), WorkType.PLANTING, base.getReport().copy(base.getReport().getId(), base.getReport().getFarmName(), base.getReport().getCropName(), base.getReport().getStartsAt(), base.getReport().getEndsAt(), base.getReport().getSourceRevision(), map(new AbstractMap.SimpleEntry<>("recordCount", 2), new AbstractMap.SimpleEntry<>("propagationMethods", java.util.Arrays.asList(map(new AbstractMap.SimpleEntry<>("code", "SEED"), new AbstractMap.SimpleEntry<>("recordCount", 1)), map(new AbstractMap.SimpleEntry<>("code", "SEEDLING"), new AbstractMap.SimpleEntry<>("recordCount", 1)))))), List.of(), null, base.getComparisons(), base.getWarnings());
        var prompt = new ReportFeedbackPromptBuilder().build(plantingContext, List.of());
        assertThat(prompt.getUser()).contains("심은 방법: 씨앗을 심음 1회").contains("심은 방법: 모종을 심음 1회").doesNotContain("SEED", "SEEDLING");
    }

    @Test
    @DisplayName("fertilizer statistics keep liquid fertilizer names in the prompt")
    void fertilizer_statistics_keep_liquid_fertilizer_names_in_the_prompt() throws Exception {
        var base = context();
        var fertilizingContext = base.copy(base.getSchemaVersion(), WorkType.FERTILIZING, base.getReport().copy(base.getReport().getId(), base.getReport().getFarmName(), base.getReport().getCropName(), base.getReport().getStartsAt(), base.getReport().getEndsAt(), base.getReport().getSourceRevision(), map(new AbstractMap.SimpleEntry<>("recordCount", 1), new AbstractMap.SimpleEntry<>("materialDistribution", java.util.Arrays.asList(map(new AbstractMap.SimpleEntry<>("code", "액상비료"), new AbstractMap.SimpleEntry<>("label", "액상비료"), new AbstractMap.SimpleEntry<>("count", 1)))))), List.of(), null, base.getComparisons(), base.getWarnings());
        var prompt = new ReportFeedbackPromptBuilder().build(fertilizingContext, List.of());
        assertThat(prompt.getUser()).contains("거름 이름: 액상비료 1회");
    }

    @Test
    @DisplayName("pesticide statistics show the product label without exposing its id")
    void pesticide_statistics_show_the_product_label_without_exposing_its_id() throws Exception {
        var pesticideId = UUID.randomUUID();
        var base = context();
        var pestControlContext = base.copy(base.getSchemaVersion(), WorkType.PEST_CONTROL, base.getReport().copy(base.getReport().getId(), base.getReport().getFarmName(), base.getReport().getCropName(), base.getReport().getStartsAt(), base.getReport().getEndsAt(), base.getReport().getSourceRevision(), map(new AbstractMap.SimpleEntry<>("recordCount", 1), new AbstractMap.SimpleEntry<>("categoryDistribution", java.util.Arrays.asList(map(new AbstractMap.SimpleEntry<>("code", pesticideId.toString()), new AbstractMap.SimpleEntry<>("label", "가가방"), new AbstractMap.SimpleEntry<>("count", 1)))))), List.of(), null, base.getComparisons(), base.getWarnings());
        var prompt = new ReportFeedbackPromptBuilder().build(pestControlContext, List.of());
        assertThat(prompt.getUser()).contains("약 이름: 가가방 1회").doesNotContain(pesticideId.toString());
    }

    @Test
    @DisplayName("harvest records distinguish cultivated crops from wild collection in easy language")
    void harvest_records_distinguish_cultivated_crops_from_wild_collection_in_easy_language() throws Exception {
        var cultivatedPrompt = new ReportFeedbackPromptBuilder().build(harvestContext(HarvestSource.CULTIVATED), List.of());
        var foragedPrompt = new ReportFeedbackPromptBuilder().build(harvestContext(HarvestSource.FORAGED), List.of());
        assertThat(cultivatedPrompt.getUser()).contains("수확한 곳: 밭에서 기름").doesNotContain("산이나 들에서 얻음", HarvestSource.CULTIVATED.name());
        assertThat(foragedPrompt.getUser()).contains("수확한 곳: 산이나 들에서 얻음").doesNotContain("밭에서 기름", HarvestSource.FORAGED.name());
    }
    private ReportFeedbackContext harvestContext(HarvestSource harvestSource) {
        var base = context();
        return base.copy(base.getSchemaVersion(), WorkType.HARVEST, base.getReport().copy(base.getReport().getId(), base.getReport().getFarmName(), base.getReport().getCropName(), base.getReport().getStartsAt(), base.getReport().getEndsAt(), base.getReport().getSourceRevision(), map(new AbstractMap.SimpleEntry<>("recordCount", 1))), java.util.Arrays.asList(base.getRecords().get(0).copy(base.getRecords().get(0).getId(), base.getRecords().get(0).getWorkedAt(), WorkType.HARVEST, base.getRecords().get(0).getMemo(), map(new AbstractMap.SimpleEntry<>("harvest", map(new AbstractMap.SimpleEntry<>("harvestSource", map(new AbstractMap.SimpleEntry<>("code", harvestSource.name()), new AbstractMap.SimpleEntry<>("label", harvestSource.getLabel())))))))), null, base.getComparisons(), base.getWarnings());
    }
    private ReportFeedbackContext context() {
        return new ReportFeedbackContext(ReportFeedbackContextKt.REPORT_FEEDBACK_CONTEXT_SCHEMA_VERSION, WorkType.WATERING, new ReportFeedbackReport(currentReportId, "약초농장", "황기", LocalDateTime.of(2026, 3, 1, 9, 0), LocalDateTime.of(2026, 7, 1, 9, 0), 1, map(new AbstractMap.SimpleEntry<>("recordCount", 4), new AbstractMap.SimpleEntry<>("averageIntervalDays", 3.5), new AbstractMap.SimpleEntry<>("amountDistribution", java.util.Arrays.asList(map(new AbstractMap.SimpleEntry<>("code", "LOW"), new AbstractMap.SimpleEntry<>("label", "적음"), new AbstractMap.SimpleEntry<>("count", 2), new AbstractMap.SimpleEntry<>("ratePct", 50)))), new AbstractMap.SimpleEntry<>("methodDistribution", java.util.Arrays.asList(map(new AbstractMap.SimpleEntry<>("code", "DRIP"), new AbstractMap.SimpleEntry<>("label", "점적관수"), new AbstractMap.SimpleEntry<>("count", 4), new AbstractMap.SimpleEntry<>("ratePct", 100)))))), java.util.Arrays.asList(new ReportFeedbackRecord(recordId, LocalDateTime.of(2026, 4, 1, 9, 0), WorkType.WATERING, "점적관수를 했어요.", map(new AbstractMap.SimpleEntry<>("weatherCondition", "맑음"), new AbstractMap.SimpleEntry<>("weatherTemperature", 18), new AbstractMap.SimpleEntry<>("hasPhoto", false), new AbstractMap.SimpleEntry<>("watering", map(new AbstractMap.SimpleEntry<>("amount", map(new AbstractMap.SimpleEntry<>("code", "LOW"), new AbstractMap.SimpleEntry<>("label", "적음"))), new AbstractMap.SimpleEntry<>("method", map(new AbstractMap.SimpleEntry<>("code", "DRIP"), new AbstractMap.SimpleEntry<>("label", "점적관수")))))))), new ReportFeedbackPreviousReport(previousReportId, LocalDateTime.of(2025, 3, 1, 9, 0), LocalDateTime.of(2025, 7, 1, 9, 0), 1, map(new AbstractMap.SimpleEntry<>("recordCount", 3), new AbstractMap.SimpleEntry<>("averageIntervalDays", 4.0), new AbstractMap.SimpleEntry<>("methodDistribution", java.util.Arrays.asList(map(new AbstractMap.SimpleEntry<>("code", "DRIP"), new AbstractMap.SimpleEntry<>("label", "점적관수"), new AbstractMap.SimpleEntry<>("count", 3), new AbstractMap.SimpleEntry<>("ratePct", 100)))))), java.util.Arrays.asList(new ReportFeedbackComparison("recordCount", "기록 횟수", new BigDecimal("4"), new BigDecimal("3"), new BigDecimal("1"), new BigDecimal("33.33"), "회", null, null)), List.of());
    }
    @SafeVarargs
    private static <K> Map<K, Object> map(Map.Entry<K, ?>... entries) {
        Map<K, Object> result = new LinkedHashMap<>();
        for (var entry : entries) result.put(entry.getKey(), entry.getValue());
        return result;
    }

}
