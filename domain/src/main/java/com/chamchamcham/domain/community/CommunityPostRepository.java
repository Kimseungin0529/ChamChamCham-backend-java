package com.chamchamcham.domain.community;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.Nullable;

public interface CommunityPostRepository extends JpaRepository<CommunityPost, UUID> {
    @Nullable
    CommunityPost findByIdAndIsDeletedFalse(UUID id);
}
