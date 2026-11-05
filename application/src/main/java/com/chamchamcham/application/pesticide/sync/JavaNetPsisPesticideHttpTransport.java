package com.chamchamcham.application.pesticide.sync;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.io.IOException;

/**
 * PSIS(농약안전정보시스템) 농약등록정보 API 호출 어댑터.
 *
 * base-url은 확정된 실제 엔드포인트가 아니라 자리표시자(placeholder)다. PSIS에서 API 키가
 * 발급되면 마이페이지에 표시되는 실제 요청 URL로 psis.pesticide.base-url 값을 교체해야 한다.
 * serviceKey는 공공데이터포털 인코딩 키가 그대로 전달되도록 pre-encoded URI로 붙인다(이중 인코딩 방지,
 * KmaWeatherProvider와 동일한 관례).
 */
@Component
public class JavaNetPsisPesticideHttpTransport implements PsisPesticideHttpTransport {
    private final String baseUrl;
    private final String serviceKey;
    private final long timeoutMillis;
    private volatile HttpClient client;
    public JavaNetPsisPesticideHttpTransport(@Value("${psis.pesticide.base-url:}") String baseUrl,
        @Value("${psis.pesticide.service-key:}") String serviceKey,
        @Value("${psis.pesticide.timeout-millis:10000}") long timeoutMillis) {
        this.baseUrl = Objects.requireNonNull(baseUrl, "baseUrl");
        this.serviceKey = Objects.requireNonNull(serviceKey, "serviceKey");
        this.timeoutMillis = timeoutMillis;
    }
    private HttpClient client() {
        HttpClient value = client;
        if (value == null) synchronized (this) {
            value = client;
            if (value == null) client = value = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(timeoutMillis)).build();
        }
        return value;
    }
    @Override public String get(Map<String, String> queryParams) throws IOException, InterruptedException {
        Objects.requireNonNull(queryParams, "queryParams");
        if (trim(baseUrl).isEmpty()) throw new IllegalStateException("psis.pesticide.base-url is not configured");
        if (trim(serviceKey).isEmpty()) throw new IllegalStateException("psis.pesticide.service-key is not configured");
        var query = new StringJoiner("&");
        queryParams.forEach((key, value) -> query.add(key + "=" + value));
        var uri = URI.create(baseUrl + "?serviceKey=" + serviceKey + "&" + query);
        var request = HttpRequest.newBuilder().uri(uri).timeout(Duration.ofMillis(timeoutMillis)).header("Accept", "application/xml").GET().build();
        var response = client().send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() > 299) throw new IllegalStateException("PSIS pesticide HTTP " + response.statusCode());
        return response.body();
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
