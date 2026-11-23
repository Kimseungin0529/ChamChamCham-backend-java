package com.chamchamcham.lab;

import com.chamchamcham.application.coaching.recordfeedback.generation.RecordFeedbackContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.regex.Pattern;
import org.springframework.ai.document.Document;
import static com.chamchamcham.lab.CoachingEvidenceSelector.*;

/** 통제 후보 분류 실험. 검색/API/임베딩/답변 모델은 실행하지 않는다. */
public final class CoachingEvidenceEvaluation {
    static final String LAB = ".local/rag-api/evidence-selection";
    static final Pattern RUN = Pattern.compile("[a-z0-9][a-z0-9-]{0,63}");
    static final List<String> CODE_FILES = List.of(
        "api/src/test/java/com/chamchamcham/lab/CoachingEvidenceSelector.java",
        "api/src/test/java/com/chamchamcham/lab/CoachingEvidenceEvaluation.java",
        "scripts/coaching-evidence.py", "api/build.gradle.kts",
        "application/src/main/java/com/chamchamcham/application/coaching/recordfeedback/generation/RecordFeedbackContext.java",
        "application/src/main/java/com/chamchamcham/application/coaching/recordfeedback/generation/RecordFeedbackRecordContext.java",
        "application/src/main/java/com/chamchamcham/application/coaching/recordfeedback/generation/HarvestFeedbackDetail.java",
        "application/src/main/java/com/chamchamcham/application/coaching/recordfeedback/generation/PlantingFeedbackDetail.java");
    private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();

    static void evaluate(Path cases, Path rules, Path corpus, Path output) throws Exception {
        output = output.toAbsolutePath().normalize();
        if (!RUN.matcher(output.getFileName().toString()).matches() || output.getNameCount() < 6)
            throw new IllegalArgumentException("invalid run name/path");
        Path root = output.getParent().getParent().getParent().getParent().getParent();
        if (!output.getParent().equals(root.resolve(LAB + "/runs"))) throw new IllegalArgumentException("invalid run folder");
        for (Path parent = output; parent != null && parent.startsWith(root); parent = parent.getParent())
            if (Files.isSymbolicLink(parent)) throw new IllegalArgumentException("symlink run path");
        var inputs = new ArrayList<Path>(List.of(cases, rules, corpus));
        CODE_FILES.forEach(file -> inputs.add(root.resolve(file)));
        Path sourceManifest = root.resolve("docs/verification/coaching-evidence-sources.json");
        if (Files.exists(sourceManifest)) inputs.add(sourceManifest);
        var before = hashes(root, inputs);
        Files.createDirectories(output.getParent());
        Files.createDirectory(output); // 모든 실행은 독립적이고 기존 성공/실패 결과를 덮어쓰지 않는다.
        MAPPER.writeValue(output.resolve("started.json").toFile(), Map.of("inputHashes", before));
        try {
            var byDocument = new LinkedHashMap<String, Document>();
            for (var row : MAPPER.readTree(corpus.toFile()).path("documents")) {
                String id = requiredText(row, "id");
                var metadata = MAPPER.convertValue(row.path("metadata"), new TypeReference<Map<String, Object>>() {});
                if (byDocument.putIfAbsent(id, new Document(id, requiredText(row, "text"), metadata)) != null)
                    throw new IllegalArgumentException("duplicate corpus id");
            }
            var byRule = new LinkedHashMap<String, Applicability>();
            for (var row : MAPPER.readTree(rules.toFile()).path("rules")) {
                var constraints = MAPPER.convertValue(row.path("constraints"), new TypeReference<Map<String, Constraint>>() {});
                var caveats = MAPPER.convertValue(row.path("caveats"), new TypeReference<List<String>>() {});
                if (byRule.putIfAbsent(requiredText(row, "documentId"), new Applicability(requiredText(row, "textSha256"), constraints,
                        row.path("unresolvedScope").asBoolean(), caveats)) != null) throw new IllegalArgumentException("duplicate rule id");
            }
            var rows = new ArrayList<Map<String, Object>>(); var caseIds = new HashSet<String>();
            int development = 0, evaluation = 0, pairs = 0, matched = 0, exact = 0, missingCandidates = 0, uncovered = 0, noApplicable = 0;
            var denominators = new EnumMap<Status, Integer>(Status.class);
            var correct = new EnumMap<Status, Integer>(Status.class);
            var csv = new StringBuilder("caseId,crop,workType,documentId,actual,expected,match,reasons\n");
            for (var input : MAPPER.readTree(cases.toFile()).path("cases")) {
                String id = requiredText(input, "id"), split = requiredText(input, "split");
                if (!caseIds.add(id) || !input.path("synthetic").asBoolean() || !Set.of("dev", "evaluation").contains(split))
                    throw new IllegalArgumentException("invalid/duplicate synthetic case");
                var context = MAPPER.treeToValue(input.path("context"), RecordFeedbackContext.class);
                var facts = MAPPER.convertValue(input.path("explicitFacts"), new TypeReference<Map<String, Fact>>() {});
                var candidates = new ArrayList<Document>(); var candidateIds = new HashSet<String>();
                for (var docId : input.path("candidateIds")) {
                    var doc = byDocument.get(docId.asText());
                    if (doc == null || !candidateIds.add(docId.asText())) throw new IllegalArgumentException("missing/duplicate candidate");
                    candidates.add(doc);
                }
                // 기대 판정·필요 근거·판정 이유를 선택기에 전달하지 않는다.
                var decisions = select(context, facts, candidates, byRule);
                var expected = input.path("expected");
                boolean exactCase = true; var absent = new ArrayList<String>();
                if (split.equals("evaluation")) {
                    evaluation++;
                    if (!expected.isObject() || expected.size() != candidateIds.size()) throw new IllegalArgumentException("incomplete expected decisions");
                    for (var required : input.path("requiredEvidenceIds")) if (!candidateIds.contains(required.asText())) absent.add(required.asText());
                    if (!absent.isEmpty()) missingCandidates++;
                    if (!input.path("uncoveredNeed").isNull() && !input.path("uncoveredNeed").asText().isBlank()) uncovered++;
                    if (decisions.stream().noneMatch(d -> d.status() == Status.APPLICABLE)) noApplicable++;
                    for (var decision : decisions) {
                        var wanted = Status.valueOf(requiredText(expected, decision.documentId()));
                        denominators.merge(wanted, 1, Integer::sum); pairs++;
                        boolean equal = decision.status() == wanted;
                        if (equal) { matched++; correct.merge(wanted, 1, Integer::sum); } else exactCase = false;
                        List<String> values = List.of(id, context.crop().name(), context.record().workType().name(), decision.documentId(), decision.status().name(), wanted.name(), Boolean.toString(equal), String.join(";", decision.reasons()));
                        csv.append(String.join(",", values.stream().map(CoachingEvidenceEvaluation::csvCell).toList())).append('\n');
                    }
                    if (exactCase) exact++;
                } else development++;
                var row = new LinkedHashMap<String, Object>();
                row.put("id", id); row.put("split", split); row.put("crop", context.crop().name());
                row.put("workType", context.record().workType().name()); row.put("context", input.path("context"));
                row.put("candidateMode", requiredText(input, "candidateMode")); row.put("decisions", decisions);
                row.put("expected", expected); row.put("missingRequiredCandidates", absent); row.put("uncoveredNeed", input.path("uncoveredNeed"));
                row.put("exactPolicyMatch", split.equals("evaluation") ? exactCase : null); rows.add(row);
            }
            if (evaluation == 0) throw new IllegalArgumentException("no evaluation cases");
            var summary = new LinkedHashMap<String, Object>();
            summary.put("developmentSituations", development); summary.put("evaluationSituations", evaluation);
            summary.put("evaluationCandidatePairs", pairs); summary.put("matchedDecisions", matched); summary.put("exactSituations", exact);
            summary.put("expectedByStatus", denominators); summary.put("correctByStatus", correct);
            summary.put("baselineUnfilteredExcludedRetained", denominators.getOrDefault(Status.EXCLUDED, 0));
            summary.put("missingRequiredCandidateCases", missingCandidates); summary.put("casesWithUncoveredNeed", uncovered);
            summary.put("casesWithoutApplicableDocument", noApplicable); summary.put("modelRequests", 0);
            summary.put("retrievalExecuted", false); summary.put("semanticAnswerEvaluation", false);
            if (!before.equals(hashes(root, inputs))) throw new IllegalStateException("inputs changed during evaluation");
            var result = Map.of("complete", true, "inputHashes", before, "summary", summary, "cases", rows,
                "limitations", List.of("통제 후보의 수동 조건 판정 일치; 검색/자연어 추출/답변 정확도 아님", "조건 주석과 기대값 작성자가 동일; 전문가 평가 없음"));
            Path resultFile = output.resolve("result.json");
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(resultFile.toFile(), result);
            Files.writeString(output.resolve("comparison.csv"), csv, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
            MAPPER.writeValue(output.resolve("identity.json").toFile(), Map.of("resultSha256", fileSha(resultFile)));
            System.out.println(MAPPER.writeValueAsString(summary));
        } catch (Exception failure) {
            MAPPER.writeValue(output.resolve("failure.json").toFile(), Map.of("complete", false, "error", failure.getClass().getSimpleName()));
            throw failure;
        }
    }

    private static Map<String, String> hashes(Path root, List<Path> inputs) throws Exception {
        var hashes = new TreeMap<String, String>();
        for (var input : inputs) {
            Path path = input.toAbsolutePath().normalize();
            if (!path.toRealPath().startsWith(root.toRealPath())) throw new IllegalArgumentException("input outside root");
            hashes.put(root.relativize(path).toString(), fileSha(path));
        }
        return hashes;
    }
    private static String fileSha(Path path) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
    }
    private static String requiredText(JsonNode row, String key) {
        var value = row.get(key);
        if (value == null || !value.isTextual() || value.asText().isBlank()) throw new IllegalArgumentException("missing text: " + key);
        return value.asText();
    }
    private static String csvCell(String value) { return "\"" + value.replace("\"", "\"\"") + "\""; }

    public static void main(String[] args) throws Exception {
        if (args.length != 1 || !RUN.matcher(args[0]).matches()) throw new IllegalArgumentException("evidenceRun required");
        var root = Path.of("").toAbsolutePath();
        int checked = new ProcessBuilder("python3", "scripts/coaching-evidence.py", "check-sources").directory(root.toFile()).inheritIO().start().waitFor();
        if (checked != 0) throw new IllegalStateException("source preflight failed");
        evaluate(root.resolve("api/src/test/resources/verification/coaching-evidence-cases.json"),
            root.resolve("api/src/test/resources/verification/coaching-evidence-applicability.json"), root.resolve(LAB + "/corpus.json"),
            root.resolve(LAB + "/runs/" + args[0]));
    }
}
