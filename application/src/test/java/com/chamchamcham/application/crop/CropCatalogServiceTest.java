package com.chamchamcham.application.crop;

import com.chamchamcham.domain.crop.Crop;
import com.chamchamcham.domain.crop.CropRepository;
import com.chamchamcham.domain.crop.CropUsePartCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CropCatalogServiceTest {
    @Mock private CropRepository cropRepository;
    private CropCatalogService service;

    @BeforeEach
    void setUp() { service = new CropCatalogService(cropRepository); }

    @Test
    void listCropsReturnsCropSummaries() {
        var cropId = UUID.fromString("20000000-0000-0000-0000-000000000001");
        when(cropRepository.findAllByOrderByNameAscExternalNoAsc()).thenReturn(
            List.of(new Crop(cropId, 422, "참당귀", CropUsePartCategory.ROOT_BARK)));
        assertThat(service.listCrops()).containsExactly(
            new CropResult.CropSummary(cropId, 422, "참당귀", "ROOT_BARK", "뿌리·껍질"));
    }

    @Test
    void listCategoriesReturnsAllEnumCategoriesInDeclarationOrder() {
        assertThat(service.listCategories()).containsExactlyElementsOf(
            Arrays.stream(CropUsePartCategory.values())
                .map(c -> new CropResult.CategorySummary(c.name(), c.getLabel())).toList());
    }

    @Test
    void listCropsByCategoryReturnsCropSummariesForRequestedCategory() {
        var cropId = UUID.fromString("20000000-0000-0000-0000-000000000002");
        when(cropRepository.findAllByUsePartCategoryOrderByNameAscExternalNoAsc(CropUsePartCategory.RHIZOME))
            .thenReturn(List.of(new Crop(cropId, 107, "작약", CropUsePartCategory.RHIZOME)));
        assertThat(service.listCropsByCategory(CropUsePartCategory.RHIZOME)).containsExactly(
            new CropResult.CropSummary(cropId, 107, "작약", "RHIZOME", "뿌리줄기"));
    }
}
