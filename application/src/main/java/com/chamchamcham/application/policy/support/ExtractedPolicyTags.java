package com.chamchamcham.application.policy.support;

import java.util.Set;
import java.util.Objects;
import org.springframework.lang.NonNull;

public record ExtractedPolicyTags(
    @NonNull Set<String> targetTags,
    @NonNull Set<String> cropTags,
    @NonNull Set<String> regionTags
) {
    public ExtractedPolicyTags {
        Objects.requireNonNull(targetTags, "targetTags");
        Objects.requireNonNull(cropTags, "cropTags");
        Objects.requireNonNull(regionTags, "regionTags");
    }

    @NonNull
    public Set<String> getTargetTags() { return targetTags; }
    @NonNull
    public Set<String> getCropTags() { return cropTags; }
    @NonNull
    public Set<String> getRegionTags() { return regionTags; }
}
