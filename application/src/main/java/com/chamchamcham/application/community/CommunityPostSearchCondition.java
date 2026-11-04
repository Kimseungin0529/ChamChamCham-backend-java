package com.chamchamcham.application.community;

import com.chamchamcham.domain.community.CommunityPostSort;
import com.chamchamcham.domain.community.CommunityPostType;
import java.util.UUID;
import java.util.List;
import java.util.Objects;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public record CommunityPostSearchCondition(
    @Nullable UUID memberId,
    @Nullable UUID authorMemberId,
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = EmptyCropIdsDefault.class)
    @NonNull List<UUID> cropIds,
    @Nullable CommunityPostType postType,
    @Nullable String keyword,
    boolean likedOnly,
    boolean mineOnly,
    @NonNull CommunityPostSort sort,
    @Nullable String cursor,
    int size
) {
    public CommunityPostSearchCondition {
        Objects.requireNonNull(cropIds, "cropIds");
        Objects.requireNonNull(sort, "sort");
    }

    @Nullable
    public UUID getMemberId() { return memberId; }
    @Nullable
    public UUID getAuthorMemberId() { return authorMemberId; }
    @NonNull
    public List<UUID> getCropIds() { return cropIds; }
    @Nullable
    public CommunityPostType getPostType() { return postType; }
    @Nullable
    public String getKeyword() { return keyword; }
    public boolean getLikedOnly() { return likedOnly; }
    public boolean getMineOnly() { return mineOnly; }
    @NonNull
    public CommunityPostSort getSort() { return sort; }
    @Nullable
    public String getCursor() { return cursor; }
    public int getSize() { return size; }
    public static final class EmptyCropIdsDefault extends com.fasterxml.jackson.databind.JsonDeserializer<List<UUID>> {
        @Override
        public List<UUID> deserialize(com.fasterxml.jackson.core.JsonParser parser,
            com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            return parser.readValueAs(new com.fasterxml.jackson.core.type.TypeReference<List<UUID>>() {});
        }
        @Override
        public List<UUID> getAbsentValue(com.fasterxml.jackson.databind.DeserializationContext context) { return List.of(); }
    }
}
