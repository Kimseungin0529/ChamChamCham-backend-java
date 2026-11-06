package com.chamchamcham.api.policy.controller;

import com.chamchamcham.api.common.ApiResponse;
import com.chamchamcham.api.policy.dto.program.PolicyProgramDetailResponse;
import com.chamchamcham.api.policy.dto.recommendation.PolicyRecommendationPageResponse;
import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.chamchamcham.application.policy.recommendation.PolicyRecommendationService;
import com.chamchamcham.application.policy.support.PolicyBenefitCategory;
import com.chamchamcham.domain.policy.PolicyRecommendationSort;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/policies")
public class PolicyController {
    private final PolicyRecommendationService policyRecommendationService;

    public PolicyController(PolicyRecommendationService policyRecommendationService) {
        this.policyRecommendationService = java.util.Objects.requireNonNull(policyRecommendationService, "policyRecommendationService");
    }

    @GetMapping("/recommendations")
    public ResponseEntity<ApiResponse<PolicyRecommendationPageResponse>> listRecommendations(
        @AuthenticationPrincipal @Nullable Object principal,
        @RequestParam(required = false) @Nullable String cursor,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) @Nullable String benefitCategory,
        @RequestParam(defaultValue = "RECOMMENDED") String sort
    ) throws JsonProcessingException {
        java.util.Objects.requireNonNull(sort, "sort");
        var parsedBenefitCategory = benefitCategory == null ? null : PolicyBenefitCategory.fromKey(benefitCategory);
        if (benefitCategory != null && parsedBenefitCategory == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        var parsedSort = PolicyRecommendationSort.fromKey(sort);
        if (parsedSort == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        var result = policyRecommendationService.listRecommendations(
            parseMemberId(principal), cursor, size, parsedBenefitCategory, parsedSort);
        return ResponseEntity.ok(ApiResponse.ok(PolicyRecommendationPageResponse.from(result)));
    }

    @GetMapping("/{policyProgramId}")
    public ResponseEntity<ApiResponse<PolicyProgramDetailResponse>> getProgramDetail(
        @AuthenticationPrincipal @Nullable Object principal,
        @PathVariable UUID policyProgramId
    ) {
        var result = policyRecommendationService.getProgramDetail(parseMemberId(principal), policyProgramId);
        return ResponseEntity.ok(ApiResponse.ok(PolicyProgramDetailResponse.from(result)));
    }

    private UUID parseMemberId(@Nullable Object principal) {
        String memberId = principal instanceof String value ? value
            : principal instanceof UserDetails userDetails ? userDetails.getUsername() : null;
        if (memberId == null || memberId.chars().allMatch(c -> Character.isWhitespace(c) || Character.isSpaceChar(c))) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        try {
            return UUID.fromString(memberId);
        } catch (Throwable exception) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }
}
