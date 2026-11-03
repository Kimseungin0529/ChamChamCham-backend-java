package com.chamchamcham.domain.farming;

public enum FertilizingMethod {
    SOIL("토양에 주기"),
    FOLIAR("엽면에 뿌리기");

    private final String label;

    FertilizingMethod(String label) {
        this.label = label;
    }

    public String getLabel() { return label; }
}
