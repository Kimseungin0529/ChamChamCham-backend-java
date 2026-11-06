package com.chamchamcham.api.coaching.chat.controller;


import com.chamchamcham.api.coaching.chat.dto.CoachingRagRequests;
import com.chamchamcham.api.coaching.chat.dto.CoachingRagResponses;
import com.chamchamcham.api.common.ApiResponse;
import com.chamchamcham.application.coaching.chat.CoachingRagService;
import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/coaching/rag")
public class CoachingRagController {
    private final CoachingRagService coachingRagService;

    public CoachingRagController(CoachingRagService coachingRagService) {
        this.coachingRagService = Objects.requireNonNull(coachingRagService, "coachingRagService");
    }

    @PostMapping("/query")
    public ResponseEntity<ApiResponse<CoachingRagResponses.QueryResponse>> query(
            Principal principal, @Valid @RequestBody CoachingRagRequests.QueryRequest request) {
        String memberId = principal == null ? null : principal.getName();
        if (memberId == null) {
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            memberId = authentication == null ? null : authentication.getName();
        }
        if (memberId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED);
        var result = coachingRagService.answer(request.toCommand(parseMemberId(memberId)));
        return ResponseEntity.ok(ApiResponse.ok(CoachingRagResponses.QueryResponse.from(result)));
    }

    private UUID parseMemberId(String memberId) {
        try {
            return UUID.fromString(memberId);
        } catch (IllegalArgumentException ignored) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }
}
