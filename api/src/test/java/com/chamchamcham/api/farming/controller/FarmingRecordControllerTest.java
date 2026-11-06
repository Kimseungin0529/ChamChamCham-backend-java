package com.chamchamcham.api.farming.controller;

import com.chamchamcham.domain.farming.EntryMode;
import com.chamchamcham.api.exception.GlobalExceptionHandler;
import com.chamchamcham.application.farming.FarmingRecordCommand;
import com.chamchamcham.application.farming.FarmingRecordResult;
import com.chamchamcham.application.farming.FarmingRecordSearchCondition;
import com.chamchamcham.application.farming.FarmingRecordService;
import com.chamchamcham.application.security.TokenProvider;
import com.chamchamcham.domain.crop.CropUsePartCategory;
import com.chamchamcham.domain.farming.HarvestSource;
import com.chamchamcham.domain.farming.WorkType;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.when;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.*;
import org.junit.jupiter.api.DisplayName;

@WebMvcTest(FarmingRecordController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class FarmingRecordControllerTest {
    @Autowired private MockMvc mockMvc;
    private final UUID memberId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final UUID recordId = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private final UUID farmId = UUID.fromString("00000000-0000-0000-0000-000000000201");
    private final UUID cropId = UUID.fromString("00000000-0000-0000-0000-000000000301");
    private final UUID mediaId = UUID.fromString("00000000-0000-0000-0000-000000000401");
    private final LocalDateTime workedAt = LocalDateTime.of(2026, 6, 1, 9, 0);
    private final LocalDateTime createdAt = LocalDateTime.of(2026, 6, 1, 9, 5);
    @MockBean private FarmingRecordService farmingRecordService;
    @MockBean private TokenProvider tokenProvider;
    @Test
    @DisplayName("create record maps request to command and returns record id")
    void create_record_maps_request_to_command_and_returns_record_id() throws Exception {
        when(farmingRecordService.create(createCommand()))
            .thenReturn(new FarmingRecordResult.RecordId(recordId, WorkType.HARVEST));
        mockMvc.perform(post("/api/v1/farming-records")
            .with(authenticatedMember(memberId.toString()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(saveRecordJson("맑음", "[\"%s\"]".formatted(mediaId), "10", "2", "오늘은 날씨가 좋아 하루 종일 수확 작업을 진행했고 별다른 문제 없이 마무리했습니다")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id", equalTo(recordId.toString())))
            .andExpect(jsonPath("$.data.workType", equalTo("HARVEST")));
    }

    @Test
    @DisplayName("create record without auth returns unauthorized")
    void create_record_without_auth_returns_unauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/farming-records")
            .contentType(MediaType.APPLICATION_JSON)
            .content(saveRecordJson("맑음", "[\"%s\"]".formatted(mediaId), "10", "2", "오늘은 날씨가 좋아 하루 종일 수확 작업을 진행했고 별다른 문제 없이 마무리했습니다")))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code", equalTo("AUTH_001")));
    }

    @Test
    @DisplayName("create record rejects blank weather condition")
    void create_record_rejects_blank_weather_condition() throws Exception {
        mockMvc.perform(post("/api/v1/farming-records")
            .with(authenticatedMember(memberId.toString()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(saveRecordJson("", "[\"%s\"]".formatted(mediaId), "10", "2", "오늘은 날씨가 좋아 하루 종일 수확 작업을 진행했고 별다른 문제 없이 마무리했습니다")))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("create record rejects blank memo")
    void create_record_rejects_blank_memo() throws Exception {
        mockMvc.perform(post("/api/v1/farming-records")
            .with(authenticatedMember(memberId.toString()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(saveRecordJson("맑음", "[\"%s\"]".formatted(mediaId), "10", "2", "")))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("create record rejects more than five media ids")
    void create_record_rejects_more_than_five_media_ids() throws Exception {
        var tooManyMediaIds = java.util.stream.IntStream.range(0,
            6).mapToObj(i -> UUID.randomUUID()).map(it -> "\"%s\"".formatted(it)).collect(java.util.stream.Collectors.joining(","));
        mockMvc.perform(post("/api/v1/farming-records")
            .with(authenticatedMember(memberId.toString()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(saveRecordJson("맑음", "[%s]".formatted(tooManyMediaIds), "10", "2", "오늘은 날씨가 좋아 하루 종일 수확 작업을 진행했고 별다른 문제 없이 마무리했습니다")))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("create record rejects duplicate media ids")
    void create_record_rejects_duplicate_media_ids() throws Exception {
        mockMvc.perform(post("/api/v1/farming-records")
            .with(authenticatedMember(memberId.toString()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(saveRecordJson("맑음", "[\"%s\",\"%s\"]".formatted(mediaId, mediaId), "10", "2", "오늘은 날씨가 좋아 하루 종일 수확 작업을 진행했고 별다른 문제 없이 마무리했습니다")))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("create record rejects non-positive harvest amount")
    void create_record_rejects_non_positive_harvest_amount() throws Exception {
        mockMvc.perform(post("/api/v1/farming-records")
            .with(authenticatedMember(memberId.toString()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(saveRecordJson("맑음", "[\"%s\"]".formatted(mediaId), "0", "2", "오늘은 날씨가 좋아 하루 종일 수확 작업을 진행했고 별다른 문제 없이 마무리했습니다")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code", equalTo("COMMON_001")));
    }

    @Test
    @DisplayName("create record rejects non-positive growth period")
    void create_record_rejects_non_positive_growth_period() throws Exception {
        mockMvc.perform(post("/api/v1/farming-records")
            .with(authenticatedMember(memberId.toString()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(saveRecordJson("맑음", "[\"%s\"]".formatted(mediaId), "10", "0", "오늘은 날씨가 좋아 하루 종일 수확 작업을 진행했고 별다른 문제 없이 마무리했습니다")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code", equalTo("COMMON_001")));
    }

    @Test
    @DisplayName("create record rejects missing growth period")
    void create_record_rejects_missing_growth_period() throws Exception {
        mockMvc.perform(post("/api/v1/farming-records")
            .with(authenticatedMember(memberId.toString()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(saveRecordJson("맑음", "[\"%s\"]".formatted(mediaId), "10", "null", "오늘은 날씨가 좋아 하루 종일 수확 작업을 진행했고 별다른 문제 없이 마무리했습니다")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code", equalTo("COMMON_001")));
    }

    @Test
    @DisplayName("create record rejects blank fertilizing material name")
    void create_record_rejects_blank_fertilizing_material_name() throws Exception {
        var json = """
            {
              "farmId":"%s",
              "cropId":"%s",
              "workType":"FERTILIZING",
              "workedAt":"2026-06-01T09:00:00",
              "weatherCondition":"맑음",
              "weatherTemperature":28,
              "memo":"오늘은 날씨가 좋아 하루 종일 시비 작업을 진행했고 별다른 문제 없이 마무리했습니다",
              "fertilizing":{
                "materialName":" ",
                "amount":10,
                "amountUnit":"G"
              }
            }
        """.formatted(farmId, cropId);
        mockMvc.perform(post("/api/v1/farming-records")
            .with(authenticatedMember(memberId.toString()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(json))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code", equalTo("COMMON_001")));
    }

    @Test
    @DisplayName("create record accepts harvest without medicinal part")
    void create_record_accepts_harvest_without_medicinal_part() throws Exception {
        var command = new FarmingRecordCommand.Create(memberId,
            farmId,
            cropId,
            WorkType.HARVEST,
            workedAt,
            "맑음",
            28,
            "오늘은 날씨가 좋아 하루 종일 수확 작업을 진행했고 별다른 문제 없이 마무리했습니다",
            null,
            null,
            null,
            null,
            null,
            new FarmingRecordCommand.HarvestDetail(BigDecimal.TEN,
            false,
            null,
            HarvestSource.CULTIVATED,
            2,
            true),
            java.util.Collections.emptyList(),
            EntryMode.MANUAL);
        when(farmingRecordService.create(command)).thenReturn(new FarmingRecordResult.RecordId(recordId, WorkType.HARVEST));
        var json = """
            {
              "farmId":"%s",
              "cropId":"%s",
              "workType":"HARVEST",
              "workedAt":"2026-06-01T09:00:00",
              "weatherCondition":"맑음",
              "weatherTemperature":28,
              "memo":"오늘은 날씨가 좋아 하루 종일 수확 작업을 진행했고 별다른 문제 없이 마무리했습니다",
              "harvest":{
                "harvestAmount":10,
                "harvestSource":"CULTIVATED",
                "growthPeriod":2,
                "isLastHarvest":true
              }
            }
        """.formatted(farmId, cropId);
        mockMvc.perform(post("/api/v1/farming-records")
            .with(authenticatedMember(memberId.toString()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(json))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("create record accepts unknown harvest amount")
    void create_record_accepts_unknown_harvest_amount() throws Exception {
        var command = new FarmingRecordCommand.Create(memberId,
            farmId,
            cropId,
            WorkType.HARVEST,
            workedAt,
            "맑음",
            28,
            "오늘은 날씨가 좋아 하루 종일 수확 작업을 진행했고 별다른 문제 없이 마무리했습니다",
            null,
            null,
            null,
            null,
            null,
            new FarmingRecordCommand.HarvestDetail(null,
            true,
            CropUsePartCategory.ROOT_BARK,
            HarvestSource.CULTIVATED,
            2,
            true),
            java.util.Collections.emptyList(),
            EntryMode.MANUAL);
        when(farmingRecordService.create(command)).thenReturn(new FarmingRecordResult.RecordId(recordId, WorkType.HARVEST));
        var json = """
            {
              "farmId":"%s",
              "cropId":"%s",
              "workType":"HARVEST",
              "workedAt":"2026-06-01T09:00:00",
              "weatherCondition":"맑음",
              "weatherTemperature":28,
              "memo":"오늘은 날씨가 좋아 하루 종일 수확 작업을 진행했고 별다른 문제 없이 마무리했습니다",
              "harvest":{
                "harvestAmountUnknown":true,
                "medicinalPart":"ROOT_BARK",
                "harvestSource":"CULTIVATED",
                "growthPeriod":2,
                "isLastHarvest":true
              }
            }
        """.formatted(farmId, cropId);
        mockMvc.perform(post("/api/v1/farming-records")
            .with(authenticatedMember(memberId.toString()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(json))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("create record rejects harvest without last harvest flag")
    void create_record_rejects_harvest_without_last_harvest_flag() throws Exception {
        var json = """
            {
              "farmId":"%s",
              "cropId":"%s",
              "workType":"HARVEST",
              "workedAt":"2026-06-01T09:00:00",
              "weatherCondition":"맑음",
              "weatherTemperature":28,
              "memo":"오늘은 날씨가 좋아 하루 종일 수확 작업을 진행했고 별다른 문제 없이 마무리했습니다",
              "harvest":{
                "harvestAmount":10,
                "medicinalPart":"ROOT_BARK",
                "harvestSource":"CULTIVATED",
                "growthPeriod":2
              }
            }
        """.formatted(farmId, cropId);
        mockMvc.perform(post("/api/v1/farming-records")
            .with(authenticatedMember(memberId.toString()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(json))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("create record rejects planting without planting method")
    void create_record_rejects_planting_without_planting_method() throws Exception {
        var json = """
            {
              "farmId":"%s",
              "cropId":"%s",
              "workType":"PLANTING",
              "workedAt":"2026-06-01T09:00:00",
              "weatherCondition":"맑음",
              "weatherTemperature":28,
              "memo":"오늘은 날씨가 좋아 하루 종일 파종 작업을 진행했고 별다른 문제 없이 마무리했습니다",
              "planting":{
                "seedAmount":10,
                "seedAmountUnit":"G"
              }
            }
        """.formatted(farmId, cropId);
        mockMvc.perform(post("/api/v1/farming-records")
            .with(authenticatedMember(memberId.toString()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(json))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("create record rejects memo shorter than thirty characters")
    void create_record_rejects_memo_shorter_than_thirty_characters() throws Exception {
        mockMvc.perform(post("/api/v1/farming-records")
            .with(authenticatedMember(memberId.toString()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(saveRecordJson("맑음", "[\"%s\"]".formatted(mediaId), "10", "2", "너무 짧은 메모")))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("create record rejects memo longer than five hundred characters")
    void create_record_rejects_memo_longer_than_five_hundred_characters() throws Exception {
        mockMvc.perform(post("/api/v1/farming-records")
            .with(authenticatedMember(memberId.toString()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(saveRecordJson("맑음", "[\"%s\"]".formatted(mediaId), "10", "2", "메".repeat(501))))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("list records returns cursor page")
    void list_records_returns_cursor_page() throws Exception {
        when(farmingRecordService.search(anySearchCondition())).thenReturn(pageResult());
        mockMvc.perform(get("/api/v1/farming-records")
            .with(authenticatedMember(memberId.toString()))
            .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].id", equalTo(recordId.toString())))
            .andExpect(jsonPath("$.data.items[0].harvestAmount", equalTo(10)))
            .andExpect(jsonPath("$.data.items[0].plantingMethod", nullValue()))
            .andExpect(jsonPath("$.data.items[0].materialName", nullValue()))
            .andExpect(jsonPath("$.data.nextCursor", equalTo("cursor-1")));
    }

    @Test
    @DisplayName("list records maps filters and cursor parameters")
    void list_records_maps_filters_and_cursor_parameters() throws Exception {
        when(farmingRecordService.search(new FarmingRecordSearchCondition(memberId,
            java.util.Arrays.asList(cropId),
            java.util.Arrays.asList(WorkType.HARVEST),
            LocalDate.of(2026,
            6,
            1),
            LocalDate.of(2026,
            6,
            30),
            null,
            "cursor-1",
            10))).thenReturn(pageResult());
        mockMvc.perform(get("/api/v1/farming-records")
            .with(authenticatedMember(memberId.toString()))
            .param("cropIds", cropId.toString())
            .param("workTypes", "HARVEST")
            .param("startDate", "2026-06-01")
            .param("endDate", "2026-06-30")
            .param("cursor", "cursor-1")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.nextCursor", equalTo("cursor-1")));
    }

    @Test
    @DisplayName("list records maps keyword parameter")
    void list_records_maps_keyword_parameter() throws Exception {
        when(farmingRecordService.search(new FarmingRecordSearchCondition(memberId,
            java.util.Collections.emptyList(),
            java.util.Collections.emptyList(),
            null,
            null,
            "수확",
            null,
            20))).thenReturn(pageResult());
        mockMvc.perform(get("/api/v1/farming-records")
            .with(authenticatedMember(memberId.toString()))
            .param("keyword", "수확"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].id", equalTo(recordId.toString())));
    }

    @Test
    @DisplayName("get record detail returns detail")
    void get_record_detail_returns_detail() throws Exception {
        when(farmingRecordService.getDetail(memberId, recordId)).thenReturn(detailResult());
        mockMvc.perform(get("/api/v1/farming-records/{recordId}", recordId)
            .with(authenticatedMember(memberId.toString())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id", equalTo(recordId.toString())))
            .andExpect(jsonPath("$.data.harvest.harvestAmount", equalTo(10)))
            .andExpect(jsonPath("$.data.harvest.amountUnknown", equalTo(false)))
            .andExpect(jsonPath("$.data.harvest.isLastHarvest", equalTo(true)))
            .andExpect(jsonPath("$.data.images[0].mediaId", equalTo(mediaId.toString())))
            .andExpect(jsonPath("$.data.images[0].url", equalTo("https://example.test/1.jpg")));
    }

    @Test
    @DisplayName("update record maps request to command")
    void update_record_maps_request_to_command() throws Exception {
        when(farmingRecordService.update(updateCommand()))
            .thenReturn(new FarmingRecordResult.RecordId(recordId, WorkType.HARVEST));
        mockMvc.perform(patch("/api/v1/farming-records/{recordId}", recordId)
            .with(authenticatedMember(memberId.toString()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(saveRecordJson("맑음", "[\"%s\"]".formatted(mediaId), "10", "2", "오늘은 날씨가 좋아 하루 종일 수확 작업을 진행했고 별다른 문제 없이 마무리했습니다")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id", equalTo(recordId.toString())));
    }

    @Test
    @DisplayName("delete record delegates to service")
    void delete_record_delegates_to_service() throws Exception {
        mockMvc.perform(delete("/api/v1/farming-records/{recordId}", recordId)
            .with(authenticatedMember(memberId.toString())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success", equalTo(true)));
    }

    @Test
    void omittedDefaultsKeepManualEntryAndCultivatedHarvest() throws Exception {
        when(farmingRecordService.create(org.mockito.ArgumentMatchers.any()))
            .thenReturn(new FarmingRecordResult.RecordId(recordId, WorkType.HARVEST));
        var node = (com.fasterxml.jackson.databind.node.ObjectNode) new com.fasterxml.jackson.databind.ObjectMapper()
            .readTree(saveRecordJson("맑음", "[]", "10", "2", "오늘은 날씨가 좋아 하루 종일 수확 작업을 진행했고 별다른 문제 없이 마무리했습니다"));
        node.remove("mediaIds");
        ((com.fasterxml.jackson.databind.node.ObjectNode) node.get("harvest")).remove("harvestSource");
        mockMvc.perform(post("/api/v1/farming-records").with(authenticatedMember(memberId.toString()))
            .contentType(MediaType.APPLICATION_JSON).content(node.toString()))
            .andExpect(status().isOk());
        var captor = org.mockito.ArgumentCaptor.forClass(FarmingRecordCommand.Create.class);
        org.mockito.Mockito.verify(farmingRecordService).create(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getMediaIds()).isEmpty();
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getEntryMode()).isEqualTo(EntryMode.MANUAL);
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getHarvest().getHarvestSource()).isEqualTo(HarvestSource.CULTIVATED);
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getHarvest().getAmountUnknown()).isFalse();
    }

    @Test
    void explicitNullDefaultsAreJsonErrorsAndNullableRequiredIdsAreValidationErrors() throws Exception {
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        var json = saveRecordJson("맑음", "[]", "10", "2", "오늘은 날씨가 좋아 하루 종일 수확 작업을 진행했고 별다른 문제 없이 마무리했습니다");
        for (var field : List.of("mediaIds", "entryMode", "memo", "harvestSource")) {
            var node = (com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree(json);
            var target = field.equals("harvestSource") ? (com.fasterxml.jackson.databind.node.ObjectNode) node.get("harvest") : node;
            target.putNull(field);
            mockMvc.perform(post("/api/v1/farming-records").with(authenticatedMember(memberId.toString()))
                .contentType(MediaType.APPLICATION_JSON).content(node.toString()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code", equalTo("COMMON_002")));
        }
        var node = (com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree(json);
        node.putNull("farmId");
        mockMvc.perform(post("/api/v1/farming-records").with(authenticatedMember(memberId.toString()))
            .contentType(MediaType.APPLICATION_JSON).content(node.toString()))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code", equalTo("COMMON_001")))
            .andExpect(jsonPath("$.error.detail.field", equalTo("farmId")));
    }


    private RequestPostProcessor authenticatedMember(String memberId) {
        return (RequestPostProcessor) (request -> {
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(memberId,
                null,
                java.util.Arrays.asList(new SimpleGrantedAuthority("ROLE_USER"))));
                return request;
        });
    }

    private String saveRecordJson(String weatherCondition, String mediaIdsJson, String harvestAmount, String growthPeriod, String memo) {
        return """
            {
              "farmId":"%s",
              "cropId":"%s",
              "workType":"HARVEST",
              "workedAt":"2026-06-01T09:00:00",
              "weatherCondition":"%s",
              "weatherTemperature":28,
              "memo":"%s",
              "harvest":{
                "harvestAmount":%s,
                "medicinalPart":"ROOT_BARK",
                "harvestSource":"CULTIVATED",
                "growthPeriod":%s,
                "isLastHarvest":true
              },
              "mediaIds":%s
            }
        """.formatted(farmId, cropId, weatherCondition, memo, harvestAmount, growthPeriod, mediaIdsJson);
    }

    private FarmingRecordCommand.Create createCommand() {
        return new FarmingRecordCommand.Create(memberId,
            farmId,
            cropId,
            WorkType.HARVEST,
            workedAt,
            "맑음",
            28,
            "오늘은 날씨가 좋아 하루 종일 수확 작업을 진행했고 별다른 문제 없이 마무리했습니다",
            null,
            null,
            null,
            null,
            null,
            new FarmingRecordCommand.HarvestDetail(BigDecimal.TEN,
            false,
            CropUsePartCategory.ROOT_BARK,
            HarvestSource.CULTIVATED,
            2,
            true),
            java.util.Arrays.asList(mediaId),
            EntryMode.MANUAL);
    }

    private FarmingRecordCommand.Update updateCommand() {
        return new FarmingRecordCommand.Update(memberId,
            recordId,
            farmId,
            cropId,
            WorkType.HARVEST,
            workedAt,
            "맑음",
            28,
            "오늘은 날씨가 좋아 하루 종일 수확 작업을 진행했고 별다른 문제 없이 마무리했습니다",
            null,
            null,
            null,
            null,
            null,
            new FarmingRecordCommand.HarvestDetail(BigDecimal.TEN,
            false,
            CropUsePartCategory.ROOT_BARK,
            HarvestSource.CULTIVATED,
            2,
            true),
            java.util.Arrays.asList(mediaId));
    }

    private FarmingRecordResult.Page pageResult() {
        return new FarmingRecordResult.Page(java.util.Arrays.asList(summaryResult()), "cursor-1");
    }

    private FarmingRecordResult.Summary summaryResult() {
        return new FarmingRecordResult.Summary(recordId,
            cropId,
            "마늘",
            WorkType.HARVEST,
            workedAt,
            "맑음",
            28,
            "수확 완료",
            "https://example.test/1.jpg",
            null,
            BigDecimal.TEN,
            null,
            null,
            null,
            null);
    }

    private FarmingRecordResult.Detail detailResult() {
        return new FarmingRecordResult.Detail(recordId,
            farmId,
            "약초농장",
            cropId,
            "마늘",
            WorkType.HARVEST,
            workedAt,
            "맑음",
            28,
            "수확 완료",
            null,
            null,
            null,
            null,
            null,
            new FarmingRecordResult.HarvestDetail(BigDecimal.TEN,
            CropUsePartCategory.ROOT_BARK,
            HarvestSource.CULTIVATED,
            2,
            true),
            java.util.Arrays.asList(new FarmingRecordResult.MediaItem(mediaId,
        "https://example.test/1.jpg")),
            createdAt,
            createdAt);
    }

    private FarmingRecordSearchCondition anySearchCondition() {
        return new FarmingRecordSearchCondition(memberId,
            java.util.Collections.emptyList(),
            java.util.Collections.emptyList(),
            null,
            null,
            null,
            null,
            20);
    }
}
