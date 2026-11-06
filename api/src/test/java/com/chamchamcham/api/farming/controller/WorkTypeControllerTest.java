package com.chamchamcham.api.farming.controller;

import com.chamchamcham.api.exception.GlobalExceptionHandler;
import com.chamchamcham.application.farming.FieldValueType;
import com.chamchamcham.application.farming.WorkTypeCatalogService;
import com.chamchamcham.application.farming.WorkTypeResult;
import com.chamchamcham.application.security.TokenProvider;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.util.*;
import org.junit.jupiter.api.DisplayName;

@WebMvcTest(WorkTypeController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class WorkTypeControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean
    private WorkTypeCatalogService workTypeCatalogService;
    @MockBean
    private TokenProvider tokenProvider;
    @Test
    @DisplayName("list work types returns full catalog with representative work types")
    void list_work_types_returns_full_catalog_with_representative_work_types() throws Exception {
        when(workTypeCatalogService.listWorkTypes()).thenReturn(java.util.Arrays.asList(new WorkTypeResult.WorkTypeSummary("PLANTING",
            "파종/정식",
            true,
            java.util.Arrays.asList(new WorkTypeResult.FieldSummary("propagationMethod",
            FieldValueType.ENUM,
            true,
            java.util.Arrays.asList(new WorkTypeResult.EnumOptionSummary("SEED",
        "종자"),
            new WorkTypeResult.EnumOptionSummary("CUTTING",
        "삽목"))))),
            new WorkTypeResult.WorkTypeSummary("WATERING",
            "관수",
            false,
            java.util.Arrays.asList(new WorkTypeResult.FieldSummary("irrigationAmount",
            FieldValueType.ENUM,
            false,
            java.util.Collections.emptyList()))),
            new WorkTypeResult.WorkTypeSummary("FERTILIZING",
            "시비",
            true,
            java.util.Arrays.asList(new WorkTypeResult.FieldSummary("materialName",
            FieldValueType.STRING,
            true,
            java.util.Collections.emptyList()))),
            new WorkTypeResult.WorkTypeSummary("PRUNING",
            "가지·순 정리",
            false,
            java.util.Collections.emptyList())));
        mockMvc.perform(get("/api/v1/work-types"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data", hasSize(4)))
            .andExpect(jsonPath("$.data[0].code", equalTo("PLANTING")))
            .andExpect(jsonPath("$.data[0].label", equalTo("파종/정식")))
            .andExpect(jsonPath("$.data[0].detailRequired", equalTo(true)))
            .andExpect(jsonPath("$.data[0].fields[0].name", equalTo("propagationMethod")))
            .andExpect(jsonPath("$.data[0].fields[0].type", equalTo("ENUM")))
            .andExpect(jsonPath("$.data[0].fields[0].options[1].code", equalTo("CUTTING")))
            .andExpect(jsonPath("$.data[0].fields[0].options[1].label", equalTo("삽목")))
            .andExpect(jsonPath("$.data[1].code", equalTo("WATERING")))
            .andExpect(jsonPath("$.data[1].fields[0].required", equalTo(false)))
            .andExpect(jsonPath("$.data[2].code", equalTo("FERTILIZING")))
            .andExpect(jsonPath("$.data[2].detailRequired", equalTo(true)))
            .andExpect(jsonPath("$.data[2].fields[0].required", equalTo(true)))
            .andExpect(jsonPath("$.data[3].code", equalTo("PRUNING")))
            .andExpect(jsonPath("$.data[3].detailRequired", equalTo(false)))
            .andExpect(jsonPath("$.data[3].fields", hasSize(0)));
    }
}
