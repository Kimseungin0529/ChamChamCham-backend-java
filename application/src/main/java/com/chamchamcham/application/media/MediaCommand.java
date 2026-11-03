package com.chamchamcham.application.media;

import com.chamchamcham.domain.media.UploadedMediaUsageType;
import java.util.UUID;
import java.util.List;
import java.util.Objects;
import org.springframework.lang.Nullable;
import org.springframework.lang.NonNull;

public final class MediaCommand {
    private MediaCommand() {}

    public record UploadImage(
        @NonNull UUID memberId,
        @NonNull UploadedMediaUsageType usageType,
        @NonNull String base64Image,
        @Nullable String originalFilename,
        @Nullable String contentType
    ) {
        public UploadImage {
            Objects.requireNonNull(memberId, "memberId");
            Objects.requireNonNull(usageType, "usageType");
            Objects.requireNonNull(base64Image, "base64Image");
        }

        @NonNull
        public UUID getMemberId() { return memberId; }
        @NonNull
        public UploadedMediaUsageType getUsageType() { return usageType; }
        @NonNull
        public String getBase64Image() { return base64Image; }
        @Nullable
        public String getOriginalFilename() { return originalFilename; }
        @Nullable
        public String getContentType() { return contentType; }

    }
}
