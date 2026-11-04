package com.chamchamcham.application.report;

import org.springframework.lang.Nullable;

public record WateringReportSource(
    @Nullable CategoryRef amount,
    @Nullable CategoryRef method
) {
    @Nullable
    public CategoryRef getAmount() { return amount; }
    @Nullable
    public CategoryRef getMethod() { return method; }
}
