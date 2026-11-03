package com.chamchamcham.application.media;

import com.chamchamcham.domain.media.UploadedMedia;
import com.chamchamcham.domain.media.UploadedMediaStatus;
import java.util.UUID;
import java.util.List;
import java.util.Objects;
import org.springframework.lang.Nullable;
import org.springframework.lang.NonNull;

public final class MediaResult {
    private MediaResult() {}

    public record UploadedImage(
        @NonNull UUID mediaId,
        @NonNull String imageUrl,
        @NonNull UploadedMediaStatus status
    ) {
        public UploadedImage {
            Objects.requireNonNull(mediaId, "mediaId");
            Objects.requireNonNull(imageUrl, "imageUrl");
            Objects.requireNonNull(status, "status");
        }

        @NonNull
        public UUID getMediaId() { return mediaId; }
        @NonNull
        public String getImageUrl() { return imageUrl; }
        @NonNull
        public UploadedMediaStatus getStatus() { return status; }

        public static UploadedImage from(UploadedMedia media) {
            Objects.requireNonNull(media, "media");
            if (media.getId() == null) throw new IllegalArgumentException("Persisted media id is required");
            return new UploadedImage(media.getId(), media.getFileUrl(), media.getStatus());
        }
    }
}
