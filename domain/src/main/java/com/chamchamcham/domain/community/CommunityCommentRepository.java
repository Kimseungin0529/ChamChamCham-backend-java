package com.chamchamcham.domain.community;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommunityCommentRepository extends JpaRepository<CommunityComment, UUID> {
    @Query(
        """
        select c
        from CommunityComment c
        join fetch c.author a
        left join fetch a.profileMedia
        left join fetch c.media
        where c.post.id = :postId
          and c.parentComment is null
        order by c.createdAt desc, c.id desc
        """
    )
    List<CommunityComment> findRootFirstPage(@Param("postId") UUID postId, Pageable pageable);

    @Query(
        """
        select c
        from CommunityComment c
        join fetch c.author a
        left join fetch a.profileMedia
        left join fetch c.media
        where c.post.id = :postId
          and c.parentComment is null
          and (
            c.createdAt < :cursorCreatedAt
            or (c.createdAt = :cursorCreatedAt and c.id < :cursorId)
          )
        order by c.createdAt desc, c.id desc
        """
    )
    List<CommunityComment> findRootPageAfter(@Param("postId") UUID postId, @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt, @Param("cursorId") UUID cursorId, Pageable pageable);

    @Query(
        """
        select c
        from CommunityComment c
        join fetch c.parentComment p
        join fetch c.author a
        left join fetch a.profileMedia
        left join fetch c.media
        where p.id in :parentIds
        order by c.createdAt asc, c.id asc
        """
    )
    List<CommunityComment> findRepliesByParentIds(@Param("parentIds") Collection<UUID> parentIds);

    long countByPostIdAndIsDeletedFalse(UUID postId);
}
