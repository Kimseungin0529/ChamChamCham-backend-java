package com.chamchamcham.application.community;

import java.util.UUID;
import java.util.Objects;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public final class CommunityCommentCommand {
    private CommunityCommentCommand() {}

    public record Create(
        @NonNull UUID memberId,
        @NonNull UUID postId,
        @Nullable UUID parentCommentId,
        @NonNull String body,
        @Nullable UUID mediaId
    ) {
        public Create {
            Objects.requireNonNull(memberId, "memberId");
            Objects.requireNonNull(postId, "postId");
            Objects.requireNonNull(body, "body");
        }

        @NonNull
        public UUID getMemberId() { return memberId; }
        @NonNull
        public UUID getPostId() { return postId; }
        @Nullable
        public UUID getParentCommentId() { return parentCommentId; }
        @NonNull
        public String getBody() { return body; }
        @Nullable
        public UUID getMediaId() { return mediaId; }
    }

    public record Delete(
        @NonNull UUID memberId,
        @NonNull UUID commentId
    ) {
        public Delete {
            Objects.requireNonNull(memberId, "memberId");
            Objects.requireNonNull(commentId, "commentId");
        }

        @NonNull
        public UUID getMemberId() { return memberId; }
        @NonNull
        public UUID getCommentId() { return commentId; }
    }
}
