package com.chamchamcham.config;


import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "openclaw")
public record OpenClawProperties(
        @DefaultValue("http://127.0.0.1:18789") String baseUrl,
        @DefaultValue("") String apiKey,
        @DefaultValue("agri-rag-coach") String agentId,
        @DefaultValue("3000") int connectTimeoutMillis,
        @DefaultValue("60000") int readTimeoutMillis) {
    @ConstructorBinding
    public OpenClawProperties {
        Objects.requireNonNull(baseUrl, "baseUrl");
        Objects.requireNonNull(apiKey, "apiKey");
        Objects.requireNonNull(agentId, "agentId");
    }
    public OpenClawProperties() { this("http://127.0.0.1:18789", "", "agri-rag-coach", 3_000, 60_000); }
    public String getBaseUrl() { return baseUrl; }
    public String getApiKey() { return apiKey; }
    public String getAgentId() { return agentId; }
    public int getConnectTimeoutMillis() { return connectTimeoutMillis; }
    public int getReadTimeoutMillis() { return readTimeoutMillis; }
}
