package com.chamchamcham.domain.crop;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface CropRepository extends JpaRepository<Crop, UUID> {
    @NonNull
    List<Crop> findAllByOrderByNameAscExternalNoAsc();

    @NonNull
    List<Crop> findAllByUsePartCategoryOrderByNameAscExternalNoAsc(@Param("usePartCategory") @NonNull CropUsePartCategory usePartCategory);

    @NonNull
    List<Crop> findByExternalNoIn(@Param("externalNos") @NonNull Collection<Integer> externalNos);

    @NonNull
    List<Crop> findTop9ByNameContainingIgnoreCaseOrderByNameAsc(@Param("keyword") @NonNull String keyword);
}
