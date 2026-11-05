package com.chamchamcham.api.media.dto;

import com.chamchamcham.domain.media.UploadedMediaUsageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public final class MediaRequests {
    private MediaRequests() {}

    public record UploadImageRequest(
        @NotNull(message = "이미지 용도를 입력해주세요")
        @Nullable UploadedMediaUsageType usageType,
        @NotBlank(message = "이미지 데이터를 입력해주세요")
        @NonNull String base64Image,
        @Nullable String originalFilename,
        @Nullable String contentType
    ) {
        public UploadImageRequest {
            Objects.requireNonNull(base64Image, "base64Image");
        }
        @Nullable public UploadedMediaUsageType getUsageType() { return usageType; }
        @NonNull public String getBase64Image() { return base64Image; }
        @Nullable public String getOriginalFilename() { return originalFilename; }
        @Nullable public String getContentType() { return contentType; }
    }
}
