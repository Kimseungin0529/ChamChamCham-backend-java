package com.chamchamcham.domain.community;

import com.chamchamcham.domain.crop.Crop;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.lang.Nullable;

public interface CommunityPostQueryRepository {
    SearchResult search(SearchCondition condition);
    long count(SearchCondition condition);

    /** Distinct crops in an author's non-deleted posts, including crops no longer being farmed. */
    List<Crop> findDistinctCropsByAuthor(UUID authorMemberId);

    record SearchCondition(@Nullable UUID memberId, @Nullable UUID authorMemberId, List<UUID> cropIds, @Nullable CommunityPostType postType,
                           @Nullable String keyword, boolean likedOnly, boolean mineOnly, CommunityPostSort sort,
                           @Nullable Cursor cursor, int size) {
        public SearchCondition {
            Objects.requireNonNull(cropIds, "cropIds");
            Objects.requireNonNull(sort, "sort");
        }
    }

    record Cursor(CommunityPostSort sort, @Nullable Long score, LocalDateTime createdAt, UUID id) {
        public Cursor {
            Objects.requireNonNull(sort, "sort");
            Objects.requireNonNull(createdAt, "createdAt");
            Objects.requireNonNull(id, "id");
        }
    }

    record Row(CommunityPost post, @Nullable String thumbnailUrl, long commentCount, long likeCount,
               boolean likedByMe, @Nullable Long score) {
        public Row { Objects.requireNonNull(post, "post"); }
    }

    record SearchResult(List<Row> rows) {
        public SearchResult { Objects.requireNonNull(rows, "rows"); }
    }
}
