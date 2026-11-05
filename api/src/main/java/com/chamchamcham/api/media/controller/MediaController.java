package com.chamchamcham.api.media.controller;

import com.chamchamcham.api.common.ApiResponse;
import com.chamchamcham.api.media.dto.MediaRequests;
import com.chamchamcham.api.media.dto.MediaResponses;
import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.chamchamcham.application.media.MediaCommand;
import com.chamchamcham.application.media.MediaUploadService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/media")
public class MediaController {
    private final MediaUploadService mediaUploadService;
    public MediaController(MediaUploadService mediaUploadService) { this.mediaUploadService = mediaUploadService; }
    @PostMapping("/images")
    public ResponseEntity<ApiResponse<MediaResponses.UploadedImageResponse>> uploadImage(@AuthenticationPrincipal String memberId,
            @Valid @RequestBody MediaRequests.UploadImageRequest request) {
        var result = mediaUploadService.upload(new MediaCommand.UploadImage(parseMemberId(memberId), Objects.requireNonNull(request.usageType()),
            request.base64Image(), request.originalFilename(), request.contentType()));
        return ResponseEntity.ok(ApiResponse.ok(MediaResponses.UploadedImageResponse.from(result)));
    }
    private UUID parseMemberId(String memberId) {
        if (memberId == null || memberId.codePoints().allMatch(c -> Character.isWhitespace(c) || Character.isSpaceChar(c))) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        try { return UUID.fromString(memberId); }
        catch (IllegalArgumentException exception) { throw new BusinessException(ErrorCode.UNAUTHORIZED); }
    }
}
