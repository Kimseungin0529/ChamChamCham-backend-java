package com.chamchamcham.application.community;

import com.chamchamcham.domain.community.CommunityPostType;
import java.util.UUID;
import java.util.List;
import java.util.Objects;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public final class CommunityPostCommand {
    private CommunityPostCommand() {}

    public record Create(
        @NonNull UUID memberId,
        @NonNull UUID cropId,
        @NonNull CommunityPostType postType,
        @NonNull String title,
        @NonNull String body,
        @Nullable UUID farmingRecordId,
        @NonNull List<UUID> mediaIds
    ) {
        public Create {
            Objects.requireNonNull(memberId, "memberId");
            Objects.requireNonNull(cropId, "cropId");
            Objects.requireNonNull(postType, "postType");
            Objects.requireNonNull(title, "title");
            Objects.requireNonNull(body, "body");
            Objects.requireNonNull(mediaIds, "mediaIds");
        }

        @NonNull
        public UUID getMemberId() { return memberId; }
        @NonNull
        public UUID getCropId() { return cropId; }
        @NonNull
        public CommunityPostType getPostType() { return postType; }
        @NonNull
        public String getTitle() { return title; }
        @NonNull
        public String getBody() { return body; }
        @Nullable
        public UUID getFarmingRecordId() { return farmingRecordId; }
        @NonNull
        public List<UUID> getMediaIds() { return mediaIds; }
    }

    public record Update(
        @NonNull UUID memberId,
        @NonNull UUID postId,
        @NonNull UUID cropId,
        @NonNull CommunityPostType postType,
        @NonNull String title,
        @NonNull String body,
        @Nullable UUID farmingRecordId,
        @NonNull List<UUID> mediaIds
    ) {
        public Update {
            Objects.requireNonNull(memberId, "memberId");
            Objects.requireNonNull(postId, "postId");
            Objects.requireNonNull(cropId, "cropId");
            Objects.requireNonNull(postType, "postType");
            Objects.requireNonNull(title, "title");
            Objects.requireNonNull(body, "body");
            Objects.requireNonNull(mediaIds, "mediaIds");
        }

        @NonNull
        public UUID getMemberId() { return memberId; }
        @NonNull
        public UUID getPostId() { return postId; }
        @NonNull
        public UUID getCropId() { return cropId; }
        @NonNull
        public CommunityPostType getPostType() { return postType; }
        @NonNull
        public String getTitle() { return title; }
        @NonNull
        public String getBody() { return body; }
        @Nullable
        public UUID getFarmingRecordId() { return farmingRecordId; }
        @NonNull
        public List<UUID> getMediaIds() { return mediaIds; }
    }

    public record Delete(
        @NonNull UUID memberId,
        @NonNull UUID postId
    ) {
        public Delete {
            Objects.requireNonNull(memberId, "memberId");
            Objects.requireNonNull(postId, "postId");
        }

        @NonNull
        public UUID getMemberId() { return memberId; }
        @NonNull
        public UUID getPostId() { return postId; }
    }

    public record ToggleLike(
        @NonNull UUID memberId,
        @NonNull UUID postId
    ) {
        public ToggleLike {
            Objects.requireNonNull(memberId, "memberId");
            Objects.requireNonNull(postId, "postId");
        }

        @NonNull
        public UUID getMemberId() { return memberId; }
        @NonNull
        public UUID getPostId() { return postId; }
    }
}
