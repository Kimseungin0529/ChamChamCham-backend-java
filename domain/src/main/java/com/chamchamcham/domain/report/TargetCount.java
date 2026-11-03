package com.chamchamcham.domain.report;

import java.util.Objects;

public record TargetCount(String target, int count) {
    public TargetCount {
        Objects.requireNonNull(target, "target");
    }

}
