package com.chamchamcham.domain.community;

import com.chamchamcham.domain.crop.Crop;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class CommunityPostQueryRepositoryImpl implements CommunityPostQueryRepository {
    private final EntityManager entityManager;

    public CommunityPostQueryRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public SearchResult search(SearchCondition condition) {
        List<SelectedPost> selectedPosts = findPosts(condition);
        if (selectedPosts.isEmpty()) return new SearchResult(List.of());
        List<UUID> postIds = selectedPosts.stream().map(row -> persistedId(row.post())).toList();
        Map<UUID, String> thumbnails = findThumbnails(postIds);
        Set<UUID> likedPostIds = findLikedPostIds(postIds, condition.memberId());
        return new SearchResult(selectedPosts.stream().map(row -> {
            UUID postId = persistedId(row.post());
            return new Row(row.post(), thumbnails.get(postId), row.commentCount(), row.likeCount(),
                    likedPostIds.contains(postId), row.score());
        }).toList());
    }

    @Override
    public long count(SearchCondition condition) {
        Filter filter = buildFilterPredicates(condition);
        var query = entityManager.createQuery("select count(p) from CommunityPost p where "
                + String.join(" and ", filter.where()), Long.class);
        filter.params().forEach(query::setParameter);
        return query.getSingleResult();
    }

    @Override
    public List<Crop> findDistinctCropsByAuthor(UUID authorMemberId) {
        return entityManager.createQuery("""
                select distinct p.crop
                from CommunityPost p
                where p.isDeleted = false and p.author.id = :authorMemberId
                """, Crop.class).setParameter("authorMemberId", authorMemberId).getResultList();
    }

    private Filter buildFilterPredicates(SearchCondition condition) {
        List<String> where = new ArrayList<>(List.of("p.isDeleted = false"));
        Map<String, Object> params = new LinkedHashMap<>();
        if (condition.authorMemberId() != null) {
            where.add("p.author.id = :authorMemberId");
            params.put("authorMemberId", condition.authorMemberId());
        }
        if (!condition.cropIds().isEmpty()) {
            where.add("p.crop.id in :cropIds");
            params.put("cropIds", condition.cropIds());
        }
        if (condition.postType() != null) {
            where.add("p.postType = :postType");
            params.put("postType", condition.postType());
        }
        if (condition.keyword() != null) {
            String keyword = trim(condition.keyword()).toLowerCase(Locale.ROOT);
            if (!keyword.isEmpty()) {
                where.add("(lower(p.title) like :keyword or lower(p.body) like :keyword)");
                params.put("keyword", "%" + keyword + "%");
            }
        }
        if (condition.mineOnly()) {
            where.add("p.author.id = :memberId");
            if (condition.memberId() == null) throw new IllegalArgumentException("Member id is required for mine-only search");
            params.put("memberId", condition.memberId());
        }
        if (condition.likedOnly()) {
            where.add("exists (select 1 from CommunityPostLike l where l.post = p and l.member.id = :memberId)");
            if (condition.memberId() == null) throw new IllegalArgumentException("Member id is required for liked-only search");
            params.put("memberId", condition.memberId());
        }
        return new Filter(where, params);
    }

    private List<SelectedPost> findPosts(SearchCondition condition) {
        Filter filter = buildFilterPredicates(condition);
        List<String> where = filter.where();
        Map<String, Object> params = filter.params();
        String commentCount = "(select count(c) from CommunityComment c where c.post = p and c.isDeleted = false)";
        String likeCount = "(select count(l) from CommunityPostLike l where l.post = p)";
        String score = switch (condition.sort()) {
            case LATEST -> "0";
            case LIKE -> likeCount;
            case COMMENT -> commentCount;
            case POPULAR -> "(" + likeCount + " + " + commentCount + ")";
        };
        String orderBy = condition.sort() == CommunityPostSort.LATEST ? "p.createdAt desc, p.id desc"
                : score + " desc, p.createdAt desc, p.id desc";
        Cursor cursor = condition.cursor();
        if (cursor != null) {
            if (condition.sort() == CommunityPostSort.LATEST) {
                where.add("(p.createdAt < :cursorCreatedAt or (p.createdAt = :cursorCreatedAt and p.id < :cursorId))");
            } else {
                where.add("(" + score + " < :cursorScore or (" + score + " = :cursorScore and p.createdAt < :cursorCreatedAt) or (" + score + " = :cursorScore and p.createdAt = :cursorCreatedAt and p.id < :cursorId))");
                if (cursor.score() == null) throw new IllegalArgumentException("Cursor score is required for " + condition.sort() + " sort");
                params.put("cursorScore", cursor.score());
            }
            params.put("cursorCreatedAt", cursor.createdAt());
            params.put("cursorId", cursor.id());
        }
        String scoreSelect = condition.sort() == CommunityPostSort.LATEST ? "null" : score;
        var query = entityManager.createQuery("select p, " + commentCount + ", " + likeCount + ", " + scoreSelect
                + " from CommunityPost p where " + String.join(" and ", where) + " order by " + orderBy, Object[].class);
        params.forEach(query::setParameter);
        query.setMaxResults(condition.size());
        return query.getResultList().stream().map(row -> new SelectedPost((CommunityPost) row[0],
                (Long) row[1], (Long) row[2], (Long) row[3])).toList();
    }

    private Map<UUID, String> findThumbnails(List<UUID> postIds) {
        List<CommunityPostMedia> media = entityManager.createQuery("""
                select m
                from CommunityPostMedia m
                where m.post.id in :postIds
                order by m.displayOrder asc
                """, CommunityPostMedia.class).setParameter("postIds", postIds).getResultList();
        Map<UUID, String> thumbnails = new HashMap<>();
        for (CommunityPostMedia row : media) {
            thumbnails.putIfAbsent(persistedId(row.getPost()), row.getUploadedMedia().getFileUrl());
        }
        return thumbnails;
    }

    private Set<UUID> findLikedPostIds(List<UUID> postIds, UUID memberId) {
        if (memberId == null) return Set.of();
        return new HashSet<>(entityManager.createQuery("""
                select l.post.id
                from CommunityPostLike l
                where l.post.id in :postIds and l.member.id = :memberId
                """, UUID.class).setParameter("postIds", postIds).setParameter("memberId", memberId).getResultList());
    }

    private UUID persistedId(CommunityPost post) {
        if (post.getId() == null) throw new IllegalArgumentException("Persisted post id is required");
        return post.getId();
    }

    private String trim(String value) {
        int start = 0;
        int end = value.length();
        while (start < end && whitespace(value.charAt(start))) start++;
        while (end > start && whitespace(value.charAt(end - 1))) end--;
        return value.substring(start, end);
    }

    private boolean whitespace(char value) {
        return Character.isWhitespace(value) || Character.isSpaceChar(value);
    }

    private record Filter(List<String> where, Map<String, Object> params) {}
    private record SelectedPost(CommunityPost post, long commentCount, long likeCount, Long score) {}
}
