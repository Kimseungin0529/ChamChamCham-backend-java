package com.chamchamcham.domain.community;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityPostMediaRepository extends JpaRepository<CommunityPostMedia, UUID> {
    List<CommunityPostMedia> findByPostIdOrderByDisplayOrderAsc(UUID postId);
    List<CommunityPostMedia> findByUploadedMediaIdIn(Collection<UUID> mediaIds);
    void deleteByPost(CommunityPost post);
}
