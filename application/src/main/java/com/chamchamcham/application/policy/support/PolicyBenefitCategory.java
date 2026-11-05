package com.chamchamcham.application.policy.support;

public enum PolicyBenefitCategory {
    GRANT("지원금"),
    FINANCE("융자·금융"),
    FACILITY_EQUIPMENT("시설·장비"),
    EDUCATION("교육"),
    WELFARE("복지"),
    CERTIFICATION("인증"),
    MARKET("판로"),
    STARTUP("창업"),
    ENVIRONMENT_INFRA("환경·인프라"),
    ETC("기타");

    private final String label;
    PolicyBenefitCategory(String label) { this.label = label; }
    public String getLabel() { return label; }
    public static PolicyBenefitCategory fromKey(String key) {
        java.util.Objects.requireNonNull(key, "key");
        for (var category : values()) if (category.name().equals(key)) return category;
        return null;
    }
}
