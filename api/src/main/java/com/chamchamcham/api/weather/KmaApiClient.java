package com.chamchamcham.api.weather;

import java.util.Objects;
import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** 기상청 네 서비스의 요청·제네릭 응답 파싱·오류 변환을 담당한다. */
@Component
public class KmaApiClient {
    private static final Logger logger = LoggerFactory.getLogger(KmaApiClient.class);
    private static final ObjectMapper OBJECT_MAPPER = Jackson2ObjectMapperBuilder.json()
        .featuresToEnable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY).build();
    private final RestClient restClient;
    private final String serviceKey;

    KmaApiClient(RestClient restClient, String serviceKey) {
        this.restClient = Objects.requireNonNull(restClient, "restClient");
        this.serviceKey = Objects.requireNonNull(serviceKey, "serviceKey");
    }

    @Autowired
    public KmaApiClient(RestClient.Builder restClientBuilder, KmaProperties properties) {
        this(restClientBuilder.requestFactory(createRequestFactory(properties.getConnectTimeoutMillis(),
            properties.getReadTimeoutMillis())).build(), properties.getServiceKey());
    }

    // 발표시각·격자가 params에 들어간다. 일시적인 빈 결과는 캐싱하지 않는다.
    @Cacheable(cacheNames = "kma", key = "#operation + '|' + #params", unless = "#result.isEmpty()")
    public <T> List<T> getItems(String baseUrl, String operation, Map<String, String> params, Class<T> itemType) {
        Objects.requireNonNull(baseUrl, "baseUrl");
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(params, "params");
        Objects.requireNonNull(itemType, "itemType");
        URI uri = buildUri(baseUrl, operation, params);
        String rawBody;
        try {
            rawBody = restClient.get().uri(uri).retrieve().body(String.class);
        } catch (RestClientException exception) {
            logger.warn("기상청 API 호출 실패: operation={}", operation);
            throw new BusinessException(ErrorCode.WEATHER_PROVIDER_UNAVAILABLE);
        }
        KmaEnvelope<T> envelope;
        try {
            var javaType = OBJECT_MAPPER.getTypeFactory().constructParametricType(KmaEnvelope.class, itemType);
            envelope = Objects.requireNonNull(OBJECT_MAPPER.readValue(rawBody == null ? "" : rawBody, javaType));
        } catch (Exception exception) {
            logger.warn("기상청 API 응답 파싱 실패: operation={}", operation);
            throw new BusinessException(ErrorCode.WEATHER_PROVIDER_UNAVAILABLE);
        }
        KmaHeader header = envelope.response() == null ? null : envelope.response().header();
        String code = header == null ? null : header.resultCode();
        if ("00".equals(code)) {
            KmaBody<T> body = envelope.response().body();
            return body == null || body.items() == null ? Collections.emptyList() : body.items().item;
        }
        if ("03".equals(code)) {
            logger.debug("기상청 API 조회결과 없음(03): operation={}", operation);
            return Collections.emptyList();
        }
        if ("99".equals(code)) {
            logger.warn("기상청 API 조회결과 없음(99): operation={} resultMsg={}", operation, header.resultMsg());
            return Collections.emptyList();
        }
        logger.warn("기상청 API 오류: operation={} resultCode={} resultMsg={}", operation, code,
            header == null ? null : header.resultMsg());
        throw new BusinessException(ErrorCode.WEATHER_PROVIDER_UNAVAILABLE);
    }

    private URI buildUri(String baseUrl, String operation, Map<String, String> params) {
        Map<String, String> merged = new LinkedHashMap<>();
        merged.put("dataType", "JSON");
        merged.put("pageNo", "1");
        merged.putAll(params);
        String query = merged.entrySet().stream().map(entry -> entry.getKey() + "=" + entry.getValue())
            .collect(Collectors.joining("&"));
        // 이미 인코딩된 serviceKey를 다시 인코딩하지 않는다.
        return URI.create(baseUrl + "/" + operation + "?serviceKey=" + serviceKey + "&" + query);
    }

    private record KmaEnvelope<T>(KmaResponseBody<T> response) {}
    private record KmaResponseBody<T>(KmaHeader header, KmaBody<T> body) {}
    private record KmaHeader(String resultCode, String resultMsg) {}
    private record KmaBody<T>(KmaItems<T> items) {}
    private static class KmaItems<T> {
        private List<T> item = Collections.emptyList();
        @JsonSetter(nulls = Nulls.FAIL)
        public void setItem(List<T> item) { this.item = item; }
    }

    private static SimpleClientHttpRequestFactory createRequestFactory(int connectTimeoutMillis, int readTimeoutMillis) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMillis);
        factory.setReadTimeout(readTimeoutMillis);
        return factory;
    }
}
