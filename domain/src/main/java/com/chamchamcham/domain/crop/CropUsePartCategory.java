package com.chamchamcham.domain.crop;

public enum CropUsePartCategory {
    WHOLE_HERB("전초"),
    ROOT_BARK("뿌리·껍질"),
    RHIZOME("뿌리줄기"),
    LEAF("잎"),
    FLOWER("꽃"),
    FRUIT("열매/과실"),
    SEED("종자"),
    STEM_BRANCH("줄기/가지"),
    UNKNOWN("기타");

    private final String label;

    CropUsePartCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
