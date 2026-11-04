package com.chamchamcham.application.community;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;
import java.util.Objects;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public final class CommunityCommentResult {
    private CommunityCommentResult() {}

    public record CommentId(
        @NonNull UUID id
    ) {
        public CommentId {
            Objects.requireNonNull(id, "id");
        }

        @NonNull
        public UUID getId() { return id; }
    }

    public record Page(
        @NonNull List<Comment> items,
        @Nullable String nextCursor
    ) {
        public Page {
            Objects.requireNonNull(items, "items");
        }

        @NonNull
        public List<Comment> getItems() { return items; }
        @Nullable
        public String getNextCursor() { return nextCursor; }
    }

    public record Comment(
        @NonNull UUID id,
        @Nullable UUID parentCommentId,
        @NonNull CommunityPostResult.AuthorSummary author,
        @NonNull String body,
        @Nullable String imageUrl,
        boolean deleted,
        @NonNull LocalDateTime createdAt,
        @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = EmptyRepliesDefault.class)
        @NonNull List<Comment> replies
    ) {
        public Comment {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(author, "author");
            Objects.requireNonNull(body, "body");
            Objects.requireNonNull(createdAt, "createdAt");
            Objects.requireNonNull(replies, "replies");
        }

        @NonNull
        public UUID getId() { return id; }
        @Nullable
        public UUID getParentCommentId() { return parentCommentId; }
        @NonNull
        public CommunityPostResult.AuthorSummary getAuthor() { return author; }
        @NonNull
        public String getBody() { return body; }
        @Nullable
        public String getImageUrl() { return imageUrl; }
        public boolean getDeleted() { return deleted; }
        @NonNull
        public LocalDateTime getCreatedAt() { return createdAt; }
        @NonNull
        public List<Comment> getReplies() { return replies; }
    }
    public static final class EmptyRepliesDefault extends com.fasterxml.jackson.databind.JsonDeserializer<List<Comment>> {
        @Override
        public List<Comment> deserialize(com.fasterxml.jackson.core.JsonParser parser,
            com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            return parser.readValueAs(new com.fasterxml.jackson.core.type.TypeReference<List<Comment>>() {});
        }
        @Override
        public List<Comment> getAbsentValue(com.fasterxml.jackson.databind.DeserializationContext context) { return List.of(); }
    }
}
