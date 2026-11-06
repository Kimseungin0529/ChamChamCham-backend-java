package com.chamchamcham.api.voice;

import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.chamchamcham.application.voice.RealtimeSessionProvider;
import com.chamchamcham.application.voice.RealtimeSessionRequest;
import com.chamchamcham.application.voice.RealtimeSessionResult;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** OpenAI Realtime의 단기 토큰을 발급한다. 오디오는 클라이언트와 OpenAI 사이에서 직접 처리된다. */
@Component
public class OpenAiRealtimeSessionProvider implements RealtimeSessionProvider {
    private static final Logger logger = LoggerFactory.getLogger(OpenAiRealtimeSessionProvider.class);
    private static final long DEFAULT_EXPIRY_MINUTES = 10L;
    private static final String PLACEHOLDER_API_KEY = "test-openai-api-key";
    private final RestClient restClient;
    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final String voice;

    public OpenAiRealtimeSessionProvider(RestClient restClient, String baseUrl, String apiKey, String model, String voice) {
        this.restClient = Objects.requireNonNull(restClient);
        this.baseUrl = Objects.requireNonNull(baseUrl);
        this.apiKey = Objects.requireNonNull(apiKey);
        this.model = Objects.requireNonNull(model);
        this.voice = Objects.requireNonNull(voice);
        if (apiKey.codePoints().allMatch(c -> Character.isWhitespace(c) || Character.isSpaceChar(c)) || apiKey.equals(PLACEHOLDER_API_KEY)) {
            logger.warn("OPENAI_API_KEY가 비어 있거나 placeholder입니다 — 음성 세션 발급이 전부 실패합니다(VOICE_003).");
        }
    }

    @Autowired
    public OpenAiRealtimeSessionProvider(RestClient.Builder restClientBuilder, OpenAiRealtimeProperties properties) {
        this(restClientBuilder.requestFactory(createRequestFactory(properties.connectTimeoutMillis(), properties.readTimeoutMillis())).build(),
            properties.baseUrl(), properties.apiKey(), properties.model(), properties.voice());
    }

    @Override
    public RealtimeSessionResult createEphemeralSession(RealtimeSessionRequest request) {
        var expiresAfter = new LinkedHashMap<String, Object>();
        expiresAfter.put("anchor", "created_at");
        expiresAfter.put("seconds", request.getExpiresAfterSeconds());
        var session = new LinkedHashMap<String, Object>();
        session.put("type", "realtime");
        session.put("model", model);
        session.put("instructions", request.getInstructions());
        session.put("audio", Map.of("output", Map.of("voice", voice)));
        session.put("tools", request.getTools());
        var body = new LinkedHashMap<String, Object>();
        body.put("expires_after", expiresAfter);
        body.put("session", session);
        final ClientSecretResponse response;
        try {
            response = restClient.post().uri(baseUrl + "/v1/realtime/client_secrets")
                .header("Authorization", "Bearer " + apiKey).contentType(MediaType.APPLICATION_JSON)
                .body(body).retrieve().body(ClientSecretResponse.class);
        } catch (RestClientResponseException exception) {
            var responseBody = exception.getResponseBodyAsString();
            logger.warn("OpenAI realtime client_secrets 발급 거부: status={} body={}",
                exception.getStatusCode().value(),
                responseBody.substring(0,
                Math.min(500,
                responseBody.length())));
            throw new BusinessException(ErrorCode.VOICE_SESSION_PROVIDER_UNAVAILABLE);
        } catch (RestClientException exception) {
            logger.warn("OpenAI realtime client_secrets 호출 실패(네트워크/타임아웃)", exception);
            throw new BusinessException(ErrorCode.VOICE_SESSION_PROVIDER_UNAVAILABLE);
        }
        if (response == null || response.value() == null) {
            logger.warn("OpenAI realtime client_secrets 응답에 value가 없음: response={}", response);
            throw new BusinessException(ErrorCode.VOICE_SESSION_PROVIDER_UNAVAILABLE);
        }
        var expiresAt = response.expiresAt() == null ? LocalDateTime.now().plusMinutes(DEFAULT_EXPIRY_MINUTES)
        : LocalDateTime.ofInstant(Instant.ofEpochSecond(response.expiresAt()), ZoneId.systemDefault());
        return new RealtimeSessionResult(response.value(), expiresAt, model);
    }

    private record ClientSecretResponse(@JsonProperty("value") String value, @JsonProperty("expires_at") Long expiresAt) {}

    private static SimpleClientHttpRequestFactory createRequestFactory(int connectTimeoutMillis, int readTimeoutMillis) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMillis);
        factory.setReadTimeout(readTimeoutMillis);
        return factory;
    }
}
