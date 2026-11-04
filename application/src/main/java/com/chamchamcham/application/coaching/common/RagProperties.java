package com.chamchamcham.application.coaching.common;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.*;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

@org.springframework.boot.context.properties.ConfigurationProperties(prefix = "rag")
public record RagProperties(
    @NonNull @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = EmbeddingDefault.class) @org.springframework.boot.context.properties.bind.DefaultValue Embedding embedding,
    @NonNull @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = ChatDefault.class) @org.springframework.boot.context.properties.bind.DefaultValue Chat chat,
    @NonNull @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = RetrievalDefault.class) @org.springframework.boot.context.properties.bind.DefaultValue Retrieval retrieval,
    @NonNull @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = ExecutionDefault.class) @org.springframework.boot.context.properties.bind.DefaultValue Execution execution
) {
    @org.springframework.boot.context.properties.bind.ConstructorBinding
    public RagProperties {
        Objects.requireNonNull(embedding, "embedding");
        Objects.requireNonNull(chat, "chat");
        Objects.requireNonNull(retrieval, "retrieval");
        Objects.requireNonNull(execution, "execution");
    }
    @NonNull public Embedding getEmbedding() { return embedding; }
    @NonNull public Chat getChat() { return chat; }
    @NonNull public Retrieval getRetrieval() { return retrieval; }
    @NonNull public Execution getExecution() { return execution; }
    public RagProperties() { this(new Embedding(), new Chat(), new Retrieval(), new Execution()); }
    public static final class EmbeddingDefault extends com.fasterxml.jackson.databind.JsonDeserializer<Embedding> {
        @Override public Embedding deserialize(com.fasterxml.jackson.core.JsonParser parser, com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            return context.readValue(parser, Embedding.class);
        }
        @Override public Embedding getAbsentValue(com.fasterxml.jackson.databind.DeserializationContext context) { return new Embedding(); }
    }
    public static final class ChatDefault extends com.fasterxml.jackson.databind.JsonDeserializer<Chat> {
        @Override public Chat deserialize(com.fasterxml.jackson.core.JsonParser parser, com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            return context.readValue(parser, Chat.class);
        }
        @Override public Chat getAbsentValue(com.fasterxml.jackson.databind.DeserializationContext context) { return new Chat(); }
    }
    public static final class RetrievalDefault extends com.fasterxml.jackson.databind.JsonDeserializer<Retrieval> {
        @Override public Retrieval deserialize(com.fasterxml.jackson.core.JsonParser parser, com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            return context.readValue(parser, Retrieval.class);
        }
        @Override public Retrieval getAbsentValue(com.fasterxml.jackson.databind.DeserializationContext context) { return new Retrieval(); }
    }
    public static final class ExecutionDefault extends com.fasterxml.jackson.databind.JsonDeserializer<Execution> {
        @Override public Execution deserialize(com.fasterxml.jackson.core.JsonParser parser, com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            return context.readValue(parser, Execution.class);
        }
        @Override public Execution getAbsentValue(com.fasterxml.jackson.databind.DeserializationContext context) { return new Execution(); }
    }

public record Embedding(
    @NonNull @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = ModelDefault.class) @org.springframework.boot.context.properties.bind.DefaultValue("bge-m3") String model
) {
    @org.springframework.boot.context.properties.bind.ConstructorBinding
    public Embedding {
        Objects.requireNonNull(model, "model");
    }
    @NonNull public String getModel() { return model; }
    public Embedding() { this("bge-m3"); }
    public static final class ModelDefault extends com.fasterxml.jackson.databind.JsonDeserializer<String> {
        @Override public String deserialize(com.fasterxml.jackson.core.JsonParser parser, com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            return context.readValue(parser, String.class);
        }
        @Override public String getAbsentValue(com.fasterxml.jackson.databind.DeserializationContext context) { return "bge-m3"; }
    }
}
public record Chat(
    @NonNull @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = ModelDefault.class) @org.springframework.boot.context.properties.bind.DefaultValue("openclaw/agri-rag-coach") String model
) {
    @org.springframework.boot.context.properties.bind.ConstructorBinding
    public Chat {
        Objects.requireNonNull(model, "model");
    }
    @NonNull public String getModel() { return model; }
    public Chat() { this("openclaw/agri-rag-coach"); }
    public static final class ModelDefault extends com.fasterxml.jackson.databind.JsonDeserializer<String> {
        @Override public String deserialize(com.fasterxml.jackson.core.JsonParser parser, com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            return context.readValue(parser, String.class);
        }
        @Override public String getAbsentValue(com.fasterxml.jackson.databind.DeserializationContext context) { return "openclaw/agri-rag-coach"; }
    }
}
public record Retrieval(
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = TopKDefaultDefault.class) @org.springframework.boot.context.properties.bind.DefaultValue("6") int topKDefault,
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = TopKMaxDefault.class) @org.springframework.boot.context.properties.bind.DefaultValue("20") int topKMax,
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = LowSimilarityThresholdDefault.class) @org.springframework.boot.context.properties.bind.DefaultValue("0.5") double lowSimilarityThreshold
) {
    @org.springframework.boot.context.properties.bind.ConstructorBinding
    public Retrieval {

    }
    public int getTopKDefault() { return topKDefault; }
    public int getTopKMax() { return topKMax; }
    public double getLowSimilarityThreshold() { return lowSimilarityThreshold; }
    public Retrieval() { this(6, 20, 0.5); }
    public static final class TopKDefaultDefault extends com.fasterxml.jackson.databind.JsonDeserializer<Integer> {
        @Override public Integer deserialize(com.fasterxml.jackson.core.JsonParser parser, com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            return context.readValue(parser, Integer.class);
        }
        @Override public Integer getAbsentValue(com.fasterxml.jackson.databind.DeserializationContext context) { return 6; }
        @Override public Integer getNullValue(com.fasterxml.jackson.databind.DeserializationContext context) { return 0; }
    }
    public static final class TopKMaxDefault extends com.fasterxml.jackson.databind.JsonDeserializer<Integer> {
        @Override public Integer deserialize(com.fasterxml.jackson.core.JsonParser parser, com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            return context.readValue(parser, Integer.class);
        }
        @Override public Integer getAbsentValue(com.fasterxml.jackson.databind.DeserializationContext context) { return 20; }
        @Override public Integer getNullValue(com.fasterxml.jackson.databind.DeserializationContext context) { return 0; }
    }
    public static final class LowSimilarityThresholdDefault extends com.fasterxml.jackson.databind.JsonDeserializer<Double> {
        @Override public Double deserialize(com.fasterxml.jackson.core.JsonParser parser, com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            return context.readValue(parser, Double.class);
        }
        @Override public Double getAbsentValue(com.fasterxml.jackson.databind.DeserializationContext context) { return 0.5; }
        @Override public Double getNullValue(com.fasterxml.jackson.databind.DeserializationContext context) { return 0.0; }
    }
}
public record Execution(
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = CorePoolSizeDefault.class) @org.springframework.boot.context.properties.bind.DefaultValue("2") int corePoolSize,
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = MaxPoolSizeDefault.class) @org.springframework.boot.context.properties.bind.DefaultValue("4") int maxPoolSize,
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = QueueCapacityDefault.class) @org.springframework.boot.context.properties.bind.DefaultValue("32") int queueCapacity,
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = PendingTimeoutMillisDefault.class) @org.springframework.boot.context.properties.bind.DefaultValue("240000") long pendingTimeoutMillis,
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = ScanIntervalMillisDefault.class) @org.springframework.boot.context.properties.bind.DefaultValue("60000") long scanIntervalMillis
) {
    @org.springframework.boot.context.properties.bind.ConstructorBinding
    public Execution {

    }
    public int getCorePoolSize() { return corePoolSize; }
    public int getMaxPoolSize() { return maxPoolSize; }
    public int getQueueCapacity() { return queueCapacity; }
    public long getPendingTimeoutMillis() { return pendingTimeoutMillis; }
    public long getScanIntervalMillis() { return scanIntervalMillis; }
    public Execution() { this(2, 4, 32, 240_000, 60_000); }
    public static final class CorePoolSizeDefault extends com.fasterxml.jackson.databind.JsonDeserializer<Integer> {
        @Override public Integer deserialize(com.fasterxml.jackson.core.JsonParser parser, com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            return context.readValue(parser, Integer.class);
        }
        @Override public Integer getAbsentValue(com.fasterxml.jackson.databind.DeserializationContext context) { return 2; }
        @Override public Integer getNullValue(com.fasterxml.jackson.databind.DeserializationContext context) { return 0; }
    }
    public static final class MaxPoolSizeDefault extends com.fasterxml.jackson.databind.JsonDeserializer<Integer> {
        @Override public Integer deserialize(com.fasterxml.jackson.core.JsonParser parser, com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            return context.readValue(parser, Integer.class);
        }
        @Override public Integer getAbsentValue(com.fasterxml.jackson.databind.DeserializationContext context) { return 4; }
        @Override public Integer getNullValue(com.fasterxml.jackson.databind.DeserializationContext context) { return 0; }
    }
    public static final class QueueCapacityDefault extends com.fasterxml.jackson.databind.JsonDeserializer<Integer> {
        @Override public Integer deserialize(com.fasterxml.jackson.core.JsonParser parser, com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            return context.readValue(parser, Integer.class);
        }
        @Override public Integer getAbsentValue(com.fasterxml.jackson.databind.DeserializationContext context) { return 32; }
        @Override public Integer getNullValue(com.fasterxml.jackson.databind.DeserializationContext context) { return 0; }
    }
    public static final class PendingTimeoutMillisDefault extends com.fasterxml.jackson.databind.JsonDeserializer<Long> {
        @Override public Long deserialize(com.fasterxml.jackson.core.JsonParser parser, com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            return context.readValue(parser, Long.class);
        }
        @Override public Long getAbsentValue(com.fasterxml.jackson.databind.DeserializationContext context) { return 240_000L; }
        @Override public Long getNullValue(com.fasterxml.jackson.databind.DeserializationContext context) { return 0L; }
    }
    public static final class ScanIntervalMillisDefault extends com.fasterxml.jackson.databind.JsonDeserializer<Long> {
        @Override public Long deserialize(com.fasterxml.jackson.core.JsonParser parser, com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            return context.readValue(parser, Long.class);
        }
        @Override public Long getAbsentValue(com.fasterxml.jackson.databind.DeserializationContext context) { return 60_000L; }
        @Override public Long getNullValue(com.fasterxml.jackson.databind.DeserializationContext context) { return 0L; }
    }
}
}
