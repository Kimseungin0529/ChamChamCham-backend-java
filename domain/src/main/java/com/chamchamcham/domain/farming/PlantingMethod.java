package com.chamchamcham.domain.farming;

public enum PlantingMethod {
    SEED("씨앗 심기"),
    SEEDLING("모종 심기");

    private final String label;

    PlantingMethod(String label) {
        this.label = label;
    }

    public String getLabel() { return label; }
}
