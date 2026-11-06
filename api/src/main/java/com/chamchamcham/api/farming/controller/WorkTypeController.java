package com.chamchamcham.api.farming.controller;

import com.chamchamcham.api.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.chamchamcham.api.farming.dto.WorkTypeResponses;
import com.chamchamcham.application.farming.WorkTypeCatalogService;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/work-types")
public class WorkTypeController {
    private final WorkTypeCatalogService workTypeCatalogService;
    public WorkTypeController(WorkTypeCatalogService workTypeCatalogService) { this.workTypeCatalogService = workTypeCatalogService; }
    @GetMapping
    public ResponseEntity<ApiResponse<List<WorkTypeResponses.WorkTypeResponse>>> listWorkTypes() {
        var workTypes = workTypeCatalogService.listWorkTypes().stream().map(WorkTypeResponses.WorkTypeResponse::from).collect(Collectors.toCollection(ArrayList::new));
        return ResponseEntity.ok(ApiResponse.ok(workTypes));
    }
}
