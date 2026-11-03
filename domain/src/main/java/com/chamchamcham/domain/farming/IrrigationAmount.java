package com.chamchamcham.domain.farming;

public enum IrrigationAmount {
    LOW("조금"),
    NORMAL("보통"),
    SUFFICIENT("많이");

    private final String label;

    IrrigationAmount(String label) {
        this.label = label;
    }

    public String getLabel() { return label; }
}
