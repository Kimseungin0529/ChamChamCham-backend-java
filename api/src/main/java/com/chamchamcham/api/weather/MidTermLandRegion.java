package com.chamchamcham.api.weather;

import java.util.Map;
import java.util.Objects;

/** 시/군 기온 지점코드에서 육상예보가 받는 10개 광역 구역코드를 계산한다. */
public final class MidTermLandRegion {
    public static final MidTermLandRegion INSTANCE = new MidTermLandRegion();
    private static final Map<String,String> BY_TA_REG_ID_PREFIX = Map.ofEntries(
        Map.entry("11A0", "11B00000"),
        Map.entry("11B1", "11B00000"),
        Map.entry("11B2", "11B00000"),
        Map.entry("11C1", "11C10000"),
        Map.entry("11C2", "11C20000"),
        Map.entry("11D1", "11D10000"),
        Map.entry("11D2", "11D20000"),
        Map.entry("11E0", "11H10000"),
        Map.entry("11F1", "11F10000"),
        Map.entry("11F2", "11F20000"),
        Map.entry("11G0", "11G00000"),
        Map.entry("11H1", "11H10000"),
        Map.entry("11H2", "11H20000"));
    private MidTermLandRegion() {}
    public static String of(String taRegId) {
        Objects.requireNonNull(taRegId,"taRegId");
        return BY_TA_REG_ID_PREFIX.get(taRegId.substring(0, Math.min(4, taRegId.length())));
    }
}
