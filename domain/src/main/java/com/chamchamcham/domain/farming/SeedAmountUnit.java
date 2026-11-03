package com.chamchamcham.domain.farming;

public enum SeedAmountUnit {
    G("그램");

    private final String label;

    SeedAmountUnit(String label) {
        this.label = label;
    }

    public String getLabel() { return label; }
}
