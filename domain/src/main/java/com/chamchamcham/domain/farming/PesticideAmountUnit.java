package com.chamchamcham.domain.farming;

public enum PesticideAmountUnit {
    ML("밀리리터"),
    G("그램");

    private final String label;

    PesticideAmountUnit(String label) {
        this.label = label;
    }

    public String getLabel() { return label; }
}
