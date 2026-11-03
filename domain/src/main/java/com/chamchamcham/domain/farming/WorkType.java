package com.chamchamcham.domain.farming;

public enum WorkType {
    PLANTING("심기", true),
    WATERING("관수", false),
    FERTILIZING("비료 주기", true),
    PEST_CONTROL("병해충 관리", true),
    WEEDING("잡초 관리", false),
    PRUNING("가지·순 정리", false),
    HARVEST("수확", true),
    ETC("기타", false);

    private final String label;
    private final boolean detailRequired;

    WorkType(String label, boolean detailRequired) {
        this.label = label;
        this.detailRequired = detailRequired;
    }

    public String getLabel() { return label; }

    public boolean getDetailRequired() { return detailRequired; }
}
