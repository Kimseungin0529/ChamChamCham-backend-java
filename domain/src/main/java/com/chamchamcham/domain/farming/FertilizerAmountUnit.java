package com.chamchamcham.domain.farming;

public enum FertilizerAmountUnit {
    G("그램"),
    ML("밀리리터");

    private final String label;

    FertilizerAmountUnit(String label) {
        this.label = label;
    }

    public String getLabel() { return label; }
}
