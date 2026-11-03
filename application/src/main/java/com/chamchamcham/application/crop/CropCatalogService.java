package com.chamchamcham.application.crop;

import com.chamchamcham.domain.crop.CropRepository;
import com.chamchamcham.domain.crop.CropUsePartCategory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class CropCatalogService {
    private final CropRepository cropRepository;

    public CropCatalogService(CropRepository cropRepository) {
        this.cropRepository = Objects.requireNonNull(cropRepository, "cropRepository");
    }

    public List<CropResult.CropSummary> listCrops() {
        return cropRepository.findAllByOrderByNameAscExternalNoAsc().stream().map(CropResult.CropSummary::from).toList();
    }

    public List<CropResult.CropSummary> listCropsByCategory(CropUsePartCategory category) {
        Objects.requireNonNull(category, "category");
        return cropRepository.findAllByUsePartCategoryOrderByNameAscExternalNoAsc(category).stream()
            .map(CropResult.CropSummary::from).toList();
    }

    public List<CropResult.CategorySummary> listCategories() {
        return Arrays.stream(CropUsePartCategory.values()).map(CropResult.CategorySummary::from).toList();
    }
}
