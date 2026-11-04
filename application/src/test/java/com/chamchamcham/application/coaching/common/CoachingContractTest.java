package com.chamchamcham.application.coaching.common;

import com.chamchamcham.application.coaching.chat.*;
import com.chamchamcham.application.coaching.recordfeedback.generation.*;
import com.chamchamcham.application.coaching.reportfeedback.generation.*;
import com.chamchamcham.domain.coaching.recordfeedback.RecordFeedbackActionCategory;
import com.chamchamcham.domain.coaching.recordfeedback.RecordFeedbackActionDue;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CoachingContractTest {
    private final ObjectMapper mapper = Jackson2ObjectMapperBuilder.json().build();

    @Test
    void ragPropertiesBindDefaultsAndOverrides() {
        var defaults = new Binder(new MapConfigurationPropertySource(Map.of())).bindOrCreate("rag", Bindable.of(RagProperties.class));
        assertThat(defaults).isEqualTo(new RagProperties());
        assertThat(defaults.getEmbedding().getModel()).isEqualTo("bge-m3");
        assertThat(defaults.getChat().getModel()).isEqualTo("openclaw/agri-rag-coach");
        assertThat(defaults.getRetrieval().getTopKDefault()).isEqualTo(6);
        assertThat(defaults.getRetrieval().getTopKMax()).isEqualTo(20);
        assertThat(defaults.getRetrieval().getLowSimilarityThreshold()).isEqualTo(0.5);
        assertThat(defaults.getExecution()).isEqualTo(new RagProperties.Execution(2, 4, 32, 240000, 60000));
        var overrides = new Binder(new MapConfigurationPropertySource(Map.of("rag.chat.model", "local-test", "rag.retrieval.top-k-default", "3", "rag.execution.queue-capacity", "7"))).bindOrCreate("rag", Bindable.of(RagProperties.class));
        assertThat(overrides.getChat().getModel()).isEqualTo("local-test");
        assertThat(overrides.getEmbedding().getModel()).isEqualTo("bge-m3");
        assertThat(overrides.getRetrieval()).isEqualTo(new RagProperties.Retrieval(3, 20, 0.5));
        assertThat(overrides.getExecution()).isEqualTo(new RagProperties.Execution(2, 4, 7, 240000, 60000));
    }

    @Test
    void jacksonDistinguishesMissingDefaultsFromExplicitNull() throws Exception {
        var content = mapper.readValue("{\"summary\":\"요약\"}", ReportFeedbackContent.class);
        assertThat(content.getComparisons()).isEmpty();
        assertThat(content.getStrengths()).isEmpty();
        assertThat(content.getImprovements()).isEmpty();
        assertThat(content.getNextActions()).isEmpty();
        assertThatThrownBy(() -> mapper.readValue("{\"summary\":\"요약\",\"strengths\":null}", ReportFeedbackContent.class)).isInstanceOf(Exception.class);
        var node = (ObjectNode) mapper.valueToTree(CoachingStructuredResult.Companion.insufficientEvidence("자료 없음"));
        node.remove(List.of("recordQuality", "limitations"));
        var structured = mapper.treeToValue(node, CoachingStructuredResult.class);
        assertThat(structured.getRecordQuality()).isEqualTo(new CoachingRecordQuality());
        assertThat(structured.getLimitations()).isEmpty();
        node.putNull("recordQuality");
        assertThatThrownBy(() -> mapper.treeToValue(node, CoachingStructuredResult.class)).isInstanceOf(Exception.class);
        var context = fixture();
        var contextNode = (ObjectNode) mapper.valueToTree(context);
        contextNode.remove(List.of("schemaVersion", "warnings"));
        var restored = mapper.treeToValue(contextNode, RecordFeedbackContext.class);
        assertThat(restored.getSchemaVersion()).isEqualTo("record-feedback-context.v2");
        assertThat(restored.getWarnings()).isEmpty();
        contextNode.putNull("warnings");
        assertThatThrownBy(() -> mapper.treeToValue(contextNode, RecordFeedbackContext.class)).isInstanceOf(Exception.class);
    }

    @Test
    void numericJsonDefaultsAndNullKeepOriginalPrimitiveMeaning() throws Exception {
        var root = (ObjectNode) mapper.valueToTree(new RagProperties());
        assertThat(mapper.treeToValue(root, RagProperties.class)).isEqualTo(new RagProperties());
        var retrieval = mapper.readValue("{}", RagProperties.Retrieval.class);
        assertThat(retrieval).isEqualTo(new RagProperties.Retrieval());
        assertThat(mapper.readValue("{\"topKDefault\":null}", RagProperties.Retrieval.class).getTopKDefault()).isZero();
        assertThat(mapper.readValue("{\"lowSimilarityThreshold\":null}", RagProperties.Retrieval.class).getLowSimilarityThreshold()).isZero();
        assertThatThrownBy(() -> mapper.readValue("{\"embedding\":null}", RagProperties.class)).isInstanceOf(Exception.class);
        var context = mapper.readTree(getClass().getResourceAsStream("/coaching/rag/today-record-feedback-watering.json"));
        ((ObjectNode) context.get("record")).remove("photoCount");
        assertThat(mapper.treeToValue(context, RecordFeedbackContext.class).getRecord().getPhotoCount()).isZero();
    }

    @Test
    void commonDetailRemainsAnObjectWithStableValueSemantics() throws Exception {
        var common = CommonFeedbackDetail.INSTANCE;
        assertThat(mapper.writeValueAsString(common)).isEqualTo("{\"type\":\"COMMON\"}");
        var restored = mapper.readValue("{\"type\":\"COMMON\"}", RecordFeedbackWorkDetail.class);
        assertThat(restored).isEqualTo(common);
        assertThat(restored.hashCode()).isEqualTo("com.chamchamcham.application.coaching.recordfeedback.generation.CommonFeedbackDetail".hashCode());
        assertThat(restored.toString()).isEqualTo("CommonFeedbackDetail");
        var context = fixture();
        var r = context.getRecord();
        var changed = context.copy(context.getSchemaVersion(), context.getMember(), context.getFarm(), context.getCrop(), r.copy(r.getRecordId(), r.getSourceRevision(), r.getWorkedAt(), r.getWorkType(), common, r.getRecordedWeatherCondition(), r.getRecordedTemperatureC(), r.getMemo(), r.getPhotoCount()), context.getWeather(), context.getWarnings());
        assertThat(mapper.readValue(mapper.writeValueAsBytes(changed), RecordFeedbackContext.class)).isEqualTo(changed);
    }

    @Test
    void harvestJsonPreservesDiscriminatorAndBooleanFieldName() throws Exception {
        var detail = new HarvestFeedbackDetail(new BigDecimal("1.00"), false, null, com.chamchamcham.domain.farming.HarvestSource.CULTIVATED, null, null, true);
        var node = mapper.valueToTree(detail);
        assertThat(node.get("type").asText()).isEqualTo("HARVEST");
        assertThat(node.get("isLastHarvest").asBoolean()).isTrue();
        assertThat(node.has("lastHarvest")).isFalse();
        assertThat(mapper.readValue(mapper.writeValueAsBytes(detail), RecordFeedbackWorkDetail.class)).isEqualTo(detail);
    }

    @Test
    void dataValuesRetainInputAliasesAndLiveDerivedAccessors() {
        var observations = new ArrayList<CoachingObservation>();
        var citations = new ArrayList<CoachingCitationRef>();
        var result = new CoachingStructuredResult("요약", CoachingRiskLevel.LOW, 0.5, observations, "진단", List.of(), List.of(), List.of(), citations, new CoachingRecordQuality(), List.of());
        var answer = new CoachingRagResult(result, new RagAuditResult(RagAuditStatus.PASS, List.of(), List.of()), new RagModelInfo("embedding", "chat"));
        assertThat(result.getObservations()).isSameAs(observations);
        assertThat(answer.getCitations()).isSameAs(citations);
        observations.add(new CoachingObservation("관찰", "상세", List.of("doc")));
        citations.add(new CoachingCitationRef("doc", "문서", RagSourceType.TECH_DOCUMENT, null, null, null));
        assertThat(result.getObservations()).hasSize(1);
        assertThat(answer.getCitations()).hasSize(1);
        assertThat(answer.getAnswer()).isEqualTo("요약");
        assertThat(result).isEqualTo(new CoachingStructuredResult("요약", CoachingRiskLevel.LOW, 0.5, observations, "진단", List.of(), List.of(), List.of(), citations, new CoachingRecordQuality(), List.of()));
        assertThatThrownBy(() -> new RagModelInfo(null, "chat")).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new RecordFeedbackGoodPoint("근거", null, List.of())).isInstanceOf(NullPointerException.class);
    }

    @Test
    void auditDistinctListsKeepCardinalityMutabilityAndOrder() {
        var validator = new CoachingStructuredOutputValidator();
        var empty = validator.validate(CoachingStructuredResult.Companion.insufficientEvidence("없음"), Set.of());
        assertThatThrownBy(() -> empty.getWarnings().add("x")).isInstanceOf(UnsupportedOperationException.class);
        var singleton = validator.validate(new CoachingStructuredResult("요약", CoachingRiskLevel.LOW, Double.NaN, List.of(), "진단", List.of(), List.of(), List.of(), List.of(), new CoachingRecordQuality(), List.of()), Set.of());
        assertThat(singleton.getWarnings()).containsExactly("invalid_confidence");
        assertThatThrownBy(() -> singleton.getWarnings().set(0, "x")).isInstanceOf(UnsupportedOperationException.class);
        var multi = validator.validate(new CoachingStructuredResult("요약", CoachingRiskLevel.LOW, Double.NaN, List.of(), "진단", List.of(new CoachingRecommendation(CoachingPriority.LOW, "확인", "이유", null, List.of())), List.of(), List.of(), List.of(), new CoachingRecordQuality(), List.of()), Set.of());
        assertThat(multi.getWarnings()).containsExactly("invalid_confidence", "recommendation_without_citation:확인");
        multi.getWarnings().set(0, "changed");
        multi.getWarnings().add("added");
        assertThat(multi.getWarnings()).hasSize(3);
    }

    @Test
    void reportItemBuildListsAreFrozenAndNormalizationMapsStayMutable() {
        var item = new ReportFeedbackContentItem("근거", "\u00a0문장\n  이어짐\u00a0", new ArrayList<>(List.of("ref")));
        for (var content : List.of(new ReportFeedbackContent("요약", List.of(), List.of(), List.of(), List.of()), new ReportFeedbackContent("요약", List.of(item), List.of(), List.of(), List.of()), new ReportFeedbackContent("요약", List.of(item), List.of(item), List.of(), List.of()))) {
            assertThatThrownBy(() -> content.items().add(new ReportFeedbackStructuredItem(com.chamchamcham.domain.coaching.reportfeedback.ReportFeedbackItemSection.STRENGTH, item))).isInstanceOf(UnsupportedOperationException.class);
            var normalized = content.normalizedParagraphs();
            normalized.getComparisons().add(item);
            assertThat(normalized.getComparisons()).hasSize(content.getComparisons().size() + 1);
        }
        var normalized = item.normalizedParagraph();
        assertThat(normalized.getText()).isEqualTo("문장 이어짐");
        assertThat(normalized.getEvidenceRefs()).isSameAs(item.getEvidenceRefs());
    }

    @Test
    void questionTrimsUnicodeSpaceAndMemoTakeUsesUtf16() {
        var vector = mock(VectorStore.class);
        when(vector.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());
        var service = new CoachingRagService(mock(ChatClient.class), vector, mock(CoachingContextProvider.class), new CoachingRetrievalFilterBuilder(), new CoachingStructuredOutputValidator(), new RagProperties());
        service.answer(new CoachingRagCommand(UUID.randomUUID(), "\u00a0\u2007질문\u202f", null, null, null, null, null, null));
        var captor = org.mockito.ArgumentCaptor.forClass(SearchRequest.class);
        verify(vector).similaritySearch(captor.capture());
        assertThat(captor.getValue().getQuery()).isEqualTo("질문");
        var base = fixture();
        var r = base.getRecord();
        var memo = "가".repeat(119) + "😀";
        var context = base.copy(base.getSchemaVersion(), base.getMember(), base.getFarm(), base.getCrop(), r.copy(r.getRecordId(), r.getSourceRevision(), r.getWorkedAt(), r.getWorkType(), r.getDetail(), r.getRecordedWeatherCondition(), r.getRecordedTemperatureC(), memo, r.getPhotoCount()), base.getWeather(), base.getWarnings());
        var query = new RecordFeedbackRetrievalQueryPlanner().plan(context).stream().filter(it -> it.getReason().equals("memo_text")).findFirst().orElseThrow();
        assertThat(query.getQuery()).isEqualTo("참당귀 " + "가".repeat(119) + "\ud83d");
    }

    @Test
    void recordValidationCountsUtf16AndUnicodeBlankFields() {
        var context = fixture();
        var recordRef = RecordFeedbackContextKt.recordCitationId(context);
        var text = "😀".repeat(7) + "요";
        var actions = List.of(new RecordFeedbackAction(RecordFeedbackActionDue.TODAY, RecordFeedbackActionCategory.GENERAL, "근거", text, List.of(recordRef)), new RecordFeedbackAction(RecordFeedbackActionDue.NEXT_CHECK, RecordFeedbackActionCategory.GENERAL, "근거", text, List.of(recordRef)));
        var content = new RecordFeedbackContent(new RecordFeedbackGoodPoint("\u00a0", text, List.of(recordRef)), actions);
        assertThat(RecordFeedbackOutputValidator.INSTANCE.validate(content, context, List.of())).containsExactly("good_point_basis_blank");
    }

    private RecordFeedbackContext fixture() {
        try (var input = getClass().getResourceAsStream("/coaching/rag/today-record-feedback-watering.json")) { return mapper.readValue(input, RecordFeedbackContext.class); }
        catch (java.io.IOException exception) { throw new java.io.UncheckedIOException(exception); }
    }
}
