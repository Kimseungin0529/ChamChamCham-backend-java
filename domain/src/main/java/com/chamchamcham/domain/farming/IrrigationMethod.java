package com.chamchamcham.domain.farming;

public enum IrrigationMethod {
    DRIP("점적"),
    SPRAYING("살수"),
    ETC("기타");

    private final String label;

    IrrigationMethod(String label) {
        this.label = label;
    }

    public String getLabel() { return label; }
}
