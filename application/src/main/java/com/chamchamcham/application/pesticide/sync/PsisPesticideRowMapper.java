package com.chamchamcham.application.pesticide.sync;

import org.springframework.stereotype.Component;
import java.util.*;

/**
 * PSIS 원본 응답의 태그명이 아직 확정되지 않아, 필드마다 그럴듯한 후보 태그명을 여러 개 시도한다.
 * API 키 발급 후 실응답을 받으면 각 필드의 실제 태그명만 후보 목록 맨 앞에 추가하면 된다
 * (구조 변경 불필요 — 이 클래스만 수정).
 */
@Component
public class PsisPesticideRowMapper {
    private static final List<String> ITEM_NAME_KEYS = List.of("prdtNm", "pestcdNm", "itemNm");
    private static final List<String> BRAND_NAME_KEYS = List.of("trdmrkNm", "brandNm", "cmpnyNm2");
    private static final List<String> CROP_NAME_KEYS = List.of("cropNm", "aplyCropNm", "cropName");
    private static final List<String> PEST_NAME_KEYS = List.of("aplyPestNm", "pestNm", "diszInsctNm", "pestName");
    private static final List<String> ACTIVE_INGREDIENT_KEYS = List.of("mkeqCn", "ftlDstncNm", "activeIngredient");
    private static final List<String> FORMULATION_KEYS = List.of("frmlcNm", "formulation");
    private static final List<String> USAGE_CATEGORY_KEYS = List.of("useNm", "purpsNm", "usageCategory");
    private static final List<String> HUMAN_TOXICITY_KEYS = List.of("humanToxNm", "hyginTxctyNm");
    private static final List<String> FISH_TOXICITY_KEYS = List.of("fishToxNm", "fshTxctyNm");
    private static final List<String> MANUFACTURER_KEYS = List.of("cmpnyNm", "manufacturer");
    private static final List<String> DILUTION_RATE_KEYS = List.of("dltnMag", "dilutionRate");
    private static final List<String> USAGE_AMOUNT_KEYS = List.of("useAmount", "useQnty");
    private static final List<String> USAGE_TIMING_KEYS = List.of("useTiming", "useTermNm");
    private static final List<String> MAX_USAGE_COUNT_KEYS = List.of("useNmtm", "useMaxCnt");

    public PsisPesticideRow map(Map<String, String> raw) {
        Objects.requireNonNull(raw, "raw");
        String itemName = firstNotBlank(raw, ITEM_NAME_KEYS);
        if (itemName == null) return null;
        String brandName = firstNotBlank(raw, BRAND_NAME_KEYS);
        if (brandName == null) brandName = itemName;
        String cropName = firstNotBlank(raw, CROP_NAME_KEYS);
        if (cropName == null) return null;
        String pestName = firstNotBlank(raw, PEST_NAME_KEYS);
        if (pestName == null) return null;
        return new PsisPesticideRow(itemName, brandName, cropName, pestName, firstNotBlank(raw, ACTIVE_INGREDIENT_KEYS), firstNotBlank(raw, FORMULATION_KEYS), firstNotBlank(raw, USAGE_CATEGORY_KEYS), firstNotBlank(raw, HUMAN_TOXICITY_KEYS), firstNotBlank(raw, FISH_TOXICITY_KEYS), firstNotBlank(raw, MANUFACTURER_KEYS), firstNotBlank(raw, DILUTION_RATE_KEYS), firstNotBlank(raw, USAGE_AMOUNT_KEYS), firstNotBlank(raw, USAGE_TIMING_KEYS), firstNotBlank(raw, MAX_USAGE_COUNT_KEYS));
    }
    private String firstNotBlank(Map<String, String> raw, List<String> keys) {
        for (String key : keys) {
            String value = raw.get(key);
            if (value != null && !trim(value).isEmpty()) return trim(value);
        }
        return null;
    }
    private static String trim(String value) {
        int start = 0, end = value.length();
        while (start < end && whitespace(value.charAt(start))) start++;
        while (end > start && whitespace(value.charAt(end - 1))) end--;
        return value.substring(start, end);
    }
    private static boolean whitespace(char value) {
        return Character.isWhitespace(value) || Character.isSpaceChar(value);
    }
}
