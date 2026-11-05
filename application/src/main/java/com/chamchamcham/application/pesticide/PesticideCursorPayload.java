package com.chamchamcham.application.pesticide;

import java.util.UUID;
import java.util.Objects;
import org.springframework.lang.NonNull;

public record PesticideCursorPayload(
    @NonNull String brandName,
    @NonNull UUID id
) {
    public PesticideCursorPayload {
        Objects.requireNonNull(brandName, "brandName");
        Objects.requireNonNull(id, "id");
    }

    @NonNull
    public String getBrandName() { return brandName; }
    @NonNull
    public UUID getId() { return id; }
}
