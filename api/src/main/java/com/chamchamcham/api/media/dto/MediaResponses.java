package com.chamchamcham.api.media.dto;

import com.chamchamcham.application.media.MediaResult;
import com.chamchamcham.domain.media.UploadedMediaStatus;
import java.util.UUID;
import java.util.Objects;
import org.springframework.lang.NonNull;

public final class MediaResponses {
    private MediaResponses() {}

    public record UploadedImageResponse(
        @NonNull UUID mediaId,
        @NonNull String imageUrl,
        @NonNull UploadedMediaStatus status
    ) {
        public UploadedImageResponse {
            Objects.requireNonNull(mediaId, "mediaId");
            Objects.requireNonNull(imageUrl, "imageUrl");
            Objects.requireNonNull(status, "status");
        }
        @NonNull public UUID getMediaId() { return mediaId; }
        @NonNull public String getImageUrl() { return imageUrl; }
        @NonNull public UploadedMediaStatus getStatus() { return status; }
        public static UploadedImageResponse from(MediaResult.UploadedImage result) {
            return new UploadedImageResponse(result.getMediaId(), result.getImageUrl(), result.getStatus());
        }
    }
}
