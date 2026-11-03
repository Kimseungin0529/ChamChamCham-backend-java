package com.chamchamcham.application.crop;

import com.chamchamcham.domain.crop.Crop;
import com.chamchamcham.domain.crop.CropUsePartCategory;
import java.util.UUID;
import java.util.List;
import java.util.Objects;
import org.springframework.lang.Nullable;
import org.springframework.lang.NonNull;

public final class CropResult {
    private CropResult() {}

    public record CropSummary(
        @NonNull UUID id,
        int externalNo,
        @NonNull String name,
        @NonNull String usePartCategory,
        @NonNull String usePartCategoryLabel
    ) {
        public CropSummary {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(usePartCategory, "usePartCategory");
            Objects.requireNonNull(usePartCategoryLabel, "usePartCategoryLabel");
        }

        @NonNull
        public UUID getId() { return id; }
        public int getExternalNo() { return externalNo; }
        @NonNull
        public String getName() { return name; }
        @NonNull
        public String getUsePartCategory() { return usePartCategory; }
        @NonNull
        public String getUsePartCategoryLabel() { return usePartCategoryLabel; }

        public static CropSummary from(Crop crop) {
            Objects.requireNonNull(crop, "crop");
            if (crop.getId() == null) throw new IllegalArgumentException("Persisted crop id is required");
            return new CropSummary(crop.getId(), crop.getExternalNo(), crop.getName(), crop.getUsePartCategory().name(), crop.getUsePartCategory().getLabel());
        }
    }

    public record CategorySummary(
        @NonNull String code,
        @NonNull String label
    ) {
        public CategorySummary {
            Objects.requireNonNull(code, "code");
            Objects.requireNonNull(label, "label");
        }

        @NonNull
        public String getCode() { return code; }
        @NonNull
        public String getLabel() { return label; }

        public static CategorySummary from(CropUsePartCategory category) {
            Objects.requireNonNull(category, "category");
            return new CategorySummary(category.name(), category.getLabel());
        }
    }
}
