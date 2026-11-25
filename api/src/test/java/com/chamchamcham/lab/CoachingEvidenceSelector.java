package com.chamchamcham.lab;

import com.chamchamcham.application.coaching.recordfeedback.generation.*;
import com.chamchamcham.domain.farming.PlantingMethod;
import com.chamchamcham.domain.farming.WorkType;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import org.springframework.ai.document.Document;

/** 고정 후보의 적용 조건 실험. 자연어 추출기나 제품의 농업 적합성 판정기가 아니다. */
public final class CoachingEvidenceSelector {
    public enum Status { APPLICABLE, CONDITIONAL, EXCLUDED }
    public enum State { EXPLICIT, NOT_STATED, UNRESTRICTED }
    public record Constraint(State state, List<String> allowed) {}
    public record Applicability(String textSha256, Map<String, Constraint> constraints,
                                boolean unresolvedScope, List<String> caveats) {}
    public record Fact(String fieldPath, int offset, String quote, String value) {}
    public record Decision(String documentId, Status status, List<String> reasons) {}
    static final Set<String> FIELDS = Set.of("cropName", "workType", "species", "variety", "harvestPart", "growthStage", "cultivation", "propagation", "region");
    private static final Set<String> ANNOTATED_FIELDS = Set.of("species", "variety", "harvestPart", "growthStage", "cultivation", "region");

    public static List<Decision> select(RecordFeedbackContext context, Map<String, Fact> explicitFacts,
            List<Document> candidates, Map<String, Applicability> rules) {
        var unique = new LinkedHashMap<String, Document>();
        for (var doc : candidates) {
            if (doc.getId() == null || doc.getId().isBlank() || doc.getText() == null || doc.getText().isBlank())
                throw new IllegalArgumentException("invalid document");
            var prior = unique.putIfAbsent(doc.getId(), doc);
            if (prior != null && (!prior.getText().equals(doc.getText()) || !prior.getMetadata().equals(doc.getMetadata())))
                throw new IllegalArgumentException("duplicate document id with conflicting data");
        }
        var values = new HashMap<String, String>();
        values.put("cropName", context.crop().name());
        values.put("workType", context.record().workType().name());
        if (context.record().detail() instanceof HarvestFeedbackDetail detail && detail.medicinalPart() != null
                && detail.medicinalPart() != com.chamchamcham.domain.crop.CropUsePartCategory.UNKNOWN)
            values.put("harvestPart", detail.medicinalPart().name());
        if (context.record().detail() instanceof PlantingFeedbackDetail detail) {
            if (detail.plantingMethod() == PlantingMethod.SEED) values.put("propagation", "SEED");
            else if (detail.propagationMethod() != null) values.put("propagation", detail.propagationMethod().name());
        }
        var invalidFacts = new ArrayList<String>();
        for (var entry : new TreeMap<>(explicitFacts).entrySet()) {
            String field = entry.getKey(); var fact = entry.getValue();
            // 실제 구조화된 값은 메모 주석으로 덮어쓰지 않는다. 수확 부위 누락도 추측하지 않는다.
            if (values.containsKey(field) || (field.equals("harvestPart") && context.record().workType() == WorkType.HARVEST)) continue;
            String memo = context.record().memo();
            if (!ANNOTATED_FIELDS.contains(field) || fact == null || !"/record/memo".equals(fact.fieldPath())
                    || fact.quote() == null || fact.quote().isBlank() || !fact.quote().equals(fact.value())
                    || fact.offset() < 0 || fact.offset() > memo.length() - fact.quote().length()
                    || !memo.startsWith(fact.quote(), fact.offset())) {
                invalidFacts.add("INVALID_FACT:" + field);
            } else values.put(field, fact.value());
        }
        var result = new ArrayList<Decision>();
        for (var doc : unique.values()) {
            var rule = rules.get(doc.getId());
            if (rule == null) {
                result.add(new Decision(doc.getId(), Status.CONDITIONAL, List.of("UNREVIEWED_SCOPE")));
                continue;
            }
            if (!sha(doc.getText()).equals(rule.textSha256())) throw new IllegalArgumentException("document hash mismatch");
            var reasons = new ArrayList<>(invalidFacts);
            boolean conflict = false, missing = false;
            for (var entry : new TreeMap<>(rule.constraints()).entrySet()) {
                var field = entry.getKey(); var constraint = entry.getValue();
                if (!FIELDS.contains(field) || constraint == null || constraint.state() == null || constraint.allowed() == null
                        || (constraint.state() == State.EXPLICIT && constraint.allowed().isEmpty())
                        || (constraint.state() != State.EXPLICIT && !constraint.allowed().isEmpty()))
                    throw new IllegalArgumentException("invalid constraint: " + field);
                if (constraint.state() != State.EXPLICIT) continue;
                var actual = values.get(field);
                if (actual == null) { missing = true; reasons.add("MISSING:" + field); }
                else if (!constraint.allowed().contains(actual)) { conflict = true; reasons.add("CONFLICT:" + field); }
            }
            if (rule.unresolvedScope()) reasons.add("UNRESOLVED_SCOPE");
            reasons.addAll(rule.caveats());
            Status status = conflict ? Status.EXCLUDED : (missing || rule.unresolvedScope() ? Status.CONDITIONAL : Status.APPLICABLE);
            result.add(new Decision(doc.getId(), status, List.copyOf(reasons)));
        }
        return List.copyOf(result);
    }

    static String sha(String text) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
}
