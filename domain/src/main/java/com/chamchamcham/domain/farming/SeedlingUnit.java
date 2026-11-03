package com.chamchamcham.domain.farming;

public enum SeedlingUnit {
    JU("주");

    private final String label;

    SeedlingUnit(String label) {
        this.label = label;
    }

    public String getLabel() { return label; }
}
