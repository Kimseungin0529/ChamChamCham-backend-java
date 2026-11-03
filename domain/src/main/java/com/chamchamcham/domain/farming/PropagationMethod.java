package com.chamchamcham.domain.farming;

public enum PropagationMethod {
    CUTTING("꺾꽂이"),
    GRAFTING("접붙이기"),
    LAYERING("휘묻이"),
    DIVISION("포기나누기"),
    TISSUE_CULTURE("조직 배양"),
    PURCHASED("시판 구매");

    private final String label;

    PropagationMethod(String label) {
        this.label = label;
    }

    public String getLabel() { return label; }
}
