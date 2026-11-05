package com.chamchamcham.api.farm.controller;
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
import com.chamchamcham.application.crop.CropResult;
import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.chamchamcham.application.farm.*;
import java.math.BigDecimal;
import java.util.stream.IntStream;
import java.util.stream.Collectors;
@WebMvcTest(FarmController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class FarmControllerTest {
    @Autowired private MockMvc mockMvc;
    private final UUID memberId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final UUID farmId = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private final UUID cropId = UUID.fromString("00000000-0000-0000-0000-000000000201");
    @MockBean private FarmService farmService;
    @MockBean private TokenProvider tokenProvider;
    @Test @DisplayName("lists authenticated member farms with editable fields and crops")
    void listsAuthenticatedMemberFarmsWithEditableFieldsAndCrops() throws Exception {
        given(farmService.list(memberId)).willReturn(List.of(farmDetail()));
        mockMvc.perform(get("/api/v1/farms").with(authenticatedMember(memberId.toString())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].farmId", equalTo(farmId.toString())))
            .andExpect(jsonPath("$.data[0].roadAddress", equalTo("강원특별자치도 횡성군 둔내면 1")))
            .andExpect(jsonPath("$.data[0].boundaryCoordinates[0].latitude", equalTo(37.5)))
            .andExpect(jsonPath("$.data[0].crops[0].id", equalTo(cropId.toString())));
    }
    @Test @DisplayName("creates farm with crop links and returns 201")
    void createsFarmWithCropLinksAndReturns201() throws Exception {
        given(farmService.create(expectedCreateCommand())).willReturn(farmDetail());
        mockMvc.perform(post("/api/v1/farms").with(authenticatedMember(memberId.toString()))
            .contentType(MediaType.APPLICATION_JSON).content(validFarmJson()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.farmId", equalTo(farmId.toString())))
            .andExpect(jsonPath("$.data.crops[0].name", equalTo("황기")));
    }
    @Test @DisplayName("replaces farm with full draft")
    void replacesFarmWithFullDraft() throws Exception {
        given(farmService.replace(expectedReplaceCommand())).willReturn(farmDetail());
        mockMvc.perform(put("/api/v1/farms/{farmId}", farmId).with(authenticatedMember(memberId.toString()))
            .contentType(MediaType.APPLICATION_JSON).content(validFarmJson()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.farmId", equalTo(farmId.toString())));
    }
    @Test @DisplayName("deletes unreferenced farm with no content")
    void deletesUnreferencedFarmWithNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/farms/{farmId}", farmId).with(authenticatedMember(memberId.toString())))
            .andExpect(status().isNoContent());
        verify(farmService).delete(new FarmCommand.Delete(memberId, farmId));
    }
    @Test @DisplayName("rejects invalid farm payload")
    void rejectsInvalidFarmPayload() throws Exception {
        mockMvc.perform(post("/api/v1/farms").with(authenticatedMember(memberId.toString()))
            .contentType(MediaType.APPLICATION_JSON).content(validFarmJson().replaceFirst("\"latitude\":37.5", "\"latitude\":91.0")))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code", equalTo("COMMON_001")))
            .andExpect(jsonPath("$.error.detail.field", equalTo("latitude")));
    }
    @Test @DisplayName("rejects duplicate crop ids")
    void rejectsDuplicateCropIds() throws Exception {
        mockMvc.perform(post("/api/v1/farms").with(authenticatedMember(memberId.toString()))
            .contentType(MediaType.APPLICATION_JSON).content(validFarmJson().replace("[\"" + cropId + "\"]", "[\"" + cropId + "\",\"" + cropId + "\"]")))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code", equalTo("COMMON_001")))
            .andExpect(jsonPath("$.error.detail.field", equalTo("cropIds")));
    }
    @Test @DisplayName("rejects more than five crop ids")
    void rejectsMoreThanFiveCropIds() throws Exception {
        var extraCropIds = IntStream.range(0, 5).mapToObj(i -> UUID.randomUUID()).toList();
        var cropIds = java.util.stream.Stream.concat(java.util.stream.Stream.of(cropId), extraCropIds.stream())
            .map(id -> "\"" + id + "\"").collect(Collectors.joining(","));
        mockMvc.perform(post("/api/v1/farms").with(authenticatedMember(memberId.toString()))
            .contentType(MediaType.APPLICATION_JSON).content(validFarmJson().replace("[\"" + cropId + "\"]", "[" + cropIds + "]")))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code", equalTo("COMMON_001")))
            .andExpect(jsonPath("$.error.detail.field", equalTo("cropIds")));
    }
    @Test @DisplayName("maps farm in use to conflict")
    void mapsFarmInUseToConflict() throws Exception {
        willThrow(new BusinessException(ErrorCode.FARM_IN_USE)).given(farmService).delete(new FarmCommand.Delete(memberId, farmId));
        mockMvc.perform(delete("/api/v1/farms/{farmId}", farmId).with(authenticatedMember(memberId.toString())))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code", equalTo("FARM_002")));
    }
    @Test @DisplayName("requires authenticated member")
    void requiresAuthenticatedMember() throws Exception {
        mockMvc.perform(get("/api/v1/farms")).andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code", equalTo("AUTH_001")));
    }
    private RequestPostProcessor authenticatedMember(String id) {
        return request -> {
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                id, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
            return request;
        };
    }
    private FarmResult.Detail farmDetail() {
        return new FarmResult.Detail(farmId, "횡성 황기밭", "강원특별자치도 횡성군 둔내면 1", "강원특별자치도 횡성군 둔내면 현천리 101",
            37.5, 128.1, "4273031021101010000", "전", new BigDecimal("1200.5"), false,
            List.of(new FarmResult.BoundaryCoordinate(37.5, 128.1)), new FarmResult.DataSource("JUSO", "VWORLD", "VWORLD", "VWORLD"),
            List.of(new CropResult.CropSummary(cropId, 422, "황기", "ROOT_BARK", "뿌리·껍질")));
    }
    private FarmCommand.Create expectedCreateCommand() { return new FarmCommand.Create(memberId, expectedDraft(), List.of(cropId)); }
    private FarmCommand.Replace expectedReplaceCommand() { return new FarmCommand.Replace(memberId, farmId, expectedDraft(), List.of(cropId)); }
    private FarmCommand.Draft expectedDraft() {
        return new FarmCommand.Draft("횡성 황기밭", "강원특별자치도 횡성군 둔내면 1", "강원특별자치도 횡성군 둔내면 현천리 101",
            37.5, 128.1, "4273031021101010000", "전", new BigDecimal("1200.5"), false,
            List.of(new FarmCommand.BoundaryCoordinate(37.5, 128.1)), new FarmCommand.DataSource("JUSO", "VWORLD", "VWORLD", "VWORLD"));
    }
    private String validFarmJson() {
        return """
            {
              "name":"횡성 황기밭",
              "roadAddress":"강원특별자치도 횡성군 둔내면 1",
              "jibunAddress":"강원특별자치도 횡성군 둔내면 현천리 101",
              "latitude":37.5,
              "longitude":128.1,
              "pnu":"4273031021101010000",
              "landCategory":"전",
              "areaSqm":1200.5,
              "areaIsManualEntry":false,
              "boundaryCoordinates":[{"latitude":37.5,"longitude":128.1}],
              "dataSource":{"address":"JUSO","coordinate":"VWORLD","parcel":"VWORLD","landCharacteristic":"VWORLD"},
              "cropIds":["%s"]
            }
            """.formatted(cropId).stripTrailing();
    }
}
