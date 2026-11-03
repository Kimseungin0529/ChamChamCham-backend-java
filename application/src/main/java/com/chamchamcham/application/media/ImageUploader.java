package com.chamchamcham.application.media;

import java.util.UUID;
import java.util.List;
import java.util.Objects;
import org.springframework.lang.Nullable;
import org.springframework.lang.NonNull;

public interface ImageUploader {
    UploadedImage upload(UploadCommand command);

    void delete(String cloudinaryPublicId);

    public record UploadCommand(
        @NonNull byte[] bytes,
        @Nullable String originalFilename,
        @Nullable String contentType,
        @NonNull String usageFolder
    ) {
        public UploadCommand {
            Objects.requireNonNull(bytes, "bytes");
            Objects.requireNonNull(usageFolder, "usageFolder");
        }

        @NonNull
        public byte[] getBytes() { return bytes; }
        @Nullable
        public String getOriginalFilename() { return originalFilename; }
        @Nullable
        public String getContentType() { return contentType; }
        @NonNull
        public String getUsageFolder() { return usageFolder; }

        @Override
        public int hashCode() {
            int result = java.util.Arrays.hashCode(bytes);
            result = 31 * result + Objects.hashCode(originalFilename);
            result = 31 * result + Objects.hashCode(contentType);
            return 31 * result + usageFolder.hashCode();
        }
    }

    public record UploadedImage(
        @NonNull String imageUrl,
        @NonNull String cloudinaryPublicId
    ) {
        public UploadedImage {
            Objects.requireNonNull(imageUrl, "imageUrl");
            Objects.requireNonNull(cloudinaryPublicId, "cloudinaryPublicId");
        }

        @NonNull
        public String getImageUrl() { return imageUrl; }
        @NonNull
        public String getCloudinaryPublicId() { return cloudinaryPublicId; }

    }
}
