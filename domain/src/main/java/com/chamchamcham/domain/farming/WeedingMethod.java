package com.chamchamcham.domain.farming;

public enum WeedingMethod {
    HAND("손으로 뽑기"),
    MACHINE("예초기 사용"),
    MULCHING("멀칭(비닐 덮기)"),
    HERBICIDE("제초제 사용");

    private final String label;

    WeedingMethod(String label) {
        this.label = label;
    }

    public String getLabel() { return label; }
}
