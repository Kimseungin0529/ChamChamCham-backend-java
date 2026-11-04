package com.chamchamcham.application.voice;

import java.util.List;
import java.util.Objects;
import org.springframework.lang.NonNull;

public record VoicePesticideOption(
    @NonNull String name,
    @NonNull List<String> pests
) {
    public VoicePesticideOption {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(pests, "pests");
    }

    @NonNull
    public String getName() { return name; }
    @NonNull
    public List<String> getPests() { return pests; }
}
