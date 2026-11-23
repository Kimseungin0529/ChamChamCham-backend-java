package com.chamchamcham.lab;

import com.chamchamcham.application.coaching.recordfeedback.generation.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import static org.assertj.core.api.Assertions.*;
import static com.chamchamcham.lab.CoachingEvidenceSelector.*;

class CoachingEvidenceSelectorTest {
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private final Document doc = new Document("test-doc", "검토한 원문", Map.of("sourceId", "test-source"));

    RecordFeedbackContext context(String crop, String work, String memo, String part) throws Exception {
        var tree = mapper.readTree(getClass().getResourceAsStream("/verification/coaching-policy-cases.json"));
        for (var node : tree.path("cases")) {
            if (!node.path("context").path("crop").path("name").asText().equals(crop)
                    || !node.path("context").path("record").path("workType").asText().equals(work)) continue;
            ObjectNode c = node.path("context").deepCopy();
            ((ObjectNode)c.path("record")).put("memo", memo);
            if (part != null) ((ObjectNode)c.path("record").path("detail")).put("medicinalPart", part);
            return mapper.treeToValue(c, RecordFeedbackContext.class);
        }
        throw new AssertionError("missing synthetic fixture");
    }
    Applicability rule(boolean unresolved, String... pairs) {
        var constraints = new LinkedHashMap<String, Constraint>();
        for (int i = 0; i < pairs.length; i += 2)
            constraints.put(pairs[i], new Constraint(State.EXPLICIT, List.of(pairs[i + 1])));
        return new Applicability(sha(doc.getText()), constraints, unresolved, List.of());
    }
    Decision decide(RecordFeedbackContext c, Applicability rule, Map<String, Fact> facts) {
        return select(c, facts, List.of(doc), Map.of(doc.getId(), rule)).getFirst();
    }
    Fact fact(String text, int offset) { return new Fact("/record/memo", offset, text, text); }

    @Test void excludesOtherCrop() throws Exception {
        assertThat(decide(context("천궁", "WATERING", "합성 기록", null), rule(false, "cropName", "참당귀"), Map.of()).status()).isEqualTo(Status.EXCLUDED);
    }
    @Test void unknownVarietyIsConditionalWithoutAssumingDocumentVariety() throws Exception {
        assertThat(decide(context("구기자나무", "PRUNING", "품종 미상", null), rule(false, "variety", "청감"), Map.of()).reasons()).contains("MISSING:variety");
    }
    @Test void genericAdviceIsKeptWhenVarietyIsUnknown() throws Exception {
        assertThat(decide(context("구기자나무", "WEEDING", "품종 모름", null), rule(false, "cropName", "구기자나무", "workType", "WEEDING"), Map.of()).status()).isEqualTo(Status.APPLICABLE);
    }
    @Test void explicitlyDifferentVarietyIsExcluded() throws Exception {
        var c = context("구기자나무", "PRUNING", "호광", null);
        assertThat(decide(c, rule(false, "variety", "청감"), Map.of("variety", fact("호광", 0))).status()).isEqualTo(Status.EXCLUDED);
    }
    @Test void actualHarvestPartWinsOverCropCatalogPart() throws Exception {
        var c = context("참당귀", "HARVEST", "잎 수확 합성 기록", "LEAF");
        assertThat(c.getCrop().getUsePartCategory().name()).isEqualTo("ROOT_BARK");
        assertThat(decide(c, rule(false, "harvestPart", "ROOT_BARK"), Map.of()).status()).isEqualTo(Status.EXCLUDED);
        assertThat(decide(c, rule(false, "harvestPart", "LEAF"), Map.of()).status()).isEqualTo(Status.APPLICABLE);
    }
    @Test void nullHarvestPartDoesNotFallBackToCatalog() throws Exception {
        var c = context("참당귀", "HARVEST", "부위 미상", "LEAF");
        var r = c.record(); var h = (HarvestFeedbackDetail) r.detail();
        var detail = new HarvestFeedbackDetail(h.harvestAmount(), h.amountUnknown(), null, h.harvestSource(), h.growthPeriod(), h.growthPeriodUnit(), h.isLastHarvest());
        c = new RecordFeedbackContext(c.schemaVersion(), c.member(), c.farm(), c.crop(), new RecordFeedbackRecordContext(r.recordId(), r.sourceRevision(), r.workedAt(), r.workType(), detail, r.recordedWeatherCondition(), r.recordedTemperatureC(), r.memo(), r.photoCount()), c.weather(), c.warnings());
        assertThat(decide(c, rule(false, "harvestPart", "ROOT_BARK"), Map.of()).status()).isEqualTo(Status.CONDITIONAL);
    }
    @Test void unknownGrowthStageIsConditional() throws Exception {
        assertThat(decide(context("천궁", "WATERING", "물을 줌", null), rule(false, "growthStage", "뿌리생장기"), Map.of()).status()).isEqualTo(Status.CONDITIONAL);
    }
    @Test void missingRuleIsConditional() throws Exception {
        assertThat(select(context("천궁", "WATERING", "합성", null), Map.of(), List.of(doc), Map.of()).getFirst().reasons()).contains("UNREVIEWED_SCOPE");
    }
    @Test void unresolvedScopeIsConditional() throws Exception {
        assertThat(decide(context("천궁", "WATERING", "합성", null), rule(true), Map.of()).status()).isEqualTo(Status.CONDITIONAL);
    }
    @Test void explicitConflictWinsOverUnresolvedAndMissingConditions() throws Exception {
        var result = decide(context("천궁", "WATERING", "합성", null), rule(true, "cropName", "참당귀", "variety", "청감"), Map.of());
        assertThat(result.status()).isEqualTo(Status.EXCLUDED);
        assertThat(result.reasons()).contains("CONFLICT:cropName", "MISSING:variety", "UNRESOLVED_SCOPE");
    }
    @Test void invalidMemoOffsetAndValueAreNotUsed() throws Exception {
        var c = context("구기자나무", "PRUNING", "청감", null);
        for (var f : List.of(fact("청감", 1), new Fact("/record/memo", 0, "청감", "호광"), new Fact("/crop/name", 0, "청감", "청감"))) {
            var result = decide(c, rule(false, "variety", "청감"), Map.of("variety", f));
            assertThat(result.status()).isEqualTo(Status.CONDITIONAL);
            assertThat(result.reasons()).contains("INVALID_FACT:variety");
        }
    }
    @Test void annotationsCannotOverrideActualHarvestPart() throws Exception {
        var c = context("참당귀", "HARVEST", "ROOT_BARK", "LEAF");
        assertThat(decide(c, rule(false, "harvestPart", "ROOT_BARK"), Map.of("harvestPart", fact("ROOT_BARK", 0))).status()).isEqualTo(Status.EXCLUDED);
    }
    @Test void generalPesticidePrincipleKeepsProductLimitation() throws Exception {
        var r = new Applicability(sha(doc.getText()), Map.of(), false, List.of("GENERAL_PRINCIPLE_ONLY_NOT_PRODUCT_APPROVAL"));
        assertThat(decide(context("천궁", "PEST_CONTROL", "가상 농약", null), r, Map.of()).reasons()).contains("GENERAL_PRINCIPLE_ONLY_NOT_PRODUCT_APPROVAL");
    }
    @Test void changedDocumentCannotUseOldAnnotation() throws Exception {
        var changed = new Document(doc.getId(), "다른 본문", doc.getMetadata());
        assertThatThrownBy(() -> select(context("천궁", "WATERING", "합성", null), Map.of(), List.of(changed), Map.of(doc.getId(), rule(false)))).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("hash");
    }
    @Test void duplicateIdWithConflictingBodyIsRejected() throws Exception {
        var changed = new Document(doc.getId(), "다른 본문", doc.getMetadata());
        assertThatThrownBy(() -> select(context("천궁", "WATERING", "합성", null), Map.of(), List.of(doc, changed), Map.of(doc.getId(), rule(false)))).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("duplicate");
    }
    @Test void preservesOrderInputAndDeterminismAndAcceptsEmptyCandidates() throws Exception {
        var c = context("천궁", "WATERING", "합성", null);
        var second = new Document("second", "다른 원문", Map.of());
        var candidates = List.of(second, doc); var rules = Map.of(doc.getId(), rule(false));
        String before = mapper.writeValueAsString(c);
        var first = select(c, Map.of(), candidates, rules);
        assertThat(first).extracting(Decision::documentId).containsExactly("second", "test-doc");
        assertThat(select(c, Map.of(), candidates, rules)).isEqualTo(first);
        assertThat(mapper.writeValueAsString(c)).isEqualTo(before);
        assertThat(doc.getText()).isEqualTo("검토한 원문");
        assertThat(select(c, Map.of(), List.of(), rules)).isEmpty();
    }
}
