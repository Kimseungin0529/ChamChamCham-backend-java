package com.chamchamcham.domain.farming;

public enum SprayAmountUnit {
    ML("밀리리터");

    private final String label;

    SprayAmountUnit(String label) {
        this.label = label;
    }

    public String getLabel() { return label; }
}
