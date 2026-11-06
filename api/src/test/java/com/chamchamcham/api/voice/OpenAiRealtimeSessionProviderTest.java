package com.chamchamcham.api.voice;

import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.chamchamcham.application.voice.RealtimeSessionRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import org.springframework.web.client.RestClient;
import java.io.IOException;
import java.util.*;
import org.junit.jupiter.api.DisplayName;


class OpenAiRealtimeSessionProviderTest {
    private MockRestServiceServer server;
    private OpenAiRealtimeSessionProvider provider;
    private static final String BASE_URL = "https://openai.example.test";
    private static final String API_KEY = "test-api-key";
    private static final String MODEL = "gpt-realtime";
    private static final String VOICE = "marin";
    private static final int EXPIRES_AFTER_SECONDS = 360;
    @BeforeEach
    void setUp() throws Exception {
        var restClientBuilder = RestClient.builder();
        server = MockRestServiceServer.bindTo(restClientBuilder).build();
        provider = new OpenAiRealtimeSessionProvider(restClientBuilder.build(), BASE_URL, API_KEY, MODEL, VOICE);
    }

    @Test
    @DisplayName("정상 응답이면 ephemeral secret과 model을 반환한다")
    void 정상_응답이면_ephemeral_secret과_model을_반환한다() throws Exception {
        server.expect(requestTo("%s/v1/realtime/client_secrets".formatted(BASE_URL)))
            .andExpect(header("Authorization", "Bearer %s".formatted(API_KEY)))
            .andExpect(jsonPath("$.expires_after.anchor").value("created_at"))
            .andExpect(jsonPath("$.expires_after.seconds").value(EXPIRES_AFTER_SECONDS))
            .andRespond(withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON)
            .body("""
{"value":"ek_test_123","expires_at":1735689600}"""));
        var result = provider.createEphemeralSession(new RealtimeSessionRequest("안내",
            java.util.Arrays.asList(java.util.Map.of("type",
        "function")),
            EXPIRES_AFTER_SECONDS));
        assertThat(result.getClientSecret()).isEqualTo("ek_test_123");
        assertThat(result.getModel()).isEqualTo(MODEL);
        server.verify();
    }

    @Test
    @DisplayName("전송 예외는 제공자 불가 예외로 변환된다")
    void 전송_예외는_제공자_불가_예외로_변환된다() throws Exception {
        server.expect(requestTo("%s/v1/realtime/client_secrets".formatted(BASE_URL)))
            .andRespond(withException(new IOException("connection reset")));
        var exception = assertThrows(BusinessException.class, () -> {
                provider.createEphemeralSession(new RealtimeSessionRequest("안내", java.util.Collections.emptyList(), EXPIRES_AFTER_SECONDS));
        });
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.VOICE_SESSION_PROVIDER_UNAVAILABLE);
    }

    @Test
    @DisplayName("secret 값이 없으면 제공자 불가 예외를 던진다")
    void secret_값이_없으면_제공자_불가_예외를_던진다() throws Exception {
        server.expect(requestTo("%s/v1/realtime/client_secrets".formatted(BASE_URL)))
            .andRespond(withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body("{}"));
        var exception = assertThrows(BusinessException.class, () -> {
                provider.createEphemeralSession(new RealtimeSessionRequest("안내", java.util.Collections.emptyList(), EXPIRES_AFTER_SECONDS));
        });
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.VOICE_SESSION_PROVIDER_UNAVAILABLE);
    }
}
