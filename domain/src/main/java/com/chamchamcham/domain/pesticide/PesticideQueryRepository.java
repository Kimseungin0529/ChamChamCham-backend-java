package com.chamchamcham.domain.pesticide;

import org.springframework.lang.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public interface PesticideQueryRepository {
    List<Pesticide> search(SearchCondition condition);

    /** PSIS crop_name과 정확히 일치하는 작물의 농약·병해충을 조회한다. */
    List<VoiceCatalogRow> findByCropNames(List<String> cropNames, int maxRows);

    record VoiceCatalogRow(String itemName, String brandName, String pestName) {
        public VoiceCatalogRow {
            Objects.requireNonNull(itemName, "itemName");
            Objects.requireNonNull(brandName, "brandName");
            Objects.requireNonNull(pestName, "pestName");
        }
    }

    record Cursor(String brandName, UUID id) {
        public Cursor {
            Objects.requireNonNull(brandName, "brandName");
            Objects.requireNonNull(id, "id");
        }
    }

    record SearchCondition(@Nullable String keyword, @Nullable Cursor cursor, int size) {
    }
}
