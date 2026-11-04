package com.chamchamcham.application.coaching.reportfeedback.generation;

import com.chamchamcham.domain.farming.WorkType;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.*;
import org.junit.jupiter.api.DisplayName;
import com.chamchamcham.application.coaching.reportfeedback.generation.ReportFeedbackContextKt;

class ReportFeedbackOutputValidatorTest {
    private final UUID reportId = UUID.randomUUID();
    private final UUID previousReportId = UUID.randomUUID();
    private final UUID recordId = UUID.randomUUID();
    private final ReportFeedbackContext context = new ReportFeedbackContext(ReportFeedbackContextKt.REPORT_FEEDBACK_CONTEXT_SCHEMA_VERSION, WorkType.WATERING, new ReportFeedbackReport(reportId, "약초농장", "황기", LocalDateTime.of(2026, 3, 1, 9, 0), LocalDateTime.of(2026, 7, 1, 9, 0), 1, map(new AbstractMap.SimpleEntry<>("recordCount", 1))), java.util.Arrays.asList(new ReportFeedbackRecord(recordId, LocalDateTime.of(2026, 4, 1, 9, 0), WorkType.WATERING, "관수", Map.of())), new ReportFeedbackPreviousReport(previousReportId, LocalDateTime.of(2025, 3, 1, 9, 0), LocalDateTime.of(2025, 7, 1, 9, 0), 1, map(new AbstractMap.SimpleEntry<>("recordCount", 1))), java.util.Arrays.asList(serverComparison()), List.of());

    @Test
    @DisplayName("rejects generated comparison when the server has no comparable difference")
    void rejects_generated_comparison_when_the_server_has_no_comparable_difference() throws Exception {
        var unavailableContext = context.copy(context.getSchemaVersion(), context.getWorkType(), context.getReport(), context.getRecords(), null, List.of(), context.getWarnings());
        var content = contentWithComparison(java.util.Arrays.asList("report:" + reportId + ""));
        assertThat(ReportFeedbackOutputValidator.INSTANCE.validate(content, unavailableContext, List.of())).containsExactly("comparison_not_available");
    }

    @Test
    @DisplayName("requires the current report reference on every comparison")
    void requires_the_current_report_reference_on_every_comparison() throws Exception {
        var content = contentWithComparison(java.util.Arrays.asList("report:" + previousReportId + ""));
        assertThat(ReportFeedbackOutputValidator.INSTANCE.validate(content, context, List.of())).containsExactly("comparison_current_report_ref_required");
    }

    @Test
    @DisplayName("requires the previous report reference on every comparison")
    void requires_the_previous_report_reference_on_every_comparison() throws Exception {
        var content = contentWithComparison(java.util.Arrays.asList("report:" + reportId + ""));
        assertThat(ReportFeedbackOutputValidator.INSTANCE.validate(content, context, List.of())).containsExactly("comparison_previous_report_ref_required");
    }

    @Test
    @DisplayName("record evidence alone cannot ground a comparison")
    void record_evidence_alone_cannot_ground_a_comparison() throws Exception {
        var content = contentWithComparison(java.util.Arrays.asList("record:" + recordId + ""));
        assertThat(ReportFeedbackOutputValidator.INSTANCE.validate(content, context, List.of())).containsExactly("comparison_current_report_ref_required", "comparison_previous_report_ref_required");
    }

    @Test
    @DisplayName("allows polite Korean comparison grounded in current and previous reports")
    void allows_polite_Korean_comparison_grounded_in_current_and_previous_reports() throws Exception {
        var content = validContent(java.util.Arrays.asList(comparisonItem(java.util.Arrays.asList("report:" + reportId + "", "report:" + previousReportId + ""), "직전 재배보다 물 주기 기록이 한 번 늘어 흐름이 안정됐어요.")));
        assertThat(ReportFeedbackOutputValidator.INSTANCE.validate(content, context, List.of())).isEmpty();
    }

    @Test
    @DisplayName("allows stylistic deviations but still rejects unknown evidence in comparison")
    void allows_stylistic_deviations_but_still_rejects_unknown_evidence_in_comparison() throws Exception {
        var unknown = "report:" + UUID.randomUUID() + "";
        var content = validContent(java.util.Arrays.asList(comparisonItem(java.util.Arrays.asList("report:" + reportId + "", "report:" + previousReportId + ""), "직전 재배보다 물 주기 기록이 한 번 늘어 흐름이 안정됐어요."))).copy(validContent(java.util.Arrays.asList(comparisonItem(java.util.Arrays.asList("report:" + reportId + "", "report:" + previousReportId + ""), "직전 재배보다 물 주기 기록이 한 번 늘어 흐름이 안정됐어요."))).getSummary(), java.util.Arrays.asList(comparisonItem(java.util.Arrays.asList("report:" + reportId + "", "report:" + previousReportId + "", unknown), "WATERING 기록이 직전보다 늘어 작업 흐름의 변화를 확인했어요.")), validContent(java.util.Arrays.asList(comparisonItem(java.util.Arrays.asList("report:" + reportId + "", "report:" + previousReportId + ""), "직전 재배보다 물 주기 기록이 한 번 늘어 흐름이 안정됐어요."))).getStrengths(), validContent(java.util.Arrays.asList(comparisonItem(java.util.Arrays.asList("report:" + reportId + "", "report:" + previousReportId + ""), "직전 재배보다 물 주기 기록이 한 번 늘어 흐름이 안정됐어요."))).getImprovements(), validContent(java.util.Arrays.asList(comparisonItem(java.util.Arrays.asList("report:" + reportId + "", "report:" + previousReportId + ""), "직전 재배보다 물 주기 기록이 한 번 늘어 흐름이 안정됐어요."))).getNextActions());
        assertThat(ReportFeedbackOutputValidator.INSTANCE.validate(content, context, List.of())).containsExactly("unknown_evidence:" + unknown + "");
    }

    @Test
    @DisplayName("allows the same fact repeated across comparison and another section")
    void allows_the_same_fact_repeated_across_comparison_and_another_section() throws Exception {
        var comparison = new ReportFeedbackContentItem("직전보다 기록 1회 증가", "직전 재배보다 물 주기 기록이 한 번 늘어 작업 흐름이 안정됐어요.", java.util.Arrays.asList("report:" + reportId + "", "report:" + previousReportId + ""));
        var repeatedWithDifferentBasis = comparison.copy("이번 기록과 직전 기록의 차이", comparison.getText(), comparison.getEvidenceRefs());
        var content = validContent(java.util.Arrays.asList(comparisonItem(java.util.Arrays.asList("report:" + reportId + "", "report:" + previousReportId + ""), "직전 재배보다 물 주기 기록이 한 번 늘어 흐름이 안정됐어요."))).copy(validContent(java.util.Arrays.asList(comparisonItem(java.util.Arrays.asList("report:" + reportId + "", "report:" + previousReportId + ""), "직전 재배보다 물 주기 기록이 한 번 늘어 흐름이 안정됐어요."))).getSummary(), java.util.Arrays.asList(comparison), java.util.Arrays.asList(repeatedWithDifferentBasis), validContent(java.util.Arrays.asList(comparisonItem(java.util.Arrays.asList("report:" + reportId + "", "report:" + previousReportId + ""), "직전 재배보다 물 주기 기록이 한 번 늘어 흐름이 안정됐어요."))).getImprovements(), validContent(java.util.Arrays.asList(comparisonItem(java.util.Arrays.asList("report:" + reportId + "", "report:" + previousReportId + ""), "직전 재배보다 물 주기 기록이 한 번 늘어 흐름이 안정됐어요."))).getNextActions());
        assertThat(ReportFeedbackOutputValidator.INSTANCE.validate(content, context, List.of())).isEmpty();
    }

    @Test
    @DisplayName("requires exactly one item for each mandatory paragraph section")
    void requires_exactly_one_item_for_each_mandatory_paragraph_section() throws Exception {
        var content = validContent(java.util.Arrays.asList(comparisonItem(java.util.Arrays.asList("report:" + reportId + "", "report:" + previousReportId + ""), "직전 재배보다 물 주기 기록이 한 번 늘어 흐름이 안정됐어요."))).copy(validContent(java.util.Arrays.asList(comparisonItem(java.util.Arrays.asList("report:" + reportId + "", "report:" + previousReportId + ""), "직전 재배보다 물 주기 기록이 한 번 늘어 흐름이 안정됐어요."))).getSummary(), validContent(java.util.Arrays.asList(comparisonItem(java.util.Arrays.asList("report:" + reportId + "", "report:" + previousReportId + ""), "직전 재배보다 물 주기 기록이 한 번 늘어 흐름이 안정됐어요."))).getComparisons(), List.of(), java.util.Arrays.asList(item("관수 1회", "흙 상태를 살핀 뒤 물을 주어 필요한 곳부터 관리한 점이 좋았어요.", "record:" + recordId + ""), item("관수 1회", "흙 속 수분도 함께 확인해 물의 양과 젖은 깊이를 기록할 필요가 있어요.", "record:" + recordId + "")), List.of());
        assertThat(ReportFeedbackOutputValidator.INSTANCE.validate(content, context, List.of())).containsExactly("strength_count", "improvement_count", "next_action_count");
    }

    @Test
    @DisplayName("requires one comparison paragraph when server comparison exists")
    void requires_one_comparison_paragraph_when_server_comparison_exists() throws Exception {
        var content = validContent(List.of());
        assertThat(ReportFeedbackOutputValidator.INSTANCE.validate(content, context, List.of())).containsExactly("comparison_count");
    }

    @Test
    @DisplayName("allows no comparison paragraph when server comparison is unavailable")
    void allows_no_comparison_paragraph_when_server_comparison_is_unavailable() throws Exception {
        var unavailableContext = context.copy(context.getSchemaVersion(), context.getWorkType(), context.getReport(), context.getRecords(), null, List.of(), context.getWarnings());
        var content = validContent(List.of());
        assertThat(ReportFeedbackOutputValidator.INSTANCE.validate(content, unavailableContext, List.of())).isEmpty();
    }

    @Test
    @DisplayName("allows line breaks for generation service normalization")
    void allows_line_breaks_for_generation_service_normalization() throws Exception {
        var content = validContent(java.util.Arrays.asList(comparisonItem(java.util.Arrays.asList("report:" + reportId + "", "report:" + previousReportId + ""), "직전 재배보다 물 주기 기록이 한 번 늘어 흐름이 안정됐어요."))).copy(validContent(java.util.Arrays.asList(comparisonItem(java.util.Arrays.asList("report:" + reportId + "", "report:" + previousReportId + ""), "직전 재배보다 물 주기 기록이 한 번 늘어 흐름이 안정됐어요."))).getSummary(), validContent(java.util.Arrays.asList(comparisonItem(java.util.Arrays.asList("report:" + reportId + "", "report:" + previousReportId + ""), "직전 재배보다 물 주기 기록이 한 번 늘어 흐름이 안정됐어요."))).getComparisons(), validContent(java.util.Arrays.asList(comparisonItem(java.util.Arrays.asList("report:" + reportId + "", "report:" + previousReportId + ""), "직전 재배보다 물 주기 기록이 한 번 늘어 흐름이 안정됐어요."))).getStrengths(), validContent(java.util.Arrays.asList(comparisonItem(java.util.Arrays.asList("report:" + reportId + "", "report:" + previousReportId + ""), "직전 재배보다 물 주기 기록이 한 번 늘어 흐름이 안정됐어요."))).getImprovements(), java.util.Arrays.asList(item("관수 1회", "다음에는 흙 속을 확인하세요.\n젖은 깊이와 물의 양도 함께 기록하세요.", "record:" + recordId + "")));
        assertThat(ReportFeedbackOutputValidator.INSTANCE.validate(content, context, List.of())).isEmpty();
    }

    @Test
    @DisplayName("allows record-backed feedback without a retrieved technical document")
    void allows_record_backed_feedback_without_a_retrieved_technical_document() throws Exception {
        var content = validContent(java.util.Arrays.asList(comparisonItem(java.util.Arrays.asList("report:" + reportId + "", "report:" + previousReportId + ""), "직전 재배보다 물 주기 기록이 한 번 늘어 흐름이 안정됐어요."))).copy("이번 물 주기 기록을 꾸준히 남겼어요.", validContent(java.util.Arrays.asList(comparisonItem(java.util.Arrays.asList("report:" + reportId + "", "report:" + previousReportId + ""), "직전 재배보다 물 주기 기록이 한 번 늘어 흐름이 안정됐어요."))).getComparisons(), java.util.Arrays.asList(item("관수 1회", "물 준 기록을 꾸준히 남겨 전체 작업 흐름을 확인하기 좋았어요.", "record:" + recordId + "")), validContent(java.util.Arrays.asList(comparisonItem(java.util.Arrays.asList("report:" + reportId + "", "report:" + previousReportId + ""), "직전 재배보다 물 주기 기록이 한 번 늘어 흐름이 안정됐어요."))).getImprovements(), validContent(java.util.Arrays.asList(comparisonItem(java.util.Arrays.asList("report:" + reportId + "", "report:" + previousReportId + ""), "직전 재배보다 물 주기 기록이 한 번 늘어 흐름이 안정됐어요."))).getNextActions());
        assertThat(ReportFeedbackOutputValidator.INSTANCE.validate(content, context, List.of())).isEmpty();
    }

    @Test
    @DisplayName("rejects multiple items and an unknown evidence reference")
    void rejects_multiple_items_and_an_unknown_evidence_reference() throws Exception {
        var duplicate = item("관수 1회", "흙 상태를 살핀 뒤 물을 주어 필요한 곳부터 관리한 점이 좋았어요.", "record:" + recordId + "");
        var unknown = "record:" + UUID.randomUUID() + "";
        var content = validContent(java.util.Arrays.asList(comparisonItem(java.util.Arrays.asList("report:" + reportId + "", "report:" + previousReportId + ""), "직전 재배보다 물 주기 기록이 한 번 늘어 흐름이 안정됐어요."))).copy(validContent(java.util.Arrays.asList(comparisonItem(java.util.Arrays.asList("report:" + reportId + "", "report:" + previousReportId + ""), "직전 재배보다 물 주기 기록이 한 번 늘어 흐름이 안정됐어요."))).getSummary(), validContent(java.util.Arrays.asList(comparisonItem(java.util.Arrays.asList("report:" + reportId + "", "report:" + previousReportId + ""), "직전 재배보다 물 주기 기록이 한 번 늘어 흐름이 안정됐어요."))).getComparisons(), java.util.Arrays.asList(duplicate, duplicate), java.util.Arrays.asList(item("관수 1회", "흙 상태를 살핀 뒤 물을 주어 필요한 곳부터 관리한 점이 좋았어요.", unknown)), validContent(java.util.Arrays.asList(comparisonItem(java.util.Arrays.asList("report:" + reportId + "", "report:" + previousReportId + ""), "직전 재배보다 물 주기 기록이 한 번 늘어 흐름이 안정됐어요."))).getNextActions());
        assertThat(ReportFeedbackOutputValidator.INSTANCE.validate(content, context, List.of())).contains("strength_count", "unknown_evidence:" + unknown + "");
    }

    @Test
    @DisplayName("allows a summary and item text with formal honorifics")
    void allows_a_summary_and_item_text_with_formal_honorifics() throws Exception {
        var content = validContent(java.util.Arrays.asList(comparisonItem(java.util.Arrays.asList("report:" + reportId + "", "report:" + previousReportId + ""), "직전 재배보다 물 주기 기록이 한 번 늘어 흐름이 안정됐어요."))).copy("이번 물 주기 기록을 꾸준히 남겨 전체 작업 흐름을 확인할 수 있습니다.", validContent(java.util.Arrays.asList(comparisonItem(java.util.Arrays.asList("report:" + reportId + "", "report:" + previousReportId + ""), "직전 재배보다 물 주기 기록이 한 번 늘어 흐름이 안정됐어요."))).getComparisons(), java.util.Arrays.asList(item("관수 1회", "물 준 기록을 꾸준히 남겨 전체 작업 흐름을 확인할 수 있습니다.", "record:" + recordId + "")), validContent(java.util.Arrays.asList(comparisonItem(java.util.Arrays.asList("report:" + reportId + "", "report:" + previousReportId + ""), "직전 재배보다 물 주기 기록이 한 번 늘어 흐름이 안정됐어요."))).getImprovements(), validContent(java.util.Arrays.asList(comparisonItem(java.util.Arrays.asList("report:" + reportId + "", "report:" + previousReportId + ""), "직전 재배보다 물 주기 기록이 한 번 늘어 흐름이 안정됐어요."))).getNextActions());
        assertThat(ReportFeedbackOutputValidator.INSTANCE.validate(content, context, List.of())).isEmpty();
    }

    @Test
    @DisplayName("allows English and Korean farming terms in public report text")
    void allows_English_and_Korean_farming_terms_in_public_report_text() throws Exception {
        var content = validContent(java.util.Arrays.asList(comparisonItem(java.util.Arrays.asList("report:" + reportId + "", "report:" + previousReportId + ""), "직전 재배보다 물 주기 기록이 한 번 늘어 흐름이 안정됐어요."))).copy("WATERING 작업 기록의 전체 흐름과 관리 방향을 확인했어요.", validContent(java.util.Arrays.asList(comparisonItem(java.util.Arrays.asList("report:" + reportId + "", "report:" + previousReportId + ""), "직전 재배보다 물 주기 기록이 한 번 늘어 흐름이 안정됐어요."))).getComparisons(), java.util.Arrays.asList(item("DRIP 관수", "DRIP으로 물을 준 기록을 꾸준히 남겨 작업 흐름을 확인하기 좋았어요.", "record:" + recordId + "")), java.util.Arrays.asList(item("토양", "토양 수분을 더 확인하고 물을 준 양과 상태를 함께 기록할 필요가 있어요.", "record:" + recordId + "")), java.util.Arrays.asList(item("방제", "다음에는 병해충 상태를 살핀 뒤 필요한 방제 방법을 정해 실행하세요.", "record:" + recordId + "")));
        assertThat(ReportFeedbackOutputValidator.INSTANCE.validate(content, context, List.of())).isEmpty();
    }

    @Test
    @DisplayName("allows English in every report item section")
    void allows_English_in_every_report_item_section() throws Exception {
        var content = validContent(java.util.Arrays.asList(comparisonItem(java.util.Arrays.asList("report:" + reportId + "", "report:" + previousReportId + ""), "직전 재배보다 물 주기 기록이 한 번 늘어 흐름이 안정됐어요."))).copy("WATERING 작업 기록의 전체 흐름과 관리 방향을 확인했어요.", validContent(java.util.Arrays.asList(comparisonItem(java.util.Arrays.asList("report:" + reportId + "", "report:" + previousReportId + ""), "직전 재배보다 물 주기 기록이 한 번 늘어 흐름이 안정됐어요."))).getComparisons(), java.util.Arrays.asList(item("기록", "DRIP으로 물을 준 기록을 꾸준히 남겨 작업 흐름을 확인하기 좋았어요.", "record:" + recordId + "")), java.util.Arrays.asList(item("양", "다음에는 kg 단위 대신 물의 양과 흙 상태를 쉬운 한글로 함께 기록하세요.", "record:" + recordId + "")), java.util.Arrays.asList(item("확인", "다음에는 pH 수치와 흙 상태를 쉬운 말로 함께 기록해 변화를 확인하세요.", "record:" + recordId + "")));
        assertThat(ReportFeedbackOutputValidator.INSTANCE.validate(content, context, List.of())).isEmpty();
    }

    @Test
    @DisplayName("allows summary and every section text at exactly sixty five characters")
    void allows_summary_and_every_section_text_at_exactly_sixty_five_characters() throws Exception {
        var text = "가".repeat(65);
        var content = validContent(java.util.Arrays.asList(comparisonItem(java.util.Arrays.asList("report:" + reportId + "", "report:" + previousReportId + ""), "직전 재배보다 물 주기 기록이 한 번 늘어 흐름이 안정됐어요."))).copy(text, java.util.Arrays.asList(comparisonItem(java.util.Arrays.asList("report:" + reportId + "", "report:" + previousReportId + ""), text)), java.util.Arrays.asList(item("관수 1회", text, "record:" + recordId + "")), java.util.Arrays.asList(item("관수 1회", text, "record:" + recordId + "")), java.util.Arrays.asList(item("관수 1회", text, "record:" + recordId + "")));
        assertThat(ReportFeedbackOutputValidator.INSTANCE.validate(content, context, List.of())).isEmpty();
    }

    @Test
    @DisplayName("allows every public text at its exact minimum length")
    void allows_every_public_text_at_its_exact_minimum_length() throws Exception {
        var text = "가".repeat(20);
        var content = validContent(java.util.Arrays.asList(comparisonItem(java.util.Arrays.asList("report:" + reportId + "", "report:" + previousReportId + ""), "직전 재배보다 물 주기 기록이 한 번 늘어 흐름이 안정됐어요."))).copy(text, java.util.Arrays.asList(comparisonItem(java.util.Arrays.asList("report:" + reportId + "", "report:" + previousReportId + ""), text)), java.util.Arrays.asList(item("관수 1회", text, "record:" + recordId + "")), java.util.Arrays.asList(item("관수 1회", text, "record:" + recordId + "")), java.util.Arrays.asList(item("관수 1회", text, "record:" + recordId + "")));
        assertThat(ReportFeedbackOutputValidator.INSTANCE.validate(content, context, List.of())).isEmpty();
    }

    @Test
    @DisplayName("rejects every public text below its role minimum")
    void rejects_every_public_text_below_its_role_minimum() throws Exception {
        var text = "가".repeat(19);
        var content = validContent(java.util.Arrays.asList(comparisonItem(java.util.Arrays.asList("report:" + reportId + "", "report:" + previousReportId + ""), "직전 재배보다 물 주기 기록이 한 번 늘어 흐름이 안정됐어요."))).copy(text, java.util.Arrays.asList(comparisonItem(java.util.Arrays.asList("report:" + reportId + "", "report:" + previousReportId + ""), text)), java.util.Arrays.asList(item("관수 1회", text, "record:" + recordId + "")), java.util.Arrays.asList(item("관수 1회", text, "record:" + recordId + "")), java.util.Arrays.asList(item("관수 1회", text, "record:" + recordId + "")));
        assertThat(ReportFeedbackOutputValidator.INSTANCE.validate(content, context, List.of())).containsExactly("summary_text_length", "comparison_text_length", "strength_text_length", "improvement_text_length", "next_action_text_length");
    }

    @Test
    @DisplayName("rejects summary and every section text over sixty five characters")
    void rejects_summary_and_every_section_text_over_sixty_five_characters() throws Exception {
        var text = "가".repeat(66);
        var content = validContent(java.util.Arrays.asList(comparisonItem(java.util.Arrays.asList("report:" + reportId + "", "report:" + previousReportId + ""), "직전 재배보다 물 주기 기록이 한 번 늘어 흐름이 안정됐어요."))).copy(text, java.util.Arrays.asList(comparisonItem(java.util.Arrays.asList("report:" + reportId + "", "report:" + previousReportId + ""), text)), java.util.Arrays.asList(item("관수 1회", text, "record:" + recordId + "")), java.util.Arrays.asList(item("관수 1회", text, "record:" + recordId + "")), java.util.Arrays.asList(item("관수 1회", text, "record:" + recordId + "")));
        assertThat(ReportFeedbackOutputValidator.INSTANCE.validate(content, context, List.of())).containsExactly("summary_text_length", "comparison_text_length", "strength_text_length", "improvement_text_length", "next_action_text_length");
    }

    @Test
    @DisplayName("deserializes omitted item sections as empty lists")
    void deserializes_omitted_item_sections_as_empty_lists() throws Exception {
        var content = com.fasterxml.jackson.module.kotlin.ExtensionsKt.jacksonObjectMapper().readValue("{\"summary\":\"이번 물 주기 기록을 확인했어요.\"}", ReportFeedbackContent.class);
        assertThat(content.getComparisons()).isEmpty();
        assertThat(content.getStrengths()).isEmpty();
        assertThat(content.getImprovements()).isEmpty();
        assertThat(content.getNextActions()).isEmpty();
    }

    @Test
    @DisplayName("allows technical language in internal basis")
    void allows_technical_language_in_internal_basis() throws Exception {
        var content = validContent(java.util.Arrays.asList(comparisonItem(java.util.Arrays.asList("report:" + reportId + "", "report:" + previousReportId + ""), "직전 재배보다 물 주기 기록이 한 번 늘어 흐름이 안정됐어요."))).copy("이번 물 주기 기록의 전체 흐름과 관리 방향을 확인했어요.", validContent(java.util.Arrays.asList(comparisonItem(java.util.Arrays.asList("report:" + reportId + "", "report:" + previousReportId + ""), "직전 재배보다 물 주기 기록이 한 번 늘어 흐름이 안정됐어요."))).getComparisons(), java.util.Arrays.asList(item("WATERING DRIP 관수", "물 준 방법을 꾸준히 지키고 기록까지 남긴 점은 관리에 도움이 됐어요.", "record:" + recordId + "")), validContent(java.util.Arrays.asList(comparisonItem(java.util.Arrays.asList("report:" + reportId + "", "report:" + previousReportId + ""), "직전 재배보다 물 주기 기록이 한 번 늘어 흐름이 안정됐어요."))).getImprovements(), validContent(java.util.Arrays.asList(comparisonItem(java.util.Arrays.asList("report:" + reportId + "", "report:" + previousReportId + ""), "직전 재배보다 물 주기 기록이 한 번 늘어 흐름이 안정됐어요."))).getNextActions());
        assertThat(ReportFeedbackOutputValidator.INSTANCE.validate(content, context, List.of())).isEmpty();
    }
    private ReportFeedbackContentItem item(String basis, String text, String evidenceRef) {
        return new ReportFeedbackContentItem(basis, text, java.util.Arrays.asList(evidenceRef));
    }
    private ReportFeedbackContentItem comparisonItem(List<String> evidenceRefs, String text) {
        return new ReportFeedbackContentItem("직전보다 기록 1회 증가", text, evidenceRefs);
    }
    private ReportFeedbackContent validContent(List<ReportFeedbackContentItem> comparisons) {
        return new ReportFeedbackContent("이번 물 주기 기록의 전체 흐름과 관리 방향을 확인했어요.", comparisons, java.util.Arrays.asList(item("관수 1회", "흙 상태를 살핀 뒤 물을 주어 필요한 곳부터 관리한 점이 좋았어요.", "record:" + recordId + "")), java.util.Arrays.asList(item("물의 양", "물의 양이 알맞았는지 흙 속 수분까지 확인해 기록할 필요가 있어요.", "record:" + recordId + "")), java.util.Arrays.asList(item("흙 속 수분", "다음에는 물을 준 뒤 흙 속까지 젖었는지 손으로 확인해 기록하세요.", "record:" + recordId + "")));
    }
    private ReportFeedbackContent contentWithComparison(List<String> evidenceRefs) {
        return validContent(java.util.Arrays.asList(comparisonItem(evidenceRefs, "직전 재배보다 물 주기 기록이 한 번 늘어 흐름이 안정됐어요.")));
    }
    private ReportFeedbackComparison serverComparison() {
        return new ReportFeedbackComparison("recordCount", "기록 횟수", new BigDecimal("2"), new BigDecimal("1"), new BigDecimal("1"), new BigDecimal("100"), "회", null, null);
    }
    @SafeVarargs
    private static <K> Map<K, Object> map(Map.Entry<K, ?>... entries) {
        Map<K, Object> result = new LinkedHashMap<>();
        for (var entry : entries) result.put(entry.getKey(), entry.getValue());
        return result;
    }

}
