package com.chamchamcham.api.community.dto;

import com.chamchamcham.domain.community.CommunityPostType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import java.util.List;
import java.util.Objects;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public final class CommunityRequests {
    private CommunityRequests() {}

    public record SavePostRequest(
        @NotNull(message = "작물을 선택해주세요")
        @Nullable UUID cropId,
        @NotNull(message = "게시글 유형을 선택해주세요")
        @Nullable CommunityPostType postType,
        @NotBlank(message = "제목을 입력해주세요")
        @Size(max = 50, message = "제목은 50자 이하여야 합니다")
        @NonNull String title,
        @NotBlank(message = "본문을 입력해주세요")
        @NonNull String body,
        @Nullable UUID farmingRecordId,
        @Size(max = 5, message = "사진은 최대 5장까지 첨부할 수 있습니다")
        @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = MediaIdsDefault.class)
        @NonNull List<UUID> mediaIds
    ) {
        public SavePostRequest {
            Objects.requireNonNull(title, "title");
            Objects.requireNonNull(body, "body");
            Objects.requireNonNull(mediaIds, "mediaIds");
        }
        @Nullable public UUID getCropId() { return cropId; }
        @Nullable public CommunityPostType getPostType() { return postType; }
        @NonNull public String getTitle() { return title; }
        @NonNull public String getBody() { return body; }
        @Nullable public UUID getFarmingRecordId() { return farmingRecordId; }
        @NonNull public List<UUID> getMediaIds() { return mediaIds; }
    }

    public record CreateCommentRequest(
        @Nullable UUID parentCommentId,
        @NotBlank(message = "댓글 내용을 입력해주세요")
        @NonNull String body,
        @Nullable UUID mediaId
    ) {
        public CreateCommentRequest {
            Objects.requireNonNull(body, "body");
        }
        @Nullable public UUID getParentCommentId() { return parentCommentId; }
        @NonNull public String getBody() { return body; }
        @Nullable public UUID getMediaId() { return mediaId; }
    }

    public static final class MediaIdsDefault extends com.fasterxml.jackson.databind.JsonDeserializer<List<UUID>> {
        @Override public List<UUID> deserialize(com.fasterxml.jackson.core.JsonParser parser,
            com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            return context.readValue(parser, context.getTypeFactory().constructCollectionType(List.class, UUID.class));
        }
        @Override public List<UUID> getAbsentValue(com.fasterxml.jackson.databind.DeserializationContext context) { return java.util.Collections.emptyList(); }
    }
}
