package com.chamchamcham.application.farming;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import java.io.IOException;
import java.util.List;
import java.util.Set;

// Kotlin의 빈 컬렉션 기본 인자처럼 누락에만 기본값을 적용한다.
public final class EmptyCollectionDefaultDeserializer extends JsonDeserializer<Object> implements ContextualDeserializer {
    private final JavaType type;

    public EmptyCollectionDefaultDeserializer() { this(null); }
    private EmptyCollectionDefaultDeserializer(JavaType type) { this.type = type; }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext context, BeanProperty property) {
        return new EmptyCollectionDefaultDeserializer(property.getType());
    }

    @Override
    public Object deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        return context.readValue(parser, type);
    }

    @Override
    public Object getAbsentValue(DeserializationContext context) {
        return Set.class.isAssignableFrom(type.getRawClass()) ? Set.of() : List.of();
    }
}
