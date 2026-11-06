package com.chamchamcham.api.pesticide.controller;

import com.chamchamcham.api.common.ApiResponse;
import com.chamchamcham.api.pesticide.dto.PesticideResponses.PestSummaryResponse;
import com.chamchamcham.api.pesticide.dto.PesticideResponses.PesticidePageResponse;
import com.chamchamcham.application.pesticide.PesticideCatalogService;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
public class PesticideController {
    private final PesticideCatalogService pesticideCatalogService;

    public PesticideController(PesticideCatalogService pesticideCatalogService) {
        this.pesticideCatalogService = java.util.Objects.requireNonNull(pesticideCatalogService, "pesticideCatalogService");
    }

    @GetMapping("/pesticides")
    public ResponseEntity<ApiResponse<PesticidePageResponse>> searchPesticides(
        @RequestParam(required = false) @Nullable String keyword,
        @RequestParam(required = false) @Nullable String cursor,
        @RequestParam(defaultValue = "20") int size
    ) throws JsonProcessingException {
        var result = pesticideCatalogService.search(keyword, cursor, size);
        return ResponseEntity.ok(ApiResponse.ok(PesticidePageResponse.from(result)));
    }

    @GetMapping("/pesticides/{pesticideId}/pests")
    public ResponseEntity<ApiResponse<List<PestSummaryResponse>>> listPests(@PathVariable UUID pesticideId) {
        var result = pesticideCatalogService.listPestsByPesticide(pesticideId);
        return ResponseEntity.ok(ApiResponse.ok(result.stream().map(PestSummaryResponse::from)
            .collect(Collectors.toCollection(ArrayList::new))));
    }
}
