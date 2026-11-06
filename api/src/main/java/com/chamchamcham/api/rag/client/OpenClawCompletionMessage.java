package com.chamchamcham.api.rag.client;


import java.util.Objects;

public record OpenClawCompletionMessage(String role, String content) {
    public OpenClawCompletionMessage {
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(content, "content");
    }
    public String getRole() { return role; }
    public String getContent() { return content; }
}
