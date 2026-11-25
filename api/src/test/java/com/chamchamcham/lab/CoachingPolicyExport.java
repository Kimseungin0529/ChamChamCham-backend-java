package com.chamchamcham.lab;

import com.chamchamcham.application.coaching.recordfeedback.generation.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import org.springframework.ai.converter.BeanOutputConverter;

/** Spring/DB/Docker 없이 실제 제품 빌더로 실험 입력을 내보낸다. 배포 산출물에 포함되지 않는다. */
public final class CoachingPolicyExport {
    static ObjectNode exportCase(ObjectMapper mapper, JsonNode input) throws Exception {
        var context = mapper.treeToValue(input.path("context"), RecordFeedbackContext.class);
        var evidence = new ArrayList<RecordFeedbackEvidence>();
        for (var node : input.path("evidence")) evidence.add(mapper.treeToValue(node, RecordFeedbackEvidence.class));
        var queries = new RecordFeedbackRetrievalQueryPlanner().plan(context);
        var prompt = new RecordFeedbackPromptBuilder().build(context, queries, evidence);
        var result = ((ObjectNode) input).deepCopy();
        result.set("prompt", mapper.valueToTree(Map.of("system", prompt.getSystem(), "user", prompt.getUser())));
        var allowed = new ArrayList<String>(); allowed.add(context.recordCitationId());
        if (context.getWeather() != null) {
            allowed.add("weather:current");
            context.getWeather().getForecastDays().forEach(it -> allowed.add("weather:" + it.getDate()));
        }
        evidence.forEach(it -> allowed.add(it.getId()));
        result.set("allowedCitationIds", mapper.valueToTree(allowed));
        result.set("documentCitationIds", mapper.valueToTree(evidence.stream().map(RecordFeedbackEvidence::getId).toList()));
        return result;
    }
    public static void main(String[] ignored) throws Exception {
        var mapper = new ObjectMapper().findAndRegisterModules();
        var cases = Path.of("api/src/test/resources/verification/coaching-policy-cases.json");
        var input = mapper.readTree(cases.toFile());
        var output = mapper.createObjectNode();
        output.put("casesSha256", sha(cases));
        var paths = List.of("api/src/test/resources/verification/coaching-policy-cases.json",
            "application/src/main/java/com/chamchamcham/application/coaching/recordfeedback/generation/RecordFeedbackPromptBuilder.java",
            "api/src/test/java/com/chamchamcham/lab/RagApiLabApplication.java");
        var hashes = new LinkedHashMap<String, String>();
        for (var path : paths) hashes.put(path, sha(Path.of(path)));
        output.set("sourceFiles", mapper.valueToTree(hashes));
        var schema = mapper.readTree(new BeanOutputConverter<>(RecordFeedbackContent.class).getJsonSchema());
        RagApiLabApplication.LabConfiguration.constrainOutput(schema);
        output.set("baselineSchema", schema);
        var results = output.putArray("cases");
        for (var node : input.path("cases")) results.add(exportCase(mapper, node));
        var target = Path.of(".local/rag-api/policy/prepared.json");
        Files.createDirectories(target.getParent());
        mapper.writerWithDefaultPrettyPrinter().writeValue(target.toFile(), output);
        System.out.println("실험 입력 " + results.size() + "건 준비: Spring/DB/Docker 시작 없음");
    }
    private static String sha(Path path) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
    }
}
