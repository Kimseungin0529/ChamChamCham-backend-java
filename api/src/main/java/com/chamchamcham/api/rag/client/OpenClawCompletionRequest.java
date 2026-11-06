package com.chamchamcham.api.rag.client;


import java.util.List;
import java.util.Objects;

public record OpenClawCompletionRequest(String model, List<OpenClawCompletionMessage> messages, boolean stream) {
    public OpenClawCompletionRequest {
        Objects.requireNonNull(model, "model");
        Objects.requireNonNull(messages, "messages");
    }
    public OpenClawCompletionRequest(String model, List<OpenClawCompletionMessage> messages) { this(model, messages, false); }
    public String getModel() { return model; }
    public List<OpenClawCompletionMessage> getMessages() { return messages; }
    public boolean getStream() { return stream; }
}
