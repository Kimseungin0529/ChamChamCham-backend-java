package com.chamchamcham.api.pesticide.controller;

import com.chamchamcham.api.exception.GlobalExceptionHandler;
import com.chamchamcham.application.pesticide.PesticideCatalogService;
import com.chamchamcham.application.pesticide.PesticideResult;
import com.chamchamcham.application.pesticide.sync.PesticideSyncResult;
import com.chamchamcham.application.pesticide.sync.PesticideSyncService;
import com.chamchamcham.application.security.TokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import java.util.UUID;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({PesticideController.class, AdminPesticideSyncController.class})
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class PesticideControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private PesticideCatalogService pesticideCatalogService;
    @MockBean private PesticideSyncService pesticideSyncService;
    @MockBean private TokenProvider tokenProvider;
    private final UUID id = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void searchDefaultsToTwentyAndPreservesNullableFields() throws Exception {
        when(pesticideCatalogService.search(null, null, 20)).thenReturn(new PesticideResult.Page(
            List.of(new PesticideResult.Summary(id, "유효성분", "상표", null, null)), null));
        mockMvc.perform(get("/api/v1/pesticides"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success", equalTo(true)))
            .andExpect(jsonPath("$.data.items.length()", equalTo(1)))
            .andExpect(jsonPath("$.data.items[0].id", equalTo(id.toString())))
            .andExpect(jsonPath("$.data.items[0].itemName", equalTo("유효성분")))
            .andExpect(jsonPath("$.data.items[0].brandName", equalTo("상표")))
            .andExpect(jsonPath("$.data.items[0].usageCategory", nullValue()))
            .andExpect(jsonPath("$.data.items[0].manufacturer", nullValue()))
            .andExpect(jsonPath("$.data.nextCursor", nullValue()));
        verify(pesticideCatalogService).search(null, null, 20);
    }

    @Test
    void searchForwardsKeywordCursorAndSize() throws Exception {
        when(pesticideCatalogService.search("황기", "cursor-1", 7))
            .thenReturn(new PesticideResult.Page(List.of(), "cursor-2"));
        mockMvc.perform(get("/api/v1/pesticides").param("keyword", "황기")
                .param("cursor", "cursor-1").param("size", "7"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items.length()", equalTo(0)))
            .andExpect(jsonPath("$.data.nextCursor", equalTo("cursor-2")));
        verify(pesticideCatalogService).search("황기", "cursor-1", 7);
    }

    @Test
    void pestsRetainTheServiceOrder() throws Exception {
        var secondId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        when(pesticideCatalogService.listPestsByPesticide(id)).thenReturn(List.of(
            new PesticideResult.PestSummary(secondId, "나방"), new PesticideResult.PestSummary(id, "진딧물")));
        mockMvc.perform(get("/api/v1/pesticides/{pesticideId}/pests", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()", equalTo(2)))
            .andExpect(jsonPath("$.data[0].id", equalTo(secondId.toString())))
            .andExpect(jsonPath("$.data[0].name", equalTo("나방")))
            .andExpect(jsonPath("$.data[1].id", equalTo(id.toString())))
            .andExpect(jsonPath("$.data[1].name", equalTo("진딧물")));
        verify(pesticideCatalogService).listPestsByPesticide(id);
    }

    @Test
    void adminSyncReturnsSynchronousCounters() throws Exception {
        when(pesticideSyncService.sync()).thenReturn(new PesticideSyncResult(12, 9, 2));
        mockMvc.perform(post("/api/v1/admin/pesticide-sync"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success", equalTo(true)))
            .andExpect(jsonPath("$.data.fetchedRowCount", equalTo(12)))
            .andExpect(jsonPath("$.data.createdApplicationCount", equalTo(9)))
            .andExpect(jsonPath("$.data.pageCount", equalTo(2)));
        verify(pesticideSyncService).sync();
    }
}
