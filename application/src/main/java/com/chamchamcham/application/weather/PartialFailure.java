package com.chamchamcham.application.weather;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.lang.NonNull;

public record PartialFailure(
    @NonNull List<String> missing
) {
    public PartialFailure {
        Objects.requireNonNull(missing, "missing");
    }

    @NonNull
    public List<String> getMissing() { return missing; }

    public boolean getDegraded() { return !missing.isEmpty(); }

    @SafeVarargs
    @NonNull
    public static PartialFailure of(@NonNull Map.Entry<String, Boolean>... entries) {
        Objects.requireNonNull(entries, "entries");
        List<String> missing = new ArrayList<>();
        for (Map.Entry<String, Boolean> entry : entries) {
            if (entry.getValue()) missing.add(entry.getKey());
        }
        return new PartialFailure(missing);
    }
}
