package com.chamchamcham.application.report;

import java.util.Objects;
import org.springframework.lang.NonNull;

public record CategoryRef(
    @NonNull String code,
    @NonNull String label
) {
    public CategoryRef {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(label, "label");
    }

    @NonNull
    public String getCode() { return code; }
    @NonNull
    public String getLabel() { return label; }
}
