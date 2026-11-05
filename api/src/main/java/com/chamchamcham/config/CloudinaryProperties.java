package com.chamchamcham.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.Objects;

@ConfigurationProperties(prefix = "cloudinary")
public record CloudinaryProperties(String cloudName, String apiKey, String apiSecret,
    @org.springframework.boot.context.properties.bind.DefaultValue("true")
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = SecureDefault.class) boolean secure) {
    @org.springframework.boot.context.properties.bind.ConstructorBinding
    public CloudinaryProperties {
        Objects.requireNonNull(cloudName, "cloudName"); Objects.requireNonNull(apiKey, "apiKey"); Objects.requireNonNull(apiSecret, "apiSecret");
    }
    public CloudinaryProperties(String cloudName, String apiKey, String apiSecret) { this(cloudName, apiKey, apiSecret, true); }
    public String getCloudName() { return cloudName; }
    public String getApiKey() { return apiKey; }
    public String getApiSecret() { return apiSecret; }
    public boolean getSecure() { return secure; }
    public static final class SecureDefault extends com.fasterxml.jackson.databind.JsonDeserializer<Boolean> {
        @Override
        public Boolean deserialize(com.fasterxml.jackson.core.JsonParser parser,
                com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            return context.readValue(parser, Boolean.class);
        }
        @Override public Boolean getAbsentValue(com.fasterxml.jackson.databind.DeserializationContext context) { return true; }
        @Override public Boolean getNullValue(com.fasterxml.jackson.databind.DeserializationContext context) { return false; }
    }
}
