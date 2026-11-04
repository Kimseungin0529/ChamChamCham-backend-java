package com.chamchamcham.application.voice;

import java.util.UUID;
import java.util.Objects;
import org.springframework.lang.NonNull;

public record CropOption(
    @NonNull UUID cropId,
    @NonNull String name
) {
    public CropOption {
        Objects.requireNonNull(cropId, "cropId");
        Objects.requireNonNull(name, "name");
    }

    @NonNull
    public UUID getCropId() { return cropId; }
    @NonNull
    public String getName() { return name; }
}
