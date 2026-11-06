package com.chamchamcham.api.community.dto;

import com.chamchamcham.application.community.CommunityCommentResult;
import com.chamchamcham.application.community.CommunityPostResult;
import com.chamchamcham.domain.community.CommunityPostType;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;
import java.util.Objects;
import java.util.ArrayList;
import java.util.stream.Collectors;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public final class CommunityResponses {
    private CommunityResponses() {}

    public record PostIdResponse(
        @NonNull UUID id
    ) {
        public PostIdResponse {
            Objects.requireNonNull(id, "id");
        }
        @NonNull public UUID getId() { return id; }
        public static PostIdResponse from(CommunityPostResult.PostId result) {
            return new PostIdResponse(
                result.getId()
            );
        }
    }

    public record CommentIdResponse(
        @NonNull UUID id
    ) {
        public CommentIdResponse {
            Objects.requireNonNull(id, "id");
        }
        @NonNull public UUID getId() { return id; }
        public static CommentIdResponse from(CommunityCommentResult.CommentId result) {
            return new CommentIdResponse(
                result.getId()
            );
        }
    }

    public record LikeToggleResponse(
        boolean liked,
        long likeCount
    ) {
        public boolean getLiked() { return liked; }
        public long getLikeCount() { return likeCount; }
        public static LikeToggleResponse from(CommunityPostResult.LikeToggle result) {
            return new LikeToggleResponse(
                result.getLiked(),
                result.getLikeCount()
            );
        }
    }

    public record BoardResponse(
        @NonNull UUID cropId,
        @NonNull String cropName
    ) {
        public BoardResponse {
            Objects.requireNonNull(cropId, "cropId");
            Objects.requireNonNull(cropName, "cropName");
        }
        @NonNull public UUID getCropId() { return cropId; }
        @NonNull public String getCropName() { return cropName; }
        public static BoardResponse from(CommunityPostResult.Board result) {
            return new BoardResponse(
                result.getCropId(),
                result.getCropName()
            );
        }
    }

    public record AuthorResponse(
        @NonNull UUID memberId,
        @Nullable String nickname,
        @Nullable String profileImageUrl
    ) {
        public AuthorResponse {
            Objects.requireNonNull(memberId, "memberId");
        }
        @NonNull public UUID getMemberId() { return memberId; }
        @Nullable public String getNickname() { return nickname; }
        @Nullable public String getProfileImageUrl() { return profileImageUrl; }
        public static AuthorResponse from(CommunityPostResult.AuthorSummary result) {
            return new AuthorResponse(
                result.getMemberId(),
                result.getNickname(),
                result.getProfileImageUrl()
            );
        }
    }

    public record PostPageResponse(
        @NonNull List<PostSummaryResponse> items,
        @Nullable String nextCursor
    ) {
        public PostPageResponse {
            Objects.requireNonNull(items, "items");
        }
        @NonNull public List<PostSummaryResponse> getItems() { return items; }
        @Nullable public String getNextCursor() { return nextCursor; }
        public static PostPageResponse from(CommunityPostResult.Page result) {
            return new PostPageResponse(
                result.getItems().stream().map(PostSummaryResponse::from).collect(Collectors.toCollection(ArrayList::new)),
                result.getNextCursor()
            );
        }
    }

    public record PostSummaryResponse(
        @NonNull UUID id,
        @NonNull UUID cropId,
        @NonNull String cropName,
        @NonNull CommunityPostType postType,
        @NonNull String title,
        @NonNull String bodyPreview,
        @Nullable String thumbnailUrl,
        @NonNull AuthorResponse author,
        long commentCount,
        long likeCount,
        boolean likedByMe,
        @NonNull LocalDateTime createdAt
    ) {
        public PostSummaryResponse {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(cropId, "cropId");
            Objects.requireNonNull(cropName, "cropName");
            Objects.requireNonNull(postType, "postType");
            Objects.requireNonNull(title, "title");
            Objects.requireNonNull(bodyPreview, "bodyPreview");
            Objects.requireNonNull(author, "author");
            Objects.requireNonNull(createdAt, "createdAt");
        }
        @NonNull public UUID getId() { return id; }
        @NonNull public UUID getCropId() { return cropId; }
        @NonNull public String getCropName() { return cropName; }
        @NonNull public CommunityPostType getPostType() { return postType; }
        @NonNull public String getTitle() { return title; }
        @NonNull public String getBodyPreview() { return bodyPreview; }
        @Nullable public String getThumbnailUrl() { return thumbnailUrl; }
        @NonNull public AuthorResponse getAuthor() { return author; }
        public long getCommentCount() { return commentCount; }
        public long getLikeCount() { return likeCount; }
        public boolean getLikedByMe() { return likedByMe; }
        @NonNull public LocalDateTime getCreatedAt() { return createdAt; }
        public static PostSummaryResponse from(CommunityPostResult.PostSummary result) {
            return new PostSummaryResponse(
                result.getId(),
                result.getCropId(),
                result.getCropName(),
                result.getPostType(),
                result.getTitle(),
                result.getBodyPreview(),
                result.getThumbnailUrl(),
                AuthorResponse.from(result.getAuthor()),
                result.getCommentCount(),
                result.getLikeCount(),
                result.getLikedByMe(),
                result.getCreatedAt()
            );
        }
    }

    public record PostDetailResponse(
        @NonNull UUID id,
        @NonNull UUID cropId,
        @NonNull String cropName,
        @NonNull CommunityPostType postType,
        @NonNull String title,
        @NonNull String body,
        @NonNull List<String> imageUrls,
        @Nullable UUID farmingRecordId,
        @NonNull AuthorResponse author,
        long commentCount,
        long likeCount,
        boolean likedByMe,
        @NonNull LocalDateTime createdAt
    ) {
        public PostDetailResponse {
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
        @NonNull public UUID getId() { return id; }
        @NonNull public UUID getCropId() { return cropId; }
        @NonNull public String getCropName() { return cropName; }
        @NonNull public CommunityPostType getPostType() { return postType; }
        @NonNull public String getTitle() { return title; }
        @NonNull public String getBody() { return body; }
        @NonNull public List<String> getImageUrls() { return imageUrls; }
        @Nullable public UUID getFarmingRecordId() { return farmingRecordId; }
        @NonNull public AuthorResponse getAuthor() { return author; }
        public long getCommentCount() { return commentCount; }
        public long getLikeCount() { return likeCount; }
        public boolean getLikedByMe() { return likedByMe; }
        @NonNull public LocalDateTime getCreatedAt() { return createdAt; }
        public static PostDetailResponse from(CommunityPostResult.PostDetail result) {
            return new PostDetailResponse(
                result.getId(),
                result.getCropId(),
                result.getCropName(),
                result.getPostType(),
                result.getTitle(),
                result.getBody(),
                result.getImageUrls(),
                result.getFarmingRecordId(),
                AuthorResponse.from(result.getAuthor()),
                result.getCommentCount(),
                result.getLikeCount(),
                result.getLikedByMe(),
                result.getCreatedAt()
            );
        }
    }

    public record CommentPageResponse(
        @NonNull List<CommentResponse> items,
        @Nullable String nextCursor
    ) {
        public CommentPageResponse {
            Objects.requireNonNull(items, "items");
        }
        @NonNull public List<CommentResponse> getItems() { return items; }
        @Nullable public String getNextCursor() { return nextCursor; }
        public static CommentPageResponse from(CommunityCommentResult.Page result) {
            return new CommentPageResponse(
                result.getItems().stream().map(CommentResponse::from).collect(Collectors.toCollection(ArrayList::new)),
                result.getNextCursor()
            );
        }
    }

    public record CommentResponse(
        @NonNull UUID id,
        @Nullable UUID parentCommentId,
        @NonNull AuthorResponse author,
        @NonNull String body,
        @Nullable String imageUrl,
        boolean deleted,
        @NonNull LocalDateTime createdAt,
        @NonNull List<CommentResponse> replies
    ) {
        public CommentResponse {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(author, "author");
            Objects.requireNonNull(body, "body");
            Objects.requireNonNull(createdAt, "createdAt");
            Objects.requireNonNull(replies, "replies");
        }
        @NonNull public UUID getId() { return id; }
        @Nullable public UUID getParentCommentId() { return parentCommentId; }
        @NonNull public AuthorResponse getAuthor() { return author; }
        @NonNull public String getBody() { return body; }
        @Nullable public String getImageUrl() { return imageUrl; }
        public boolean getDeleted() { return deleted; }
        @NonNull public LocalDateTime getCreatedAt() { return createdAt; }
        @NonNull public List<CommentResponse> getReplies() { return replies; }
        public static CommentResponse from(CommunityCommentResult.Comment result) {
            return new CommentResponse(
                result.getId(),
                result.getParentCommentId(),
                AuthorResponse.from(result.getAuthor()),
                result.getBody(),
                result.getImageUrl(),
                result.getDeleted(),
                result.getCreatedAt(),
                result.getReplies().stream().map(CommentResponse::from).collect(Collectors.toCollection(ArrayList::new))
            );
        }
    }
}
