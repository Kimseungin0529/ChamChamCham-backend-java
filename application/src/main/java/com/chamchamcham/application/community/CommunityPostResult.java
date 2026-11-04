package com.chamchamcham.application.community;

import com.chamchamcham.domain.community.CommunityPost;
import com.chamchamcham.domain.community.CommunityPostType;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;
import java.util.Objects;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public final class CommunityPostResult {
    private CommunityPostResult() {}

    public record PostId(
        @NonNull UUID id
    ) {
        public PostId {
            Objects.requireNonNull(id, "id");
        }

        @NonNull
        public UUID getId() { return id; }
    }

    public record LikeToggle(
        boolean liked,
        long likeCount
    ) {
        public boolean getLiked() { return liked; }
        public long getLikeCount() { return likeCount; }
    }

    public record AuthorSummary(
        @NonNull UUID memberId,
        @Nullable String nickname,
        @Nullable String profileImageUrl
    ) {
        public AuthorSummary {
            Objects.requireNonNull(memberId, "memberId");
        }

        @NonNull
        public UUID getMemberId() { return memberId; }
        @Nullable
        public String getNickname() { return nickname; }
        @Nullable
        public String getProfileImageUrl() { return profileImageUrl; }
    }

    public record PostSummary(
        @NonNull UUID id,
        @NonNull UUID cropId,
        @NonNull String cropName,
        @NonNull CommunityPostType postType,
        @NonNull String title,
        @NonNull String bodyPreview,
        @Nullable String thumbnailUrl,
        @NonNull AuthorSummary author,
        long commentCount,
        long likeCount,
        boolean likedByMe,
        @NonNull LocalDateTime createdAt
    ) {
        public PostSummary {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(cropId, "cropId");
            Objects.requireNonNull(cropName, "cropName");
            Objects.requireNonNull(postType, "postType");
            Objects.requireNonNull(title, "title");
            Objects.requireNonNull(bodyPreview, "bodyPreview");
            Objects.requireNonNull(author, "author");
            Objects.requireNonNull(createdAt, "createdAt");
        }

        @NonNull
        public UUID getId() { return id; }
        @NonNull
        public UUID getCropId() { return cropId; }
        @NonNull
        public String getCropName() { return cropName; }
        @NonNull
        public CommunityPostType getPostType() { return postType; }
        @NonNull
        public String getTitle() { return title; }
        @NonNull
        public String getBodyPreview() { return bodyPreview; }
        @Nullable
        public String getThumbnailUrl() { return thumbnailUrl; }
        @NonNull
        public AuthorSummary getAuthor() { return author; }
        public long getCommentCount() { return commentCount; }
        public long getLikeCount() { return likeCount; }
        public boolean getLikedByMe() { return likedByMe; }
        @NonNull
        public LocalDateTime getCreatedAt() { return createdAt; }
    }

    public record PostDetail(
        @NonNull UUID id,
        @NonNull UUID cropId,
        @NonNull String cropName,
        @NonNull CommunityPostType postType,
        @NonNull String title,
        @NonNull String body,
        @NonNull List<String> imageUrls,
        @Nullable UUID farmingRecordId,
        @NonNull AuthorSummary author,
        long commentCount,
        long likeCount,
        boolean likedByMe,
        @NonNull LocalDateTime createdAt
    ) {
        public PostDetail {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(cropId, "cropId");
            Objects.requireNonNull(cropName, "cropName");
            Objects.requireNonNull(postType, "postType");
            Objects.requireNonNull(title, "title");
            Objects.requireNonNull(body, "body");
            Objects.requireNonNull(imageUrls, "imageUrls");
            Objects.requireNonNull(author, "author");
            Objects.requireNonNull(createdAt, "createdAt");
        }

        @NonNull
        public UUID getId() { return id; }
        @NonNull
        public UUID getCropId() { return cropId; }
        @NonNull
        public String getCropName() { return cropName; }
        @NonNull
        public CommunityPostType getPostType() { return postType; }
        @NonNull
        public String getTitle() { return title; }
        @NonNull
        public String getBody() { return body; }
        @NonNull
        public List<String> getImageUrls() { return imageUrls; }
        @Nullable
        public UUID getFarmingRecordId() { return farmingRecordId; }
        @NonNull
        public AuthorSummary getAuthor() { return author; }
        public long getCommentCount() { return commentCount; }
        public long getLikeCount() { return likeCount; }
        public boolean getLikedByMe() { return likedByMe; }
        @NonNull
        public LocalDateTime getCreatedAt() { return createdAt; }
    }

    public record Page(
        @NonNull List<PostSummary> items,
        @Nullable String nextCursor
    ) {
        public Page {
            Objects.requireNonNull(items, "items");
        }

        @NonNull
        public List<PostSummary> getItems() { return items; }
        @Nullable
        public String getNextCursor() { return nextCursor; }
    }

    public record Board(
        @NonNull UUID cropId,
        @NonNull String cropName
    ) {
        public Board {
            Objects.requireNonNull(cropId, "cropId");
            Objects.requireNonNull(cropName, "cropName");
        }

        @NonNull
        public UUID getCropId() { return cropId; }
        @NonNull
        public String getCropName() { return cropName; }
    }
    public static PostId idOf(CommunityPost post) {
        Objects.requireNonNull(post, "post");
        if (post.getId() == null) throw new IllegalArgumentException("Persisted post id is required");
        return new PostId(post.getId());
    }
}
