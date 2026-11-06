package com.chamchamcham.api.rag.client;


import org.springframework.lang.Nullable;

public record OpenClawCompletionChoice(@Nullable OpenClawCompletionMessage message) {
    public OpenClawCompletionChoice() { this(null); }
    public OpenClawCompletionMessage getMessage() { return message; }
}
