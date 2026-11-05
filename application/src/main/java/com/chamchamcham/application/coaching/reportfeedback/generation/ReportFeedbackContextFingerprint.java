package com.chamchamcham.application.coaching.reportfeedback.generation;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Component
public class ReportFeedbackContextFingerprint {
    private static final TypeReference<Map<String, Object>> SNAPSHOT_TYPE = new TypeReference<>() {};
    private final ObjectMapper objectMapper;
    public ReportFeedbackContextFingerprint(ObjectMapper objectMapper) { this.objectMapper = Objects.requireNonNull(objectMapper); }
    public String calculate(ReportFeedbackContext context) { Objects.requireNonNull(context, "context"); return calculate(objectMapper.convertValue(context, SNAPSHOT_TYPE)); }
    public String calculate(Map<String, Object> snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        JsonNode tree = objectMapper.valueToTree(snapshot);
        try {
            var bytes = objectMapper.writer().with(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN).writeValueAsBytes(canonicalize(tree));
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (JsonProcessingException | NoSuchAlgorithmException exception) { return ReportFeedbackContextFingerprint.<RuntimeException, String>rethrow(exception); }
    }
    private JsonNode canonicalize(JsonNode node) {
        if (node.isObject()) {
            var canonical = objectMapper.createObjectNode();
            var fields = new TreeMap<String, JsonNode>(); node.fields().forEachRemaining(it -> fields.put(it.getKey(), it.getValue()));
            fields.forEach((name, value) -> canonical.set(name, canonicalize(value)));
            return canonical;
        }
        if (node.isArray()) { var canonical = objectMapper.createArrayNode(); node.forEach(it -> canonical.add(canonicalize(it))); return canonical; }
        if (node.isNumber()) { var value = node.decimalValue().stripTrailingZeros(); if (value.scale() < 0) value = value.setScale(0); return objectMapper.getNodeFactory().numberNode(value); }
        return node;
    }
    @SuppressWarnings("unchecked")
    private static <E extends Throwable, T> T rethrow(Throwable exception) throws E { throw (E) exception; }
}
