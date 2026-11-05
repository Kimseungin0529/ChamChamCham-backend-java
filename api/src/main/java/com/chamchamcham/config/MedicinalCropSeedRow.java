package com.chamchamcham.config;

import com.chamchamcham.domain.crop.CropUsePartCategory;
import java.util.Objects;

public record MedicinalCropSeedRow(int externalNo, String name, CropUsePartCategory usePartCategory) {
    public MedicinalCropSeedRow { Objects.requireNonNull(name, "name"); Objects.requireNonNull(usePartCategory, "usePartCategory"); }
    public int getExternalNo() { return externalNo; }
    public String getName() { return name; }
    public CropUsePartCategory getUsePartCategory() { return usePartCategory; }
}
