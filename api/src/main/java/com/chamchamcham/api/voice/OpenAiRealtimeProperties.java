package com.chamchamcham.api.voice;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.Objects;

@ConfigurationProperties(prefix = "openai.realtime")
public record OpenAiRealtimeProperties(String apiKey, String baseUrl, String model, String voice,
    int connectTimeoutMillis, int readTimeoutMillis) {
    public OpenAiRealtimeProperties {
        Objects.requireNonNull(apiKey, "apiKey");
        Objects.requireNonNull(baseUrl, "baseUrl");
        Objects.requireNonNull(model, "model");
        Objects.requireNonNull(voice, "voice");
    }
    public String getApiKey() { return apiKey; }
    public String getBaseUrl() { return baseUrl; }
    public String getModel() { return model; }
    public String getVoice() { return voice; }
    public int getConnectTimeoutMillis() { return connectTimeoutMillis; }
    public int getReadTimeoutMillis() { return readTimeoutMillis; }
}
