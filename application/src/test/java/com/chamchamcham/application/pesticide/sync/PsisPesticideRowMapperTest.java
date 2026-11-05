package com.chamchamcham.application.pesticide.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;
import java.util.*;
import org.junit.jupiter.api.DisplayName;

class PsisPesticideRowMapperTest {
    private final PsisPesticideRowMapper mapper = new PsisPesticideRowMapper();

    @Test
    @DisplayName("maps a raw row using primary candidate tag names")
    void maps_a_raw_row_using_primary_candidate_tag_names() throws Exception {
        var raw = map(new AbstractMap.SimpleEntry<>("prdtNm", "만코제브 수화제"), new AbstractMap.SimpleEntry<>("trdmrkNm", "가가방"), new AbstractMap.SimpleEntry<>("cropNm", "감자"), new AbstractMap.SimpleEntry<>("aplyPestNm", "역병"), new AbstractMap.SimpleEntry<>("dltnMag", "500배"));
        var row = mapper.map(raw);
        Objects.requireNonNull(row);
        assertEquals("만코제브 수화제", row.getItemName());
        assertEquals("가가방", row.getBrandName());
        assertEquals("감자", row.getCropName());
        assertEquals("역병", row.getPestName());
        assertEquals("500배", row.getDilutionRate());
    }

    @Test
    @DisplayName("falls back to item name when brand name is missing")
    void falls_back_to_item_name_when_brand_name_is_missing() throws Exception {
        var raw = map(new AbstractMap.SimpleEntry<>("prdtNm", "만코제브 수화제"), new AbstractMap.SimpleEntry<>("cropNm", "감자"), new AbstractMap.SimpleEntry<>("aplyPestNm", "역병"));
        var row = mapper.map(raw);
        Objects.requireNonNull(row);
        assertEquals("만코제브 수화제", row.getBrandName());
    }

    @Test
    @DisplayName("returns null when a required field is missing")
    void returns_null_when_a_required_field_is_missing() throws Exception {
        var raw = map(new AbstractMap.SimpleEntry<>("prdtNm", "만코제브 수화제"), new AbstractMap.SimpleEntry<>("cropNm", "감자"));
        assertNull(mapper.map(raw));
    }
    @SafeVarargs
    private static <K,V> Map<K,V> map(Map.Entry<K,V>... entries) {
        var result = new LinkedHashMap<K,V>();
        for (var entry : entries) result.put(entry.getKey(), entry.getValue());
        return result;
    }
}
