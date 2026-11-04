package com.chamchamcham.application.coaching.recordfeedback.generation;

import com.chamchamcham.domain.coaching.recordfeedback.RecordFeedbackActionCategory;
import com.chamchamcham.domain.coaching.recordfeedback.RecordFeedbackActionDue;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import java.util.*;
import org.junit.jupiter.api.DisplayName;
import com.chamchamcham.application.coaching.reportfeedback.generation.ReportFeedbackContextKt;

class RecordFeedbackOutputValidatorTest {
    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();
    private final RecordFeedbackContext context = readFixture("today-record-feedback-watering.json");
    private final List<RecordFeedbackEvidence> documents = java.util.Arrays.asList(new RecordFeedbackEvidence("doc-1", "농업기술길잡이 007 약용작물", 123, "관수 후 토양 상태를 확인한다."));

    @Test
    @DisplayName("validates exactly one good point and two to three cited actions")
    void validates_exactly_one_good_point_and_two_to_three_cited_actions() throws Exception {
        var warnings = RecordFeedbackOutputValidator.validate(validResult(RecordFeedbackActionCategory.IRRIGATION, java.util.Arrays.asList(context.recordCitationId(), "doc-1")), context, documents);
        assertThat(warnings).isEmpty();
    }

    @Test
    @DisplayName("rejects text below 15 characters and unknown evidence")
    void rejects_text_below_15_characters_and_unknown_evidence() throws Exception {
        var invalid = new RecordFeedbackContent(validItem("점적관수", "짧음", java.util.Arrays.asList("unknown")), validResult(RecordFeedbackActionCategory.IRRIGATION, java.util.Arrays.asList(context.recordCitationId(), "doc-1")).getNextActions());
        assertThat(RecordFeedbackOutputValidator.validate(invalid, context, documents)).contains("good_point_text_length", "unknown_evidence");
    }

    @Test
    @DisplayName("rejects text that does not end in a friendly honorific")
    void rejects_text_that_does_not_end_in_a_friendly_honorific() throws Exception {
        var invalid = new RecordFeedbackContent(validItem("점적관수", "흙 상태를 꼼꼼하게 확인했다.", java.util.Arrays.asList(context.recordCitationId())), validResult(RecordFeedbackActionCategory.IRRIGATION, java.util.Arrays.asList(context.recordCitationId(), "doc-1")).getNextActions());
        assertThat(RecordFeedbackOutputValidator.validate(invalid, context, documents)).contains("good_point_text_tone");
    }

    @Test
    @DisplayName("rejects English only in public text")
    void rejects_English_only_in_public_text() throws Exception {
        var invalid = new RecordFeedbackContent(validItem("DRIP 관수", "DRIP으로 물을 준 점은 잘했어요.", java.util.Arrays.asList(context.recordCitationId())), java.util.Arrays.asList(validAction(RecordFeedbackActionDue.THIS_WEEK, RecordFeedbackActionCategory.WEATHER, "비 예보", "비 오기 전 물길을 정리하세요.", java.util.Arrays.asList("weather:2026-07-04")), validAction(RecordFeedbackActionDue.NEXT_CHECK, RecordFeedbackActionCategory.IRRIGATION, "토양 수분", "다음에는 토양 수분을 확인하세요.", java.util.Arrays.asList(context.recordCitationId(), "doc-1"))));
        assertThat(RecordFeedbackOutputValidator.validate(invalid, context, documents)).contains("good_point_text_english").doesNotContain("next_action_1_text_english", "good_point_basis_english");
    }

    @Test
    @DisplayName("rejects English in next action text")
    void rejects_English_in_next_action_text() throws Exception {
        var valid = validResult(RecordFeedbackActionCategory.IRRIGATION, java.util.Arrays.asList(context.recordCitationId(), "doc-1"));
        var invalid = new RecordFeedbackContent(valid.getGoodPoint(), java.util.Arrays.asList(valid.getNextActions().get(0), new RecordFeedbackAction(valid.getNextActions().get(1).getDue(), valid.getNextActions().get(1).getCategory(), valid.getNextActions().get(1).getBasis(), "DRIP 상태를 다음에 확인하세요.", valid.getNextActions().get(1).getEvidenceRefs())));
        assertThat(RecordFeedbackOutputValidator.validate(invalid, context, documents)).contains("next_action_1_text_english");
    }

    @Test
    @DisplayName("rejects weather action without weather evidence")
    void rejects_weather_action_without_weather_evidence() throws Exception {
        var invalid = validResult(RecordFeedbackActionCategory.WEATHER, java.util.Arrays.asList(context.recordCitationId()));
        assertThat(RecordFeedbackOutputValidator.validate(invalid, context, documents)).contains("weather_action_without_weather_evidence");
    }

    @Test
    @DisplayName("allows a hidden basis while rejecting blank fields and wrong action count")
    void allows_a_hidden_basis_while_rejecting_blank_fields_and_wrong_action_count() throws Exception {
        var invalid = new RecordFeedbackContent(validItem(" ", "흙 상태를 살핀 점은 잘했어요.", java.util.Arrays.asList(context.recordCitationId())), java.util.Arrays.asList(validAction(RecordFeedbackActionDue.NEXT_CHECK, RecordFeedbackActionCategory.IRRIGATION, "비 예보", "배수로 막힌 흙을 치우세요.", java.util.Arrays.asList("weather:2026-07-04"))));
        var warnings = RecordFeedbackOutputValidator.validate(invalid, context, documents);
        assertThat(warnings).contains("good_point_basis_blank", "action_count");
        assertThat(warnings).doesNotContain("next_action_0_basis_token_missing");
    }

    @Test
    @DisplayName("accepts text at exact 15 and 60 character boundaries")
    void accepts_text_at_exact_15_and_60_character_boundaries() throws Exception {
        var valid = new RecordFeedbackContent(validItem("토양", textOfLengthWithBasis("흙", 15), java.util.Arrays.asList(context.recordCitationId())), java.util.Arrays.asList(validAction(RecordFeedbackActionDue.THIS_WEEK, RecordFeedbackActionCategory.WEATHER, "비예보", textOfLengthWithBasis("비예보", 60), java.util.Arrays.asList("weather:2026-07-04")), validAction(RecordFeedbackActionDue.NEXT_CHECK, RecordFeedbackActionCategory.IRRIGATION, "토양", textOfLengthWithBasis("흙", 15), java.util.Arrays.asList(context.recordCitationId(), "doc-1"))));
        var warnings = RecordFeedbackOutputValidator.validate(valid, context, documents);
        assertThat(warnings).doesNotContain("good_point_text_length", "next_action_0_text_length", "next_action_1_text_length");
        assertThat(warnings).isEmpty();
    }

    @Test
    @DisplayName("rejects text at 14 and 61 character boundaries")
    void rejects_text_at_14_and_61_character_boundaries() throws Exception {
        var invalid = new RecordFeedbackContent(validItem("토양", textOfLengthWithBasis("흙", 14), java.util.Arrays.asList(context.recordCitationId())), java.util.Arrays.asList(validAction(RecordFeedbackActionDue.THIS_WEEK, RecordFeedbackActionCategory.WEATHER, "비예보", textOfLengthWithBasis("비예보", 61), java.util.Arrays.asList("weather:2026-07-04")), validAction(RecordFeedbackActionDue.NEXT_CHECK, RecordFeedbackActionCategory.IRRIGATION, "토양 상태", "다음에 흙 상태를 살펴보세요.", java.util.Arrays.asList(context.recordCitationId(), "doc-1"))));
        assertThat(RecordFeedbackOutputValidator.validate(invalid, context, documents)).contains("good_point_text_length", "next_action_0_text_length");
    }

    @Test
    @DisplayName("accepts three actions and rejects four actions")
    void accepts_three_actions_and_rejects_four_actions() throws Exception {
        var threeActions = new RecordFeedbackContent(validResult(RecordFeedbackActionCategory.IRRIGATION, java.util.Arrays.asList(context.recordCitationId(), "doc-1")).getGoodPoint(), java.util.Arrays.asList(validAction(RecordFeedbackActionDue.THIS_WEEK, RecordFeedbackActionCategory.WEATHER, "비 예보", "비 오기 전 물길을 정리하세요.", java.util.Arrays.asList("weather:2026-07-04")), validAction(RecordFeedbackActionDue.NEXT_CHECK, RecordFeedbackActionCategory.IRRIGATION, "토양 상태", "다음에 흙 상태를 살펴보세요.", java.util.Arrays.asList(context.recordCitationId(), "doc-1")), validAction(RecordFeedbackActionDue.THIS_WEEK, RecordFeedbackActionCategory.PEST_DISEASE, "토양 상태", "잎 앞뒤를 자세히 살펴보세요.", java.util.Arrays.asList("doc-1"))));
        var fourActions = new RecordFeedbackContent(threeActions.getGoodPoint(), plus(threeActions.getNextActions(), validAction(RecordFeedbackActionDue.NEXT_CHECK, RecordFeedbackActionCategory.IRRIGATION, "토양 상태", "다음에 흙 상태를 살펴보세요.", List.of(context.recordCitationId(), "doc-1"))));
        assertThat(RecordFeedbackOutputValidator.validate(threeActions, context, documents)).isEmpty();
        assertThat(RecordFeedbackOutputValidator.validate(fourActions, context, documents)).contains("action_count");
    }

    @Test
    @DisplayName("rejects blank text and blank evidence ref")
    void rejects_blank_text_and_blank_evidence_ref() throws Exception {
        var invalid = new RecordFeedbackContent(validItem("점적관수", " ", java.util.Arrays.asList(" ")), validResult(RecordFeedbackActionCategory.IRRIGATION, java.util.Arrays.asList(context.recordCitationId(), "doc-1")).getNextActions());
        assertThat(RecordFeedbackOutputValidator.validate(invalid, context, documents)).contains("good_point_text_blank", "good_point_evidence_ref_blank");
    }

    @Test
    @DisplayName("does not allow blank document ids as evidence refs")
    void does_not_allow_blank_document_ids_as_evidence_refs() throws Exception {
        var blankDocument = new RecordFeedbackEvidence(" ", "빈 문서", null, "빈 문서 ID는 근거가 될 수 없다.");
        var invalid = new RecordFeedbackContent(validItem("점적관수", "흙 상태를 살핀 점은 잘했어요.", java.util.Arrays.asList(" ")), validResult(RecordFeedbackActionCategory.IRRIGATION, java.util.Arrays.asList(context.recordCitationId(), "doc-1")).getNextActions());
        assertThat(RecordFeedbackOutputValidator.validate(invalid, context, plus(documents, blankDocument))).contains("good_point_evidence_ref_blank");
    }

    @Test
    @DisplayName("rejects pest disease action without document evidence")
    void rejects_pest_disease_action_without_document_evidence() throws Exception {
        var invalid = validResult(RecordFeedbackActionCategory.PEST_DISEASE, java.util.Arrays.asList(context.recordCitationId()));
        assertThat(RecordFeedbackOutputValidator.validate(invalid, context, documents)).contains("pest_disease_action_without_document_evidence");
    }
    private RecordFeedbackContent validResult(RecordFeedbackActionCategory category, List<String> refs) {
        return new RecordFeedbackContent(validItem("점적관수", "흙 상태를 살핀 점은 잘했어요.", java.util.Arrays.asList(context.recordCitationId())), java.util.Arrays.asList(validAction(RecordFeedbackActionDue.THIS_WEEK, RecordFeedbackActionCategory.WEATHER, "비 예보", "비 오기 전 물길을 정리하세요.", java.util.Arrays.asList("weather:2026-07-04")), validAction(RecordFeedbackActionDue.NEXT_CHECK, category, "토양 상태", "다음에 흙 상태를 살펴보세요.", refs)));
    }
    private RecordFeedbackGoodPoint validItem(String basis, String text, List<String> refs) {
        return new RecordFeedbackGoodPoint(basis, text, refs);
    }
    private RecordFeedbackAction validAction(RecordFeedbackActionDue due, RecordFeedbackActionCategory category, String basis, String text, List<String> refs) {
        return new RecordFeedbackAction(due, category, basis, text, refs);
    }
    private String textOfLengthWithBasis(String basis, int length) {
        if (basis.length() >= length) throw new IllegalArgumentException();
        return basis+ "가".repeat(length - basis.length() - 1) + "요";
    }
    private RecordFeedbackContext readFixture(String name) {
        var resource = Objects.requireNonNull(getClass().getClassLoader().getResource("coaching/rag/" + name), "Missing fixture: " + name);
        try (var input = resource.openStream()) { return objectMapper.readValue(input, RecordFeedbackContext.class); }
        catch (java.io.IOException exception) { throw new java.io.UncheckedIOException(exception); }
    }
    private static <T> List<T> plus(List<T> values, T value) { var result = new ArrayList<>(values); result.add(value); return result; }

}
