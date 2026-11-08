package com.chamchamcham.application.common;

import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

@Component
public class OpaqueCursorCodec {
    private final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    public String encode(Object payload) throws com.fasterxml.jackson.core.JsonProcessingException {
        Objects.requireNonNull(payload, "payload");
        var json = objectMapper.writeValueAsString(payload);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    public <T> T decode(String cursor, Class<T> payloadType) {
        Objects.requireNonNull(cursor, "cursor");
        Objects.requireNonNull(payloadType, "payloadType");
        try {
            var json = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            return Objects.requireNonNull(objectMapper.readValue(json, payloadType));
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.INVALID_CURSOR);
        }
    }
}
