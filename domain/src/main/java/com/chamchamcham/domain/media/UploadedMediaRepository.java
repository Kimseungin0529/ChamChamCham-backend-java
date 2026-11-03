package com.chamchamcham.domain.media;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.UUID;

public interface UploadedMediaRepository extends JpaRepository<UploadedMedia, UUID> {
    @Query("select m.cloudinaryPublicId from UploadedMedia m where m.owner.id = :memberId")
    @NonNull
    List<String> findCloudinaryPublicIdsByOwnerId(@Param("memberId") @NonNull UUID memberId);
}
