package com.chamchamcham.api.crop.dto;

import com.chamchamcham.application.crop.CropResult;
import java.util.UUID;
import java.util.Objects;
import org.springframework.lang.NonNull;

public final class CropResponses {
    private CropResponses() {}

    public record CropResponse(
        @NonNull UUID id,
        int externalNo,
        @NonNull String name,
        @NonNull String usePartCategory,
        @NonNull String usePartCategoryLabel
    ) {
        public CropResponse {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(usePartCategory, "usePartCategory");
            Objects.requireNonNull(usePartCategoryLabel, "usePartCategoryLabel");
        }
        @NonNull public UUID getId() { return id; }
        public int getExternalNo() { return externalNo; }
        @NonNull public String getName() { return name; }
        @NonNull public String getUsePartCategory() { return usePartCategory; }
        @NonNull public String getUsePartCategoryLabel() { return usePartCategoryLabel; }
        public static CropResponse from(CropResult.CropSummary result) {
            return new CropResponse(result.getId(), result.getExternalNo(), result.getName(), result.getUsePartCategory(), result.getUsePartCategoryLabel());
        }
    }

    public record CategoryResponse(
        @NonNull String code,
        @NonNull String label
    ) {
        public CategoryResponse {
            Objects.requireNonNull(code, "code");
            Objects.requireNonNull(label, "label");
        }
        @NonNull public String getCode() { return code; }
        @NonNull public String getLabel() { return label; }
        public static CategoryResponse from(CropResult.CategorySummary result) {
            return new CategoryResponse(result.getCode(), result.getLabel());
        }
    }
}
