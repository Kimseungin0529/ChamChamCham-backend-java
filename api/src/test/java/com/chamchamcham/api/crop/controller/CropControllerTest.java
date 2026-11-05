package com.chamchamcham.api.crop.controller;
import com.chamchamcham.api.exception.GlobalExceptionHandler;
import com.chamchamcham.application.security.TokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import java.util.List;
import java.util.UUID;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.chamchamcham.application.crop.CropCatalogService;
import com.chamchamcham.application.crop.CropResult;
import com.chamchamcham.domain.crop.CropUsePartCategory;
@WebMvcTest(CropController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class CropControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private CropCatalogService cropCatalogService;
    @MockBean private TokenProvider tokenProvider;
    @Test @DisplayName("list crops returns crop catalog")
    void listCropsReturnsCropCatalog() throws Exception {
        var cropId = UUID.fromString("00000000-0000-0000-0000-000000000101");
        when(cropCatalogService.listCrops()).thenReturn(List.of(new CropResult.CropSummary(cropId, 1001, "토마토", "FRUIT", "열매")));
        mockMvc.perform(get("/api/v1/crops")).andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].id", equalTo(cropId.toString())))
            .andExpect(jsonPath("$.data[0].externalNo", equalTo(1001)))
            .andExpect(jsonPath("$.data[0].name", equalTo("토마토")))
            .andExpect(jsonPath("$.data[0].usePartCategory", equalTo("FRUIT")))
            .andExpect(jsonPath("$.data[0].usePartCategoryLabel", equalTo("열매")));
    }
    @Test @DisplayName("list categories returns crop use part categories")
    void listCategoriesReturnsCropUsePartCategories() throws Exception {
        when(cropCatalogService.listCategories()).thenReturn(List.of(new CropResult.CategorySummary("FRUIT", "열매")));
        mockMvc.perform(get("/api/v1/crops/categories")).andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].code", equalTo("FRUIT")))
            .andExpect(jsonPath("$.data[0].label", equalTo("열매")));
    }
    @Test @DisplayName("list crops by category returns filtered crop catalog")
    void listCropsByCategoryReturnsFilteredCropCatalog() throws Exception {
        var cropId = UUID.fromString("00000000-0000-0000-0000-000000000102");
        when(cropCatalogService.listCropsByCategory(CropUsePartCategory.ROOT_BARK)).thenReturn(List.of(
            new CropResult.CropSummary(cropId, 422, "참당귀", "ROOT_BARK", "뿌리·껍질")));
        mockMvc.perform(get("/api/v1/crops/categories/ROOT_BARK/crops")).andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].id", equalTo(cropId.toString())))
            .andExpect(jsonPath("$.data[0].usePartCategory", equalTo("ROOT_BARK")))
            .andExpect(jsonPath("$.data[0].usePartCategoryLabel", equalTo("뿌리·껍질")));
    }
}
