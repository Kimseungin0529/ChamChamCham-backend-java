package com.chamchamcham.api.crop.controller;

import com.chamchamcham.api.common.ApiResponse;
import com.chamchamcham.api.crop.dto.CropResponses;
import com.chamchamcham.application.crop.CropCatalogService;
import com.chamchamcham.domain.crop.CropUsePartCategory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/crops")
public class CropController {
    private final CropCatalogService cropCatalogService;
    public CropController(CropCatalogService cropCatalogService) { this.cropCatalogService = cropCatalogService; }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CropResponses.CropResponse>>> listCrops() {
        var crops = cropCatalogService.listCrops().stream().map(CropResponses.CropResponse::from).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(crops));
    }
    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<CropResponses.CategoryResponse>>> listCategories() {
        var categories = cropCatalogService.listCategories().stream().map(CropResponses.CategoryResponse::from).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(categories));
    }
    @GetMapping("/categories/{category}/crops")
    public ResponseEntity<ApiResponse<List<CropResponses.CropResponse>>> listCropsByCategory(@PathVariable CropUsePartCategory category) {
        var crops = cropCatalogService.listCropsByCategory(category).stream().map(CropResponses.CropResponse::from).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(crops));
    }
}
