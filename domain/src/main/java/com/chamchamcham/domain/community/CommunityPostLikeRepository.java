package com.chamchamcham.domain.community;

import com.chamchamcham.domain.member.Member;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.Nullable;

public interface CommunityPostLikeRepository extends JpaRepository<CommunityPostLike, UUID> {
    @Nullable
    CommunityPostLike findByPostIdAndMemberId(UUID postId, UUID memberId);
    boolean existsByPostIdAndMemberId(UUID postId, UUID memberId);
    long countByPostId(UUID postId);
    void deleteByPostAndMember(CommunityPost post, Member member);
}
