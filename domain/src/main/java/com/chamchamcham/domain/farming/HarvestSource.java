package com.chamchamcham.domain.farming;

public enum HarvestSource {
    CULTIVATED("재배"),
    FORAGED("야생 채취");

    private final String label;

    HarvestSource(String label) {
        this.label = label;
    }

    public String getLabel() { return label; }
}
