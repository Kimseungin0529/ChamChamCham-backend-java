package com.chamchamcham.application.voice;

import java.util.UUID;
import java.util.Objects;
import org.springframework.lang.NonNull;

public record FarmOption(
    @NonNull UUID farmId,
    @NonNull String name
) {
    public FarmOption {
        Objects.requireNonNull(farmId, "farmId");
        Objects.requireNonNull(name, "name");
    }

    @NonNull
    public UUID getFarmId() { return farmId; }
    @NonNull
    public String getName() { return name; }
}
