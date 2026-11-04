package com.chamchamcham.application.voice;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.voice-session")
public record VoiceSessionProperties(
    @org.springframework.boot.context.properties.bind.DefaultValue("20")
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = MaxRoundsDefault.class)
    int maxRounds,
    @org.springframework.boot.context.properties.bind.DefaultValue("330")
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = MaxDurationDefault.class)
    int maxDurationSeconds
) {
    public int getMaxRounds() { return maxRounds; }
    public int getMaxDurationSeconds() { return maxDurationSeconds; }
    @org.springframework.boot.context.properties.bind.ConstructorBinding
    public VoiceSessionProperties {}

    public VoiceSessionProperties() { this(20, 330); }

    public static final class MaxRoundsDefault extends com.fasterxml.jackson.databind.JsonDeserializer<Integer> {
        @Override
        public Integer deserialize(com.fasterxml.jackson.core.JsonParser parser,
            com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            return context.readValue(parser, Integer.class);
        }
        @Override
        public Integer getAbsentValue(com.fasterxml.jackson.databind.DeserializationContext context) { return 20; }
        @Override
        public Integer getNullValue(com.fasterxml.jackson.databind.DeserializationContext context) { return 0; }
    }
    public static final class MaxDurationDefault extends com.fasterxml.jackson.databind.JsonDeserializer<Integer> {
        @Override
        public Integer deserialize(com.fasterxml.jackson.core.JsonParser parser,
            com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            return context.readValue(parser, Integer.class);
        }
        @Override
        public Integer getAbsentValue(com.fasterxml.jackson.databind.DeserializationContext context) { return 330; }
        @Override
        public Integer getNullValue(com.fasterxml.jackson.databind.DeserializationContext context) { return 0; }
    }
}
