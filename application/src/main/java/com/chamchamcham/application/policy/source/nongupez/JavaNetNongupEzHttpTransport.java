package com.chamchamcham.application.policy.source.nongupez;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.io.IOException;

@Component
public class JavaNetNongupEzHttpTransport implements NongupEzHttpTransport {
    private final String baseUrl;
    private final long timeoutMillis;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient client;
    public JavaNetNongupEzHttpTransport(@Value("${policy.nongup-ez.base-url:https://www.nongupez.go.kr}") String baseUrl,
        @Value("${policy.nongup-ez.timeout-millis:10000}") long timeoutMillis) {
        this.baseUrl = Objects.requireNonNull(baseUrl, "baseUrl");
        this.timeoutMillis = timeoutMillis;
        this.client = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(timeoutMillis)).build();
    }
    @Override public String post(String path, Map<String, ?> body) throws IOException, InterruptedException {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(body, "body");
        String requestBody = objectMapper.writeValueAsString(body);
        int end = baseUrl.length();
        while (end > 0 && baseUrl.charAt(end - 1) == '/') end--;
        var request = HttpRequest.newBuilder().uri(URI.create(baseUrl.substring(0, end) + path))
            .timeout(Duration.ofMillis(timeoutMillis)).header("Accept", "application/json")
            .header("Content-Type", "application/json; charset=UTF-8")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8)).build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() > 299) throw new IllegalStateException("NongupEZ HTTP " + response.statusCode() + " for " + path);
        return response.body();
    }
}
