package com.chamchamcham.application.report;

import org.springframework.lang.Nullable;

public record WeedingReportSource(
    @Nullable CategoryRef method
) {
    @Nullable
    public CategoryRef getMethod() { return method; }
}
