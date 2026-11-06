package com.chamchamcham.api.policy.dto.program;

import com.chamchamcham.application.policy.recommendation.PolicyRecommendationResult;
import org.springframework.lang.Nullable;

public record PolicyAttachmentResponse(
    @Nullable String fileName,
    @Nullable String extension,
    @Nullable Long sizeBytes,
    @Nullable String url
) {
    @Nullable public String getFileName() { return fileName; }
    @Nullable public String getExtension() { return extension; }
    @Nullable public Long getSizeBytes() { return sizeBytes; }
    @Nullable public String getUrl() { return url; }

    public static PolicyAttachmentResponse from(PolicyRecommendationResult.Attachment result) {
        return new PolicyAttachmentResponse(
            result.getFileName(),
            result.getExtension(),
            result.getSizeBytes(),
            result.getUrl()
        );
    }
}
