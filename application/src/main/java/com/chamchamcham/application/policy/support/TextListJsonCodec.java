package com.chamchamcham.application.policy.support;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class TextListJsonCodec {
    private final ObjectMapper objectMapper = new ObjectMapper();
    public String encode(Collection<String> values) throws com.fasterxml.jackson.core.JsonProcessingException {
        Objects.requireNonNull(values, "values");
        var normalized = new LinkedHashSet<String>();
        for (String value : values) {
            String text = trim(value);
            if (!text.isEmpty()) normalized.add(text);
        }
        return objectMapper.writeValueAsString(normalized);
    }
    public Set<String> decode(String json) {
        if (json == null || trim(json).isEmpty()) return Set.of();
        try {
            List<String> values = objectMapper.readValue(json, new TypeReference<List<String>>() {});
            if (values.isEmpty()) return Set.of();
            if (values.size() == 1) return Collections.singleton(values.getFirst());
            return new LinkedHashSet<>(values);
        } catch (Throwable e) { throw new IllegalArgumentException("Malformed text list JSON", e); }
    }
    private static String trim(String value) {
        int start = 0, end = value.length();
        while (start < end && whitespace(value.charAt(start))) start++;
        while (end > start && whitespace(value.charAt(end - 1))) end--;
        return value.substring(start, end);
    }
    private static boolean whitespace(char value) {
        return Character.isWhitespace(value) || Character.isSpaceChar(value);
    }
}
