package com.chamchamcham.application.common;

import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OpaqueCursorCodecTest {
    private final OpaqueCursorCodec codec = new OpaqueCursorCodec();

    @Test
    void encodeAndDecodeCursorPayload() throws Exception {
        var payload = new TestCursorPayload("LIKE", 8L,
            LocalDateTime.of(2026, 6, 12, 9, 0), UUID.fromString("00000000-0000-0000-0000-000000000101"));
        var encoded = codec.encode(payload);
        var decoded = codec.decode(encoded, TestCursorPayload.class);
        assertThat(encoded).doesNotContain("{", "=", "+", "/");
        assertThat(decoded).isEqualTo(payload);
    }

    @Test
    void decodeRejectsMalformedCursor() {
        var exception = assertThrows(BusinessException.class,
            () -> codec.decode("not-a-valid-cursor", TestCursorPayload.class));
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_CURSOR);
    }

    @Test
    void decodeRejectsJsonNullPayload() {
        var exception = assertThrows(BusinessException.class,
            () -> codec.decode("bnVsbA", TestCursorPayload.class));
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_CURSOR);
    }

    record TestCursorPayload(String sort, Long score, LocalDateTime createdAt, UUID id) {}
}
