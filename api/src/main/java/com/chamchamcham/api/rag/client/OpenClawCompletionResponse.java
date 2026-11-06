package com.chamchamcham.api.rag.client;


import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public record OpenClawCompletionResponse(
        @JsonDeserialize(using = ChoicesDefault.class) List<OpenClawCompletionChoice> choices) {
    public OpenClawCompletionResponse { Objects.requireNonNull(choices, "choices"); }
    public OpenClawCompletionResponse() { this(Collections.emptyList()); }
    public List<OpenClawCompletionChoice> getChoices() { return choices; }

    public static final class ChoicesDefault extends JsonDeserializer<List<OpenClawCompletionChoice>> {
        @Override
        public List<OpenClawCompletionChoice> deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            return context.readValue(parser, context.getTypeFactory().constructType(new TypeReference<List<OpenClawCompletionChoice>>() { }));
        }
        @Override
        public List<OpenClawCompletionChoice> getAbsentValue(DeserializationContext context) { return Collections.emptyList(); }
    }
}
